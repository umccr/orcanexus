# Requirements Document

## Introduction

`/api/v1/lims` currently parses `page` and `size` by hand (`PaginationRequestFactory`,
`PaginationProperties`) and returns a custom flat envelope (`LimsResponse`). This feature
replaces that with Spring Data's `Pageable` web support and the `PagedModel` response
envelope, and adds whitelisted `sort` support. Pagination remains offset-based; keyset/cursor
pagination is deferred until needed. The hexagonal architecture is preserved: the domain stays
plain Java and only `orcanexus-rest` takes the Spring Data dependency.

## Requirements

### Requirement 1: Spring-based pagination input

**User Story:** As an API consumer, I want to page through results using standard `page` and
`size` parameters, so that I can retrieve large result sets in manageable chunks.

#### Acceptance Criteria

1. WHEN a request to `/api/v1/lims` includes `page` and `size` THEN the system SHALL return the
   corresponding slice of records using zero-based page numbers.
2. WHEN `page` or `size` is omitted THEN the system SHALL apply the configured default page and
   default size.
3. WHEN the controller receives pagination input THEN the system SHALL resolve it through Spring
   Data's `Pageable` argument support instead of hand-parsed request parameters.
4. WHEN Swagger UI is rendered THEN the system SHALL document the `page`, `size` and `sort`
   parameters for the endpoint.

### Requirement 2: Input validation

**User Story:** As an API consumer, I want clear errors for invalid pagination input, so that I
am never silently given different data than I asked for.

#### Acceptance Criteria

1. WHEN `page` is negative THEN the system SHALL respond with HTTP 400 and a message stating that
   page must not be negative.
2. WHEN `size` is less than 1 THEN the system SHALL respond with HTTP 400 and a message stating
   that size must be at least 1.
3. WHEN `size` exceeds the configured maximum THEN the system SHALL respond with HTTP 400 and a
   message stating the maximum allowed size, rather than silently clamping it.
4. WHEN a request value is not a valid integer THEN the system SHALL respond with HTTP 400.
5. The maximum page size SHALL be configurable via the framework property
   `spring.data.web.pageable.max-page-size` and be overridable through environment variables;
   no redundant `orcanexus.pagination.*` setting SHALL remain.

### Requirement 3: PagedModel response envelope

**User Story:** As an API consumer, I want a standard, documented paging envelope, so that I can
use common client tooling.

#### Acceptance Criteria

1. WHEN a paged request succeeds THEN the system SHALL return a Spring `PagedModel` containing
   `content` and a nested `page` object with `size`, `number`, `totalElements` and `totalPages`.
2. The system SHALL return only the `PagedModel` envelope; the previous flat envelope
   (`LimsResponse` with `hasNext` and `hasPrevious`) SHALL NOT be returned.
3. WHEN records contain arbitrary column maps THEN the system SHALL serialize them as `content`
   entries without losing column values.

### Requirement 4: Sorting

**User Story:** As an API consumer, I want to sort results by a column, so that I can read data
in a meaningful order.

#### Acceptance Criteria

1. WHEN `sort=<column>,<asc|desc>` is supplied THEN the system SHALL order results accordingly.
2. WHEN multiple `sort` parameters are supplied THEN the system SHALL apply them in order.
3. WHEN a sort field is not a column of the queried table THEN the system SHALL respond with
   HTTP 400 and SHALL NOT include the field in any generated SQL.
4. WHEN no `sort` is supplied THEN the system SHALL order by the first column, as it does today.
5. WHEN any sort is applied THEN the system SHALL add a stable tiebreaker so that paging is
   deterministic across requests.

### Requirement 5: Architectural integrity

**User Story:** As a maintainer, I want the hexagonal boundaries preserved, so that the domain
remains independent of frameworks.

#### Acceptance Criteria

1. The `orcanexus-domain` module SHALL NOT depend on Spring, Spring Data or jOOQ.
2. The `orcanexus-rest` module SHALL map `Pageable` to the domain `PageRequest` and the domain
   `PageResult` to `PagedModel`.
3. The `orcanexus-rest` module SHALL NOT depend on `orcanexus-aurora` or `orcanexus-jooq`.
4. Sort handling in the domain SHALL use plain Java types (a `SortOrder` with field name and
   direction).
5. The `spring-data-commons` dependency version SHALL be managed by the Spring Boot BOM, not
   pinned separately.

### Requirement 6: Quality and verification

**User Story:** As a maintainer, I want the change verified, so that it ships with confidence.

#### Acceptance Criteria

1. WHEN the change is complete THEN `mvn verify` SHALL pass, including Spotless, Checkstyle and
   SpotBugs.
2. WHEN the DB tunnel and AWS session are available THEN the system SHALL be exercised against
   `/api/v1/lims` with and without paging and sort; otherwise the lack of runtime verification
   SHALL be stated explicitly.
3. Developer documentation SHALL describe the paging response shape.
