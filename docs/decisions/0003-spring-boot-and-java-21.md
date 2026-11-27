# 3. Spring Boot and Java 21

- Status: Accepted
- Date: 2026-11-10 (recorded retroactively)

> The reasoning below is inferred from the build configuration, not from original notes.

## Context

The service needs a mainstream web framework with first-class jOOQ, Actuator and OpenAPI
support, on a supported long-term runtime.

## Decision

Use Spring Boot (Web MVC, Actuator, Validation) on Java 21 (Amazon Corretto). The parent is
`spring-boot-starter-parent`, and maven-enforcer requires Java `[21,22)`. API docs come from
springdoc-openapi.

## Consequences

- Boot's BOM manages dependency versions, including jOOQ and Spring Data Commons.
- Modern Java features (records, `var`, sealed types) are available.
- Major Boot or Java upgrades are deliberate changes and need a new ADR if they alter this.
