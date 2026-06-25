# ADR-004 — Monorepo over Polyrepo

**Date :** 2026-06-24  
**Status :** Accepted  
**Deciders :** Camel (Architect)

---

## Context

With 7 backend services + 1 Angular frontend, a choice must be made between a monorepo (all code in one Git repository) and a polyrepo (one repository per service).

---

## Decision

All services are hosted in a **single Git repository** (monorepo) with a Maven multi-module structure.

---

## Justification

For a solo developer or a small team, a polyrepo creates friction with zero benefit: cloning 8 repositories to run the project locally, coordinating version bumps across repos, managing 8 separate CI pipelines. The organizational overhead exists to support large teams with strict ownership boundaries — which is not our context.

A monorepo with Maven multi-module gives us:
- One `git clone`, one IDE window, one CI pipeline
- Shared parent POM for dependency version management across all services
- Atomic commits when a change touches multiple services (e.g. updating a shared event schema)
- A single GitHub repository that showcases the complete system

The boundaries between services are enforced by Maven module isolation and the `internal` package convention — not by repository separation.

---

## Consequences

- One GitHub repository: `github.com/{username}/fintrack`
- Maven parent POM at root manages all dependency versions
- Each service is a Maven module: `services/{service-name}/pom.xml`
- CI runs all service builds and tests in parallel on every PR
- Future: if the team grows and ownership boundaries require it, individual services can be extracted into separate repos (strangler fig pattern)
