# Implementation Plan

## Overview

Add an optional offline development path: a local PostgreSQL in Docker Compose, a dev-only
`orcanexus-mockdb` CLI that builds the schema from the jOOQ model and seeds deterministic
synthetic data, a `mockdb` Spring profile, and the rename of profile `local` to `tunnel`.

## Tasks

Each task ends with `mvn -DskipTests compile` passing. Formatting is done with
`mvn spotless:apply`, never by hand. Run `mvn verify` at the end of the final task. Do not read
`.env` and ignore `.local/`. Do not edit the generated jOOQ model. Tests are required sub-tasks
in this spec, as in `spring-pagination`.

- [x] 1. Verification spike: resolve the open points before building on them
  - [x] 1.1 Compose file and DDL against a real PostgreSQL
    - Create `orcanexus-mockdb/compose.yaml` with `postgres:18.6`, named volume
      `orcanexus-mockdb-data` mounted at `/var/lib/postgresql` (not `/var/lib/postgresql/data`,
      which silently creates an anonymous volume on PostgreSQL 18), loopback-only port
      `${MOCKDB_PORT:-5433}`, TCP healthcheck (5s interval, 5s timeout, 10 retries)
    - Start it, render the DDL with `dsl.ddl(Mart.MART, ...)` using the `SCHEMA` and `TABLE` flags
      plus `createSchemaIfNotExists`, `createTableIfNotExists` and `inlinePrimaryKeyConstraints`
      (dialect `POSTGRES`), and execute it in one transaction twice to confirm validity and
      idempotence
    - Confirm `up --wait` leaves the container running on timeout, a busy port fails with a
      port-binding error, `down` without `-v` keeps data, and that Compose does not load the
      repository `.env`
    - Verify persistence: seed a few rows, then run `docker compose stop` and `start`, and also
      `down` and `up` without `-v`; row counts must be identical after each. Check with
      `docker volume ls` and `docker inspect` that no anonymous volume was created and that the
      named volume holds the data
    - If PostgreSQL rejects a statement, apply the design's fallback ladder
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.7, 1.8, 1.9, 5.1, 5.4, 5.6, 5.10, 10.2_
  - [x] 1.2 Confirm the image tag `postgres:18.6` exists and pulls
    - Run `docker pull postgres:18.6` (or `docker manifest inspect`) and confirm success; the
      version is the user's decision and needs no Aurora comparison
    - Record the Aurora major version in the recipe only if the user states it (not required)
    - _Requirements: 1.1_
  - [x] 1.3 Confirm the Boot-repackaged CLI jar and Datafaker loading
    - In a throwaway module (or the future module skeleton), build a repackaged jar with
      `spring-boot-maven-plugin`, `mainClass` set, and Datafaker 2.7.0
    - Run `java -jar` once: the PostgreSQL driver registers, a seeded `Faker` produces output, and
      `dependency:tree` shows no version conflicts
    - Fallback: `maven-shade-plugin` with the service-file transformer
    - _Requirements: 3.2, 3.5, 4.1, 4.2_
  - [x] 1.4 Confirm Spring startup behaviour under a `mockdb` profile
    - With a temporary `application-mockdb.yaml` and a one-statement `select 1` script through
      `spring.sql.init`, start the application: it must fail with a database connection error when
      the database is down and start when it is up
    - Start with a deliberately wrong `DB_HOST` and `DB_USER` exported and confirm the unresolved
      `${DB_HOST}` in the base datasource does not break startup and is not used
    - Fallback: a `@Profile("mockdb")` check bean in `orcanexus-aurora` configuration
    - _Requirements: 2.1, 2.2, 2.3, 2.11, 2.12, 11.7_
  - [x] 1.5 Record outcomes
    - Write each outcome (confirmed or fallback used) to the Decisions section of `design.md`
      only where it changes something; otherwise note "confirmed" in the task notes
    - Run `mvn -DskipTests compile`
    - _Requirements: 1.1, 2.11, 5.6_

