# 1. Record architecture decisions

- Status: Accepted
- Date: 2026-11-10

## Context

Decisions were scattered across README files and spec documents, and the reasoning behind
them was easy to lose. Specs are point-in-time history and are not maintained after delivery.

## Decision

We record architecturally significant decisions as ADRs in `docs/adr/`, named
`NNNN-title.md`, using the [template](0000-template.md).

- An accepted ADR is not edited. A later ADR supersedes it, and the old one's status is
  updated to point to the new one.
- A spec that delivers a real decision promotes it to an ADR (see [docs/README.md](../README.md)).

## Consequences

- The reasoning behind a decision survives the spec that produced it.
- Each decision costs a short document to write.
