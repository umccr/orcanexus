---
inclusion: always
---
# Spring Configuration Conventions

- Externalize settings with `@ConfigurationProperties(prefix = "orcanexus.<area>")`
  on a `@Component` POJO (see `SystemProperties`, `PaginationProperties`,
  `DatasourceProperties`). No explicit `@EnableConfigurationProperties` — these are
  component-scanned from the `@SpringBootApplication` base package.
- Prefer configurable, env-overridable values (`${ENV_VAR:default}` in YAML) over
  hardcoded constants. Defaults may encode the current situation but must be
  overridable.
- Config beans belong to the module that owns the concern: DB/jOOQ config in
  `orcanexus-aurora`, web config in `orcanexus-rest`, cross-cutting composition in
  `orcanexus-application`.
- Name `@Configuration` classes descriptively and avoid collisions with Spring Boot
  auto-config bean names. Do **not** use a bare `JooqConfiguration` — it collides
  with Spring Boot's auto-config bean `jooqConfiguration`; prefer a specific name
  such as `JooqSchemaMappingConfiguration`.
- Extend Spring Boot auto-configuration through its customizer hooks (e.g.
  `DefaultConfigurationCustomizer` for jOOQ) rather than redefining the
  auto-configured bean.

See `jooq.md` for the jOOQ schema-mapping configuration that applies these
conventions.
