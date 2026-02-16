# Supermarket POS SaaS - Microservices Architecture

## 📋 Project Overview

This is a comprehensive **Supermarket POS (Point of Sale) SaaS** system built with **Spring Boot Microservices** architecture. The system supports **multi-tenancy**, allowing multiple supermarket branches to operate independently under one platform.

### Key Features:
- 🏪 Multi-tenant supermarket management
- 📦 Product management with category hierarchy
- 📊 Inventory tracking with unit conversions (Case/Box/Pack/Pcs)
- 💳 Payment gateway integration (ABA PayWay support)
- 🏦 General Ledger (GL) accounting
- 📱 POS terminal integration
- 📦 Order management
- 🔐 JWT Authentication
- 📈 Analytics & Reporting

---

## 🏗️ Architecture Overview

```
                                    ┌─────────────────┐
                                    │   API Gateway   │
                                    │    (Port 8080)  │
                                    └────────┬────────┘
                                             │
         ┌───────────────────────────────────┼───────────────────────────────────┐
         │                                   │                                   │
         ▼                                   ▼                                   ▼
┌─────────────────┐              ┌─────────────────┐              ┌─────────────────┐
│  Auth Service  │              │ Product Service │              │  Order Service  │
│   (Port 8081)  │              │   (Port 8082)  │              │   (Port 8084)  │
└────────┬────────┘              └────────┬────────┘              └────────┬────────┘
         │                                   │                                   │
         ▼                                   ▼                                   ▼
┌─────────────────┐              ┌─────────────────┐              ┌─────────────────┐
│  Tenant Service │              │Inventory Service│              │Payment Service  │
│   (Port 8087)  │              │   (Port 8083)  │              │   (Port 8085)  │
└─────────────────┘              └─────────────────┘              └─────────────────┘
                                          │
                                          ▼
                               ┌─────────────────┐
                               │Accounting Service│
                               │   (Port 8088)   │
                               └─────────────────┘
```

---

## 📁 Project Structure

```
supermarket/
├── pom.xml                          # Parent POM
├── docker-compose.yml               # Infrastructure services
│
├── common-library/                  # Shared DTOs and utilities
│   └── ApiResponse.java
│
├── service-discovery/               # Eureka Server (Service Registry)
│   └── DiscoveryApplication.java
│
├── config-service/                  # Spring Cloud Config Server
│   └── ConfigApplication.java
│
├── api-gateway/                     # Spring Cloud Gateway
│   └── GatewayApplication.java
│
├── auth-service/                    # Authentication & JWT
│   ├── entity/User.java
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── SecurityConfig.java
│   └── controller/AuthController.java
│
├── tenant-service/                  # Multi-tenant management
│   └── entity/Tenant.java
│
├── product-service/                 # Product & Category Management
│   ├── entity/
│   │   ├── Product.java
│   │   ├── Category.java           # Hierarchical categories
│   │   └── ProductUnit.java       # Unit conversions (Case/Box/Pack)
│   ├── repository/
│   ├── service/
│   └── controller/
│
├── inventory-service/               # Stock management
│   ├── entity/Inventory.java
│   └── service/InventoryService.java
│
├── order-service/                  # Order processing
│   ├── entity/
│   │   ├── Order.java
│   │   └── OrderItem.java
│   └── service/OrderService.java
│
├── payment-service/                 # Payment processing
│   ├── entity/
│   │   ├── Payment.java
│   │   └── PaymentGatewayConfig.java  # Dynamic payment gateways
│   └── service/PaymentService.java
│
├── pos-service/                     # POS Terminal management
│   └── entity/POSTerminal.java
│
└── accounting-service/              # GL Accounting
    ├── entity/
    │   ├── GLAccount.java           # General Ledger accounts
    │   ├── JournalEntry.java       # Double-entry bookkeeping
    │   ├── JournalEntryLine.java
    │   └── Account.java            # Bank/Cash accounts
    └── service/GLAccountService.java
```

---

## 🚀 Quick Start Guide

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. Start Infrastructure Services

```bash
# Start PostgreSQL, Kafka, Redis, ELK Stack
docker-compose up -d

# Verify services are running
docker-compose ps
```

### 2. Build the Project

