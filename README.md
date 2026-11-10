# OrcaNexus

The warehouse Data API project.

For project documentation, please refer to https://github.com/umccr/orcahouse-doc

For data warehouse issue tracking, please submit an issue to https://github.com/umccr/orcahouse-project/issues

## Development

See [README_DEV.md](README_DEV.md) for more _comprehensive_ setup details.

For TL;DR, prepare and install the Java dev toolchain `Amazon Corretto` JDK via https://sdkman.io.

```
sdk install java 21.0.12-amzn
sdk install maven
```

We need an authenticated AWS session as we are developing against the remote OrcaNexus dev environment.

Use your usual AWS CLI setup to authenticate.

For example:
```
export AWS_PROFILE=unimelb-warehouse-prod-poweruser
aws sso login
```

Source the [dx script](dx.sh). It exports shell functions (command shortcut) related to the remote OrcaNexus dev environment.
```
source dx.sh
```

Make the nexus key and tunnel.
```
nexus_rule
nexus_key
nexus_tunnel
nexus_status
nexus_nc
```

Make the nexus credentials.
```
nexus_env
nexus_cred
```

## Build

The project is Maven managed Multi-Module Java application project.

The application stack is centered around the middleware frameworks Spring Boot and jOOQ.

You can build the application like so:
```
mvn clean
mvn compile
mvn package -DskipTests
```

You can run the application like so:
```
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

### Pagination and sorting

List endpoints are paged (offset based) and sortable:

| Parameter | Meaning                                                              |
|-----------|----------------------------------------------------------------------|
| `page`    | Zero-based page number. Default `0`.                                 |
| `size`    | Page size, at least `1` and at most the configured maximum.          |
| `sort`    | `column` or `column,asc` / `column,desc`. Repeat for multiple sorts. |

```bash
curl -s 'http://localhost:8080/api/v1/lims?page=1&size=5&sort=library_id,desc' | jq
```

The response is a Spring `PagedModel`:

```json
{
  "content": [ { "<column>": "<value>" } ],
  "page": { "size": 5, "number": 1, "totalElements": 1234, "totalPages": 247 }
}
```

* `sort` fields are the same column names that appear as keys in `content`. They are matched
  exactly (case-sensitive); an unknown field is rejected. A trailing token other than `asc` or
  `desc` is treated as another field name and is rejected the same way.
* Ordering always ends with a stable tiebreaker (the first column), so paging is deterministic.
* Invalid input (negative `page`, `size` below 1 or above the maximum, non-integer values, unknown
  `sort` field) returns HTTP 400 as an RFC 9457 problem detail, for example
  `{"status": 400, "detail": "size must not exceed 1000", ...}`.
* Defaults and the maximum size are configured through the Spring property namespace
  `spring.data.web.pageable.default-page-size` and `spring.data.web.pageable.max-page-size`
  (environment overrides `PAGE_DEFAULT_SIZE` and `PAGE_MAX_SIZE`).
