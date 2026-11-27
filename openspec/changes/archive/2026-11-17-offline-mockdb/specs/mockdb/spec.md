# Spec Delta

## Purpose

Developers can work without AWS access or the tunnel, against a local PostgreSQL filled with
deterministic, purely synthetic data. The local schema is derived from the committed jOOQ
model, so it does not drift from the code that queries it. The tooling never touches a remote
database and never ships in the application.

## ADDED Requirements

### Requirement: Local database via Docker Compose
The Compose stack SHALL run PostgreSQL from an image pinned to an explicit major.minor tag,
with data in a named volume mounted where that image version expects it. It SHALL publish the
database on loopback only, on host port `MOCKDB_PORT` (default `5433`, so it does not collide
with a tunnel on 5432), and take the database name, user and password from `MOCKDB_NAME`,
`MOCKDB_USER` and `MOCKDB_PASSWORD` with the local-only defaults.

#### Scenario: Data survives a restart
- **WHEN** the stack is stopped and started again, or brought down and up without removing volumes
- **THEN** the row count of every seeded table is unchanged

#### Scenario: Port in use
- **WHEN** the configured host port is already in use
- **THEN** the stack fails to start with a port-binding error and the volume is unchanged

### Requirement: Compose healthcheck
The Compose stack SHALL report healthy only when the database accepts TCP connections,
checking every 5 seconds with a 5-second timeout and 10 retries.

#### Scenario: First start
- **WHEN** the stack starts on a host where the volume does not exist yet
- **THEN** it creates an empty database with the configured name and user, and reports healthy once it accepts connections

### Requirement: Mock tooling stays out of the runtime
The `orcanexus-mockdb` module SHALL depend at compile or runtime scope only on `orcanexus-jooq`,
the PostgreSQL JDBC driver and Datafaker, each at an exact or Boot-managed version. No other
module SHALL depend on it, directly or transitively. The application jar SHALL contain no
`orcanexus-mockdb` classes and no `net.datafaker` classes.

#### Scenario: Application jar is clean
- **WHEN** `mvn -DskipTests package` completes
- **THEN** the application jar contains no class from `orcanexus-mockdb` or `net.datafaker`

#### Scenario: Build needs no Docker
- **WHEN** `mvn verify` runs on a machine with no Docker and no local database
- **THEN** the build succeeds, with database-backed tests reported as skipped

### Requirement: CLI commands and options
The tool SHALL provide the subcommands `schema`, `seed`, `reset` and `up`, parsed without a CLI
framework. `--scale N` (1 to 100000, default 100) and `--seed N` (0 or more, default 42) SHALL
be accepted on `seed` and `up` only. Exit status SHALL be 0 on success or `--help`, 1 on a
runtime failure and 2 on a usage error. On success the tool SHALL print a summary of the action,
including the row count per table for `seed` and `up`.

#### Scenario: Help
- **WHEN** the tool runs with `--help`
- **THEN** it prints the usage text listing every subcommand, option and variable, and exits 0

#### Scenario: Missing or unknown subcommand
- **WHEN** the tool runs with no subcommand, or with an unknown one
- **THEN** it prints the usage text and a message naming the problem, and exits 2

#### Scenario: Invalid option value
- **WHEN** the tool runs `seed --scale 0`
- **THEN** it prints a message naming `--scale` and the rejected value, exits 2, and makes no connection

#### Scenario: Option on the wrong subcommand
- **WHEN** the tool runs `reset --seed 1`
- **THEN** it prints a message naming the unsupported option, exits 2, and leaves the database unchanged

### Requirement: CLI connection settings
The tool SHALL read its connection from the `MOCKDB_*` variables with the same defaults as the
`mockdb` profile. It SHALL ignore the `DB_*` tunnel variables, SHALL NOT use AWS IAM
credentials, and SHALL NOT read `.env`.

#### Scenario: Database unreachable
- **WHEN** the database does not accept a connection within 10 seconds
- **THEN** the tool prints the host and port, suggests starting the Compose stack, and exits 1

### Requirement: Localhost only
Before `schema`, `seed`, `reset` or `up` connects, the tool SHALL require the target host to be
exactly `localhost`, `127.0.0.1` or `::1`. Otherwise it SHALL make no connection, print the
rejected host, and exit with a non-zero status.

#### Scenario: Remote host rejected
- **WHEN** `MOCKDB_HOST` is set to a remote endpoint and the tool runs `reset`
- **THEN** it refuses, names the host, makes no connection and exits non-zero

### Requirement: Schema derived from the jOOQ model
`schema` SHALL create schema `mart` and every relation of the committed jOOQ model as physical
tables, using DDL produced by jOOQ from the model, with the declared column names, types,
lengths, precision, scale and nullability. A regenerated model SHALL be reflected without a
source change in the tool.

#### Scenario: Fresh database
- **WHEN** `schema` runs against a database without schema `mart`
- **THEN** schema `mart` and all model relations exist, and their catalog columns match the model

