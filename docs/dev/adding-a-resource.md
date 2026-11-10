# Adding a resource

Follow the LIMS slice. Pagination parsing, validation and the response envelope are shared;
only the resource-specific parts are new. Module rules are in the
[architecture overview](../architecture/overview.md).

## Steps

1. `orcanexus-domain`: add a use-case interface, a service, a query port and a model. The port
   takes `PageRequest` (which carries the requested `SortOrder`s) and returns `PageResult<T>`.
   Keep it plain Java.
2. `orcanexus-aurora`: add a jOOQ repository implementing the port. Apply `limit`/`offset` and
   a count query, and build the ordering from the requested sort orders. Only real column names
   of the table may be sortable, and the first column ascending is the tiebreaker. Unknown
   fields must throw `InvalidSortFieldException`, which the REST layer already maps to HTTP 400.
3. `orcanexus-rest`: add a controller and DTOs. A paged method takes `@ParameterObject Pageable`,
   maps it with `PageRequestMapper`, and returns through `PagedModels.from(result, mapper)`.
   `@ParameterObject` is required for Swagger UI to show `page`, `size` and `sort` as separate
   parameters. Validation of `page` and `size` is applied automatically to any `Pageable`
   argument.
4. `orcanexus-application`: wire the new domain service as a bean in `DomainConfiguration`.

## Notes

- Limits and defaults come from `spring.data.web.pageable.*`; do not add custom settings
  ([ADR 0007](../adr/0007-prefer-framework-configuration.md)).
- Offset pagination was chosen on purpose
  ([ADR 0006](../adr/0006-offset-pagination-via-spring-data-web.md)). Revisit keyset (cursor)
  pagination for a resource only if its row counts make deep pages or the count query slow.
- Shared API behaviour is in [api/conventions.md](../api/conventions.md).
