---
inclusion: always
---
# Architecture & Module Structure

Hexagonal (ports & adapters) across five Maven modules. The invariant: **everything points
inward toward the domain; the domain depends on nothing.** Module table, dependency graph,
request flow and package layout: `docs/architecture/overview.md`. Rationale:
`docs/adr/0002-hexagonal-architecture.md`.

## Hard rules
- **Never** make `orcanexus-domain` depend on another module or on Spring/jOOQ/infrastructure.
  Keep it plain Java.
- `orcanexus-rest` must not depend on `orcanexus-aurora` or `orcanexus-jooq` directly; it
  talks to the domain through ports only.
- Infrastructure types (`org.jooq.*`, DB concerns) live in `orcanexus-aurora`, not in `rest`
  or `domain`.
- `orcanexus-application` is the only module that wires adapters together.
- Domain services are plain Java (`final class ... implements <UseCase>`), wired as beans in
  `DomainConfiguration`, with no Spring stereotypes. Ports are domain interfaces; adapters
  implement them.
- Base package `dev.ccgcm.platform.orcanexus`; `...jooq` is generated, do not hand-edit.

## New resource
Use the LIMS slice as the template: `docs/dev/adding-a-resource.md`.
