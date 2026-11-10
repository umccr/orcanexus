# OrcaNexus

The warehouse Data API project.

For project documentation, please refer to https://github.com/umccr/orcahouse-doc

For data warehouse issue tracking, please submit an issue to https://github.com/umccr/orcahouse-project/issues

## Quickstart

Full setup (toolchain, AWS session, tunnel, lint): [docs/dev/setup.md](docs/dev/setup.md).

```
sdk install java 21.0.12-amzn
sdk install maven
aws sso login          # any AWS CLI auth works
source dx.sh           # then nexus_key, nexus_tunnel, nexus_env, nexus_cred
mvn clean package -DskipTests
java -jar orcanexus-application/target/orcanexus-application-1.0-SNAPSHOT.jar
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
