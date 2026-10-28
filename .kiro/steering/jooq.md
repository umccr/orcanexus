---
inclusion: always
---
# jOOQ Conventions

## jOOQ model & code generation

- The generated jOOQ model lives in `orcanexus-jooq` under
  `dev.ccgcm.platform.orcanexus.jooq` and is **committed**. Treat it as generated:
  do not hand-edit; it is excluded from Spotless/Checkstyle.
- Regeneration runs via the Maven profile `generate-jooq` in
  `orcanexus-jooq/pom.xml`, which connects to the DB through the local tunnel
  (see `dx.sh` `nexus_jooq`).
- The generator reads the **physical** DB schema (`inputSchema`) and emits the model
  under a possibly different **generated** name (`outputSchema`). Keep these two
  concepts distinct in both config and comments.
- Database-first, no ORM: the schema is modelled in the database and drives the Java
  model. Do not reshape the generated model to fit Java preferences.

## Schema name mapping (physical vs. generated)

The generated model is bound to the schema name captured at generation time. When
the physical DB schema is named differently, jOOQ's runtime `RenderMapping` rewrites
the generated schema name to the physical one at query-render time — **no
regeneration needed**.

- Wired in `orcanexus-aurora` as `JooqSchemaMappingConfiguration` (a
  `DefaultConfigurationCustomizer` bean) driven by `DatasourceProperties`.
- Mappings are **configuration, not code**: a list under
  `orcanexus.datasource.schema-mappings` (each entry `input` → `output`) in
  `application.yaml`, with env-var overrides. (See `spring.md` for the config-binding
  conventions.)
- **Keep names out of Java and comments.** Code and Javadoc describe the mechanism
  generically (input/generated → output/physical); concrete schema names live only
  as configuration values. A future rename is then a config change, not a
  code/comment sweep.

## Verifying DB-facing changes

- `mvn -DskipTests package` only proves compilation/wiring.
- To confirm runtime schema behavior, start the app (free port via `SERVER_PORT`,
  with DB tunnel + AWS session active) and exercise a data endpoint such as
  `/api/v1/lims`. A `200` on `/api/v1/system/info` confirms startup only, not
  schema-rewrite correctness.
