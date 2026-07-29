# Handoff — Goat Indicators

Written 2026-07-29. Plan file: `/home/deck/.claude/plans/gleaming-launching-papert.md`.

## Next action

Run `./gradlew build` in this folder. It has never been run — nothing here is
compile-verified yet.

## What exists

Local folder only: `/home/deck/Documents/GitHub/Goat-Indicators-Plugin`.
No git repo, no remote, no commit.

**Scaffolding**, copied from `../Pricewatch-Plugin` (same layout as Stockpile):

- `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `.gitignore`, `LICENSE` — verbatim
- `.github/workflows/{build,pr-checks,release}.yml` — verbatim
- `.github/{FUNDING,dependabot}.yml` — verbatim
- `scripts/check-style.py` — verbatim (13 style rules, wired into `./gradlew check`)
- `.gitattributes` — copied minus the `*.snapshot` stanza (no persisted schema here)
- `build.gradle` — copied, `pluginMainClass` repointed to `GoatIndicatorsPluginTest`
- `settings.gradle` — `rootProject.name = 'goatindicators'`
- `runelite-plugin.properties` — written fresh, version 0.1
- `.github/ISSUE_TEMPLATE/*.yml` — rewritten for this plugin; area dropdowns are
  now Pit overlay / Goat count accuracy / Spikes prompt / Config / Other

**Source**, package `com.oveduumnakal.goatindicators`:

| File | Role |
|---|---|
| `GoatIds.java` | All ids, name fragments, `PIT_CAPACITY = 20`, varbit overrides |
| `GoatPitState.java` | Count + spiked, clamped; `isFull` / `needsSpikes` / `label()` |
| `GoatPitTracker.java` | Collects pits from spawn events, reads count and spikes state |
| `GoatIndicatorsConfig.java` | Toggles, three fill colours, text colour, draw distance |
| `GoatPitOverlay.java` | Unions the pit's tile polys, fills, draws `X / 20` + "Add Spikes" |
| `GoatIndicatorsPlugin.java` | Wiring, event subscriptions, overlay add/remove |

**Tests**: `GoatPitStateTest` (boundaries at 0/1/19/20, clamping, label),
`GoatPitTrackerTest` (name matching, `spikedFromActions`). No Mockito — the
tracker's decision logic was extracted into static package-private methods
specifically so it could be tested without stubbing `Client`. That was a
deliberate change from the plan, which assumed mocked-client tests.

## Design decision worth knowing

The goat pit has **no constants in runelite-api 1.12.33** — there is no
`GOAT_PIT` object, varbit, or NPC id in the shipped `gameval` classes. Rather
than block on that, the plugin identifies things structurally:

- **Which object is a pit** — `ObjectComposition.getName()` contains `"goat pit"`.
  `GoatIds.PIT_OBJECT_IDS` is an empty allowlist; fill it in to pin detection to
  exact ids instead.
- **Goat count** — the varbit the pit's own composition declares
  (`ObjectComposition.getVarbitId()`), which is what drives its multiloc state.
  Falls back to counting goat NPCs standing on the pit footprint if there is none.
- **Spikes** — the pit is unspiked if its current composition still offers an
  "Add spikes" action.

The first two are sound. **The spikes heuristic and the assumption that the
object varbit equals the goat count are both unverified guesses** and are the
most likely things to be wrong in-game.

## Not done

1. `./gradlew build` never run. Expect compile errors and style-check failures on
   the first pass — `check-style.py` is strict (Javadoc on every type, no `//`
   comments, Allman braces, 120 cols, wrapped 3+ link chains).
2. No `README.md`, no `banner.png`/`icon.png`, no `docs/discovery.md`.
3. GitHub repo not created. Still to do: `gh repo create
   Oveduumnakal/Goat-Indicators-Plugin --public`, push `main`, recreate
   Stockpile's 11 labels (`bug` d73a4a, `enhancement` a2eeef, `documentation`
   0075ca, `duplicate` cfd3d7, `invalid` e4e669, `question` d876e3, `wontfix`
   ffffff, `In Progress` fbca04, `dependencies` 0366d6, `github_actions` 000000,
   `maintenance/architecture` c5def5), enable Discussions (the issue-form
   `config.yml` links to it), and create the `Release 0.1` milestone —
   `pr-checks.yml` fails any PR without one.
4. **In-game discovery, still blocking correctness.** With `./gradlew run`, at a
   goat pit, in developer mode, capture: the pit's object id(s) per state, the
   varbit that steps 0→20 as goats are added, the varbit or action that flips
   with spikes, and whether the count is per-player. Then update `GoatIds` and
   write `docs/discovery.md`.

## Repo conventions inherited from Stockpile

- Branches: `type/type-issue#-short-title`, e.g. `feature/feature-1-pit-overlay`.
- PR body must contain `Closes #<issue>` and the PR must carry a milestone, or
  `pr-checks.yml` fails.
- Releases: tag `R-<version>`; the release workflow refuses to run unless a
  matching `Release <version>` milestone exists with zero open issues.
