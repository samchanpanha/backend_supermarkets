We'll now implement **Security Hardening** (Phase 16) across all microservices to prepare the system for production deployment. This includes:

- **HTTPS/TLS** for all services (including internal communication)
- **Rate limiting** to prevent abuse
- **Secrets management** using HashiCorp Vault
- **Input validation** and sanitization
- **Security headers** and CORS hardening
- **Audit logging** for sensitive operations

---

## 39. Security Hardening

### 39.1 Enable HTTPS/TLS for All Services

#### 39.1.1 Generate Certificates

For development, we can use self‑signed certificates. For production, use Let's Encrypt or a corporate CA. We'll create a keystore for each service (or share one with multiple aliases).

Example using `keytool`:

```bash
keytool -genkeypair -alias supermarket -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore supermarket.p12 -validity 3650 \
  -storepass changeit -keypass changeit \
  -dname "CN=localhost, OU=Supermarket, O=Example, L=City, ST=State, C=US"
```

Copy the generated `supermarket.p12` to each service's `src/main/resources/keystore/`.

#### 39.1.2 Configure Each Service to Enable HTTPS

In each service's `application.yml` (or via config server), add:

```yaml
server:
  port: 8443
  ssl:
    key-store: classpath:keystore/supermarket.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: supermarket
```

Also, we need to redirect HTTP to HTTPS. Add a separate configuration class:

```java
@Configuration
public class HttpsRedirectConfig {

    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        tomcat.addAdditionalTomcatConnectors(createHttpConnector());
        return tomcat;
    }

    private Connector createHttpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        return connector;
    }
}
```

This ensures any request to HTTP (port 8080) is redirected to HTTPS (8443).

#### 39.1.3 Update API Gateway

