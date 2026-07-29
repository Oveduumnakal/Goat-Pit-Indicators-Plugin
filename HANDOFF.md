# Handoff — Goat Indicators

Written 2026-07-29, updated 2026-07-29 after the first build + local-work pass.
Plan file: `/home/deck/.claude/plans/gleaming-launching-papert.md`.

## Next action

**In-game discovery** — the only remaining correctness blocker. Run
`./gradlew run`, go to a goat pit in developer mode, and fill in
`docs/discovery.md` (template already written). Then update `GoatIds.java`.

## Build status: GREEN

`./gradlew build` passes clean — compile, tests, `check-style.py`, and javadoc,
no warnings.

Fixes applied this pass, both in `GoatPitTracker`:
- `countGoatsInside` called `npc.getPlane()`, which does not exist on
  `NPC`/`Actor` in runelite-api 1.12.33. Now reads the plane from
  `npc.getWorldLocation().getPlane()` with a null guard.
- Replaced the deprecated `Client.getNpcs()` with
  `client.getTopLevelWorldView().npcs()`.

## What exists

Local folder: `/home/deck/Documents/GitHub/Goat-Indicators-Plugin`.
**Git repo initialised** (branch `main`, one initial commit, 31 files). No
remote, not pushed.

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

## Done this pass

1. `./gradlew build` runs green (see Build status above).
2. `README.md` written (features, config table, links).
3. `docs/discovery.md` written as a fill-in-the-blanks template for the in-game
   session.
4. Git repo initialised with an initial commit on `main`.

## Not done — needs the user

1. **In-game discovery, still blocking correctness.** See Next action. This is
   the one item that needs a live game client. Two heuristics are unverified
   guesses: (a) the object's declared varbit equals the goat count, (b) an
   "Add spikes" action means the pit is unspiked. Confirm or replace both.
2. No `banner.png` / `icon.png` — needs a design pass; README omits image refs
   for now so nothing is broken.
3. GitHub repo not created — outward-facing publish, left for explicit go-ahead.
   `gh` is authenticated as `Oveduumnakal`, so when ready: `gh repo create
   Oveduumnakal/Goat-Indicators-Plugin --public --source . --push`, recreate
   Stockpile's 11 labels (`bug` d73a4a, `enhancement` a2eeef, `documentation`
   0075ca, `duplicate` cfd3d7, `invalid` e4e669, `question` d876e3, `wontfix`
   ffffff, `In Progress` fbca04, `dependencies` 0366d6, `github_actions` 000000,
   `maintenance/architecture` c5def5), enable Discussions (the issue-form
   `config.yml` links to it), and create the `Release 0.1` milestone —
   `pr-checks.yml` fails any PR without one.

## Repo conventions inherited from Stockpile

- Branches: `type/type-issue#-short-title`, e.g. `feature/feature-1-pit-overlay`.
- PR body must contain `Closes #<issue>` and the PR must carry a milestone, or
  `pr-checks.yml` fails.
- Releases: tag `R-<version>`; the release workflow refuses to run unless a
  matching `Release <version>` milestone exists with zero open issues.
