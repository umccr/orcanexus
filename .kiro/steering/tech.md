---
inclusion: always
---
# Technology Stack

- Java 21 (Amazon Corretto), enforced by maven-enforcer (`[21,22)`). Use modern Java
  (records, `var`, sealed types where they aid clarity).
- Maven multi-module, parent `dev.ccgcm.platform:orcanexus`, inheriting
  `spring-boot-starter-parent` 4.1.1. Spring Boot 4.1.1 (Web MVC, Actuator, Validation).
- jOOQ over Aurora PostgreSQL. The jOOQ runtime version is managed by `spring-boot-jooq`;
  the codegen plugin version is pinned to match.
- DB connectivity: AWS Advanced JDBC Wrapper with IAM auth (`software.amazon.jdbc.Driver`).
- API docs: springdoc-openapi (Swagger UI).

Rationale: `docs/adr/0003-spring-boot-and-java-21.md`. Setup and tooling:
`docs/dev/setup.md`.

## Quality gates (`verify` phase)

- **Spotless** (google-java-format); generated jOOQ code is skipped (`spotless.skip` in the
  jooq module).
- **Checkstyle**: config in `config/checkstyle/`; jOOQ generated package and
  `**/generated/**` excluded.
- **SpotBugs** (effort Max): exclude filter in `config/spotbugs/`.

## Commands

```bash
mvn clean
mvn -DskipTests compile          # fast compile check
mvn -DskipTests package          # build runnable jar
mvn verify                       # tests + spotless + checkstyle + spotbugs
```

Default Spring profile is `local`; `SERVER_PORT` overrides the HTTP port.

## Gotchas

- Match google-java-format output; do not hand-format. Run `mvn spotless:apply` before
  committing.
- Do **not** name a `@Configuration` class `JooqConfiguration` (see `spring.md`).
- Always `mvn -DskipTests compile` (or `package`) after a change to verify.
