# 8. RFC 9457 problem details for errors

- Status: Accepted
- Date: 2026-11-10

## Context

Spring Data's stock `Pageable` resolver is lenient: it silently turns invalid `page` or
`size` into defaults and clamps oversized pages. Consumers should never silently receive
different data than they asked for. There was also no global error handling.

## Decision

Reject invalid input with HTTP 400 rather than correcting it. A validating wrapper checks the
raw `page` and `size` values and then delegates to the stock resolver, which keeps Boot's
auto-configuration active. Errors are rendered as RFC 9457 problem details
(`spring.mvc.problemdetails.enabled=true`). An unknown sort field is mapped to a 400 by a
narrowly scoped exception handler.

## Consequences

- Clients get a consistent, standard error shape.
- The problem-details setting applies to every MVC error response in the application, not
  only pagination.
- Parameter qualifiers on `Pageable` are not supported by the wrapper.

Error cases are listed in [api/conventions.md](../api/conventions.md).
