# 11. Spec-driven changes with OpenSpec

- Status: Accepted
- Date: 2026-11-27

## Context

OrcaNexus started as a Kiro-first repository: agent guidance in `.kiro/steering/` and feature
specs in `.kiro/specs/` (`spring-pagination`, `offline-mockdb`). Developers use two coding
agents, Kiro (`kiro-cli`) and Claude Code, and we want a spec-driven workflow in which both feel
native, with specs owned by the repository rather than by one agent. Options considered: Kiro
specs, OpenSpec, GitHub Spec Kit and BMAD.

- Kiro specs are Kiro-native; Claude Code users would work inside another agent's process.
- Spec Kit is suited to greenfield work with heavier, gated phases.
- BMAD's role-based process is too heavy for per-repo work. It may suit higher-level project
  requirements and architecture, and is to be tried there.

On 2026-11-25 the warehouse team agreed to pilot OpenSpec in the technical warehouse repos and
revisit it if it doesn't work
([orcahouse-project#96](https://github.com/umccr/orcahouse-project/issues/96)). OrcaVault was
set up first; this repository follows the same layout.

## Decision

We use OpenSpec, initialised for both agents (`openspec init --tools kiro,claude`).

- In-flight work lives in `openspec/changes/<name>/`; archiving merges behaviour into
  `openspec/specs/`.
- The two Kiro specs move to `openspec/changes/archive/` with their original documents, plus a
  proposal and spec deltas archived into living specs. No code changes.
- Agent rules live in `AGENTS.md`, which Kiro loads natively and `CLAUDE.md` imports.
- The code conventions that were in `.kiro/steering/` move to `docs/` (architecture overview,
  `docs/dev/jooq.md`, `docs/dev/spring.md`), shared by people and agents. `CLAUDE.md` imports
  them and steering inlines them; steering holds pointers only.
- Long-lived context stays outside OpenSpec, in team-owned `docs/`: architecture in
  `docs/architecture/`, shared API behaviour in `docs/api/`, decisions in `docs/decisions/`.
- Decision records move from `docs/adr/` to `docs/decisions/`.

How we work with it: [docs/dev/spec-driven.md](../dev/spec-driven.md).

## Consequences

- Both agents get the same OpenSpec skills and read the same specs and rules.
- Kiro's spec workflow (requirements-first `.kiro/specs/`, task execution state) is no longer
  used in this repository.
- Features and bugfixes share one change layout by default; telling them apart needs a
  convention (folder prefix or a custom schema).
- Specs are plain Markdown in Git, so a later move to another framework changes the workflow
  wrapper, not the content.

## References

- [orcahouse-project#96](https://github.com/umccr/orcahouse-project/issues/96): AI docs and
  spec-driven development in the warehouse repos.
- [Warehouse meeting 2026-11-25](https://github.com/umccr/management-knowledgebase/blob/c64fc69/PlatformServices/Meetings/Project/2026-11-25-warehouse.md)
  (knowledge base, private repository): framework comparison and the decision to pilot OpenSpec.
- OrcaVault [decision 0002](https://github.com/umccr/orcavault/blob/main/docs/decisions/0002-adopt-openspec.md).