- [x] 2. Rename profile `local` to `tunnel`
  - [x] 2.1 Apply the rename
    - `git mv` `orcanexus-application/src/main/resources/application-local.yaml` to
      `application-tunnel.yaml`; set `on-profile: tunnel` and
      `orcanexus.system.environment: tunnel`; change nothing else in the file
    - Set `spring.profiles.default: tunnel` in `application.yaml`
    - Set `SPRING_PROFILES_ACTIVE=tunnel` in `.env.example` (edit only; do not read `.env`)
    - Update the default-profile line in `.kiro/steering/tech.md`
    - Do not touch the excluded files (delivered `spring-pagination` spec, this spec, `.local/`,
      accepted ADR bodies)
    - _Requirements: 2.5, 2.7, 14.1, 14.2, 14.3, 14.4, 14.5, 14.12, 14.13_
  - [x] 2.2 Zero-match check and build
    - Run the `git grep` command from the design's rename section; it must print nothing
    - Review a looser `git grep -nIw local` pass by hand for profile lists
    - Run `mvn -DskipTests compile` and the existing tests of `orcanexus-application`
    - _Requirements: 14.6, 14.7, 14.8, 14.10, 14.11_

- [x] 3. Mockdb profile and Compose stack
  - [x] 3.1 Finalise `orcanexus-mockdb/compose.yaml`
    - Match the design table: project name, `container_name`, env defaults from `MOCKDB_*`,
      `# pragma: allowlist secret` on the default password, volume `orcanexus-mockdb-data`
    - Leave `.local/dev/compose.yml` and the root `Makefile` untouched
    - Verify with `docker compose -f orcanexus-mockdb/compose.yaml config`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_
  - [x] 3.2 Add `application-mockdb.yaml` and the startup check
    - Create `application-mockdb.yaml` as in the design: plain `jdbc:postgresql` URL,
      `org.postgresql.Driver`, `MOCKDB_*` placeholders with defaults, Hikari settings,
      `spring.sql.init` pointing at `classpath:mockdb/startup-check.sql`, actuator exposure and
      `devtools` parity with the tunnel profile, `orcanexus.system.environment: mockdb`, paging
      settings
    - Create `mockdb/startup-check.sql` containing only `select 1` (no schema or table name)
    - Add no custom `orcanexus.*` property and leave the AWS wrapper dependency in place
    - Run `mvn -DskipTests compile`
    - _Requirements: 2.1, 2.2, 2.4, 2.6, 2.8, 2.9, 2.10, 2.11, 2.12, 12.12_

- [x] 4. Module `orcanexus-mockdb` skeleton
  - [x] 4.1 Create the module and register it
    - Add `<module>orcanexus-mockdb</module>` after `orcanexus-domain` in the parent `pom.xml`
    - Module POM: parent `dev.ccgcm.platform:orcanexus`, `datafaker.version` 2.7.0, dependencies
      on `orcanexus-jooq`, `org.postgresql:postgresql` (Boot-managed) and Datafaker; test
      dependencies JUnit Jupiter and AssertJ; `spring-boot-maven-plugin` with
      `mainClass` `dev.ccgcm.platform.orcanexus.mockdb.MockdbMain`
    - Add a minimal `MockdbMain` that prints usage
    - No dependency on `domain`, `rest`, `aurora` or `application`, and none of them depends on it
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 3.6, 3.9_
  - [x] 4.2 Quality gates on the skeleton
    - Run `mvn spotless:apply` and `mvn -pl orcanexus-mockdb -am verify`
    - Run the dependency checks from the design's build-boundary table
    - Confirm the application jar contains no `mockdb` or `net/datafaker` entries
    - _Requirements: 3.3, 3.4, 3.7, 3.8, 13.1, 13.8_

