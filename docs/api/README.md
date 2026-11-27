# API conventions

Behaviour shared by all list endpoints. Endpoint paths and schemas are in Swagger UI
(`/swagger-ui/index.html`), not here.

This page is the guide for API consumers. The normative behaviour is the
[api-pagination spec](../../openspec/specs/api-pagination/spec.md), changed only through an
OpenSpec change; keep this page in step when a change is archived.

## Pagination and sorting

List endpoints are paged (offset based) and sortable:

| Parameter | Meaning                                                              |
|-----------|----------------------------------------------------------------------|
| `page`    | Zero-based page number. Default `0`.                                 |
| `size`    | Page size, at least `1` and at most the configured maximum.          |
| `sort`    | `column` or `column,asc` / `column,desc`. Repeat for multiple sorts. |

```bash
curl -s 'http://localhost:8080/api/v1/lims?page=1&size=5&sort=library_id,desc' | jq
```

The response is a Spring `PagedModel`:

```json
{
  "content": [ { "<column>": "<value>" } ],
  "page": { "size": 5, "number": 1, "totalElements": 1234, "totalPages": 247 }
}
```

* `sort` fields are the same column names that appear as keys in `content`. They are matched
  exactly (case-sensitive); an unknown field is rejected. A trailing token other than `asc` or
  `desc` is treated as another field name and is rejected the same way.
* Ordering always ends with a stable tiebreaker (the first column), so paging is deterministic.
* Defaults and the maximum size are configured through the Spring property namespace
  `spring.data.web.pageable.default-page-size` and `spring.data.web.pageable.max-page-size`
  (environment overrides `PAGE_DEFAULT_SIZE` and `PAGE_MAX_SIZE`).

See [ADR 0006](../decisions/0006-offset-pagination-via-spring-data-web.md) and
[ADR 0009](../decisions/0009-sort-fields-are-whitelisted-column-names.md).

## Errors

Invalid input returns HTTP 400 as an RFC 9457 problem detail, for example
`{"status": 400, "detail": "size must not exceed 1000", ...}`.

| Condition                                        | Result                           |
|--------------------------------------------------|----------------------------------|
| `page` negative                                  | 400, `page must not be negative` |
| `size` below 1                                   | 400, `size must be at least 1`   |
| `size` above the configured maximum              | 400, `size must not exceed <max>`|
| Non-integer `page` or `size`                     | 400                              |
| Unknown `sort` field                             | 400, message names the field     |

See [ADR 0008](../decisions/0008-rfc-9457-problem-details-for-errors.md).
