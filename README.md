# Supermarket POS SaaS - Microservices Architecture

A comprehensive multi-tenant Supermarket Point of Sale (POS) system built with Spring Boot microservices architecture.

## 🏗️ Architecture Overview

### Technology Stack
- **Framework**: Spring Boot 3.2 / Java 17
- **Cloud**: Spring Cloud 2023.0.0 (Eureka, Config Server, Gateway)
- **Security**: Keycloak OAuth2/OIDC Authentication
- **Database**: PostgreSQL (multi-tenant per-service databases)
- **Messaging**: Apache Kafka
- **Container**: Docker & Docker Compose
- **API Documentation**: OpenAPI/Swagger

### Microservices

| Service | Port | Description |
|---------|------|-------------|
| [api-gateway](./api-gateway) | 8080 | API Gateway with Keycloak OAuth2 |
| [service-discovery](./service-discovery) | 8761 | Eureka Service Discovery |
| [config-service](./config-service) | 8888 | Centralized Configuration |
| [auth-service](./auth-service) | 8081 | Authentication Service |
| [product-service](./product-service) | 8082 | Product Management |
| [inventory-service](./inventory-service) | 8083 | Inventory Management |
| [order-service](./order-service) | 8084 | Order Processing |
| [payment-service](./payment-service) | 8085 | Payment Processing |
| [pos-service](./pos-service) | 8086 | POS Terminal Management |
| [tenant-service](./tenant-service) | 8087 | Multi-tenant Management |
| [accounting-service](./accounting-service) | 8088 | Accounting & GL |

### Infrastructure Services

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL | 5432 | Database |
| Kafka | 9092 | Message Broker |
| Zookeeper | 2181 | Kafka Coordinator |
| Keycloak | 8180 | OAuth2 Identity Provider |

## 🔐 Keycloak OAuth2 Integration

### Overview
All microservices are secured with Keycloak OAuth2/OIDC authentication. Each service is configured as a resource server that validates JWT tokens issued by Keycloak.

### Keycloak Configuration

1. **Start Keycloak** (already included in docker-compose.yml):
   ```bash
   docker-compose up -d keycloak
   ```

2. **Access Keycloak Admin Console**:
   - URL: http://localhost:8180
   - Username: `admin`
   - Password: `admin`

3. **Import Realm Configuration**:
   - Go to "Add realm" → "Import"
   - Select `keycloak/supermarket-realm.json`
   - Click "Create"

### Pre-configured Clients

| Client ID | Type | Purpose |
|-----------|------|---------|
| supermarket-gateway | Confidential | API Gateway |
| supermarket-auth | Confidential | Auth Service |
| supermarket-product | Bearer-only | Product Service |
| supermarket-inventory | Bearer-only | Inventory Service |
| supermarket-order | Bearer-only | Order Service |
| supermarket-payment | Bearer-only | Payment Service |
| supermarket-pos | Bearer-only | POS Service |
| supermarket-tenant | Bearer-only | Tenant Service |
| supermarket-accounting | Bearer-only | Accounting Service |

### Pre-configured Users

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | admin |
| manager | manager123 | manager |
| cashier | cashier123 | cashier |

### Service Configuration

Each service has the following Keycloak configuration in `application.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/supermarket

keycloak:
  enabled: true
  issuer-uri: http://localhost:8180/realms/supermarket
  realm: supermarket
  client-id: <service-name>
```

### Security Configuration

Each service implements JWT token validation with role-based access control:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class KeycloakSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/health").permitAll()
                .requestMatchers("/api/v1/<service>/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        
        return http.build();
    }
}
```

### API Authentication Flow

```
┌─────────┐     JWT Token     ┌────────────┐     Validate     ┌─────────┐
│  Client │ ───────────────► │ API Gateway│ ───────────────► │Keycloak│
└─────────┘                   └────────────┘                  └────────┘
                                  │
                                  ▼
                           ┌────────────┐
                           │ Microservice│
                           └────────────┘
