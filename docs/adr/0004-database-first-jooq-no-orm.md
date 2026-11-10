# 4. Database-first with jOOQ, no ORM

- Status: Accepted
- Date: 2026-11-10 (recorded retroactively)

> Consequences are partly inferred. The decision itself is stated in the former
> `README_DEV.md` "Concepts" section.

## Context

The warehouse schema is owned by the upstream data platform. OrcaNexus is a read-oriented
serving tier over it.

## Decision

Model the data in the database and let it drive the Java model. jOOQ introspects the physical
schema and generates the model in `orcanexus-jooq`, which is committed and never hand-edited.
There is no ORM. The generated schema name can differ from the physical one; a runtime
mapping handles that without regeneration.

## Consequences

- Queries stay explicit and close to SQL; the generated model follows the database.
- Schema changes surface as regenerated code and visible API changes.
- Regeneration needs access to the database through the dev tunnel.
