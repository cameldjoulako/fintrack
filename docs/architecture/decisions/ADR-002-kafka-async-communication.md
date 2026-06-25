# ADR-002 — Kafka for Asynchronous Cross-Service Communication

**Date :** 2026-06-24  
**Status :** Accepted  
**Deciders :** Camel (Architect)

---

## Context

When a transaction is created, several side effects must occur: the budget service must evaluate whether a threshold has been crossed, and the notification service may need to alert the user. These side effects are not part of the transaction's consistency boundary.

Two options were considered:
- **Synchronous REST calls** from transaction-service to budget-service and notification-service
- **Asynchronous events via Apache Kafka**

---

## Decision

All cross-service side effects are communicated via **Apache Kafka domain events**. Synchronous REST is used only for user-facing queries through the API Gateway.

---

## Justification

### 1. Side effects must not fail the primary operation

If budget-service is down, a transaction write must still succeed. With synchronous REST, a budget-service outage causes transaction creation to fail — which is wrong. The user recorded a valid transaction; the budget evaluation is a downstream concern.

With Kafka, the transaction-service publishes a `TransactionCreated` event and returns 201. The budget-service processes it when it is available. The event is durably stored in Kafka and will be consumed on recovery.

### 2. Decoupling the producer from consumers

The transaction-service does not know that budget-service or notification-service exist. It publishes a domain event. Any number of consumers can react to it — today that's two services, tomorrow it could be five — without touching the transaction-service.

### 3. Retry and dead-letter handling

Kafka consumer groups with automatic retry and dead-letter topics give us resilience that REST callbacks do not. A transient database failure in budget-service will be retried automatically; a poison message goes to a dead-letter topic for inspection, not lost.

### 4. Audit trail

Kafka topics are an immutable, replayable log of domain events. This is intrinsically valuable in a financial application: we can replay `TransactionCreated` events to rebuild read models, audit user activity, or recover from data corruption.

---

## Event Schema Conventions

All events follow this envelope:

```json
{
  "eventId": "uuid-v4",
  "eventType": "TransactionCreated",
  "aggregateId": "transaction-uuid",
  "aggregateType": "Transaction",
  "occurredOn": "2026-06-24T14:32:00Z",
  "version": 1,
  "payload": { }
}
```

Topic naming: `fintrack.{aggregate}.{event}` in snake_case  
Examples: `fintrack.transaction.created`, `fintrack.budget.alert-triggered`

---

## Trade-offs Accepted

| Trade-off | Mitigation |
|---|---|
| Eventual consistency (budget check happens after transaction write) | Acceptable: budget alert at t+100ms vs t+0ms is not a business constraint |
| Operational complexity (Kafka cluster) | Docker Compose for local; Confluent Cloud as managed option for prod |
| Event schema evolution | Spring Cloud Contract consumer-driven contracts; versioned event envelope |
| Duplicate message delivery (at-least-once) | Idempotent consumers using `eventId` deduplication |