- [x] 5. CLI: arguments, configuration, host guard, connector
  - [x] 5.1 Argument parsing
    - Implement `Command`, `Invocation` (defaults scale 100, seed 42), `ArgParser`,
      `UsageException` and the `Output` interface, without a CLI framework
    - Enforce: four subcommands, `--scale` 1..100000, `--seed` >= 0, options only on `seed` and
      `up`, no duplicates or missing values, `--help` exit 0, usage exit 2
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 6.3, 6.8_
  - [x] 5.2 Configuration, host guard and connector
    - `MockdbConfig.fromEnv` reads only `MOCKDB_*` with defaults; `toString` omits the password
    - `HostGuard.require` accepts exactly `localhost`, `127.0.0.1`, `::1`
    - `Connector` interface and `Connections` (explicit driver registration, 10 s timeouts,
      `reWriteBatchedInserts=true`, credentials passed through `Properties`, friendly messages for
      unreachable host and bad credentials)
    - _Requirements: 4.8, 4.10, 4.12, 9.6, 9.7, 9.8_
  - [x] 5.3 `MockdbCli.run` orchestration
    - `run(args, env, Output, Connector)`: parse, config, guard, connect, dispatch; exit codes 0,
      1 and 2; `System.exit` only in `MockdbMain`
    - Commands dispatch to placeholders until tasks 6, 12 and 13 fill them in
    - _Requirements: 4.3, 4.9, 4.10, 4.11, 9.6_
  - [x] 5.4 Unit tests for the CLI
    - `ArgParserTest` including boundary values `0`, `1`, `100000`, `100001`, `-1`, `abc`
    - `MockdbConfigTest` and `HostGuardTest` with a recording `Connector` that must see zero calls
      on every rejected input
    - **Property 12: Argument parsing**
    - **Property 13: Configuration ignores the tunnel variables**
    - **Property 14: Host guard**
    - **Validates: Requirements 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.12, 6.8, 9.6, 9.7, 13.4**

- [x] 6. Schema service
  - [x] 6.1 Implement `SchemaService`
    - Build queries from `Mart.MART` with `dsl.ddl` and the design's flags; run them in one
      transaction; on `DataAccessException` print the failing statement and the database error to
      standard error and return 1
    - `reset`: drop the model's schema with cascade, then recreate, in one transaction
    - Read the schema name from the model; hardcode no schema name in Java or comments
    - Fill in the `schema` and `reset` dispatch in `MockdbCli`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.10, 9.1, 12.12_
  - [x] 6.2 DB-free tests
    - Render the DDL, interpret it with `dsl.meta(String...)` and compare name, type, length,
      precision, scale and nullability with the model
    - Assert every statement is `create ... if not exists` and none is `drop`, `truncate` or `alter`
    - **Property 9: Schema fidelity round trip** (DB-free half)
    - **Property 10: DDL statements are idempotent in shape**
    - **Validates: Requirements 5.3, 5.4, 5.8, 5.9, 5.10**

- [x] 7. Seed engine core
  - [x] 7.1 `SeedContext`, `Pool`, `KeyPools`, `SeedPlan`
    - Per-stream `Random` through the SplitMix64 mix of the Seed_Value and the stream name;
      `Faker` built from that `Random`; fixed date range 2022-01-01 to 2025-12-31
    - Pool sizes and id shapes as in the design (`L2400001`-style library ids, unique by index);
      compact arrays in `SeedPlan`; row-count formulas with `MIN_ROWS = 4`
    - No clock, `UUID.randomUUID`, hash-ordered iteration or parallel streams
    - _Requirements: 6.2, 6.3, 6.5, 6.6, 6.7, 6.11, 6.12, 6.13, 7.1_
  - [x] 7.2 `RelationshipSpec` and validation
    - Table to column to `Pool` map as in the design; `validate(Schema)` throws
      `SpecMismatchException(table, column)`; `SeedNote` record
    - _Requirements: 7.1, 7.2, 7.7, 7.8_
  - [x] 7.3 Tests for the spec
    - Every table and column in the shipped spec exists in `Mart.MART`; a spec naming a missing
      column fails with a message containing both names
    - **Property 11: Relationship_Spec matches the model**
    - **Validates: Requirements 7.2, 7.7, 13.6**

