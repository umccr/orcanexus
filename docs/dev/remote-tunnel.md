# Remote dev database over the tunnel

Run OrcaNexus against the remote dev Aurora PostgreSQL. You reach it through an SSM tunnel to a
small EC2 instance in a private subnet, and authenticate with IAM database auth (15-minute
tokens, read-only user, no static passwords). This is the Spring profile `tunnel`, the default.
Background: [ADR 0005](../adr/0005-remote-only-dev-with-iam-auth.md). To work without AWS, use
the [offline mock database](offline-mockdb.md) instead.

## Prerequisites

- The toolchain from [setup](setup.md#tools), including `aws-cli`.
- Access to the OrcaNexus dev AWS account.

## AWS session

You need an authenticated AWS session. Use your usual AWS CLI setup and manage profiles however
you like. Any profile or command shown in the docs is only an example; it signals that you must
be authenticated at that point.

```
export AWS_PROFILE=unimelb-warehouse-prod-poweruser
aws sso login
```

The project uses the [AWS SDK for Java](https://aws.amazon.com/sdk-for-java/), which loads
credentials through the default
[credential chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html).

## Tunnel and credentials

Source the dx script. It exports shell functions (`nexus_*` shortcuts) for the remote dev
environment.

```
source dx.sh
```

Make the key and tunnel:

```
nexus_rule
nexus_key
nexus_tunnel
nexus_status
nexus_nc
```

Make the credentials:

```
nexus_env
nexus_cred
```

- `nexus_key` fetches the SSH key from AWS SSM into `~/.ssh/orcahouse-mgmt`.
- `nexus_tunnel` forwards `127.0.0.1:5432` to the Aurora reader endpoint. `nexus_tunnel_fg`
  keeps it in the foreground, and `nexus_stop` ends it.
- `nexus_status` and `nexus_nc` check that the tunnel is up.
- `nexus_env` writes the repository `.env` from AWS SSM. `nexus_cred` loads it into your shell
  as `AWS_REGION` and the `DB_*` variables. `.env` is git-ignored: never commit it.
- You do not need to export `DB_PASS`. The AWS JDBC wrapper requests and refreshes the IAM token
  itself.

## Run the application

The `tunnel` profile is the default, so no profile needs selecting.

```
mvn -DskipTests package
java -jar orcanexus-application/target/orcanexus-application-1.0-SNAPSHOT.jar
```

`SERVER_PORT` overrides the HTTP port. Swagger UI and the OpenAPI document list the endpoints
(see [setup](setup.md#build-and-run)). A `200` from the system info endpoint only proves
startup; call a data endpoint to prove the database connection.

## Environment variables

| Variable     | Source                         | Used for                                      |
|--------------|--------------------------------|-----------------------------------------------|
| `AWS_REGION` | `.env` through `nexus_cred`    | IAM token region                              |
| `DB_HOST`    | `.env` through `nexus_cred`    | Aurora endpoint, used for IAM signing         |
| `DB_NAME`    | `.env` through `nexus_cred`    | Database name                                 |
| `DB_USER`    | `.env` through `nexus_cred`    | IAM database user                             |
| `DB_PORT`    | `nexus_cred`, default `5432`   | Local end of the tunnel                       |

The mock database uses separate `MOCKDB_*` variables, and the `tunnel` profile ignores them
([offline-mockdb.md](offline-mockdb.md#environment-variables)).

## Profile `local` is now `tunnel`

The default profile used to be called `local`. Update any `.env` or IDE run configuration that
still selects `local` (see [the rename](offline-mockdb.md#profile-rename-local-is-now-tunnel)).

## Troubleshooting

| Symptom | Cause | Resolution |
|---------|-------|------------|
| `nexus_nc` fails, or the application cannot connect | The tunnel is not running | `nexus_status`, then `nexus_tunnel`. Check your AWS session is still valid. |
| The application starts but data endpoints return 500, and the log shows an unresolved `${DB_PORT}` or `${DB_NAME}` | `DB_*` variables are not set in the shell | Run `nexus_env` and `nexus_cred` in the same shell that starts the application. |
| `nexus_key`, `nexus_tunnel` or `nexus_env` fail with an authentication error | The AWS session expired | `aws sso login`, then retry. |
| Port 5432 is already in use when opening the tunnel | Another process, or an old tunnel, holds the port | `nexus_stop`, or free the port. A local Postgres on 5432 conflicts: the mock database uses 5433 for this reason. |
