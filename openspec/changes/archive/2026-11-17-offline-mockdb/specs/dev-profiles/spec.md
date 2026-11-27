# Spec Delta

## Purpose

The application can run against the remote dev database through the SSM tunnel or against a
local mock database. Each Spring profile is named after how it reaches its database, so the
choice is unambiguous.

## ADDED Requirements

### Requirement: Tunnel is the default profile
When no Spring profile is selected, the application SHALL activate the `tunnel` profile. The
`tunnel` profile SHALL connect through the AWS Advanced JDBC Wrapper with IAM database auth to
`127.0.0.1` on `DB_PORT`, using `DB_HOST`, `DB_NAME` and `DB_USER`, and SHALL set
`orcanexus.system.environment` to `tunnel`.

#### Scenario: No profile selected
- **WHEN** the application starts with no profile selected
- **THEN** the `tunnel` settings apply and the system info reports environment `tunnel`

### Requirement: No local profile
The application SHALL define no profile named `local`, and SHALL provide no alias or fallback
from `local` to another profile.

#### Scenario: Retired profile selected
- **WHEN** the application starts with profile `local`
- **THEN** only the base configuration applies; neither the `tunnel` nor the `mockdb` settings take effect

### Requirement: Mockdb profile connects to the local database
The `mockdb` profile SHALL connect with a plain `jdbc:postgresql` URL, the
`org.postgresql.Driver` class and a static user and password. It SHALL read `MOCKDB_HOST`,
`MOCKDB_PORT`, `MOCKDB_NAME`, `MOCKDB_USER` and `MOCKDB_PASSWORD`, defaulting to `127.0.0.1`,
`5433`, `orcanexus`, `orcanexus` and a documented local-only password, and SHALL set
`orcanexus.system.environment` to `mockdb`. It SHALL add no custom `orcanexus.*` property for a
setting Spring Boot provides.

#### Scenario: Starts without AWS
- **WHEN** the application starts with profile `mockdb` against a running local database, with no AWS session and no tunnel
- **THEN** startup completes and the system info reports environment `mockdb`

### Requirement: Mockdb ignores the tunnel path
Under the `mockdb` profile the application SHALL ignore `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USER` and `DB_PASS`, and SHALL NOT use AWS IAM credentials for the connection.

#### Scenario: Tunnel variables have no effect
- **WHEN** `DB_HOST` and `DB_PORT` are set and the application starts with profile `mockdb`
- **THEN** it still connects to the `MOCKDB_*` target

### Requirement: Mockdb uses the generated schema name
Under the `mockdb` profile the application SHALL use schema `mart` without `DB_SCHEMA_INPUT` or
`DB_SCHEMA_OUTPUT` being set, and the schema mapping SHALL be an identity mapping.

#### Scenario: No schema variables
- **WHEN** the application runs with profile `mockdb` and no schema variables
- **THEN** queries render against schema `mart`

### Requirement: Mockdb startup fails fast
Under the `mockdb` profile, if the local database is unreachable or rejects the credentials,
the application SHALL fail startup with a non-zero exit status and a database connection error,
using a startup check that does not need the schema name. It SHALL NOT fall back to IAM auth.
If the database is reachable but the mart relations do not exist, a LIMS request SHALL return
an error status, not HTTP 200 with empty `content`.

#### Scenario: Database down
- **WHEN** the application starts with profile `mockdb` and no local database is running
- **THEN** startup fails with a non-zero exit status and a connection error

#### Scenario: Schema missing
- **WHEN** the local database is running but schema `mart` has no tables, and a client requests the LIMS list
- **THEN** the response has an error status
