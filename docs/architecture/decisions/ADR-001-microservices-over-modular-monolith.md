# ADR-001 — Microservices over Modular Monolith

**Date :** 2026-06-24  
**Status :** Accepted  
**Deciders :** Camel (Architect)

---

## Context

FinTrack is a personal finance platform built as a portfolio project to demonstrate enterprise-grade software engineering skills. The architecture choice must satisfy two constraints simultaneously: (1) technical correctness — the architecture must be genuinely justified, not cargo-culted, and (2) demonstrability — it must showcase skills relevant to large organizations (Desjardins, CGI, Banque Nationale, Google, Amazon).

The primary alternative considered was a **modular monolith**: a single deployable unit with enforced module boundaries, shared database, and synchronous inter-module communication. This is the correct default for most projects.

---

## Decision

We use a **microservices architecture** with one deployable unit per bounded context, one database per service, and asynchronous communication via Apache Kafka for cross-service side effects.

---

## Justification

### 1. The domain decomposes into genuinely independent bounded contexts

Each service maps to a bounded context with its own ubiquitous language, its own rate of change, and its own failure tolerance:

- **Auth** changes when security requirements change (MFA, OAuth2 provider, password policy). This must never require redeploying transaction logic.
- **Transactions** has strict write consistency and audit requirements. Its database schema is optimized for high-frequency inserts and point-in-time queries.
- **Budgets** is computation-heavy, stateless between evaluations, and changes when product rules change — independently of how transactions are stored.
- **Notifications** is inherently async. A notification failure must never propagate to the transaction write path.
- **Reports** is CPU-intensive (PDF generation). Its resource profile is opposite to the transaction service (burst CPU vs. sustained low-latency).
- **Exchange Rates** depends on an external SLA. It caches aggressively and has no business logic coupling to other services.

A modular monolith could enforce these boundaries at compile time, but could not enforce independent deployability, independent scaling, or independent failure domains.

### 2. Independent failure domains

In a monolith, a memory leak in the report generator degrades response times for the transaction API. With microservices and a circuit breaker at the gateway, report-service degradation is contained. Users can still record transactions while reports are unavailable.

### 3. Independent scaling

Transaction ingestion and report generation have opposite performance profiles:
- Transaction API: high concurrency, low latency, many small requests
- Report service: low concurrency, high CPU, long-running requests

These cannot be scaled independently in a monolith. In production, the transaction service runs 3-5 replicas; the report service runs 1 replica with higher CPU limits.

### 4. Portfolio alignment

The target employers (Desjardins, CGI, Banque Nationale) operate microservices architectures at scale. A candidate who has designed, built, and operated a microservices system — including dealing with eventual consistency, distributed tracing, and Kafka event schemas — is a materially stronger candidate than one who has only worked in monoliths.

---

## Trade-offs Accepted

| Trade-off | Mitigation |
|---|---|
| Distributed systems complexity (network failures, eventual consistency) | Circuit breakers (Resilience4j), idempotent consumers, dead-letter topics |
| Operational overhead (7 services to deploy) | Monorepo + Docker Compose for local dev, GitHub Actions CI/CD, single `docker-compose up` |
| Cross-service queries require API calls or denormalization | Event-carried state transfer in Kafka payloads; read models in consuming services |
| Developer experience overhead | Monorepo keeps all code in one place; shared parent POM; single `docker-compose.infra.yml` |

---

## Consequences

- Each service has its own `pom.xml`, `Dockerfile`, and database schema
- Cross-service communication uses Kafka for side effects and REST (via Gateway) for user-facing queries
- Distributed tracing (Zipkin) is mandatory from day one to debug cross-service flows
- Contract tests (Spring Cloud Contract) are required to prevent event schema drift
