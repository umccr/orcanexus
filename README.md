<!-- Landing page: capped at 60 lines by pre-commit (readme-line-cap). Do not add content here;
     put it under docs/ (see docs/README.md). -->
# OrcaNexus

The warehouse Data API project.

For project documentation, please refer to https://github.com/umccr/orcahouse-doc

For data warehouse issue tracking, please submit an issue to https://github.com/umccr/orcahouse-project/issues

## Quickstart

Most of us develop with an agent (Claude Code or Kiro), against the local mock database.

1. **Set up and run** (you run this; needs Docker, no AWS access):

   ```
   brew bundle                  # or install the Brewfile tools another way
   sdk install java 21.0.12-amzn
   sdk install maven
   make setup && make check

   source dx.sh
   nexus_docker_up              # local Postgres in Docker, seeded with mock data
   mvn clean package -DskipTests
   SPRING_PROFILES_ACTIVE=mockdb java -jar orcanexus-application/target/orcanexus-application-1.0-SNAPSHOT.jar
   ```

   Details: [docs/dev/offline-mockdb.md](docs/dev/offline-mockdb.md). Remote dev database over
   the SSM tunnel (needs AWS): [docs/dev/remote-tunnel.md](docs/dev/remote-tunnel.md).
   Toolchain and lint: [docs/dev/README.md](docs/dev/README.md).

2. **Make a change** with your agent, spec first:
   [docs/dev/spec-driven.md](docs/dev/spec-driven.md).

## API

The OpenAPI and Swagger UI is at:
* http://localhost:8080/swagger-ui/index.html

```bash
curl -s http://localhost:8080/api/v1/system/info | jq
```

```bash
curl -s http://localhost:8080/api/v1/lims | jq
```

Paging, sorting and error behaviour: [docs/api/README.md](docs/api/README.md).

## Documentation

See [docs/](docs/README.md) for setup, API conventions, architecture and decision records.
