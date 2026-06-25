# Contributing to FinTrack

## Workflow Git

Ce projet suit un **trunk-based development** avec branches courtes.

```
main          ← production (protégée)
  └── develop ← intégration (protégée)
        └── feature/xxx  ← développement (durée de vie < 2 jours)
```

### Branches

| Préfixe | Usage | Exemple |
|---|---|---|
| `feature/` | Nouvelle fonctionnalité | `feature/transaction-category-filter` |
| `fix/` | Correction de bug | `fix/budget-threshold-calculation` |
| `refactor/` | Amélioration interne | `refactor/transaction-domain-model` |
| `test/` | Ajout de tests | `test/budget-integration-tests` |
| `docs/` | Documentation | `docs/adr-kafka-decision` |
| `chore/` | Build, dépendances | `chore/upgrade-spring-boot-4.1` |

### Commits — Conventional Commits

Format : `type(scope): description courte`

```
feat(transaction): add multi-currency support
fix(budget): correct threshold evaluation on monthly rollover
refactor(auth): extract token validation to domain service
test(transaction): add Testcontainers integration tests
docs(arch): add ADR-003 database-per-service
chore(deps): upgrade Spring Boot to 4.1.0
```

### Cycle d'une tâche quotidienne

```bash
# 1. Créer une issue GitHub sur le board
# 2. Créer la branche depuis develop
git checkout develop && git pull
git checkout -b feature/ma-feature

# 3. Développer (TDD : test d'abord)
# 4. Committer régulièrement
git add .
git commit -m "feat(service): description"

# 5. Pousser et ouvrir une PR vers develop
git push -u origin feature/ma-feature
# → Ouvrir la PR sur GitHub, remplir le template

# 6. Vérifier que la CI passe
# 7. Merger (squash) avec un message Conventional Commit
# 8. Supprimer la branche
```

## Standards de code

### Clean Architecture (obligatoire dans chaque service)

```
src/main/java/com/fintrack/{service}/
├── domain/          ← Entités, Value Objects, Events, Ports (interfaces)
│                      Aucune dépendance Spring ici
├── application/     ← Use Cases, Services applicatifs
│                      Dépend uniquement de domain/
├── infrastructure/  ← JPA, Kafka, HTTP clients, implémentations des ports
│                      Dépend de domain/ et application/
└── api/             ← Controllers REST, DTOs, mappers
                       Point d'entrée HTTP
```

**Règle absolue :** `domain/` n'importe rien de Spring, JPA, ou Kafka.

### Tests

- Écrire le test avant le code (TDD)
- Tests unitaires dans `domain/` et `application/` : pas de Spring context
- Tests d'intégration avec Testcontainers pour `infrastructure/`
- Coverage minimum : **80%**

```bash
# Tests unitaires uniquement
mvn test

# Tests + intégration + coverage
mvn verify

# Tests d'intégration uniquement
mvn verify -P integration-tests
```