- [x] 8. Null policy and boundary helpers
  - [x] 8.1 `NullPlan` and `Boundaries`
    - Null counts per the design formula (`k = 1` below 10 rows, otherwise 10% to 50%), chosen by
      partial Fisher-Yates over eligible indexes, returned as a `BitSet`
    - Never null for pool columns or non-nullable columns
    - Boundary values read precision and scale from the model; skip with a `SeedNote` when the
      table has too few rows
    - _Requirements: 8.1, 8.2, 8.6, 8.7_
  - [x] 8.2 Unit tests for the helpers
    - Null counts and protected rows; boundary maxima for `NUMERIC(12,4)`, `NUMERIC(18,2)` and
      BIGINT; skip note for tiny tables
    - _Requirements: 8.1, 8.2, 8.6, 8.7_

- [x] 9. Seeders for `lims` and `workflow`
  - [x] 9.1 `TableSeeder` interface and `LimsSeeder`
    - Pool columns, `MOCK`-prefixed free ids, vocabularies, run date inheritance, tie rows in
      `type` for `L >= 5`, unique `library_id`; every field set explicitly so batches stay uniform
    - _Requirements: 6.1, 6.6, 6.7, 6.10, 6.11, 6.12, 7.1, 8.1, 8.3, 8.4, 8.5_
  - [x] 9.2 `WorkflowSeeder`
    - Portal run ids from the plan, library link `j mod L`, `workflow_end = start + duration`,
      `NUMERIC(12,4)` duration, `mock:` comment text via Datafaker lorem
    - _Requirements: 6.1, 6.10, 6.12, 7.1, 7.3, 7.6_
  - [x] 9.3 Tests for `lims`
    - **Property 3: Library ids are distinct and well formed** (grid plus scale 100000, counted
      lazily without storing rows)
    - **Property 7: Paging and sort shape of `lims`**
    - **Validates: Requirements 6.6, 6.7, 6.11, 8.3, 8.4, 8.5**

- [x] 10. Seeders for `fastq`, `bam`, `vcf`
  - [x] 10.1 Implement `FastqSeeder`, `BamSeeder`, `VcfSeeder`
    - Library and portal run ids from the workflow plan, run columns copied from the library plan
      for `fastq`, `key` as a free column, `e_tag` via `regexify`, `NUMERIC` and BIGINT fit
    - _Requirements: 6.1, 6.10, 7.1, 7.3, 7.4, 7.5, 7.6_

- [x] 11. Seeders for output tables and the generic fallback
  - [x] 11.1 `WorkflowOutputSeeder`, `WorkflowCostReportSeeder`, `GenericSeeder`
    - Child rows take `j mod W`; cost columns use `NUMERIC(18,2)` with boundary rows
    - `GenericSeeder` produces type-valid values for any table or column without a dedicated
      strategy and emits one `NO_SPEC` note per table
    - _Requirements: 6.1, 6.10, 7.4, 7.6, 7.8_

- [x] 12. `SeedRunner`
  - [x] 12.1 Implement the runner
    - Validate the spec, check that every relation is empty (name non-empty ones and return 1;
      message "run `schema` first" on `42P01`), lazy iterators flushed in batches of 1000 with
      `dsl.batchInsert`, one transaction, row counts and notes returned
    - Fill in the `seed` dispatch and the summary output
    - _Requirements: 4.9, 4.11, 6.1, 6.9, 7.7, 7.8, 8.7, 9.5_
  - [x] 12.2 Generation property tests
    - Detached jOOQ records (`DSL.using(SQLDialect.POSTGRES)`), grid of seeds `0, 1, 42, 2024,
      2147483647` and scales `1, 2, 3, 4, 5, 9, 10, 11, 100, 1000`, as parameterised JUnit 5 tests
    - **Property 1: Determinism**
    - **Property 2: Row counts depend only on Scale**
    - **Property 4: Key pool membership and referential consistency**
    - **Property 5: Null policy**
    - **Property 6: Boundary rows**
    - **Property 8: Generated values fit the model**
    - **Validates: Requirements 6.1, 6.3, 6.4, 6.5, 6.10, 7.1, 7.3, 7.4, 7.5, 7.6, 8.1, 8.2, 8.6, 8.7, 13.5**