```bash
# Build all modules
mvn clean install -DskipTests

# Or build specific module
mvn clean install -pl auth-service -am
```

### 3. Run Services (Order matters!)

```bash
# 1. Start Service Discovery (Eureka)
cd service-discovery
mvn spring-boot:run

# 2. Start API Gateway
cd api-gateway
mvn spring-boot:run

# 3. Start Auth Service
cd auth-service
mvn spring-boot:run

# 4. Start other services
cd product-service && mvn spring-boot:run
cd inventory-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd accounting-service && mvn spring-boot:run
```

---

## 🔧 Service Configuration

### Port Mapping

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | Entry point for all requests |
| Service Discovery | 8761 | Eureka Server |
| Config Server | 8888 | Configuration management |
| Auth Service | 8081 | JWT Authentication |
| Product Service | 8082 | Product & Category |
| Inventory Service | 8083 | Stock Management |
| Order Service | 8084 | Order Processing |
| Payment Service | 8085 | Payment Processing |
| POS Service | 8086 | POS Terminals |
| Tenant Service | 8087 | Multi-tenant |
| Accounting Service | 8088 | GL & Finance |

### Database Configuration

Each service has its own PostgreSQL database:
- `supermarket_auth` - Auth Service
- `supermarket_product` - Product Service
- `supermarket_inventory` - Inventory Service
- `supermarket_order` - Order Service
- `supermarket_payment` - Payment Service
- `supermarket_accounting` - Accounting Service
- `supermarket_tenant` - Tenant Service

---

## 🔐 Multi-Tenancy

All services use **X-Tenant-ID** header for tenant isolation:

```http
GET /api/products HTTP/1.1
Host: localhost:8080
X-Tenant-ID: TENANT001
Authorization: Bearer <jwt-token>
```

---

## 📡 API Flow Examples

### 1. Authentication Flow
```http
# Login
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123",
  "tenantId": "TENANT001"
}

# Response
{
  "success": true,
  "data": {
    "token": "eyJhbGc...",
    "username": "admin",
    "role": "ADMIN",
    "tenantId": "TENANT001"
  }
}
```

### 2. Product Management with Category
```http
# Create Category (with parent)
POST /api/products/categories
X-Tenant-ID: TENANT001
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Beverages",
  "code": "CAT001",
  "description": "All beverages"
}

# Create Sub-Category
POST /api/products/categories
X-Tenant-ID: TENANT001
{
  "name": "Soft Drinks",
  "code": "CAT001-01",
  "parent": { "id": 1 }
}

# Get Root Categories
GET /api/products/categories/root
X-Tenant-ID: TENANT001
```

### 3. Product with Unit Conversion
```http
# Create Product with Units
POST /api/products
X-Tenant-ID: TENANT001
{
  "name": "Coca Cola",
  "sku": "CC001",
  "basePrice": 1.00,
  "category": { "id": 1 },
  "quantity": 100
}

# Add Unit Conversions
POST /api/products/1/units
{
  "unitCode": "CASE",
  "unitName": "Case",
  "conversionRate": 24,
  "sellingPrice": 20.00,
  "isBaseUnit": false
}

POST /api/products/1/units
{
  "unitCode": "PCS",
  "unitName": "Pieces",
  "conversionRate": 1,
  "sellingPrice": 1.00,
  "isBaseUnit": true
}
```

### 4. Payment Gateway Configuration (ABA PayWay)
```http
# Configure ABA PayWay
POST /api/payments/gateways
X-Tenant-ID: TENANT001
{
  "gatewayName": "ABA PayWay",
  "gatewayCode": "ABA_PAYWAY",
  "merchantId": "your-merchant-id",
  "apiUrl": "https://checkout.payway.com.kh",
  "currency": "USD",
  "transactionFee": 2.5,
  "enabled": true
}

# Enable/Disable Gateway
POST /api/payments/gateways/enable/ABA_PAYWAY
POST /api/payments/gateways/disable/ABA_PAYWAY

# Get Enabled Gateways
GET /api/payments/gateways/enabled
```

