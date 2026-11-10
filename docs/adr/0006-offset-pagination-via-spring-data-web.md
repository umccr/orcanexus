# 6. Offset pagination via Spring Data web support

- Status: Accepted
- Date: 2026-11-10

## Context

`/api/v1/lims` parsed `page` and `size` by hand and returned a custom flat envelope. We
wanted standard parameters, a standard documented envelope, and sorting.

## Decision

Use Spring Data's `Pageable` argument support for input and `PagedModel` for output, with
offset-based paging. The domain keeps its own plain-Java `PageRequest`, `SortOrder` and
`PageResult`; only `orcanexus-rest` depends on Spring Data. Keyset (cursor) pagination is
deferred.

## Consequences

- Consumers get a standard envelope (`content` plus `page`) and common client tooling works.
- Deep pages and the count query can be slow on very large tables. Revisit keyset pagination
  per resource if that happens.
- Controllers taking `Pageable` must use `@ParameterObject` so Swagger UI shows `page`, `size`
  and `sort` separately.

Behaviour is described in [api/conventions.md](../api/conventions.md).
