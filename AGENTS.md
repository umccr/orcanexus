# Shared agent instructions

OrcaNexus is the UMCCR Warehouse Data API: a read-oriented Spring Boot (Java 21) REST service
over Aurora PostgreSQL, using jOOQ. Docs index: [docs/README.md](docs/README.md).

## Before implementation

- Read the files under `.kiro/steering/`. They hold the hard rules (module dependencies,
  generated jOOQ code, configuration conventions).
- Check `docs/` for architecture, API conventions and decisions (`docs/adr/`).
- Read the spec under `.kiro/specs/` for the feature in progress, if there is one. Delivered
  specs are history; `docs/` is the source of truth.
- Search the repository before creating new abstractions.
- Inspect related tests before changing behaviour.
- Do not read `.env`. Ignore `.local/`.

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
  ```

- Check the final diff for accidental changes.
- Summarise assumptions, tests and known limitations.
