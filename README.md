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
| [audit-service](./audit-service) | 8089 | Audit Logging & Monitoring |

### Infrastructure Services

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL | 5432 | Database |
| Kafka | 9092 | Message Broker |
| Zookeeper | 2181 | Kafka Coordinator |
| Keycloak | 8180 | OAuth2 Identity Provider |
| Redis | 6379 | Caching |
| Elasticsearch | 9200 | Log Storage |
| Kibana | 5601 | Log Visualization |
| Prometheus | 9090 | Metrics |
| Grafana | 3000 | Dashboards |

---

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
| supermarket-audit | Bearer-only | Audit Service |

### Pre-configured Users

| Username | Password | Role | Description |
|----------|----------|------|-------------|
| admin | admin123 | ADMIN | Full system access |
| manager | manager123 | MANAGER | Store management |
| cashier | cashier123 | CASHIER | POS operations |
| auditor | auditor123 | AUDITOR | Audit log access |

---

## 🎯 Roles & Permissions

### Role Hierarchy

```
ADMIN
├── Full system access
├── User management
├── View all audit logs
└── System configuration

MANAGER
├── Product management
├── Inventory management
├── Order management
├── View reports
└── View audit logs

CASHIER
├── Process sales
├── Process payments
├── View products
└── Process returns

AUDITOR
├── View all audit logs
├── View user activities
├── View action summaries
└── View failed activities
```

### Permission Model

The system supports granular permissions with:
- **Resource**: The entity being accessed (e.g., PRODUCT, ORDER, USER)
- **Action**: The operation being performed (e.g., CREATE, UPDATE, DELETE, VIEW)
- **Roles**: Groups of permissions assigned to users

---

## 📊 Audit Service

### Overview
The audit-service provides comprehensive logging of all user activities across the platform. It supports both synchronous and asynchronous logging via Apache Kafka.

### Features
- **Real-time Logging**: Track all user actions as they happen
- **Async Processing**: Non-blocking audit logging via Kafka
- **Search & Analytics**: Advanced search with pagination and filtering
- **Activity Summary**: View action counts and most active users
- **Failed Activity Tracking**: Monitor failed login attempts and errors

### Audit Actions Tracked

| Action | Description |
|--------|-------------|
| LOGIN | User login success |
| LOGIN_FAILED | Failed login attempt |
| LOGOUT | User logout |
| CREATE | New entity created |
| UPDATE | Entity updated |
| DELETE | Entity deleted |
| VIEW | Entity viewed |
| PAYMENT | Payment processed |
| REFUND | Payment refunded |
| EXPORT | Data exported |
| IMPORT | Data imported |
| APPROVE | Entity approved |
| REJECT | Entity rejected |
| PERMISSION_DENIED | Access denied |
| ACCESS_DENIED | Unauthorized access |

### Audit API Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | /api/audit | Search audit logs | ADMIN, MANAGER, AUDITOR |
| GET | /api/audit/{id} | Get audit log by ID | ADMIN, MANAGER, AUDITOR |
| GET | /api/audit/tenant/{tenantId} | Get logs by tenant | ADMIN, MANAGER, AUDITOR |
| GET | /api/audit/user/{userId} | Get logs by user | ADMIN, MANAGER, AUDITOR |
| GET | /api/audit/action/{action} | Get logs by action | ADMIN, MANAGER, AUDITOR |
| GET | /api/audit/entity/{entityId} | Get logs by entity | ADMIN, MANAGER, AUDITOR |
| GET | /api/audit/summary | Get action summary | ADMIN, MANAGER, AUDITOR |
| GET | /api/audit/active-users | Most active users | ADMIN, MANAGER, AUDITOR |
| GET | /api/audit/failed | Failed activities | ADMIN, AUDITOR |
| POST | /api/audit | Create audit log | ADMIN, AUDITOR |

### Using Audit Log Utility

Inject the `AuditLogUtil` in any service to log activities:

```java
@Autowired
private AuditLogUtil auditLogUtil;

// Log a login
auditLogUtil.logAndSend(
    userId,           // User ID
    userName,         // User Name
    tenantId,         // Tenant ID
    "LOGIN",          // Action
    null,             // Entity Type
    null,             // Entity ID
    "User logged in", // Description
    ipAddress,        // IP Address
    200               // Response Status
);

// Or build custom log
CreateAuditLogRequest log = auditLogUtil.buildPaymentLog(
    userId, userName, tenantId, 
    orderId, amount, "CASH", "COMPLETED", 
    ipAddress, 200
);
auditLogUtil.sendAsyncAuditLog(log);
```

---

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

2. **Import Keycloak Realm**:
   - Access http://localhost:8180
   - Import `keycloak/supermarket-realm.json`

3. **Start Microservices**:
   ```bash
   # Start in order
   cd service-discovery && mvn spring-boot:run
   cd config-service && mvn spring-boot:run
   cd auth-service && mvn spring-boot:run
   cd audit-service && mvn spring-boot:run
   # ... other services
   cd api-gateway && mvn spring-boot:run
   ```

### Or Use Docker Compose

```bash
# Build all services
mvn clean package -DskipTests

# Start all services
docker-compose up -d
```

---

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
| GET | /api/v1/audit | audit-service | Search audit logs |

### Multi-tenancy

All API requests must include the `X-Tenant-ID` header:
```bash
curl -X GET http://localhost:8080/api/v1/products \
  -H "X-Tenant-ID: tenant-1" \
  -H "Authorization: Bearer <token>"
```

### Authentication Flow

```bash
# 1. Get access token
curl -X POST http://localhost:8180/realms/supermarket/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=supermarket-gateway" \
  -d "username=admin" \
  -d "password=admin123"

# 2. Use token in API request
curl -X GET http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer <access_token>" \
  -H "X-Tenant-ID: tenant-1"
```

---

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -DskipTests=false
```

---

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
├── accounting-service/       # Accounting & GL
├── audit-service/           # Audit Logging & Monitoring
├── common-library/           # Shared Code
├── docker-compose.yml        # Docker Orchestration
├── keycloak/                 # Keycloak Configuration
│   └── supermarket-realm.json
└── README.md                # This File
```

---

## 📋 Database Schema

### Service Databases

| Service | Database | Tables |
|---------|----------|--------|
| auth | auth_db | users, roles, permissions |
| product | product_db | products, categories, units |
| inventory | inventory_db | inventory, stock_transactions |
| order | order_db | orders, order_items |
| payment | payment_db | payments, transactions |
| pos | pos_db | terminals, shifts |
| tenant | tenant_db | tenants, configurations |
| accounting | accounting_db | gl_accounts, journal_entries |
| audit | audit_db | audit_logs |

---

## 🔧 Configuration

### Kafka Topics

- `audit-logs` - Audit log events
- `order-created` - New order events
- `payment-completed` - Payment success events
- `inventory-updated` - Stock changes
- `product-created` - New product events

---

## 🔒 Security Features

1. **OAuth2/OIDC**: All services use Keycloak for authentication
2. **JWT Tokens**: Stateless authentication with JWT
3. **Role-Based Access Control**: ADMIN, MANAGER, CASHIER, AUDITOR roles
4. **Multi-tenancy**: Tenant isolation via X-Tenant-ID header
5. **API Gateway**: Centralized security and rate limiting
6. **Audit Logging**: Complete tracking of all user activities

---

## 📄 License

This project is proprietary software for Supermarket POS SaaS.

---

## 🆘 Support

For issues and questions:
- Check Keycloak logs: `docker-compose logs keycloak`
- Check Kafka logs: `docker-compose logs kafka`
- Check audit-service logs: `docker-compose logs audit-service`
