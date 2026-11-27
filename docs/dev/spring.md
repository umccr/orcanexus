# Spring conventions

- Externalise settings with `@ConfigurationProperties(prefix = "orcanexus.<area>")` on a
  `@Component` POJO (see `SystemProperties`, `DatasourceProperties`). They are component-scanned;
  do not add `@EnableConfigurationProperties`.
- Where Spring Boot already provides a setting (e.g. `spring.data.web.pageable.*`), configure
  that instead of adding a custom property
  ([ADR 0007](../decisions/0007-prefer-framework-configuration.md)).
- Prefer environment-overridable values (`${ENV_VAR:default}` in YAML) over hard-coded constants.
- Configuration beans belong to the module that owns the concern: DB and jOOQ in
  `orcanexus-aurora`, web in `orcanexus-rest`, cross-cutting composition in
  `orcanexus-application`.
- Name `@Configuration` classes descriptively. Do **not** use a bare `JooqConfiguration`; it
  collides with Boot's auto-configured bean `jooqConfiguration`. Use e.g.
  `JooqSchemaMappingConfiguration`.
- Extend auto-configuration through customizer hooks (e.g. `DefaultConfigurationCustomizer`)
  rather than redefining the auto-configured bean.
- Controllers taking `Pageable` must use `@ParameterObject`, so Swagger UI shows `page`, `size`
  and `sort` separately. Shared paging and error behaviour:
  [api/README.md](../api/README.md).
- Profiles: `tunnel` (the default) and `mockdb`
  ([README.md](README.md#database-access)).

jOOQ schema-mapping configuration: [jooq.md](jooq.md).
