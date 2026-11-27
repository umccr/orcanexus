# Architecture decision records

**ADR** stands for _Architecture Decision Record_: a short note of one significant decision,
why it was made, and what it changes. Each file in this folder is one ADR.

Use [0000-template.md](0000-template.md). Accepted ADRs are not edited; supersede them with a
new one and update the old one's status to point to it.

This folder was `docs/adr/` until [0011](0011-adopt-openspec.md); older links may still use
that path.

## When to write an ADR

Keep the bar high. Write one only when the decision is hard or costly to reverse **and**
someone will plausibly ask "why?" later.

Write an ADR for:

- Structure and boundaries: modules, dependency rules, layering.
- Technology or framework choices, and major upgrades that change them.
- Cross-cutting behaviour: error format, pagination style, auth, configuration approach.
- Data and integration approach: how we model, access or secure data.
- Deliberately rejected or deferred options that people will keep suggesting.

Do not write an ADR for:

- Implementation details of one feature. They stay in the OpenSpec change (`openspec/`).
- Anything easily changed in a single PR without wider impact.
- Naming, formatting or style. Those belong in lint config or `AGENTS.md`.
- Recipes or current behaviour. Those belong in `docs/dev/`, `docs/api/` or
  `docs/architecture/`.

Quick test: if you cannot name a realistic alternative that was considered, it is probably
not an ADR.

## How

- Copy the template, take the next number, and name the file `NNNN-short-title.md`.
- Keep it short: Context, Decision, Consequences. Link instead of repeating other docs.
- Do not invent history. If the reasoning is reconstructed, say so in the text.
- Add a row to the index below in the same change.
- Reversing a decision means a new ADR that supersedes the old one. Never edit an accepted one.
- Exception: fixing a path or link after files move is not a decision change, so it may be
  applied to accepted ADRs.

## Index

Status values: Accepted, Superseded by NNNN.

| #    | Decision                                                                                                | Status             |
|------|---------------------------------------------------------------------------------------------------------|--------------------|
| 0001 | [Record architecture decisions](0001-record-architecture-decisions.md)                                  | Accepted           |
| 0002 | [Hexagonal architecture across Maven modules](0002-hexagonal-architecture.md)                           | Accepted           |
| 0003 | [Spring Boot and Java 21](0003-spring-boot-and-java-21.md)                                              | Accepted           |
| 0004 | [Database-first with jOOQ, no ORM](0004-database-first-jooq-no-orm.md)                                  | Accepted           |
| 0005 | [Remote-only dev with IAM database auth](0005-remote-only-dev-with-iam-auth.md)                         | Superseded by 0010 |
| 0006 | [Offset pagination via Spring Data web support](0006-offset-pagination-via-spring-data-web.md)          | Accepted           |
| 0007 | [Prefer framework configuration over custom properties](0007-prefer-framework-configuration.md)         | Accepted           |
| 0008 | [RFC 9457 problem details for errors](0008-rfc-9457-problem-details-for-errors.md)                      | Accepted           |
| 0009 | [Sort fields are physical column names, whitelisted](0009-sort-fields-are-whitelisted-column-names.md)  | Accepted           |
| 0010 | [Local mock database](0010-local-mock-database.md)                                                      | Accepted           |
| 0011 | [Spec-driven changes with OpenSpec](0011-adopt-openspec.md)                                             | Accepted           |

ADRs 0002 to 0005 were recorded retroactively; their reasoning is inferred and marked as such.
