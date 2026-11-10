# 9. Sort fields are physical column names, whitelisted

- Status: Accepted
- Date: 2026-11-10

## Context

Clients need to sort results. Sort input flows toward SQL, so it must not be able to inject
arbitrary identifiers.

## Decision

`sort` accepts only real column names of the queried table, matched exactly and
case-sensitively. These are the same names as the keys in the response `content`, with no
alias layer. Unknown fields are rejected before any SQL is built. The first column ascending
is appended as a tiebreaker so paging is deterministic.

## Consequences

- No injection surface and no mapping layer to maintain.
- A schema change is already a visible API change, consistent with
  [database-first](0004-database-first-jooq-no-orm.md).
- Sorting on non-indexed columns may be slow on large tables. Indexes are added in the
  database as needed.
