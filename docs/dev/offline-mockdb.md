
# Offline mock database

A way to run OrcaNexus without AWS: a local PostgreSQL in Docker Compose, filled
with deterministic synthetic data, selected through the Spring profile `mockdb`. The remote dev
database over the SSM tunnel stays available ([remote-tunnel](remote-tunnel.md)). Decision and
trade-offs: [ADR 0010](../adr/0010-local-mock-database.md).

All data is fictional. Never use it as a stand-in for real warehouse content.

## Prerequisites

- Docker with Compose (`docker compose`), running.
- Java 21 and Maven, as in [setup](setup.md#tools).
- Nothing from AWS: no session, no tunnel, no credentials.

## Start the Compose_Stack

```
source dx.sh
nexus_docker_up     # starts PostgreSQL (waits up to 120 s), then resets and seeds it
nexus_docker_down   # stops and removes the container, keeps the data
```

- The database listens on `127.0.0.1` only, default port 5433.
- Data lives in a named Docker volume. Compose prefixes the project name, so the real volume
  name is `orcanexus-mockdb_orcanexus-mockdb-data`.
- `nexus_docker_down` never deletes the volume. To wipe the data on purpose:

  ```
  docker compose -f orcanexus-mockdb/compose.yaml down -v
  ```

## Run the Mockdb_Tool

`nexus_docker_up` builds the module and runs the `up` subcommand once, with the tool defaults
(scale 100, seed 42) and no arguments. Each run resets and reseeds the data. The data is
deterministic, so the same rows come back; it takes about a second at the default scale.

For another scale or seed, run the jar directly (after the stack is up):

```
mvn -q -DskipTests -pl orcanexus-mockdb -am package
java -jar orcanexus-mockdb/target/orcanexus-mockdb-1.0-SNAPSHOT.jar up --scale 1000 --seed 7
```

| Subcommand | What it does                                                                 |
|------------|------------------------------------------------------------------------------|
| `schema`   | Creates the schema and tables if they do not exist. Safe to repeat.          |
| `seed`     | Inserts synthetic rows. Refuses if any table already has rows.               |
| `reset`    | Drops the schema and recreates it empty.                                     |
| `up`       | `reset`, then `schema`, then `seed`. This is what `nexus_docker_up` runs.    |

- `--scale N` is the number of libraries, 1 to 100000 (default 100). `--seed N` is 0 or more
  (default 42). Both apply to `seed` and `up` only.
- Deterministic: the same scale and seed always produce the same rows. Scale alone fixes the row
  counts.
- DDL is built from the committed jOOQ model, so the tables match the generated code.
- Localhost-only guard: the tool refuses any host other than `localhost`, `127.0.0.1` or `::1`
  before it connects, so it cannot reset a remote database.

Other invocations of the jar:

```
java -jar orcanexus-mockdb/target/orcanexus-mockdb-1.0-SNAPSHOT.jar --help
java -jar orcanexus-mockdb/target/orcanexus-mockdb-1.0-SNAPSHOT.jar up --scale 100 --seed 42
```

Exit codes: 0 success, 1 runtime failure, 2 usage error.

## Start the Application with the Mockdb_Profile

```
export SPRING_PROFILES_ACTIVE=mockdb
export SERVER_PORT=8081      # any free port
mvn -DskipTests package
java -jar orcanexus-application/target/orcanexus-application-1.0-SNAPSHOT.jar
```

- The profile uses a plain JDBC connection with the `MOCKDB_*` variables. No AWS session, IAM
  token or tunnel is involved.
- Unreachable database: the application fails at startup (a one-statement check runs against
  the database). Start the stack and retry.
- Reachable database but no tables: the application starts, and data requests return a 5xx.
  Run `nexus_docker_up`.
- Check it with Swagger UI at `http://localhost:<SERVER_PORT>/swagger-ui/index.html`, or the
  OpenAPI document at `/v3/api-docs`. Endpoints and parameters are listed there, not here. A
  `200` from the system info endpoint only proves startup; call a data endpoint to prove the
  schema ([jooq.md](../../.kiro/steering/jooq.md) has the same advice).

## Environment variables

| Variable          | Default            | Used by                 |
|-------------------|--------------------|-------------------------|
| `MOCKDB_HOST`     | `127.0.0.1`        | tool, application       |
| `MOCKDB_PORT`     | `5433`             | Compose, tool, application |
| `MOCKDB_NAME`     | `orcanexus`        | Compose, tool, application |
| `MOCKDB_USER`     | `orcanexus`        | Compose, tool, application |
| `MOCKDB_PASSWORD` | a local-only default | Compose, tool, application |

The Compose file always binds to `127.0.0.1`, so `MOCKDB_HOST` only affects the tool and the
application. The tunnel variables (`DB_*`) are ignored on the mockdb path. Compose reads
variables from the shell, not from the repository `.env`.

## Profile rename: `local` is now `tunnel`

The default profile used to be called `local`. It is renamed `tunnel`, because each profile is
now named after how it reaches its data:

| Profile  | Data source                                          |
|----------|------------------------------------------------------|
| `tunnel` | Default. The remote dev database over the SSM tunnel. |
| `mockdb` | The local Compose database with synthetic data.      |

`local` is retired.

> **WARNING:** update any `.env` file or IDE run configuration that still selects `local`. A
> stale name behaves like an unknown profile: only the base configuration applies. Observed
> result: the application starts, data requests fail, and the environment label in the system
> info shows the old name.

## Seed volume and timing

| Scale  | Rows (all tables) | Time                                   |
|--------|-------------------|----------------------------------------|
| 100    | about 1100        | about 1 s                              |
| 100000 | about 1.1 million | about 17.5 s on the dev laptop         |

Measured once, indicative only.

## Query the data with DuckDB

Any SQL client works. With the [DuckDB CLI](https://duckdb.org/docs/installation/) and its
`postgres` extension (a one-time `INSTALL` needs network access), attach the database read-only
and query it. The connection string below uses the default `MOCKDB_*` values; change it if you
overrode any of them.

```
duckdb
```

```sql
INSTALL postgres; LOAD postgres;
ATTACH 'host=127.0.0.1 port=5433 dbname=orcanexus user=orcanexus password=mockdb-local-only' AS mock (TYPE postgres, READ_ONLY); -- pragma: allowlist secret
SELECT * FROM mock.mart.lims LIMIT 10;
SELECT count(*), count(DISTINCT library_id) FROM mock.mart.lims;
SELECT type, count(*) FROM mock.mart.lims GROUP BY ALL ORDER BY 2 DESC;
```

The same works as a one-off command: `duckdb -c "INSTALL postgres; LOAD postgres; ATTACH '<connection string>' AS mock (TYPE postgres, READ_ONLY); SELECT count(*) FROM mock.mart.lims;"`.

## Paging and sort demo tips

- The default scale of 100 gives several pages at the default page size. Scale 11 or more is
  enough for two pages.
- Sort ties and null values exist on purpose, so ascending and descending sorts give different
  first rows and page boundaries are visible. Sort field rules: [API conventions](../api/conventions.md).
- Same seed, same data: use it to reproduce a bug report. Change `--seed` to get different
  values with the same row counts.

## Troubleshooting

| Symptom | Cause | Resolution |
|---------|-------|------------|
| `nexus_docker_up` fails with a port-binding error on 5433 | Another process uses the port | `export MOCKDB_PORT=<free port>`, then `nexus_docker_up`. Use the same value for the tool and the application. |
| `seed` prints `tables are not empty` | Data already exists | Use `up` or `reset` (they wipe and reload). |
| Application fails at startup with connection refused | Stack not started, or `MOCKDB_PORT` differs from the stack | Run `nexus_docker_up`. Make sure the variable matches. |
| A data endpoint returns 500 | Tables are missing | Run `nexus_docker_up`. |
| Application behaves as before the rename, or `local` appears in the environment label | A stale `application-local.yaml` remains in `target/`, or `local` is still selected | Run `mvn clean package`. Change the profile name (see the rename section). |
| Data disappears after a restart, and `docker volume ls` shows an anonymous volume | A volume was mounted at `/var/lib/postgresql/data`. PostgreSQL 18 uses a versioned directory under `/var/lib/postgresql` | Mount the volume at `/var/lib/postgresql`, as `orcanexus-mockdb/compose.yaml` does. |
| `Cannot connect to the Docker daemon` | Docker is not running | Start Docker Desktop or the Docker service, then retry. |
