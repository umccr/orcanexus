# 10. Local mock database

- Status: Accepted
- Date: 2026-11-16

## Context

[ADR 0005](0005-remote-only-dev-with-iam-auth.md) made development remote-only. That path
is slow, the tunnel drops, and it rules out working offline. The profile name `local` was
also ambiguous: it meant the remote dev database reached through a local tunnel, and it
could equally be read as a database on the developer's machine.

## Decision

This ADR supersedes 0005. Developers can run the application against either the remote dev
database reached through the tunnel, or a local mock database.

- Run PostgreSQL with Docker Compose.
- Derive the schema from the generated jOOQ model, and fill it with deterministic synthetic
  data. No warehouse data is copied.
- Put the tooling in a dev-only module, `orcanexus-mockdb`. No runtime module depends on it.
- Configure it with `MOCKDB_*` variables, separate from the tunnel `DB_*` variables. The
  tooling refuses any host other than localhost.
- Name the Spring profiles `mockdb` and `tunnel`, and retire `local`.

Usage is in [the recipe](../dev/offline-mockdb.md).

Alternatives considered:

- Static SQL dump: drifts from the model and adds review burden.
- Testcontainers: out of scope, needs Docker for the build and gives no persistent dev
  database.
- Hand-written DDL: drifts from the model.
- A mocked repository adapter instead of a database: does not exercise SQL or jOOQ.
- A copy of real warehouse data: data-handling risk, the reasoning in 0005.

## Consequences

- Offline development is possible.
- The mock path adds a module and a Docker prerequisite. The tunnel path needs neither.
- The tool must keep the mock schema and data in step with the model itself.
- The PostgreSQL major version in the compose file is pinned by hand.
