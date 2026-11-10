# 2. Hexagonal architecture across Maven modules

- Status: Accepted
- Date: 2026-11-10 (recorded retroactively)

> The reasoning below is inferred from the code structure, not from original notes.

## Context

The service exposes warehouse data through REST today, backed by one storage technology. The
domain should stay independent of web and database frameworks.

## Decision

Use ports and adapters across five Maven modules: `orcanexus-domain`, `orcanexus-rest`,
`orcanexus-aurora`, `orcanexus-jooq` and `orcanexus-application`. Everything points inward;
the domain is plain Java and depends on no other module. Only `orcanexus-application` wires
adapters together. See the [overview](../architecture/overview.md) for the dependency rules.

## Consequences

- Framework and storage choices stay replaceable and are confined to adapters.
- The module boundary enforces the rule at build time.
- More modules and mapping code than a single-module layout.
