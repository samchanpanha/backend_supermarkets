We'll now implement **Centralized Logging** using the **ELK Stack** (Elasticsearch, Logstash, Kibana). This will aggregate logs from all microservices, provide search capabilities, and enable visualization of log data. We'll also include **Filebeat** as a lightweight log shipper to forward logs from containers to Logstash.

---

## 35. Centralized Logging with ELK

### 35.1 Architecture

```
┌─────────────────┐      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Microservice  │─────▶│   Filebeat   │─────▶│   Logstash   │─────▶│ Elasticsearch │
│   (logs to      │      │ (per node)   │      │              │      │              │
│    stdout/stderr│      └──────────────┘      └──────────────┘      └──────┬───────┘
│    in JSON)     │                                                     │
└─────────────────┘                                                     ▼
                                                                 ┌──────────────┐
                                                                 │   Kibana     │
                                                                 │  (Dashboards)│
                                                                 └──────────────┘
```

- **Filebeat** reads container logs (stdout/stderr) and forwards them to Logstash.
- **Logstash** parses, filters, and enriches log data before sending to Elasticsearch.
- **Elasticsearch** stores and indexes logs.
- **Kibana** provides a UI for searching, analyzing, and visualizing logs.

### 35.2 Configuration Steps

#### 35.2.1 Update Microservices to Output Structured JSON Logs

Modify each service's `logback-spring.xml` to output logs in JSON format. This makes parsing by Logstash easier.

Create `src/main/resources/logback-spring.xml` in each service:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <!-- JSON appender for structured logging -->
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.classic.encoder.JsonEncoder"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON_CONSOLE"/>
    </root>
</configuration>
```

This replaces the default plain text console logging with JSON. Each log line becomes a JSON object with fields like timestamp, level, thread, logger, message, etc.

#### 35.2.2 Docker Compose ELK Stack

Add the following services to `docker-compose.yml`:

```yaml
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
  container_name: elasticsearch
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
    - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
  ports:
    - "9200:9200"
  volumes:
    - elasticsearch-data:/usr/share/elasticsearch/data
  networks:
    - supermarket-network

logstash:
  image: docker.elastic.co/logstash/logstash:8.11.0
  container_name: logstash
  ports:
    - "5000:5000/tcp"   # For Filebeat input
    - "5000:5000/udp"
    - "9600:9600"       # Monitoring API
  volumes:
    - ./elk/logstash/logstash.conf:/usr/share/logstash/pipeline/logstash.conf
  depends_on:
    - elasticsearch
  networks:
    - supermarket-network

kibana:
  image: docker.elastic.co/kibana/kibana:8.11.0
  container_name: kibana
  ports:
    - "5601:5601"
  environment:
    - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
  depends_on:
    - elasticsearch
  networks:
    - supermarket-network

filebeat:
  image: docker.elastic.co/beats/filebeat:8.11.0
  container_name: filebeat
  user: root
  volumes:
    - ./elk/filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
    - /var/lib/docker/containers:/var/lib/docker/containers:ro
    - /var/run/docker.sock:/var/run/docker.sock:ro
  depends_on:
    - logstash
  networks:
    - supermarket-network
```

Add a volume for Elasticsearch data at the bottom:

```yaml
volumes:
  elasticsearch-data:
```

#### 35.2.3 Logstash Configuration

Create `elk/logstash/logstash.conf`:

```
input {
  beats {
    port => 5000
  }
}

filter {
  # If logs are already JSON from the microservices, parse them
  json {
    source => "message"
    remove_field => ["message"]
    target => "log"
  }

  # Add container metadata (from Filebeat)
  mutate {
    add_field => {
      "service_name" => "%{[log][logger]}"
      "environment" => "production"
    }
  }

  # Extract tenant ID if present in log message (via MDC)
  if [log][mdc] {
    mutate {
      add_field => {
        "tenant_id" => "%{[log][mdc][tenantId]}"
      }
    }
  }

  # Timestamp conversion
  date {
    match => ["[log][timestamp]", "ISO8601"]
    target => "@timestamp"
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "supermarket-logs-%{+YYYY.MM.dd}"
  }
  stdout { codec => rubydebug }  # for debugging
}
```

#### 35.2.4 Filebeat Configuration

Create `elk/filebeat/filebeat.yml`:

```yaml
filebeat.inputs:
- type: container
  paths:
    - '/var/lib/docker/containers/*/*.log'
  processors:
    - add_docker_metadata:
        host: "unix:///var/run/docker.sock"

# Optional: decode JSON logs
processors:
  - decode_json_fields:
      fields: ["message"]
      target: "json"
      overwrite_keys: true

output.logstash:
  hosts: ["logstash:5000"]

logging.json: true
logging.metrics.enabled: false
```

Filebeat reads all container logs, adds Docker metadata (container name, image, etc.), and forwards to Logstash.

#### 35.2.5 Update Each Service to Include MDC for Tenant ID

To have tenant ID in logs, we need to add it to the MDC (Mapped Diagnostic Context) in each service's filter or interceptor.

In `TenantFilter.java` of each service, modify to set tenant ID in MDC:

```java
import org.slf4j.MDC;

@Component
public class TenantFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String tenantId = request.getHeader("X-Tenant-ID");
        if (tenantId == null) tenantId = "default";
        TenantContext.setCurrentTenant(tenantId);
        MDC.put("tenantId", tenantId);   // <-- add this
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");      // <-- clear
        }
    }
}
```

Also add MDC support in each service's `logback-spring.xml` – the JSON encoder automatically includes MDC properties if they exist (they become part of the "mdc" field in the JSON log).

#### 35.2.6 Rebuild and Test

1. Create the `elk` directory structure:
   ```
   mkdir -p elk/logstash elk/filebeat
   ```
2. Place the configuration files as above.
3. Rebuild services to include the new logback config (if not already done).
4. Run `docker-compose up --build`.

5. Access Kibana at http://localhost:5601. Create an index pattern `supermarket-logs-*` and start exploring logs.

---

### 35.3 Using Kibana

- **Discover** – search and filter logs across all services.
- **Visualize** – create charts of log levels over time, error counts per service, tenant-specific logs, etc.
- **Dashboard** – combine visualizations into a single view for operations.

Example queries:
- `service_name: "com.supermarket.auth.AuthApplication"` – show only auth service logs.
- `log.level: "ERROR"` – show only errors.
- `tenant_id: "tenant-abc123"` – filter by tenant.

---

### 35.4 Log Aggregation Benefits

- Centralized troubleshooting without `docker logs`.
- Long-term retention and analysis.
- Correlate events across services (e.g., order creation and inventory adjustment).
- Alerting on error patterns (can be added with ElastAlert or Watcher).

---

## 36. Next Steps

With ELK integrated, we can now:

- **Monitoring** – Add Prometheus and Grafana for metrics.
- **Distributed Tracing** – Already have Zipkin; can visualize traces in Kibana (Elastic APM) or Jaeger.
- **Security Hardening** – Add rate limiting, HTTPS, secrets management.
- **CI/CD** – Set up automated pipelines.
- **Kubernetes** – Deploy to a cluster.

If you'd like me to proceed with **Monitoring** (Prometheus/Grafana), let me know.