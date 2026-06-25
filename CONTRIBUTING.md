# Contributing to FinTrack

## Git Workflow

This project follows trunk-based development with short-lived branches.

```
main          <- production (protected)
  └── develop <- integration (protected)
        └── feature/xxx  <- development (lifetime < 2 days)
```

### Branch naming

| Prefix      | Usage                | Example                               |
| ----------- | -------------------- | ------------------------------------- |
| `feature/`  | New functionality    | `feature/transaction-category-filter` |
| `fix/`      | Bug fix              | `fix/budget-threshold-calculation`    |
| `refactor/` | Internal improvement | `refactor/transaction-domain-model`   |
| `test/`     | Adding tests         | `test/budget-integration-tests`       |
| `docs/`     | Documentation        | `docs/adr-kafka-decision`             |
| `chore/`    | Build, dependencies  | `chore/upgrade-spring-boot-4.1`       |

### Commits - Conventional Commits

Format: `type(scope): short description`

```
feat(transaction): add multi-currency support
fix(budget): correct threshold evaluation on monthly rollover
refactor(auth): extract token validation to domain service
test(transaction): add Testcontainers integration tests
docs(arch): add ADR-003 database-per-service
chore(deps): upgrade Spring Boot to 4.1.0
```

### Daily task cycle

```bash
# 1. Create a GitHub issue on the board
# 2. Create the branch from develop
git checkout develop && git pull
git checkout -b feature/my-feature

# 3. Develop (TDD: test first)
# 4. Commit regularly
git add .
git commit -m "feat(service): description"

# 5. Push and open a PR toward develop
git push -u origin feature/my-feature
# Open the PR on GitHub and fill in the template

# 6. Verify CI passes
# 7. Merge (squash) with a Conventional Commit message
# 8. Delete the branch
```

## Code Standards

### Clean Architecture (mandatory in every service)

```
src/main/java/com/fintrack/{service}/
├── domain/          <- Entities, Value Objects, Events, Ports (interfaces)
│                       No Spring dependency allowed here
├── application/     <- Use Cases
│                       Depends only on domain/
├── infrastructure/  <- JPA, Kafka, HTTP clients, port implementations
│                       Depends on domain/ and application/
└── api/             <- REST controllers, DTOs, mappers
                        HTTP entry point
```

Absolute rule: `domain/` must never import Spring, JPA, or Kafka.

### Tests

- Write the test before the code (TDD)
- Unit tests in `domain/` and `application/`: no Spring context
- Integration tests with Testcontainers for `infrastructure/`
- Minimum coverage: 80%

```bash
# Unit tests only
mvn test

# Tests + integration + coverage
mvn verify

# Integration tests only
mvn verify -P integration-tests
```