### 5. GL Accounting Flow
```http
# Create GL Account
POST /api/accounting/gl-accounts
X-Tenant-ID: TENANT001
{
  "accountCode": "1000",
  "accountName": "Cash",
  "accountType": "ASSET",
  "balanceType": "DEBIT",
  "isActive": true
}

# Create Journal Entry
POST /api/accounting/journal-entries
{
  "description": "Sales Revenue",
  "lines": [
    { "accountCode": "1000", "debitAmount": 100.00, "creditAmount": 0 },
    { "accountCode": "4000", "debitAmount": 0, "creditAmount": 100.00 }
  ]
}
```

---

## 🏢 Multi-Tenant Workflow

```
┌─────────────────────────────────────────────────────────────┐
│                    TENANT ONBOARDING                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Create Tenant                                          │
│     POST /api/tenants                                      │
│     { "name": "Supermarket A", "tenantId": "TENANT001" }  │
│                                                             │
│  2. Create User (Tenant Admin)                             │
│     POST /api/auth/signup                                  │
│     { "username": "admin", "tenantId": "TENANT001" }       │
│                                                             │
│  3. Configure Payment Gateway                              │
│     POST /api/payments/gateways                            │
│                                                             │
│  4. Create Categories & Products                           │
│     POST /api/products/categories                          │
│     POST /api/products                                     │
│                                                             │
│  5. Setup GL Accounts                                      │
│     POST /api/accounting/gl-accounts                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Data Flow - Order to Payment to GL

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    POS      │────▶│   Order    │────▶│   Payment   │────▶│     GL      │
│  Service    │     │  Service   │     │  Service    │     │ Accounting  │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                          │                   │                   │
                          ▼                   ▼                   ▼
                    ┌───────────┐       ┌───────────┐       ┌───────────┐
                    │ Inventory │       │  Payment  │       │   debit   │
                    │  -stock   │       │ Gateway   │       │  credit   │
                    └───────────┘       └───────────┘       └───────────┘
```

---

## 🔧 Development Guidelines

### Adding New Service

1. **Create Module Directory**
   ```bash
   mkdir new-service
   cd new-service
   mkdir -p src/main/java/com/supermarket/newservice
   mkdir -p src/main/resources
   ```

2. **Create pom.xml** (copy from other service and modify)

3. **Create Main Application**
   ```java
   @SpringBootApplication
   @EnableDiscoveryClient
   @EnableFeignClients
   public class NewServiceApplication {
       public static void main(String[] args) {
           SpringApplication.run(NewServiceApplication.class, args);
       }
   }
   ```

4. **Add to Parent pom.xml**
   ```xml
   <module>new-service</module>
   ```

5. **Update API Gateway routes** in `application.yml`

### Adding New Feature to Product Service

1. **Create Entity** in `entity/` package
2. **Create Repository** in `repository/` package
3. **Create Service** in `service/` package
4. **Create Controller** in `controller/` package
5. **Add to API Gateway** route configuration

---

## 📝 GL Account Types

| Type | Balance | Examples |
|------|---------|----------|
| ASSET | Debit | Cash, Inventory, Equipment |
| LIABILITY | Credit | Accounts Payable, Loans |
| EQUITY | Credit | Capital, Retained Earnings |
| REVENUE | Credit | Sales, Service Income |
| EXPENSE | Debit | Cost of Goods Sold, Rent |

---

## 🔒 Security

- **JWT Authentication**: All services require valid JWT token
- **Multi-tenant Isolation**: X-Tenant-ID header for data separation
- **Password Encryption**: BCrypt password hashing
- **Role-based Access**: ADMIN, MANAGER, CASHIER, USER

---

## 📦 Infrastructure Services

```yaml
# docker-compose.yml includes:
- PostgreSQL (port 5432)
- Kafka & Zookeeper
- Redis (port 6379)
- Elasticsearch (port 9200)
- Logstash (port 9600)
- Kibana (port 5601)
- Prometheus (port 9090)
- Grafana (port 3000)
```

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific service tests
mvn test -pl auth-service

# Run with coverage
mvn test -pl auth-service -Djacoco
```

---

## 📚 API Documentation

After starting services, access:
- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **Kibana**: http://localhost:5601
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000

---

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Add unit tests
4. Submit pull request

---

## 📄 License

This project is proprietary software for Supermarket POS SaaS.