```

### Making Authenticated Requests

```bash
# Get access token
curl -X POST http://localhost:8180/realms/supermarket/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=supermarket-gateway" \
  -d "username=admin" \
  -d "password=admin123"

# Use token in API request
curl -X GET http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer <access_token>" \
  -H "X-Tenant-ID: tenant-1"
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### Running the Application

1. **Start Infrastructure Services**:
   ```bash
   docker-compose up -d postgres kafka zookeeper keycloak
   ```

2. **Start Service Discovery**:
   ```bash
   cd service-discovery
   mvn spring-boot:run
   ```

3. **Start Config Service**:
   ```bash
   cd config-service
   mvn spring-boot:run
   ```

4. **Start Microservices** (in any order):
   ```bash
   cd auth-service && mvn spring-boot:run
   cd product-service && mvn spring-boot:run
   # ... and so on
   ```

5. **Start API Gateway**:
   ```bash
   cd api-gateway
   mvn spring-boot:run
   ```

### Or Use Docker Compose

```bash
# Build all services
mvn clean package -DskipTests

# Start all services
docker-compose up -d
```

## 📡 API Endpoints

### Gateway Routes

| Method | Path | Service | Description |
|--------|------|---------|-------------|
| POST | /api/auth/login | auth-service | User login |
| GET | /api/v1/products | product-service | List products |
| GET | /api/v1/inventory | inventory-service | Get inventory |
| POST | /api/v1/orders | order-service | Create order |
| POST | /api/v1/payments | payment-service | Process payment |
| GET | /api/v1/pos/terminals | pos-service | List terminals |
| GET | /api/v1/tenants | tenant-service | List tenants |
| GET | /api/v1/accounting/gl-accounts | accounting-service | List GL accounts |

### Multi-tenancy

All API requests must include the `X-Tenant-ID` header:
```bash
curl -X GET http://localhost:8080/api/v1/products \
  -H "X-Tenant-ID: tenant-1" \
  -H "Authorization: Bearer <token>"
```

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -DskipTests=false
```

## 📁 Project Structure

```
supermarket/
├── api-gateway/              # API Gateway with Keycloak OAuth2
├── service-discovery/       # Eureka Server
├── config-service/          # Config Server
├── auth-service/            # Authentication Service
├── product-service/         # Product Management
├── inventory-service/       # Inventory Management
├── order-service/           # Order Management
├── payment-service/         # Payment Processing
├── pos-service/             # POS Terminal Management
├── tenant-service/          # Multi-tenant Management
├── accounting-service/      # Accounting & GL
├── common-library/           # Shared Code
├── docker-compose.yml        # Docker Orchestration
├── keycloak/                 # Keycloak Configuration
└── README.md                # This File
```

## 🔧 Configuration

### Database Configuration

Each service has its own PostgreSQL database:
- `auth_db` - Authentication data
- `product_db` - Products and categories
- `inventory_db` - Inventory levels
- `order_db` - Orders and transactions
- `payment_db` - Payment records
- `pos_db` - POS terminal data
- `tenant_db` - Tenant configuration
- `accounting_db` - GL accounts and journals

### Kafka Topics

- `order-created` - New order events
- `payment-completed` - Payment success events
- `inventory-updated` - Stock changes
- `product-created` - New product events

## 📝 API Documentation

- API Gateway: http://localhost:8080/actuator
- Eureka Dashboard: http://localhost:8761
- Keycloak Admin: http://localhost:8180

## 🔒 Security Features

1. **OAuth2/OIDC**: All services use Keycloak for authentication
2. **JWT Tokens**: Stateless authentication with JWT
3. **Role-Based Access Control**: ADMIN, MANAGER, CASHIER roles
4. **Multi-tenancy**: Tenant isolation via X-Tenant-ID header
5. **API Gateway**: Centralized security and rate limiting

## 📄 License

This project is proprietary software for Supermarket POS SaaS.
