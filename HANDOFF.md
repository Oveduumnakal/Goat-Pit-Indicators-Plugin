# Handoff — Goat Pit Indicators

Written 2026-07-29, updated 2026-07-29 (Release 1.0 cut, hub PR submitted).
Plan file: `/home/deck/.claude/plans/gleaming-launching-papert.md`.
Renamed from "Goat Indicators" → **Goat Pit Indicators** (display name only; the
Java package `com.oveduumnakal.goatindicators` and class names are unchanged).

## Next action

Watch the plugin-hub PR — https://github.com/runelite/plugin-hub/pull/14452 —
and respond to any maintainer review. Their `build` check runs the plugin at the
pinned commit `6323a22`; a first-time contributor's workflow may need maintainer
approval before it runs. Nothing on our side is outstanding.

## Status

- **Release 1.0 SHIPPED**: tag `R-1.0` (commit `6323a22`), GitHub release
  "Release 1.0" published + marked latest, `Release 1.0` milestone auto-closed.
- **Plugin-hub PR OPEN**: runelite/plugin-hub#14452 adds `plugins/goat-pit-indicators`
  (repository + `commit=6323a22`). Submitted from fork `Oveduumnakal/plugin-hub`,
  branch `add-goat-pit-indicators`.
- **Artwork DONE**: `banner.png` (root, in README), `icon.png` 48x48 (root, under
  the hub's 48x72 max), source kept at `docs/icon-source.png`. Four overlay
  screenshots in `docs/images/`, shown in the README (merged via PR #5, issue #3).
- **Build GREEN**: `./gradlew build` passes compile, tests, `check-style.py`,
  javadoc, no warnings.
- **In-game discovery DONE** — real ids confirmed, overlay verified live.
- **GitHub repo LIVE**: https://github.com/Oveduumnakal/Goat-Pit-Indicators-Plugin
  — `main` pushed, 11 labels, Discussions on.
- **Version 1.0** in `runelite-plugin.properties`.
- **First real PR merged**: PR #5 (artwork/screenshots) — the flow is proven end
  to end. No open PRs or issues on our repo.

## Plugin-hub compliance (verified against runelite/plugin-hub README)

All requirements met at submission: `runelite-plugin.properties` has every
required key (`displayName`, `author`, `description`, `tags`, `plugins`,
`build=standard`); `plugins=` class exists; `icon.png` 48x48 ≤ 48x72; BSD
2-Clause license; public repo; main code bundles no classpath resources (no
`getResource()` risk). `build=standard` means the hub replaces `build.gradle` at
review, so the local `mavenLocal()` / `latest.release` / Test-class-as-`run`-main
bits do not affect the hub build. To re-submit at a new version: tag `R-<v>`,
then bump `commit=` in the hub file to the new tag's hash.

## Discovery results (confirmed in-game)

Recorded in full in `docs/discovery.md`. Key facts:

| Thing | Id / varbit | Notes |
|---|---|---|
| Pit **game object** | `62343` | What the overlay draws its footprint on. |
| Pit **ground object** | `19750` | Beneath the pit; empty composition, not used. |
| Wyrmscraid Goat (NPC) | `16298` | The goat the pit catches. |
| **Count** | varbit `15725` | Part of VarPlayer 5706; steps 0→20. Verified 1→2→3. |
| **Spikes** | varbit `15724` | 1 = spiked, 0 = needs spikes. |
| Capacity | 20 | Confirmed by the account holder. |

The pit is **not** a standard multiloc object — object 62343/19750 carry no
declared varbit or actions, so the original "declared varbit = count" and
"'Add spikes' action = unspiked" heuristics were both wrong. State lives entirely
in VarPlayer 5706. `GoatIds.COUNT_VARBIT_OVERRIDE` / `SPIKES_VARBIT_OVERRIDE` are
set to these ids; `PIT_OBJECT_IDS = {62343}`.

## Overlay behaviour (as built, to the account holder's spec)

- Outline always drawn; colour runs **red → gold → green** with the count.
- Unspiked pit: solid red outline.
- Full (20/20): green fill.
- Empty **and** unspiked (`0/20`): red fill + "Add Spikes" text, count hidden.
- Every other state: outline only + `n / 20`.
- Config toggles **Full: outline only** / **Needs spikes: outline only** suppress
  those two fills.
- Default colours: full `#AF00FF00`, partial `#AFFFDD00`, needs-spikes `#4BFF0000`.

## Colour caveat

The new default colours only apply to a **fresh** config. A profile that already
ran the plugin has the old colours saved and will not pick up the new defaults —
reset the three colour settings by hand, or use a clean profile, to see them.

## Discovery logging (shipped, default off)

`GoatPitDiscovery.java` + the **"Debug logging (developer)"** toggle log pit ids
and varbit changes to `~/.runelite/logs/client.log` (grep `goat-discovery`). It
reads pit definitions straight from the object cache and filters the varbit
firehose to the count range. Left in as a maintenance aid; safe to ship off.

## Source layout — package `com.oveduumnakal.goatindicators`

| File | Role |
|---|---|
| `GoatIds.java` | Ids/varbits: pit object `62343`, count `15725`, spikes `15724`, capacity 20 |
| `GoatPitState.java` | Count + spiked, clamped; `isFull` / `needsSpikes` (= unspiked) / `label()` |
| `GoatPitTracker.java` | Collects pit game objects, reads count/spikes from the varbits |
| `GoatPitDiscovery.java` | Debug-only id/varbit logging |
| `GoatIndicatorsConfig.java` | Toggles, outline-only options, colours, draw distance, debug flag |
| `GoatPitOverlay.java` | Footprint outline (red→gold→green), fills, `n / 20` + "Add Spikes" |
| `GoatIndicatorsPlugin.java` | Wiring, event subscriptions, overlay add/remove |

Tests: `GoatPitStateTest`, `GoatPitTrackerTest` (JUnit 4, no Mockito — decision
logic is in static package-private methods so `Client` never needs stubbing).

## Not done

1. **Plugin-hub review pending** — PR #14452 is open but not yet merged by the
   RuneLite team; watch it and answer any review comments. This is the only open
   thread.
2. **Client relaunch etiquette** — the account holder asked not to restart their
   running client without a say-so. Build locally; only `./gradlew run` on
   request. (Kill stale clients with `pkill -9 java` — several piled up earlier.)

## Repo conventions (inherited from Stockpile)

- Branches: `type/type-issue#-short-title`, e.g. `feature/feature-1-pit-overlay`.
- PR body must contain `Closes #<issue>` and carry a milestone.
- Releases: tag `R-<version>`; the release workflow needs a matching
  `Release <version>` milestone with zero open issues.
