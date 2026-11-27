# api-pagination Specification

## Purpose

List endpoints return large result sets in pages, sorted on request. Paging is offset based,
uses standard Spring parameters and envelope, and rejects invalid input instead of silently
returning different data than was asked for.

## Requirements

### Requirement: Offset paging with standard parameters
A list endpoint SHALL accept the query parameters `page` (zero-based) and `size`, resolved
through Spring Data `Pageable` support. When `page` is omitted the first page (0) SHALL be
returned. When `size` is omitted the default page size SHALL apply.

#### Scenario: Explicit page and size
- **WHEN** a client requests `page=1&size=5` and at least 10 records exist
- **THEN** records 6 to 10 of the ordered result are returned

#### Scenario: Defaults apply
- **WHEN** a client omits `page` and `size`
- **THEN** the first page is returned with at most the default page size records

#### Scenario: Paging parameters are documented
- **WHEN** the OpenAPI document is generated
- **THEN** `page`, `size` and `sort` appear as separate query parameters of each paged endpoint

### Requirement: Paging limits are framework configuration
The default and maximum page size SHALL be configured only through
`spring.data.web.pageable.default-page-size` and `spring.data.web.pageable.max-page-size`,
overridable by the environment variables `PAGE_DEFAULT_SIZE` and `PAGE_MAX_SIZE`. No
`orcanexus.pagination.*` setting SHALL exist.

#### Scenario: Maximum overridden by environment
- **WHEN** the application starts with `PAGE_MAX_SIZE=50`
- **THEN** a request with `size=51` is rejected and `size=50` is accepted

### Requirement: Invalid paging input is rejected
The API SHALL respond with HTTP 400 as an RFC 9457 problem detail, whose `detail` carries the
message, for invalid paging input. It SHALL NOT clamp or substitute a value.

#### Scenario: Negative page
- **WHEN** a client requests `page=-1`
- **THEN** the response is 400 with detail `page must not be negative`

#### Scenario: Size below one
- **WHEN** a client requests `size=0`
- **THEN** the response is 400 with detail `size must be at least 1`

#### Scenario: Size above the maximum
- **WHEN** a client requests a `size` larger than the configured maximum
- **THEN** the response is 400 with detail `size must not exceed <max>`, naming the maximum

#### Scenario: Non-integer value
- **WHEN** a client requests `page=abc` or `size=abc`
- **THEN** the response is 400

### Requirement: PagedModel response envelope
A paged response SHALL be a Spring `PagedModel`: a `content` array of records and a `page`
object with `size`, `number`, `totalElements` and `totalPages`. Record keys in `content` SHALL
be the physical column names, with all column values kept. The response SHALL NOT include
`hasNext` or `hasPrevious`.

#### Scenario: Envelope shape
- **WHEN** a client requests `page=0&size=5` over 12 records
- **THEN** `content` has 5 records and `page` is `{size: 5, number: 0, totalElements: 12, totalPages: 3}`

### Requirement: Sorting by whitelisted column names
A list endpoint SHALL accept repeatable `sort=<column>` or `sort=<column>,asc|desc` parameters
and apply them in the order given. A sort field SHALL be a column name of the queried table,
matched exactly and case-sensitively; these are the same names as the keys in `content`. An
unknown field SHALL be rejected with HTTP 400, whose detail names the field, before any SQL is
built.

#### Scenario: Descending sort
- **WHEN** a client requests `sort=library_id,desc`
- **THEN** records are ordered by `library_id` from highest to lowest

#### Scenario: Multiple sorts
- **WHEN** a client requests `sort=sequencing_run_id,asc&sort=library_id,desc`
- **THEN** records are ordered by `sequencing_run_id` ascending, then `library_id` descending

#### Scenario: Unknown field
- **WHEN** a client requests `sort=no_such_column`
- **THEN** the response is 400 with detail `Unknown sort field: no_such_column`, and no query is run

### Requirement: Sort parameter parsing
A trailing token in a `sort` value other than `asc` or `desc` (case-insensitive) SHALL be
treated as another field name. When the same field is requested more than once, the first
request SHALL win.

#### Scenario: Invalid direction
- **WHEN** a client requests `sort=library_id,sideways`
- **THEN** `sideways` is treated as a field name and the response is 400 naming it

#### Scenario: Repeated field
- **WHEN** a client requests `sort=library_id,desc&sort=library_id,asc`
- **THEN** records are ordered by `library_id` descending

### Requirement: Deterministic ordering
Every paged query SHALL end its ordering with the table's first column ascending, unless that
column is already part of the requested sort. With no `sort` parameter the result SHALL be
ordered by the first column ascending.

#### Scenario: Tiebreaker appended
- **WHEN** a client sorts by a column that is not the first column
- **THEN** the first column ascending is added as the last ordering term

#### Scenario: Tiebreaker not duplicated
- **WHEN** a client sorts by the first column descending
- **THEN** the ordering has the first column once, descending
