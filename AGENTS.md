# Shared agent instructions

OrcaNexus is the UMCCR Warehouse Data API: a read-oriented Spring Boot (Java 21) REST service
over Aurora PostgreSQL, using jOOQ. It serves curated warehouse content and is not the system
of record. Architecture: [docs/architecture/README.md](docs/architecture/README.md).
Docs index: [docs/README.md](docs/README.md).

This file is the single source of agent rules for all agents. Kiro loads it natively,
`CLAUDE.md` imports it, and `.kiro/steering/` holds pointers only. Use plain links here,
not `@` imports.

## Before implementation

- Read the OpenSpec change under `openspec/changes/<name>/` for the work in progress, if
  there is one, and the current behaviour in `openspec/specs/`.
- Check `docs/` for architecture, shared API behaviour
  ([docs/api/README.md](docs/api/README.md)) and decisions (`docs/decisions/`).
- Search the repository before creating new abstractions. A new resource follows the LIMS
  slice: [docs/dev/adding-a-resource.md](docs/dev/adding-a-resource.md).
- Inspect related tests before changing behaviour.

## Standards

- Respect files and directories whose names start with a dot (`.`); do not treat them as
  disposable or auto-generated unless proven so.
- Ask permission before reading `.env`, and never echo secret values.
- Ignore anything under `.local/` unless the user points you to a file there; it is local
  scratch, not part of the project.
- Warehouse data is clinical genomics metadata. Never copy query results, identifiers
  (subject, sample, library IDs) or other real records into code, fixtures, specs, docs,
  commit messages or PRs. Use synthetic examples.
- Do not bypass the pre-commit hooks or edit `.secrets.baseline` by hand
  (`make baseline` regenerates it).
- Work on a branch; the `no-commit-to-branch` hook blocks commits to `main`.

## Conventions

Read these before changing code. They are the hard rules for this repository:

- Module dependency rules:
  [docs/architecture/README.md](docs/architecture/README.md#dependency-rules)
- jOOQ (generated model, schema mapping, verifying DB-facing changes):
  [docs/dev/jooq.md](docs/dev/jooq.md)
- Spring configuration: [docs/dev/spring.md](docs/dev/spring.md)
- Build, lint and formatting: [docs/dev/README.md](docs/dev/README.md)

## Docs and specs

- Never add content to the root `README.md`. It is the landing page, capped at 60 lines by a
  pre-commit hook and owned by maintainers (`.github/CODEOWNERS`). New content goes under
  `docs/`, usually a new file in `docs/dev/`; see [docs/README.md](docs/README.md).
- Read [docs/README.md](docs/README.md) (where things go) before adding docs, and
  [docs/decisions/README.md](docs/decisions/README.md) before adding an ADR. Never edit an accepted ADR;
  supersede it.
- Do not copy endpoint paths, parameters or schemas into markdown; OpenAPI is the source.
- Changes are specified with OpenSpec in `openspec/changes/<name>/`; do not hand-edit
  `openspec/specs/` outside a change. How we use it:
  [docs/dev/spec-driven.md](docs/dev/spec-driven.md).
- When archiving a change, promote consumer-facing behaviour to `docs/api/`, structural
  outcomes to `docs/architecture/` and decisions to `docs/decisions/`.

## During implementation

- Follow the existing project structure and naming conventions.
- Keep changes narrowly scoped and do not change unrelated files.
- Do not weaken tests merely to make them pass.
- Do not hand-edit generated code or hand-format Java.

## Before completion

- Run the validation commands:

  ```bash
  mvn -DskipTests compile   # after every change
  mvn spotless:apply
  mvn verify                # tests + spotless + checkstyle + spotbugs
  make check                # pre-commit hooks
  ```

- For DB-facing changes, also verify at runtime:
  [docs/dev/jooq.md](docs/dev/jooq.md#verifying-db-facing-changes).
- Check the final diff for accidental changes.
- Summarise assumptions, tests and known limitations.
