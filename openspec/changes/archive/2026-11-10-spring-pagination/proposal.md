# Proposal

## Why

`/api/v1/lims` parsed `page` and `size` by hand and returned a custom flat envelope, with no
sorting. We wanted standard parameters, a standard documented envelope, and safe sorting.

This change was specified and delivered as a Kiro spec in PR
[#4](https://github.com/umccr/orcanexus/pull/4) (2026-11-10), before the repository adopted
OpenSpec ([ADR 0011](../../../../docs/decisions/0011-adopt-openspec.md)). It was moved here on
2026-11-27 with no code changes. The original Kiro documents are kept as they were:
`requirements.md` (numbered acceptance criteria, cited by `tasks.md` and `design.md`),
`design.md` and `tasks.md`. The spec delta under `specs/` restates the lasting behaviour in
OpenSpec form, checked against the code on `main` at b01402d.

## What Changes

- Paging input through Spring Data `Pageable` (`page`, `size`, `sort`), validated strictly:
  invalid values are rejected with HTTP 400, never clamped.
- Responses use the Spring `PagedModel` envelope; the flat `LimsResponse` envelope is removed.
- Sorting by whitelisted physical column names, with a stable tiebreaker.
- Paging settings move to `spring.data.web.pageable.*`; `orcanexus.pagination.*` is removed.
- Decisions recorded as ADRs [0006](../../../../docs/decisions/0006-offset-pagination-via-spring-data-web.md),
  [0007](../../../../docs/decisions/0007-prefer-framework-configuration.md),
  [0008](../../../../docs/decisions/0008-rfc-9457-problem-details-for-errors.md) and
  [0009](../../../../docs/decisions/0009-sort-fields-are-whitelisted-column-names.md).

## Capabilities

### New Capabilities

- `api-pagination`: how list endpoints page, sort and reject invalid paging input.

### Modified Capabilities

None.

## Impact

- Code: `orcanexus-domain` (`PageRequest`, `SortOrder`, `InvalidSortFieldException`),
  `orcanexus-rest` (validating resolver, mapper, `PagedModel` assembly, sort error handler),
  `orcanexus-aurora` (`LimsRepository` ordering), application config.
- API: breaking change to the `/api/v1/lims` response envelope.
- Docs: [api/README.md](../../../../docs/api/README.md),
  [dev/adding-a-resource.md](../../../../docs/dev/adding-a-resource.md).
