# Implementation Plan

Each task ends with `mvn -DskipTests compile` passing. Formatting is done with
`mvn spotless:apply`, never by hand. Run `mvn verify` at the end of the final task.

- [x] 1. Spike: Spring Data web support, `PagedModel` and resolver ordering
  - Add `spring-boot-data-commons` (version managed by the Boot BOM) to `orcanexus-rest/pom.xml`
  - Change `LimsController` to accept a `Pageable` and return a `PagedModel` built from the
    domain `PageResult` (via `PageImpl`), keeping the current use case and port unchanged
  - Set `spring.data.web.pageable.default-page-size` and `max-page-size` in
    `application.yaml` / `application-local.yaml` with env-overridable defaults
  - Add a MockMvc test confirming the JSON shape (`content` plus `page{size, number,
    totalElements, totalPages}`) with `Map` content
  - Confirm Springdoc renders `page`, `size` and `sort` for the `Pageable` argument
  - Confirm generated column names in the jOOQ `Lims` table are plain identifiers
  - Record any deviation from `design.md` back into the design document
  - _Requirements: 1.1, 1.3, 1.4, 3.1, 3.3, 5.5_

- [x] 2. Validating resolver wrapper and Pageable-to-domain mapping
  - [x] 2.1 Implement the validating resolver wrapper in `orcanexus-rest`
    - A `HandlerMethodArgumentResolver` (not a `PageableHandlerMethodArgumentResolver` bean)
      that supports `Pageable` parameters
    - Read parameter names and max page size from the injected `DataWebProperties`
    - Validate raw `page` and `size`; throw `ResponseStatusException(400)` with the messages
      `page must not be negative`, `size must be at least 1`, `size must not exceed <max>`;
      non-integer values are also a 400
    - Delegate valid requests to the framework's stock resolver
    - Register through a `WebMvcConfigurer` and verify it takes precedence over the stock
      resolver
    - _Requirements: 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 2.5_
  - [x] 2.2 Implement `PageRequestMapper` and wire the controller
    - Map `Pageable` to the domain `PageRequest`
    - Wire the controller to use the mapper and the `PagedModel` assembly
    - _Requirements: 3.1, 5.2_
  - [x] 2.3 Remove superseded code and config
    - Delete `LimsResponse`, `PaginationRequestFactory`, `PaginationProperties`
    - Remove the `orcanexus.pagination` block from `application.yaml` and
      `application-local.yaml`
    - Confirm nothing else references the removed classes or properties
    - _Requirements: 3.2, 2.5_
  - [x] 2.4 Tests
    - Unit tests for the wrapper (negative page, size 0, oversized size, non-integer, defaults)
    - MockMvc tests for each 400 case, default values and the envelope shape
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1_

- [x] 3. Sorting in the domain and persistence
  - [x] 3.1 Add domain sort types in `orcanexus-domain`
    - `SortDirection` enum, `SortOrder` record (rejects blank field),
      `InvalidSortFieldException`
    - Extend `PageRequest` with a defensively copied `List<SortOrder> sort` and keep a
      two-argument convenience constructor
    - Keep the domain free of Spring, Spring Data and jOOQ
    - _Requirements: 4.4, 5.1, 5.4_
  - [x] 3.2 Implement sort handling in `LimsRepository`
    - Resolve each `SortOrder.field` against the table's fields by exact name; throw
      `InvalidSortFieldException` for unknown names before building SQL
    - Build jOOQ `SortField`s; append the first column ascending as a tiebreaker when it is not
      already part of the sort; keep current ordering when no sort is given
    - Leave limit, offset and count unchanged
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  - [x] 3.3 Tests
    - Domain unit tests for `PageRequest` and `SortOrder`
    - Tests of the ordering logic that do not need a live database (for example rendering SQL
      from a `DSLContext` without a connection): single sort, multiple sorts, tiebreaker
      added, tiebreaker not duplicated, unknown field, no sort
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 5.1_

- [x] 4. Wire `sort` through the REST layer
  - Extend `PageRequestMapper` to convert `Pageable.getSort()` into `SortOrder`s
  - Reject a sort direction other than `asc` or `desc` with a 400
  - Add a narrowly scoped `@RestControllerAdvice` mapping `InvalidSortFieldException` to a 400
    whose message names the field
  - MockMvc tests: `sort=<column>,desc`, multiple `sort` parameters, unknown column, malformed
    direction
  - _Requirements: 4.1, 4.2, 4.3, 5.2_

- [x] 5. Documentation and final verification
  - Status: complete. Docs written, `mvn verify` passes for all modules with tests, module
    dependency rules checked, and the jar was run on a free port against the dev database through
    the tunnel: paging, sort (asc/desc/multi), stable paging, every 400 case, OpenAPI parameters.
  - Update `README_DEV.md` if it mentions paging; document the `PagedModel` response and that
    sort fields equal the response keys
  - Add a short note on the reusable pattern for adding pagination to the next resource
  - Run `mvn spotless:apply`, then `mvn verify` (Spotless, Checkstyle, SpotBugs, tests) and fix
    any findings
  - If the DB tunnel and AWS session are available, start the app on a free `SERVER_PORT` and
    exercise `/api/v1/lims` with and without paging and sort; otherwise state explicitly that
    runtime verification against the database was not done
  - Confirm `orcanexus-domain` has no Spring or jOOQ dependency and `orcanexus-rest` has no
    dependency on aurora or jooq
  - _Requirements: 5.1, 5.3, 6.1, 6.2, 6.3_
