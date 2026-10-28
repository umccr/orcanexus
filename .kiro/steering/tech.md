---
inclusion: always
---
# Technology Stack

- **Language/runtime**: Java 21 (Amazon Corretto). Enforced via maven-enforcer
  (`[21,22)`). Use modern Java (records, `var`, sealed types where they aid clarity).
- **Build**: Maven, multi-module, parent `dev.ccgcm.platform:orcanexus`, inheriting
  `spring-boot-starter-parent` 4.1.1.
- **Framework**: Spring Boot 4.1.1 (Web MVC, Actuator, Validation).
- **Persistence**: jOOQ over Aurora PostgreSQL. jOOQ runtime version is managed
  transitively by `spring-boot-jooq`; the codegen plugin version is pinned to match.
- **DB connectivity**: AWS Advanced JDBC Wrapper with IAM auth
  (`software.amazon.jdbc.Driver`), PostgreSQL driver underneath.
- **API docs**: springdoc-openapi (Swagger UI).

## Quality gates (run at `verify` phase)

- **Spotless** with google-java-format — formatting is enforced; generated jOOQ
  code is skipped (`spotless.skip` in the jooq module).
- **Checkstyle** — config at `config/checkstyle/`; jOOQ generated package and
  `**/generated/**` are excluded.
- **SpotBugs** (effort Max) — exclude filter at `config/spotbugs/`.

## Common commands

```bash
mvn clean
mvn -DskipTests compile          # fast compile check
mvn -DskipTests package          # build runnable jar
mvn verify                       # run tests + spotless + checkstyle + spotbugs
java -jar orcanexus-application/target/orcanexus-application-1.0-SNAPSHOT.jar
```

Local dev runs against the remote dev DB via an SSH tunnel and AWS SSO session
(see `dx.sh` `nexus_*` shortcuts and `README_DEV.md`). Default Spring profile is
`local`; `SERVER_PORT` overrides the HTTP port.

## Conventions / gotchas

- Match google-java-format output; do not hand-format. Run the IDE reformatter or
  `mvn spotless:apply` before committing.
- Do **not** name a `@Configuration` class `JooqConfiguration` — it collides with
  Spring Boot's auto-config bean `jooqConfiguration`. Give config classes distinct,
  descriptive names (e.g. `JooqSchemaMappingConfiguration`).
- Always `mvn -DskipTests compile` (or `package`) after a change to verify.
