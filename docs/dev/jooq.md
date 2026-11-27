# jOOQ conventions

Database-first, no ORM ([ADR 0004](../decisions/0004-database-first-jooq-no-orm.md)). Do not
reshape the generated model to fit Java preferences.

## Generated model

- The generated jOOQ model lives in `orcanexus-jooq` under `dev.ccgcm.platform.orcanexus.jooq`
  and is **committed**. Treat it as generated: do not hand-edit it. It is excluded from
  Spotless and Checkstyle.
- Regenerate with the Maven profile `generate-jooq` in `orcanexus-jooq/pom.xml`. It connects to
  the database through the tunnel ([remote-tunnel.md](remote-tunnel.md); `dx.sh` `nexus_jooq`).
- The generator reads the **physical** schema (`inputSchema`) and emits the model under a
  possibly different **generated** name (`outputSchema`). Keep the two concepts distinct in
  configuration and comments.

## Schema name mapping

The generated model is bound to the schema name captured at generation time. When the physical
schema is named differently, jOOQ's runtime `RenderMapping` rewrites the generated name to the
physical one at query-render time. No regeneration is needed.

- Wired in `orcanexus-aurora` as `JooqSchemaMappingConfiguration` (a
  `DefaultConfigurationCustomizer` bean), driven by `DatasourceProperties`.
- Mappings are configuration, not code: a list under `orcanexus.datasource.schema-mappings`
  (each entry `input` → `output`) in `application.yaml`, with environment overrides. Binding
  conventions: [spring.md](spring.md).
- **Keep concrete schema names out of Java and comments.** Code and Javadoc describe the
  mechanism generically (generated → physical); names live only as configuration values, so a
  future rename is a configuration change, not a code and comment sweep.

## Verifying DB-facing changes

- `mvn -DskipTests package` proves compilation and wiring only.
- To confirm runtime schema behaviour, start the app on a free `SERVER_PORT` and call a data
  endpoint such as `/api/v1/lims`. A `200` on `/api/v1/system/info` confirms startup only.
- Offline, use the `mockdb` profile with `nexus_docker_up`
  ([offline-mockdb.md](offline-mockdb.md)). Its DDL comes from the committed model, so
  regenerating the model needs no tool change, and the schema mapping is an identity mapping.
- Through the tunnel, the `tunnel` profile needs the tunnel and an AWS session
  ([remote-tunnel.md](remote-tunnel.md)).
