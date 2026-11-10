---
inclusion: always
---
# Documentation

Human docs live in `docs/`. Placement rules and the index are in `docs/README.md`; read
them before adding documentation.

- Do not grow `README.md` (about one screen). Add content under `docs/` instead.
- Do not copy endpoint paths, params or schemas into markdown; OpenAPI is the source.
- Shared API behaviour goes in `docs/api/conventions.md`, recipes in `docs/dev/`, current
  structure in `docs/architecture/`.
- A real decision becomes an ADR in `docs/adr/` (template: `docs/adr/0000-template.md`).
  Keep the bar high: see "When to write an ADR" in `docs/adr/README.md` and do not add one
  for feature-level details. Never edit an accepted ADR; add a new one that supersedes it,
  and add it to the index with its status.
- When a spec is delivered, promote its lasting outcomes to `docs/` and ADRs.
- Steering files stay short and agent-specific. Link to `docs/` rather than duplicating prose.
