# Proposal

## Why

Development was remote-only ([ADR 0005](../../../../docs/decisions/0005-remote-only-dev-with-iam-auth.md)):
the app reached the dev Aurora PostgreSQL through an SSM tunnel with IAM database auth. That
path is slow, the tunnel drops, and it rules out working offline. The profile name `local` was
also ambiguous: it meant the remote database reached through a local tunnel.

This change was specified as a Kiro spec in PR [#6](https://github.com/umccr/orcanexus/pull/6)
(2026-11-16) and delivered in PR [#7](https://github.com/umccr/orcanexus/pull/7) (2026-11-17),
before the repository adopted OpenSpec ([ADR 0011](../../../../docs/decisions/0011-adopt-openspec.md)).
It was moved here on 2026-11-27 with no code changes. The original Kiro documents are kept as
they were: `requirements.md` (glossary and numbered acceptance criteria, cited by `tasks.md`
and `design.md`), `design.md` and `tasks.md`. The spec deltas under `specs/` restate the
lasting behaviour in OpenSpec form, checked against the code on `main` at b01402d. Delivery-only
criteria (documentation, quality gates, the rename search) stay in `requirements.md`.

## What Changes

- A local PostgreSQL in Docker Compose, filled with deterministic synthetic data.
- A dev-only Maven module, `orcanexus-mockdb`: a CLI (`schema`, `seed`, `reset`, `up`) that
  derives the schema from the committed jOOQ model and seeds it. No runtime module depends on it.
- A `mockdb` Spring profile; the default profile `local` is renamed `tunnel`.
- `dx.sh` helpers `nexus_docker_up` and `nexus_docker_down`.
- Decision recorded as [ADR 0010](../../../../docs/decisions/0010-local-mock-database.md), superseding 0005.

## Capabilities

### New Capabilities

- `dev-profiles`: which database each Spring profile connects to (`tunnel` default, `mockdb`).
- `mockdb`: the local mock database: Compose stack, the `orcanexus-mockdb` tool, the seeded
  data, and the `dx.sh` helpers.

### Modified Capabilities

None.

## Impact

- Code: new module `orcanexus-mockdb`; `application-mockdb.yaml`, `application-tunnel.yaml`
  (renamed from `application-local.yaml`), `application.yaml`; `dx.sh`; `.env.example`.
- Developers whose `.env` or IDE run configuration selects `local` must switch to `tunnel`.
- Docs: [dev/offline-mockdb.md](../../../../docs/dev/offline-mockdb.md),
  [architecture/README.md](../../../../docs/architecture/README.md).
