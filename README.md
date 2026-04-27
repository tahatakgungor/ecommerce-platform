# ecommerce-platform

> Event-driven microservices backend for e-commerce — built with Java 21, Spring Boot, Kafka, and PostgreSQL. Fully containerized with Docker Compose.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?logo=spring-boot)
![Kafka](https://img.shields.io/badge/Event_Streaming-Kafka-231F20?logo=apache-kafka)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL_15-336791?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)

---

## Overview

A production-ready e-commerce backend following a **microservices architecture**. Services communicate asynchronously via **Apache Kafka**, ensuring loose coupling and horizontal scalability. Each service owns its data and can be deployed, scaled, and updated independently.

Built to demonstrate:
- Event-driven service communication (no direct REST calls between services)
- Domain isolation with separate databases per service
- Container orchestration with Docker Compose
- Modern Java 21 features (virtual threads, records, sealed classes)

---

## Architecture

```
  Client / API Gateway
         │
         ├──────────────────────────┐
         │                          │
  ┌──────▼──────┐            ┌──────▼──────┐
  │  Product    │            │   Order     │
  │  Service   │            │   Service   │
  │  :8081     │            │   :8082     │
  └──────┬──────┘            └──────┬──────┘
         │                          │
         └──────────┬───────────────┘
                    │  Apache Kafka
              ┌─────▼──────┐
              │  Zookeeper  │
              │  Kafka      │
              │  Kafka UI   │  ← :8080 dashboard
              └─────┬──────┘
                    │
              ┌─────▼──────┐
              │ PostgreSQL  │
              │  :5432      │
              └────────────┘
```

**Event flow example:** `ProductCreated` event → Kafka topic → consumed by Order Service for inventory checks.

---

## Services

| Service | Port | Responsibility |
|---|---|---|
| **product-service** | 8081 | Product catalog — CRUD for products, categories, inventory |
| **PostgreSQL** | 5432 | Shared persistence layer |
| **Kafka** | 9092 | Async inter-service messaging |
| **Kafka UI** | configurable | Visual Kafka topic & consumer monitoring |
| **Zookeeper** | 2181 | Kafka cluster coordination |

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 (LTS) |
| **Framework** | Spring Boot 3.x |
| **Event Streaming** | Apache Kafka 7.4 + Zookeeper |
| **Database** | PostgreSQL 15 |
| **Containerization** | Docker + Docker Compose |
| **Build Tool** | Maven (multi-module) |
| **Monitoring** | Kafka UI (provectuslabs) |

---

## Getting Started

### Prerequisites
- JDK 21+, Docker, Docker Compose

### Run with Docker Compose

```bash
git clone https://github.com/tahatakgungor/ecommerce-platform
cd ecommerce-platform

# Start all infrastructure (PostgreSQL + Kafka stack)
docker compose up -d

# Verify services are running
docker compose ps

# Access Kafka UI dashboard
open http://localhost:${KAFKA_UI_PORT}
```

### Build & run individual service

```bash
cd product-service
./mvnw spring-boot:run
```

---

## Project Structure

```
ecommerce-platform/
├── docker-compose.yml          # Full stack: Postgres + Kafka + Zookeeper + Kafka UI
├── pom.xml                     # Parent Maven POM (multi-module)
├── product-service/            # Product catalog microservice
│   ├── src/
│   │   ├── main/java/          # Domain logic, REST controllers, Kafka producers
│   │   └── test/               # Unit & integration tests
│   └── pom.xml
├── ops/                        # Operational scripts & deployment configs
└── src/                        # Shared utilities / common module
```

---

## Environment Variables

Copy `.env.example` to `.env` before starting:

```bash
KAFKA_UI_PORT=8080
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password
POSTGRES_DB=ecommerce
```

---

## License

MIT — see [LICENSE](LICENSE)
