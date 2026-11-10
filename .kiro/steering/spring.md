---
inclusion: always
---
# Spring Configuration Conventions

- Externalize settings with `@ConfigurationProperties(prefix = "orcanexus.<area>")` on a
  `@Component` POJO (see `SystemProperties`, `DatasourceProperties`). Where Spring Boot
  already provides a setting (e.g. `spring.data.web.pageable.*`), configure that instead of
  adding a custom property (`docs/adr/0007-prefer-framework-configuration.md`). No explicit
  `@EnableConfigurationProperties`; these are component-scanned.
- Prefer env-overridable values (`${ENV_VAR:default}` in YAML) over hardcoded constants.
- Config beans belong to the module that owns the concern: DB/jOOQ in `orcanexus-aurora`, web
  in `orcanexus-rest`, cross-cutting composition in `orcanexus-application`.
- Name `@Configuration` classes descriptively. Do **not** use a bare `JooqConfiguration`; it
  collides with Boot's auto-config bean `jooqConfiguration`. Use e.g.
  `JooqSchemaMappingConfiguration`.
- Extend auto-configuration through customizer hooks (e.g. `DefaultConfigurationCustomizer`)
  rather than redefining the auto-configured bean.
- Controllers taking `Pageable` must use `@ParameterObject`. Shared paging and error
  behaviour: `docs/api/conventions.md`.

See `jooq.md` for the jOOQ schema-mapping configuration.