- [x] 13. Wire `up` and finish command dispatch
  - [x] 13.1 `up` orchestration
    - `up` runs reset, schema, seed as three steps, each in its own transaction; a failure names
      the step and skips the rest; each command prints its summary
    - _Requirements: 4.9, 4.11, 9.1, 9.2, 9.3_
  - [x] 13.2 Orchestration tests
    - With fake steps: order reset, schema, seed; stop and name the failed step; no connection when
      the host guard or the parser rejects input
    - _Requirements: 9.2, 9.3, 9.6, 13.4_

- [x] 14. Checkpoint - Ensure all tests pass
  - Run `mvn spotless:apply` and `mvn verify` for the whole build, on a machine path without a
    reachable database. Ensure all tests pass, ask the user if questions arise.

- [x] 15. Database-backed tests behind `mockdb-it`
  - [x] 15.1 Harness and Maven profile
    - Add profile `mockdb-it` to the module POM that sets `mockdb.it=true` for Surefire; tests carry
      `@EnabledIfSystemProperty(named = "mockdb.it", matches = "true")` so they are reported as
      skipped by default
    - Harness creates and drops the scratch database `MOCKDB_IT_NAME` (default `orcanexus_it`)
      through the maintenance database and passes through `HostGuard`
    - _Requirements: 13.2, 13.3_
  - [x] 15.2 DB-backed tests
    - DDL acceptance and rollback, idempotent `schema`, catalog fidelity, `reset`, not-empty
      refusal, the nine joins, connection-failure message against a closed local port (no database
      needed)
    - **Property 9: Schema fidelity round trip** (catalog half)
    - **Property 15: `up` is idempotent** for scales 1, 10 and 100
    - Run with `mvn -pl orcanexus-mockdb -Pmockdb-it verify` against the running Compose stack
    - **Validates: Requirements 4.10, 5.1, 5.4, 5.6, 5.9, 7.5, 7.6, 9.1, 9.4, 9.5**

- [x] 16. Dx helpers
  - [x] 16.1 Add `nexus_docker_up` and `nexus_docker_down` to `dx.sh`
    - Thin wrappers with cwd-relative paths; `nexus_docker_up` runs `up -d --wait --wait-timeout 120`
      with a failure message that leaves the container running, then packages and runs the jar
      with `up` once, using the tool defaults and no arguments (no argument parsing in the shell)
    - `nexus_docker_down` runs `down` without `-v`
    - Leave existing functions unchanged; never source `.env`
    - _Requirements: 9.8, 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

- [x] 17. Runtime verification
  - [x] 17.1 Application against the Local_Database
    - Run steps V1 to V10 from the design on a free `SERVER_PORT`: seeded start, wrong `DB_*`
      ignored, `/api/v1/lims` default page, `page=0&size=5` (5 records, 100 total, 20 pages), sort
      asc and desc, unknown sort field gives 400 with the usual problem-detail body, paging
      stability on the unique library id, unreachable database fails startup, empty database gives
      a 5xx and not an empty 200, restart keeps data, profile `local` is unknown
    - **Property 16: Paging stability (runtime)**
    - **Validates: Requirements 1.7, 2.3, 2.12, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8, 14.5**
  - [x] 17.2 Boundary checks
    - Run the build-boundary checks from the design: no `mockdb` or `datafaker` in the application
      dependency tree or jar, module dependencies limited as designed, `orcanexus-domain` has no
      compile dependencies, `orcanexus-rest` has no dependency on aurora or jooq
    - _Requirements: 3.2, 3.3, 3.4, 3.6_

