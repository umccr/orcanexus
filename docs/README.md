# Documentation

Each fact lives in one place. Everything else links to it.

## Where things go

| Content                                  | Home                            | Rule                                          |
|------------------------------------------|---------------------------------|-----------------------------------------------|
| Endpoint paths, params, schemas          | OpenAPI / Swagger UI            | Generated. Never copied into markdown.        |
| Behaviour shared by all endpoints        | [api/](api/README.md)      | Written once, not per resource.               |
| Setup, tooling, conventions, recipes     | [dev/](dev/README.md)            | One topic per file.                           |
| How the system is built now              | [architecture/](architecture/README.md) | Living. Update when structure changes. |
| Why a decision was made                  | [decisions/](decisions/README.md)           | High bar, numbered, immutable. Supersede, never edit. |
| Current behaviour (requirements)         | `openspec/specs/`               | Living. Updated only by archiving a change.   |
| In-flight change: proposal, design, tasks | `openspec/changes/<name>/`     | Archived to `openspec/changes/archive/` when done. |
| Agent rules                              | `AGENTS.md`                     | Brief. Link here instead of duplicating. `CLAUDE.md` and `.kiro/steering/` are pointers only. |
| `README.md`                              | repo root                       | Landing page. Capped at 60 lines by pre-commit; maintainer-owned. Do not add content. |

## Promotion rule

Archiving an OpenSpec change merges its behaviour deltas into `openspec/specs/`. In the same
change, also:

- Move consumer-facing behaviour shared by endpoints to `docs/api/`.
- Update `docs/architecture/` if the structure changed.
- Record each real decision as an ADR in `docs/decisions/`.

The archived change folder is history and is no longer maintained.

## Index

- [api/README.md](api/README.md): pagination, sorting, errors
- [dev/README.md](dev/README.md): toolchain, build, lint, Makefile
- [dev/remote-tunnel.md](dev/remote-tunnel.md): remote dev database (AWS session, SSM tunnel, credentials, `tunnel` profile)
- [dev/offline-mockdb.md](dev/offline-mockdb.md): offline mock database (Compose, seed tool, `mockdb` profile)
- [dev/adding-a-resource.md](dev/adding-a-resource.md): the LIMS-slice recipe
- [dev/jooq.md](dev/jooq.md): jOOQ conventions: generated model, schema mapping, verifying DB-facing changes
- [dev/spring.md](dev/spring.md): Spring configuration conventions
- [dev/spec-driven.md](dev/spec-driven.md): spec-driven changes with OpenSpec, and the coding agents (Kiro, Claude Code)
- [architecture/README.md](architecture/README.md): stack, modules and dependency rules, flow, concepts
- [decisions/](decisions/README.md): decision log
