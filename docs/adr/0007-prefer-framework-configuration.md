# 7. Prefer framework configuration over custom properties

- Status: Accepted
- Date: 2026-11-10

## Context

Pagination had a custom `orcanexus.pagination.*` block that duplicated what Spring Boot
already offers under `spring.data.web.*`.

## Decision

Where Spring Boot provides a setting, configure that instead of adding a custom property.
Custom settings use `@ConfigurationProperties(prefix = "orcanexus.<area>")` only when the
framework has no equivalent. Values stay overridable through environment variables.
Pagination defaults and the maximum size now use `spring.data.web.pageable.*`.

## Consequences

- Less configuration code and fewer settings to document.
- Behaviour follows the framework's semantics, including its gaps. For example there is no
  default-page property; the fallback is always page 0.
