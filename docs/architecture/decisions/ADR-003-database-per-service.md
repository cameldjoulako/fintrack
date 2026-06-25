# ADR-003 — Database per Service

**Date :** 2026-06-24  
**Status :** Accepted  
**Deciders :** Camel (Architect)

---

## Context

With a microservices architecture, a decision must be made on data ownership: shared database vs. database per service.

---

## Decision

Each service owns its own **dedicated PostgreSQL database**. No service reads from or writes to another service's database directly.

---

## Justification

A shared database negates the primary benefits of microservices: if auth-service and transaction-service share a database, a schema migration in one service can break the other. Independent deployability requires independent data ownership.

Each service's schema is an implementation detail — it can evolve, be optimized, or even be replaced with a different storage technology without impacting other services.

---

## Cross-Service Data Access Strategy

| Need | Solution |
|---|---|
| Read data owned by another service | REST API call through the Gateway, or denormalized in Kafka event payload |
| Join data across services | API Composition at the Gateway level (parallel calls, merge in response) |
| Consistency across services | Saga pattern via Kafka events (choreography-based) |

---

## Consequences

- 4 PostgreSQL instances in local Docker Compose (ports 5432–5435)
- Each service has its own Flyway migration scripts in `src/main/resources/db/migration/`
- No foreign keys across service boundaries — references are by UUID only
- Cross-service consistency is eventual, not transactional
