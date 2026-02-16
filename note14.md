We'll now add **Monitoring** using **Prometheus** and **Grafana**. This will collect metrics from all microservices, Kafka, PostgreSQL, and the host, providing dashboards for system health and business KPIs.

---

## 37. Monitoring with Prometheus and Grafana

### 37.1 Architecture

```
┌─────────────────┐     ┌──────────────┐     ┌──────────────┐
│  Microservice   │────▶│  Prometheus  │────▶│   Grafana    │
│  (Micrometer)   │     │  (scraping)  │     │ (dashboards) │
└─────────────────┘     └──────────────┘     └──────────────┘
         ▲                      ▲
         │                      │
         └────── Kafka ─────────┘
         (via JMX exporter)
```

- Each Spring Boot service exposes metrics at `/actuator/prometheus` (Micrometer + Prometheus registry).
- Prometheus scrapes these endpoints periodically.
- Grafana queries Prometheus and visualizes metrics.

### 37.2 Update Each Microservice

Add the Prometheus registry dependency to every service's `pom.xml`:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Ensure each service has the following in its configuration (already in the shared `application.yml` from config server, or add to each service's bootstrap):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

This enables the `/actuator/prometheus` endpoint.

#### 37.2.1 Add Custom Business Metrics (Example)

In the `OrderService`, we can add a counter for orders created:

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {
    private final Counter ordersCreatedCounter;

    public OrderMetrics(MeterRegistry registry) {
        this.ordersCreatedCounter = Counter.builder("orders.created.total")
                .description("Total number of orders created")
                .register(registry);
    }

    public void incrementOrderCreated() {
        ordersCreatedCounter.increment();
    }
}
```

Then in `OrderService.createOrder()`, call `orderMetrics.incrementOrderCreated()`.

Similarly, add counters for payments, refunds, etc.

### 37.3 Docker Compose Additions

Add Prometheus and Grafana services to `docker-compose.yml`:

```yaml
prometheus:
  image: prom/prometheus:v2.48.0
  container_name: prometheus
  ports:
    - "9090:9090"
  volumes:
    - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    - prometheus-data:/prometheus
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.path=/prometheus'
  networks:
    - supermarket-network

grafana:
  image: grafana/grafana:10.2.0
  container_name: grafana
  ports:
    - "3000:3000"
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
    - GF_INSTALL_PLUGINS=grafana-piechart-panel
  volumes:
    - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources
    - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
    - grafana-data:/var/lib/grafana
  depends_on:
    - prometheus
  networks:
    - supermarket-network
```

Add volumes at the bottom:

```yaml
volumes:
  prometheus-data:
  grafana-data:
```

### 37.4 Prometheus Configuration

Create `monitoring/prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Discover services via Docker labels (if using Docker Swarm, but for compose we can use static targets)
  # Since we're using docker-compose with fixed service names, we can use DNS-based discovery.
  - job_name: 'spring-boot-apps'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
        - 'auth-service:8081'
        - 'product-service:8082'
        - 'inventory-service:8083'
        - 'order-service:8084'
        - 'pos-service:8085'
        - 'payment-service:8086'
        - 'ecommerce-service:8087'
        - 'loyalty-service:8088'
        - 'tenant-service:8089'
        - 'customer-service:8090'
        - 'api-gateway:8080'
    relabel_configs:
      - source_labels: [__address__]
        regex: '([^:]+):.*'
        target_label: instance
        replacement: '${1}'

  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']  # we'll add a postgres exporter

  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']      # optional
```

### 37.5 PostgreSQL Exporter (Optional)

Add a PostgreSQL exporter to collect database metrics:

```yaml
postgres-exporter:
  image: prometheuscommunity/postgres-exporter:latest
  container_name: postgres-exporter
  environment:
    DATA_SOURCE_NAME: "postgresql://postgres:postgres@postgres:5432/supermarket?sslmode=disable"
  ports:
    - "9187:9187"
  networks:
    - supermarket-network
```

### 37.6 Grafana Provisioning

Create datasource configuration: `monitoring/grafana/datasources/datasource.yml`:

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

Create dashboard provisioning: `monitoring/grafana/dashboards/dashboards.yml`:

```yaml
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    editable: true
    options:
      path: /etc/grafana/provisioning/dashboards
```

Place a sample dashboard JSON file in `monitoring/grafana/dashboards/` (e.g., `spring-boot-dashboard.json`). We can download a standard Spring Boot dashboard from Grafana.com or create a simple one.

For simplicity, we can create a basic dashboard manually later. Provisioning is optional; we can also set up dashboards via UI.

### 37.7 Testing

1. Create directories:
   ```
   mkdir -p monitoring/prometheus monitoring/grafana/{datasources,dashboards}
   ```

2. Place configuration files as above.

3. Rebuild services (to include micrometer-prometheus) and restart:

   ```bash
   mvn clean package
   docker-compose up --build
   ```

4. Access Prometheus at http://localhost:9090 – check targets are up.

5. Access Grafana at http://localhost:3000 (admin/admin). Add Prometheus datasource (if not provisioned) and explore metrics like `http_server_requests_seconds_count`, `jvm_memory_used_bytes`, etc.

6. Generate some load by creating orders, payments, etc., and watch metrics change.

### 37.8 Custom Dashboards

We can create dashboards for:
- **JVM metrics** (heap, GC, threads) per service.
- **HTTP request rates** and latencies.
- **Business metrics** (orders per minute, revenue).
- **Database connection pool**.
- **Kafka consumer lag**.

Grafana community has many prebuilt dashboards; we can import them.

---

## 38. Next Steps

With monitoring in place, we have a production‑ready observability stack:

- Logging: ELK (Elasticsearch, Logstash, Kibana)
- Metrics: Prometheus + Grafana
- Tracing: Zipkin (already integrated)

The remaining phases from the original plan:

- **Phase 16: Security Hardening** – HTTPS, rate limiting, secrets management.
- **Phase 17: Testing** – integration, performance, chaos testing.
- **Phase 18-19: CI/CD and Deployment** – pipelines, Kubernetes.
- **Phase 20: Production Readiness** – backup, disaster recovery, runbooks.

We can pick any of these. If you'd like to proceed, let me know which one.