The gateway should also run HTTPS and forward requests to internal services. Internal services can remain on HTTP (since they're in a trusted network) or also use HTTPS with client certificate validation. For simplicity, we'll keep internal HTTP and only encrypt external communication at the gateway.

Update gateway configuration:

```yaml
server:
  port: 8443
  ssl:
    key-store: classpath:keystore/supermarket.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: supermarket
```

The routes to internal services remain `lb://SERVICE-NAME` (HTTP) because Eureka advertises the HTTP port.

### 39.2 Rate Limiting

Spring Cloud Gateway supports rate limiting via Redis. We'll add a rate limiter based on tenant ID or client IP.

#### 39.2.1 Add Dependencies

In `api-gateway/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

#### 39.2.2 Configure Redis

Add Redis to `docker-compose.yml`:

```yaml
redis:
  image: redis:7-alpine
  container_name: redis
  ports:
    - "6379:6379"
  networks:
    - supermarket-network
```

#### 39.2.3 Configure Rate Limiter in Gateway

In `application.yml` of gateway:

```yaml
spring:
  redis:
    host: redis
    port: 6379
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: lb://PRODUCT-SERVICE
          predicates:
            - Path=/api/products/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                redis-rate-limiter.requestedTokens: 1
                key-resolver: "#{@tenantKeyResolver}"
            - AuthenticationFilter
```

Create a key resolver bean:

```java
@Bean
KeyResolver tenantKeyResolver() {
    return exchange -> {
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
        if (tenantId == null) {
            tenantId = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return Mono.just(tenantId);
    };
}
```

This limits each tenant to 10 requests per second with a burst of 20. Adjust values per service.

### 39.3 Secrets Management with HashiCorp Vault

We'll integrate Spring Cloud Vault to fetch sensitive configuration (database passwords, API keys) from Vault instead of storing them in config files.

#### 39.3.1 Deploy Vault

Add to `docker-compose.yml`:

```yaml
vault:
  image: vault:1.13
  container_name: vault
  ports:
    - "8200:8200"
  environment:
    - VAULT_DEV_ROOT_TOKEN_ID=root
    - VAULT_DEV_LISTEN_ADDRESS=0.0.0.0:8200
  cap_add:
    - IPC_LOCK
  networks:
    - supermarket-network
```

For production, Vault should be configured with proper storage backend and unseal keys.

#### 39.3.2 Add Vault Dependency

In each service's `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-vault-config</artifactId>
</dependency>
```

#### 39.3.3 Configure Bootstrap to Use Vault

In `bootstrap.yml` of each service:

```yaml
spring:
  cloud:
    vault:
      host: vault
      port: 8200
      scheme: http
      authentication: TOKEN
      token: root   # In production, use a proper token or Kubernetes auth
      kv:
        enabled: true
        backend: secret
        default-context: supermarket
```

#### 39.3.4 Store Secrets in Vault

Using Vault CLI or API:

```bash
vault kv put secret/supermarket/auth-service \
  spring.datasource.password=postgres \
  jwt.secret=mySuperSecretKeyForJWT
```

Then in the service's configuration, reference these values normally. Vault will inject them.

### 39.4 Input Validation and Sanitization

We've already used `@Valid` annotations in controllers. Ensure all DTOs have proper validation constraints (`@NotBlank`, `@Size`, `@Email`, etc.). Also add a global exception handler to return clean error messages (already done in common-library).

Add OWASP ESAPI or similar for HTML sanitization if accepting rich text.

### 39.5 Security Headers and CORS

In the API Gateway, add a filter to set security headers:

```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("product-service", r -> r
            .path("/api/products/**")
            .filters(f -> f
                .addResponseHeader("X-Content-Type-Options", "nosniff")
                .addResponseHeader("X-Frame-Options", "DENY")
                .addResponseHeader("X-XSS-Protection", "1; mode=block")
                .addResponseHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
                .addResponseHeader("Pragma", "no-cache")
                .addResponseHeader("Expires", "0")
                .filter(rateLimiter)
                .filter(authenticationFilter)
            )
            .uri("lb://PRODUCT-SERVICE"))
        .build();
}
```

CORS configuration can be added globally in the gateway:

```java
@Bean
public CorsConfiguration corsConfiguration() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.asList("https://your-frontend-domain.com"));
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(Arrays.asList("*"));
    config.setAllowCredentials(true);
    return config;
}
```

### 39.6 Audit Logging

For sensitive operations (create user, delete product, process payment), we should log who did what, when, and with which tenant. Use a combination of Spring Security (to get current user) and AOP.

Create an annotation `@Auditable` and an aspect:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();
}

@Aspect
@Component
public class AuditAspect {
    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = auth != null ? auth.getName() : "anonymous";
        String tenantId = TenantContext.getCurrentTenant();
        String action = auditable.action();
        String method = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("AUDIT: user={}, tenant={}, action={}, method={}, args={}",
                user, tenantId, action, method, Arrays.toString(args));

        return joinPoint.proceed();
    }
}
```

Then annotate sensitive methods:

```java
@Auditable(action = "DELETE_PRODUCT")
public void deleteProduct(Long id) { ... }
```

Logs can be sent to ELK for analysis.

### 39.7 Update Docker Compose

Add Redis and Vault to `docker-compose.yml` as shown above. Also ensure each service's `bootstrap.yml` references Vault.

### 39.8 Testing Security Hardening

1. Start the stack: `docker-compose up --build`
2. Access services via HTTPS: https://localhost:8443/api/products (you'll get a browser warning for self‑signed certs).
3. Test rate limiting by sending many requests in a short time (should see 429 Too Many Requests).
4. Verify secrets are injected correctly (e.g., check logs for database connection using Vault password).
5. Check response headers using curl or browser dev tools.
6. Trigger audit logs and verify they appear in ELK.

---

## 40. Next Steps

With security hardening implemented, the system is much closer to production. The remaining phases:

- **Phase 17: Testing** – integration tests, performance tests, chaos engineering.
- **Phase 18-19: CI/CD and Deployment** – Jenkins pipelines, Kubernetes manifests, Helm charts.
- **Phase 20: Production Readiness** – backup, disaster recovery, runbooks.

We can proceed with any of these. If you'd like to continue, please specify.