## Description

<!-- Décris brièvement ce que cette PR fait et pourquoi -->

Closes #<!-- numéro de l'issue -->

## Type de changement

- [ ] `feat` — nouvelle fonctionnalité
- [ ] `fix` — correction de bug
- [ ] `refactor` — refactoring sans changement de comportement
- [ ] `test` — ajout ou correction de tests
- [ ] `docs` — documentation uniquement
- [ ] `chore` — build, dépendances, configuration

## Service(s) impacté(s)

- [ ] api-gateway
- [ ] auth-service
- [ ] transaction-service
- [ ] budget-service
- [ ] notification-service
- [ ] report-service
- [ ] exchange-service
- [ ] frontend (fintrack-web)
- [ ] infrastructure

## Checklist

- [ ] Le code suit la Clean Architecture du service concerné
- [ ] Les tests unitaires passent (`mvn test`)
- [ ] Les tests d'intégration passent (`mvn verify -P integration-tests`)
- [ ] La couverture JaCoCo est ≥ 80%
- [ ] Le Dockerfile build correctement
- [ ] Les variables d'environnement ajoutées sont documentées dans `.env.example`
- [ ] Si un event Kafka est modifié : le schéma est versionné (`"version": N`)

## Comment tester

<!-- Étapes pour reproduire ou tester ce changement localement -->

1.
2.
3.
