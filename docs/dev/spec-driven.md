# Spec-driven development

We specify changes before building them. The framework is OpenSpec
([ADR 0011](../decisions/0011-adopt-openspec.md)); the rest of this page is how we use it.

## Quickstart

New here, with only Claude Code or Kiro? Start in the repo root, on a branch.

1. **Install the CLI.** `brew bundle` installs `openspec` from the [Brewfile](../../Brewfile).
   For Java, Maven and a database, follow the [README Quickstart](../../README.md#quickstart).

2. **Start your agent** (`claude` or `kiro-cli chat`). It loads `AGENTS.md`, the repo rules, by
   itself.

3. **See what the API already promises.** Run `openspec list --specs`, then ask the agent:

   > Explain the api-pagination spec in plain words.

4. **Think before you build.** Describe your idea and ask:

   > openspec explore: I want to add X. What would it touch?

   Or start from an issue:

   > openspec explore: read https://github.com/umccr/orcahouse-project/issues/NN, including its
   > comments. What would it touch?

5. **Propose the change.**

   > openspec propose: add X

   This creates `openspec/changes/<name>/` with a proposal, design, spec changes and tasks. Read
   them, and edit or ask for changes until they are right. If a maintainer should agree before
   you build, open a draft PR with just the change folder and get it reviewed first. Do this for
   anything that changes the API contract, module boundaries or an existing spec. Small,
   self-contained fixes can go straight on.

6. **Build it.**

   > openspec apply

   The agent works through `tasks.md` and ticks each task off. You validate with `mvn verify`
   and, for DB-facing changes, against the local mock database
   ([offline-mockdb.md](offline-mockdb.md)).

7. **Finish.**

   > openspec archive

   The spec changes merge into `openspec/specs/`. Update `docs/` and add an ADR if the change
   needs one (see below), then raise a PR.

In Claude Code you can also type the skills as commands, e.g. `/openspec-propose`.

## OpenSpec drill

Try OpenSpec without leaving anything behind. Enter the following lines one by one into your
agent prompt and have a chat.

```
openspec new change tek1
openspec status --change tek1
openspec instructions proposal --change tek1
openspec list --specs
openspec show api-pagination --type spec
rm -rf openspec/changes/tek1
```

## Agents

- Two agents are supported: Kiro (`kiro-cli`) and Claude Code. Neither owns the process.
- Both agents get the same skills, generated into `.agents/skills/` and linked from
  `.claude/skills/` and `.kiro/skills/`: `/openspec` (the entry point), `/openspec-propose`,
  `/openspec-apply-change`, `/openspec-archive-change` (plus `explore`, `update-change`,
  `sync-specs`). `openspec update` regenerates them.
- Agent rules live only in `AGENTS.md`. Kiro loads it natively; `CLAUDE.md` imports it.
- Code conventions live in `docs/` (architecture overview, `docs/dev/jooq.md`,
  `docs/dev/spring.md`), so people and agents read the same text. Both agents load them every
  session: `CLAUDE.md` imports them and `.kiro/steering/overview.md` inlines them via
  `#[[file:...]]`. Do not add rules to steering; edit the docs.
- OpenSpec project context and per-artifact rules: `openspec/config.yaml`.
- Telemetry: OpenSpec telemetry is off for Claude Code via `.claude/settings.json`. Elsewhere
  run `openspec config set telemetry.enabled false` once per machine.

## Where things live

OpenSpec covers only behaviour and the change in flight. A change's `design.md` is frozen when
the change is archived, so long-lived context lives outside OpenSpec:

| Question | Home |
|---|---|
| What must it do? | `openspec/specs/` (living, updated only by archiving a change) |
| How is it built now? | `docs/architecture/` (living, updated in the same change) |
| How do all endpoints behave? | `docs/api/` (living) |
| Why this way? | `docs/decisions/` (one record per real decision) |
| What did one change do? | `openspec/changes/archive/` (history, not maintained) |

When archiving, promote shared API behaviour to `docs/api/`, structural outcomes to
`docs/architecture/` and decisions to `docs/decisions/`. See [docs/README.md](../README.md).

## Existing features

A feature built before OpenSpec gets a retrospective baseline change before anyone changes it:

- The change records current behaviour only. It changes no code, and all its tasks are done.
- Write the spec from the code and tests, not from memory or old docs.
- Put the rationale in an ADR, citing the issues, PRs and meeting notes. Say that it was
  recorded after the fact.
- Archive it. Later work then modifies the living spec instead of rewriting docs.

The two features first specified as Kiro specs were moved into the archive this way:
`2026-11-10-spring-pagination` and `2026-11-17-offline-mockdb`. Each keeps its original Kiro
`requirements.md`, `design.md` and `tasks.md`, and adds a `proposal.md` and OpenSpec spec deltas
that were archived into `openspec/specs/` (`api-pagination`, `dev-profiles`, `mockdb`).