- [x] 18. Documentation and steering
  - [x] 18.1 ADR and index
    - New `docs/adr/0010-local-mock-database.md` from `0000-template.md`: supersedes 0005,
      remote dev stays preferred, rename `local` to `tunnel` and `mockdb` with the reason,
      alternatives (static SQL dump, Testcontainers, hand-written DDL)
    - Change only the status line of ADR 0005 to `Superseded by` 0010; update the index rows
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 14.8, 14.9_
  - [x] 18.2 Recipe and architecture docs
    - `docs/dev/offline-mockdb.md` with prerequisites, Compose start, tool usage, application
      start, environment variables (tunnel variables ignored), warning to update `.env` and IDE
      run configurations that select `local`, and at least three troubleshooting rows; add measured
      seed time at scale 100000 from task 17
    - Link it from `docs/README.md`; update `docs/architecture/overview.md` (module table, diagram,
      "no runtime module depends on it", six modules, optional offline path)
    - No endpoint paths or schemas copied from OpenAPI; no concrete schema names
    - _Requirements: 12.5, 12.6, 12.7, 12.8, 12.11, 12.12, 12.13, 14.9_
  - [x] 18.3 Steering and README
    - Add at most 10 lines each to `structure.md`, `tech.md`, `jooq.md`, linking to `docs/`
    - Add one link to the recipe in `README.md`, keeping it at most 40 lines
    - _Requirements: 12.9, 12.10, 12.13_

- [x] 19. Final quality gates
  - [x] 19.1 Build gates
    - Run `mvn -DskipTests compile`, `mvn spotless:apply`, then `mvn verify`; fix findings without
      weakening tests; confirm pre-existing tests still run and pass, and that the DB-backed tests
      are reported as skipped
    - _Requirements: 3.7, 13.1, 13.2, 13.3, 13.7, 13.8, 14.11_
  - [x] 19.2 Final diff check
    - Re-run the zero-match `git grep`, check relative links in the new docs, confirm nothing
      under the generated jOOQ package changed, and review the final diff for accidental changes
    - Summarise assumptions, tests and known limitations
    - _Requirements: 12.13, 13.8, 14.6, 14.10_

## Notes

- Task 1 settles the open points (DDL execution, Postgres major, jar launch, startup behaviour)
  before later tasks depend on them
- Every top-level task ends with `mvn -DskipTests compile`; formatting only via `mvn spotless:apply`
- Property tests are parameterised JUnit 5 tests over a fixed seed and scale grid (no jqwik)
- DB-backed tests run only with `-Pmockdb-it` against a scratch database
- Tests are required sub-tasks, matching the `spring-pagination` convention

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "1.4"] },
    { "id": 2, "tasks": ["1.5"] },
    { "id": 3, "tasks": ["2.1", "4.1"] },
    { "id": 4, "tasks": ["2.2", "3.1", "4.2"] },
    { "id": 5, "tasks": ["3.2", "5.1", "5.2", "7.1"] },
    { "id": 6, "tasks": ["5.3", "6.1", "7.2", "8.1"] },
    { "id": 7, "tasks": ["5.4", "6.2", "7.3", "8.2", "9.1", "9.2"] },
    { "id": 8, "tasks": ["9.3", "10.1", "11.1"] },
    { "id": 9, "tasks": ["12.1"] },
    { "id": 10, "tasks": ["12.2", "13.1"] },
    { "id": 11, "tasks": ["13.2", "15.1"] },
    { "id": 12, "tasks": ["15.2", "16.1"] },
    { "id": 13, "tasks": ["17.1", "17.2"] },
    { "id": 14, "tasks": ["18.1", "18.2", "18.3"] },
    { "id": 15, "tasks": ["19.1"] },
    { "id": 16, "tasks": ["19.2"] }
  ]
}
```
