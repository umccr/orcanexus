# Documentation

Each fact lives in one place. Everything else links to it.

## Where things go

| Content                                  | Home                            | Rule                                          |
|------------------------------------------|---------------------------------|-----------------------------------------------|
| Endpoint paths, params, schemas          | OpenAPI / Swagger UI            | Generated. Never copied into markdown.        |
| Behaviour shared by all endpoints        | [api/](api/conventions.md)      | Written once, not per resource.               |
| Setup, tooling, how-to recipes           | [dev/](dev/setup.md)            | One topic per file.                           |
| How the system is built now              | [architecture/](architecture/overview.md) | Living. Update when structure changes. |
| Why a decision was made                  | [adr/](adr/README.md)           | High bar, numbered, immutable. Supersede, never edit. |
| Feature requirements, design, tasks     | `.kiro/specs/`                  | History once delivered.                       |
| Agent guidance                           | `.kiro/steering/`               | Brief. Link here instead of duplicating.      |
| `README.md`                              | repo root                       | About one screen.                             |

## Promotion rule

When a spec is delivered, move its lasting outcomes out of `.kiro/specs/`:

- Consumer-facing behaviour goes to `docs/api/`.
- Structure changes go to `docs/architecture/`.
- Each real decision becomes an ADR in `docs/adr/`.

The spec folder then stays as history and is no longer maintained.

## Index

- [api/conventions.md](api/conventions.md): pagination, sorting, errors
- [dev/setup.md](dev/setup.md): toolchain, AWS session, tunnel, lint, Makefile
- [dev/adding-a-resource.md](dev/adding-a-resource.md): the LIMS-slice recipe
- [dev/ai-engineering.md](dev/ai-engineering.md): Kiro notes
- [architecture/overview.md](architecture/overview.md): modules, flow, concepts
- [adr/](adr/README.md): decision log
