# FinTrack - Personal Finance Platform

> Production-grade personal finance management platform built with a distributed
> microservices architecture on a monorepo. Designed to demonstrate real-world
> software engineering practices: Clean Architecture, DDD, event-driven design,
> CI/CD, and cloud-native deployment on Kubernetes.

[![CI](https://github.com/cameldjoulako/fintrack/actions/workflows/ci.yml/badge.svg)](https://github.com/cameldjoulako/fintrack/actions)
[![Coverage](https://img.shields.io/badge/coverage-80%25-brightgreen)](https://github.com/cameldjoulako/fintrack)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-22-red)](https://angular.io/)

---

## Table of Contents

- [Overview](#overview)
- [Why Microservices?](#why-microservices)
- [Architecture](#architecture)
- [Monorepo Structure](#monorepo-structure)
- [Services](#services)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [API Documentation](#api-documentation)
- [Testing Strategy](#testing-strategy)
- [CI/CD Pipeline](#cicd-pipeline)
- [Deployment](#deployment)
- [ADR](#adr)

---

## Overview

FinTrack is a personal finance management platform that allows users to:

- Track income and expenses across multiple accounts and categories
- Define monthly budgets per category and receive real-time alerts when thresholds are crossed
- Visualize spending trends through interactive dashboards and exportable PDF reports
- Receive email and push notifications for unusual activity and budget overruns
- Manage multiple currencies with live exchange rates
- Use the platform in English or French, with support for additional languages

The platform is intentionally scoped to personal finance, a domain rich in business
rules, real-time constraints, and regulatory concerns, making it an ideal showcase
for enterprise software engineering patterns.

---

## Why Microservices?

This is a deliberate architectural choice, not a default. Here is the justification
for each decision.

### Bounded Contexts with Distinct Business Rules

The domain decomposes naturally into bounded contexts that change at different rates
and for different reasons:

| Context           | Reason for isolation                                                                                              |
| ----------------- | ----------------------------------------------------------------------------------------------------------------- |
| Identity and Auth | Security patches, MFA changes, and OAuth2 provider updates must never require redeployment of the whole system    |
| Transactions      | High write frequency, strict consistency requirements, audit trail -- benefits from dedicated persistence tuning  |
| Budgets           | Pure business logic, computation-heavy, evolves based on product decisions independently of transaction ingestion |
| Notifications     | Inherently async, requires its own retry and dead-letter strategy, replaceable without touching other services    |
| Reports           | CPU-intensive PDF generation that must not compete for resources with low-latency transaction APIs                |
| Exchange Rates    | External dependency with a different SLA, caches aggressively, changes independently of all business logic        |

### Independent Deployability

Each service can be built, tested, and deployed independently. A bug fix in the
notification system does not require a full regression of transaction logic. This is
the core value proposition of microservices in a team context.

### Scalability by Domain

Transaction ingestion and report generation have opposite performance profiles.
Transactions need low latency at high concurrency. Reports need high CPU for short
bursts. Microservices allow independent horizontal scaling: running 5 replicas of
the transaction service and 1 of the report service is not possible in a monolith.

### Trade-offs Acknowledged

For a solo developer or small team, microservices introduce real overhead: distributed
tracing, network latency between services, eventual consistency challenges, and
operational complexity. The monorepo structure mitigates the developer experience
cost while preserving the architectural boundaries.

---

## Architecture

```
+-----------------------------------------------------------------------+
|                           Client Layer                                |
|                    Angular SPA (fintrack-web)                         |
+-----------------------------+-----------------------------------------+
                              | HTTPS
+-----------------------------v-----------------------------------------+
|                       API Gateway (port 8080)                         |
|                 Spring Cloud Gateway + Rate Limiting                  |
|              Route /api/auth       -> auth-service:8081               |
|              Route /api/tx         -> transaction-service:8082        |
|              Route /api/budgets    -> budget-service:8083             |
|              Route /api/reports    -> report-service:8084             |
|              Route /api/rates      -> exchange-service:8085           |
+-------+---------------+---------------+--------------+---------------+
        |               |               |              |
+-------v-----+ +-------v-----+ +-------v---+ +--------v----------+
| auth-service| |transaction  | |  budget   | | report-service    |
|    :8081    | |service :8082| |service    | |    :8084          |
|             | |             | |  :8083    | |                   |
| PostgreSQL  | | PostgreSQL  | | PostgreSQL| | PostgreSQL        |
| (auth_db)   | | (tx_db)    | |(budget_db)| | (report_db)       |
+-------------+ +------+------+ +-----+-----+ +-------------------+
                        |             |
               +--------v-------------v-----------+
               |           Apache Kafka            |
               |  topic: fintrack.transaction.created      |
               |  topic: fintrack.budget.alert-triggered   |
               |  topic: fintrack.report.requested         |
               +------------------+----------------+
                                  |
                      +-----------v------------+
                      |  notification-service  |
                      |        :8086           |
                      |  Email / Push / SMS    |
                      +------------------------+

Shared Infrastructure:
  Redis        -> Session cache, rate limit counters, exchange rate cache
  Prometheus   -> Metrics scraping (all services expose /actuator/prometheus)
  Grafana      -> Dashboards (transactions/sec, budget alerts rate, error rate)
  Zipkin       -> Distributed tracing
  PostgreSQL   -> One database per service (database-per-service pattern)
```

### Communication Patterns

| Pattern            | Used for                                                                   | Justification                                                        |
| ------------------ | -------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| Synchronous REST   | User-facing reads and writes through the gateway                           | Low latency required, user waits for response                        |
| Async Kafka events | Cross-service side effects (transaction created -> check budget -> notify) | Decoupling, resilience, retry capability                             |
| Redis cache        | Exchange rates, user sessions, rate limit state                            | Shared read-only state without transactional consistency requirement |

### Key Design Decisions

- Database per service: each service owns its schema. No shared tables. Cross-service
  queries are resolved at the application level via API calls or denormalized event payloads.
- Event-carried state transfer: Kafka events include enough data for consumers to act
  without calling back the producer.
- API Gateway as the single entry point: all JWT validation happens at the gateway level.
  Downstream services trust the forwarded claims via headers.
- Saga pattern (Phase 3): multi-step operations are coordinated via Kafka events,
  not distributed transactions.

---

## Monorepo Structure

```
fintrack/
+-- services/
|   +-- auth-service/               # OAuth2, JWT, user management, i18n
|   +-- transaction-service/        # Core transaction management
|   +-- budget-service/             # Budget rules and alert engine
|   +-- notification-service/       # Email and push via Kafka consumer
|   +-- report-service/             # PDF report generation
|   +-- exchange-service/           # Live exchange rates (Redis cache)
|   +-- api-gateway/                # Spring Cloud Gateway
|
+-- frontend/
|   +-- fintrack-web/               # Angular 22 SPA (ngx-translate i18n)
|       +-- src/app/
|           +-- core/               # Auth guards, interceptors, services
|           +-- features/
|           |   +-- dashboard/
|           |   +-- transactions/
|           |   +-- budgets/
|           |   +-- reports/
|           +-- shared/             # Components, pipes, directives
|           +-- i18n/               # Translation files (en.json, fr.json)
|
+-- infrastructure/
|   +-- docker/
|   |   +-- docker-compose.infra.yml  # Local development infrastructure only
|   |   +-- monitoring/
|   |       +-- prometheus.yml
|   +-- k8s/                          # Kubernetes manifests (staging and production)
|       +-- namespace.yaml
|       +-- services/
|       +-- ingress.yaml
|
+-- docs/
|   +-- architecture/
|       +-- decisions/              # Architecture Decision Records
|       +-- diagrams/
|
+-- .github/
|   +-- workflows/
|   |   +-- ci.yml
|   |   +-- cd-staging.yml
|   |   +-- cd-prod.yml
|   +-- ISSUE_TEMPLATE/
|   +-- PULL_REQUEST_TEMPLATE.md
|
+-- pom.xml                         # Maven multi-module parent POM
+-- README.md
```

---

## Services

### auth-service (port 8081)

Handles user registration, authentication, token lifecycle, and language preferences.

Responsibilities:

- User registration with email verification
- JWT issuance and refresh token rotation
- OAuth2 social login (Google)
- Password reset flow
- Role-based access control (USER, ADMIN)
- User language preference (EN, FR -- extensible)

Key endpoints:

```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
POST /api/auth/forgot-password
```

### transaction-service (port 8082)

The core of the platform. Manages all financial transactions.

Responsibilities:

- Create, read, update, delete transactions
- Categorize transactions (FOOD, TRANSPORT, HOUSING, SALARY, etc.)
- Multi-currency support via exchange-service
- Publish TransactionCreated event to Kafka on every write

Domain model:

```
Transaction (Aggregate Root)
  +-- id : UUID
  +-- accountId : UUID
  +-- amount : Money (Value Object - amount + currency)
  +-- category : Category (Value Object)
  +-- type : INCOME | EXPENSE
  +-- description : String
  +-- transactionDate : LocalDate
  +-- tags : Set<Tag>
```

### budget-service (port 8083)

Manages budget rules and evaluates them on every transaction event.

Responsibilities:

- Define monthly budgets per category and user
- Listen to TransactionCreated Kafka events
- Evaluate if a budget threshold has been crossed
- Publish BudgetAlertTriggered when a threshold is reached (50%, 80%, 100%)

Why Kafka here instead of a direct call? The budget check is a side effect of a
transaction, not part of its consistency boundary. A budget-service outage must
not fail the transaction write.

### notification-service (port 8086)

Async consumer that sends notifications in the user's preferred language.

Listens to:

- fintrack.budget.alert-triggered -> sends localized email alert
- fintrack.transaction.created -> sends daily digest (batched)
- fintrack.report.ready -> sends email with PDF attachment

### report-service (port 8084)

Generates PDF and CSV financial reports on demand.

Why isolated? PDF generation is CPU-intensive and can take several seconds for large
datasets. Running it in the same JVM as the transaction API would degrade response
times under load.

### exchange-service (port 8085)

Fetches and caches live exchange rates from an external provider.

Caching strategy: rates are fetched every hour and stored in Redis with a 2-hour TTL.
Services call this service synchronously, never the external API directly.

### api-gateway (port 8080)

Single entry point for all client traffic.

Responsibilities:

- JWT validation on all requests
- Rate limiting per user (100 requests/minute) via Redis token bucket
- Request routing to downstream services
- Response aggregation for dashboard endpoint (parallel calls)
- Circuit breaker (Resilience4j) on all downstream routes

---

## Tech Stack

### Backend

| Technology              | Version | Purpose                                                                       |
| ----------------------- | ------- | ----------------------------------------------------------------------------- |
| Java                    | 25      | Language (Records, Pattern Matching, Virtual Threads, Structured Concurrency) |
| Spring Boot             | 4.1     | Application framework                                                         |
| Spring Security         | 7.x     | OAuth2 resource server, JWT                                                   |
| Spring Cloud Gateway    | 5.x     | API Gateway, routing, rate limiting                                           |
| Spring Data JPA         | 4.x     | ORM, repositories                                                             |
| Spring Kafka            | 4.x     | Kafka producer and consumer                                                   |
| Spring Cache + Redis    | -       | Cache abstraction                                                             |
| Spring MessageSource    | -       | Backend i18n (localized error messages)                                       |
| Flyway                  | 10.x    | Database migrations                                                           |
| Resilience4j            | 2.x     | Circuit breaker, retry, bulkhead                                              |
| Micrometer + Prometheus | -       | Metrics                                                                       |
| Zipkin                  | -       | Distributed tracing                                                           |
| Lombok                  | -       | Boilerplate reduction                                                         |
| MapStruct               | 1.6     | DTO mapping                                                                   |

### Frontend

| Technology       | Version | Purpose                           |
| ---------------- | ------- | --------------------------------- |
| Angular          | 22      | SPA framework                     |
| TypeScript       | 5.x     | Language                          |
| NgRx             | 19      | State management                  |
| Angular Material | 22      | UI components                     |
| ngx-translate    | 16.x    | Runtime i18n (EN, FR, extensible) |
| Chart.js         | 4.x     | Data visualization                |
| RxJS             | 7.x     | Reactive programming              |

### Infrastructure

| Technology              | Purpose                                        |
| ----------------------- | ---------------------------------------------- |
| PostgreSQL 16           | Primary persistence (one database per service) |
| Apache Kafka            | Event streaming                                |
| Redis 7                 | Cache, sessions, rate limit state              |
| Docker + Docker Compose | Local development infrastructure only          |
| Kubernetes (K3s / EKS)  | Staging and production orchestration           |
| GitHub Actions          | CI/CD pipeline                                 |
| Prometheus + Grafana    | Observability                                  |
| Zipkin                  | Distributed tracing                            |

---

## Getting Started

### Prerequisites

- Java 25+
- Node 22+, Angular CLI 22+
- Docker and Docker Compose
- Maven 3.9+

### Run the local infrastructure

Docker Compose is used exclusively for local development. It is not a deployment
solution for microservices in staging or production.

```bash
git clone https://github.com/cameldjoulako/fintrack.git
cd fintrack

# Start all local infrastructure
# (PostgreSQL x4, Kafka, Redis, Prometheus, Grafana, Zipkin)
docker-compose -f infrastructure/docker/docker-compose.infra.yml up -d

# Start a service
cd services/auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Start the Angular frontend
cd frontend/fintrack-web
npm install
ng serve
```

Access points:

- Application: <http://localhost:4200>
- API Gateway: <http://localhost:8080>
- Grafana: <http://localhost:3000> (admin/admin)
- Zipkin: <http://localhost:9411>
- Kafka UI: <http://localhost:8090>

---

## Development Workflow

This project follows trunk-based development with short-lived feature branches.

### Branch naming convention

```
feature/  -> new functionality       e.g. feature/transaction-category-filter
fix/      -> bug fix                 e.g. fix/budget-alert-threshold
refactor/ -> internal improvement    e.g. refactor/transaction-domain-model
test/     -> adding tests            e.g. test/budget-integration-tests
docs/     -> documentation           e.g. docs/adr-kafka-decision
chore/    -> build, dependencies     e.g. chore/upgrade-spring-boot-4.1
```

### Commit convention (Conventional Commits)

```
feat(transaction): add multi-currency support with automatic conversion
fix(budget): correct threshold evaluation for monthly rollover
refactor(auth): extract token validation to dedicated domain service
test(transaction): add Testcontainers integration tests
docs(arch): add ADR-003 database-per-service decision record
chore(deps): upgrade Spring Boot to 4.1.0
```

### Pull Request process

1. Open a PR from your feature branch to develop
2. Ensure all CI checks pass (build, tests, coverage >= 80%)
3. Self-review using the PR template checklist
4. Merge via squash commit with a Conventional Commit message

Project tracking is managed on GitHub Projects. See the board for current sprint
tasks and backlog.

---

## API Documentation

Each service exposes a Swagger UI:

| Service              | URL                                     |
| -------------------- | --------------------------------------- |
| auth-service         | <http://localhost:8081/swagger-ui.html> |
| transaction-service  | <http://localhost:8082/swagger-ui.html> |
| budget-service       | <http://localhost:8083/swagger-ui.html> |
| report-service       | <http://localhost:8084/swagger-ui.html> |
| exchange-service     | <http://localhost:8085/swagger-ui.html> |
| notification-service | <http://localhost:8086/swagger-ui.html> |

### Example: Create a transaction

```bash
curl -X POST http://localhost:8080/api/tx/transactions \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "amount": 85.50,
    "currency": "CAD",
    "category": "FOOD",
    "type": "EXPENSE",
    "description": "Groceries IGA Montreal",
    "transactionDate": "2026-06-24",
    "tags": ["groceries", "weekly"]
  }'
```

Response:

```json
{
  "id": "a1b2c3d4-...",
  "accountId": "3fa85f64-...",
  "amount": { "value": 85.5, "currency": "CAD" },
  "category": "FOOD",
  "type": "EXPENSE",
  "transactionDate": "2026-06-24",
  "createdAt": "2026-06-24T14:32:00Z"
}
```

Async side effects via Kafka:

- budget-service evaluates the FOOD budget for June 2026
- If threshold crossed, notification-service sends a localized email alert

---

## Testing Strategy

```
          +------------------------------------+
          |        E2E Tests (few)             |  Playwright
          |   Contract Tests (moderate)        |  Spring Cloud Contract
          |  Integration Tests (moderate)      |  Testcontainers
          |    Unit Tests (many)               |  JUnit 5 + Mockito
          +------------------------------------+
```

### Unit tests

- Domain logic tested in pure Java, no Spring context, no infrastructure mocks
- Use cases tested with mocked ports (Mockito)
- Target: full suite runs in under 5 seconds

```bash
cd services/transaction-service
mvn test
```

### Integration tests (Testcontainers)

- Real PostgreSQL and Kafka containers started per test class
- Tests the full slice: HTTP request -> use case -> repository -> database
- Tagged with @Tag("integration") and run separately in CI

```bash
mvn verify -P integration-tests
```

### Contract tests (Spring Cloud Contract)

- transaction-service defines contracts for its Kafka event schemas
- budget-service and notification-service verify against these contracts
- Prevents event schema drift between services

### Coverage

JaCoCo enforces a minimum of 80% line coverage. The build fails below this threshold.

```bash
mvn verify jacoco:report
# Report available at: target/site/jacoco/index.html
```

---

## CI/CD Pipeline

### On every Pull Request

```
Checkout -> Setup Java 25 -> Maven build -> Unit tests -> Integration tests -> JaCoCo check -> Docker build validation
```

Frontend in parallel:

```
npm ci -> ng lint -> ng test --watch=false -> ng build --configuration production
```

### On merge to develop

```
CI -> Docker build -> Push to GHCR -> Deploy to staging (K3s) -> Smoke tests
```

### On merge to main

```
CI -> Docker build -> Tag image with git SHA -> Push to GHCR -> Deploy to production (K8s rolling update) -> Health check
```

---

## Deployment

### Important: Docker Compose is not an orchestrator

Docker Compose is reserved for local development. It does not handle high availability,
automatic container restart on failure, horizontal scaling, rolling updates, or secret
management. Deploying microservices to staging or production with Docker Compose is
an architectural mistake. Microservice orchestration requires Kubernetes.

### Local development: Docker Compose

Sole purpose: run infrastructure dependencies on the developer machine.

```bash
docker-compose -f infrastructure/docker/docker-compose.infra.yml up -d
```

### Staging: K3s on VPS

K3s is a lightweight Kubernetes distribution, ideal for a single VPS. It provides
all Kubernetes primitives (Deployments, Services, Ingress, HPA) without the
operational complexity of a full cluster.

```bash
curl -sfL https://get.k3s.io | sh -
kubectl apply -f infrastructure/k8s/
kubectl rollout status deployment/auth-service -n fintrack
```

What K3s provides that Docker Compose cannot:

- Automatic pod restart on failure
- Zero-downtime rolling updates
- Health checks with liveness and readiness probes
- Horizontal scaling via HPA
- Kubernetes secret management

### Production: Managed Kubernetes

In production, a managed Kubernetes cluster is used: EKS on AWS, or multi-node K3s
depending on budget. Each service has its manifests under `infrastructure/k8s/`:

```
infrastructure/k8s/
+-- namespace.yaml
+-- services/
|   +-- auth-service/
|   |   +-- deployment.yaml     # Rolling update, resource limits, probes
|   |   +-- service.yaml        # ClusterIP
|   |   +-- hpa.yaml            # Horizontal Pod Autoscaler
|   +-- transaction-service/
|       +-- deployment.yaml     # 2 to 10 replicas based on CPU
|       +-- service.yaml
|       +-- hpa.yaml
+-- ingress.yaml
+-- configmap.yaml
```

---

## ADR

All significant architectural decisions are documented in `docs/architecture/decisions/`:

- ADR-001: Why microservices over a modular monolith
- ADR-002: Why Kafka over REST for cross-service side effects
- ADR-003: Database per service, trade-offs and consistency strategy
- ADR-004: Why a monorepo over separate repositories

---

## Contributing

This project is built and maintained by [Camel Djoulako](https://linkedin.com/in/cameldjoulako),
Software Architect and founder of [Aroolia](https://aroolia.com).

All commits follow Conventional Commits. All changes go through a Pull Request.
See [CONTRIBUTING.md](CONTRIBUTING.md) for the full development guide.

---

## License

MIT - see [LICENSE](LICENSE) for details.