### Requirement: Schema creation is idempotent and atomic
`schema` SHALL use IF NOT EXISTS style DDL in one transaction: existing objects and rows are
left unchanged, only missing relations are created, and no DROP, TRUNCATE or ALTER is executed.
If PostgreSQL rejects a statement, the tool SHALL print the statement and error, roll back the
whole run, and exit non-zero.

#### Scenario: Re-run is harmless
- **WHEN** `schema` runs against a database that already holds seeded relations
- **THEN** no object or row changes

### Requirement: Synthetic data only
`seed` SHALL insert at least one row into every model relation using batch inserts of jOOQ
records. Every value SHALL come from Datafaker or local deterministic logic, never from real
warehouse data or `.local/`, and SHALL be recognisably fictional. Values SHALL fit their column
types, including NUMERIC precision, BIGINT range and date types.

#### Scenario: Tables not empty
- **WHEN** `seed` runs and any relation already has rows
- **THEN** it names the non-empty relations, inserts nothing and exits non-zero

### Requirement: Deterministic seeding
The random generator SHALL be initialised from `--seed`. The same scale and seed SHALL give
identical rows; a different seed with the same scale SHALL give the same row count per table.
`lims` SHALL hold exactly `scale` distinct library ids of the form `L` + two-digit year +
five-digit sequence (e.g. `L2400001`).

#### Scenario: Reproducible seed
- **WHEN** two developers run `up --scale 50 --seed 7` on empty databases
- **THEN** both databases hold identical rows in every relation

#### Scenario: Default scale
- **WHEN** `seed` runs without `--scale`
- **THEN** `lims` holds exactly 100 distinct library ids

### Requirement: Cross-table consistency
Every seeded `library_id` in `fastq`, `bam`, `vcf` and `workflow` SHALL exist in `lims`, and
every seeded `portal_run_id` in `fastq`, `bam`, `vcf`, `workflow_output` and
`workflow_cost_report` SHALL exist in `workflow`. Key-pool columns (subjects, samples,
libraries, sequencing runs, portal run ids) SHALL never be null.

#### Scenario: Joins return rows
- **WHEN** `lims` is joined on `library_id` with each of `fastq`, `bam`, `vcf` and `workflow`, and `workflow` on `portal_run_id` with each artifact and output table
- **THEN** every join returns at least one row

### Requirement: Relationship spec
The tool SHALL hold a relationship spec stating, per relation, which columns draw from which
key pool. A spec naming a table or column missing from the model SHALL fail the run before any
insert. A relation with no spec SHALL be seeded with type-valid values and one warning naming
the table.

#### Scenario: Stale relationship spec
- **WHEN** the model is regenerated without a column that the relationship spec names
- **THEN** `seed` prints the table and column, inserts nothing and exits non-zero

### Requirement: Null and boundary values
Each nullable column that is not a key-pool column SHALL be null in 10% to 50% of a table's
rows (at least one row when the table has fewer than 10). Non-nullable columns SHALL never be
null. Each numeric and date column SHALL have a row at its minimum and at its maximum boundary
(zero and the largest value the precision allows for NUMERIC; earliest and latest seeded date
for dates). A boundary that does not fit SHALL be skipped and reported, without failing.

#### Scenario: Small scale
- **WHEN** `seed --scale 2` runs
- **THEN** boundary rows that do not fit in `lims` are reported as skipped and the run succeeds

### Requirement: Paging and sort data in lims
For `lims`, a scale of at least 11 SHALL give two or more pages at page size 10, a scale of at
least 5 SHALL give a sortable text column with repeated values, and a scale of at least 2 SHALL
give different first rows for ascending and descending sort.

#### Scenario: Stable paging over seeded data
- **WHEN** the database is seeded at scale 100 and a client requests pages 0 and 1 with `size=5` sorted by `library_id`
- **THEN** the two pages hold 10 distinct records, in order, with none skipped

### Requirement: Reset and bring-up
`reset` SHALL drop schema `mart` with cascade and recreate it with every relation empty.
`up` SHALL run `reset`, `schema` and `seed` in that order, stop at the first failing step and
name it. Running `up` twice with the same options SHALL leave identical rows.

#### Scenario: Known state
- **WHEN** `up` runs on a database with stale data
- **THEN** the database holds exactly the rows for the given scale and seed

### Requirement: Shell helpers
`dx.sh` SHALL provide `nexus_docker_up`, which starts the Compose stack, waits up to 120 seconds
for it to be healthy and then runs `up` with the tool defaults, and `nexus_docker_down`, which
stops the stack and keeps the data volume. If the stack is not healthy in time,
`nexus_docker_up` SHALL report it, leave the container running, skip `up` and return non-zero.
The helpers SHALL NOT parse scale or seed.

#### Scenario: One-command start
- **WHEN** a developer runs `nexus_docker_up` with Docker available
- **THEN** the local database is running and seeded at scale 100, seed 42
