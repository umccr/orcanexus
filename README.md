# OrcaNexus

The warehouse Data API project.

For project documentation, please refer to https://github.com/umccr/orcahouse-doc

For data warehouse issue tracking, please submit an issue to https://github.com/umccr/orcahouse-project/issues

## Quickstart

Local mock database (needs Docker, no AWS access): [docs/dev/offline-mockdb.md](docs/dev/offline-mockdb.md).
Remote dev database over the SSM tunnel (needs AWS): [docs/dev/remote-tunnel.md](docs/dev/remote-tunnel.md). Toolchain and lint: [docs/dev/setup.md](docs/dev/setup.md).

```
sdk install java 21.0.12-amzn
sdk install maven
source dx.sh
nexus_docker_up        # local Postgres in Docker, seeded with mock data
mvn clean package -DskipTests
SPRING_PROFILES_ACTIVE=mockdb java -jar orcanexus-application/target/orcanexus-application-1.0-SNAPSHOT.jar
```

## API

The OpenAPI and Swagger UI is at:
* http://localhost:8080/swagger-ui/index.html

```bash
curl -s http://localhost:8080/api/v1/system/info | jq
```

```bash
curl -s http://localhost:8080/api/v1/lims | jq
```

Paging, sorting and error behaviour: [docs/api/conventions.md](docs/api/conventions.md).

## Documentation

See [docs/](docs/README.md) for setup, API conventions, architecture and decision records.
