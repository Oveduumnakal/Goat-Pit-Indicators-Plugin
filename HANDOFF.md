# Handoff — Goat Pit Indicators

Written 2026-07-29. Plan file: `/home/deck/.claude/plans/gleaming-launching-papert.md`.
Renamed from "Goat Indicators" → **Goat Pit Indicators** (display name only; the
Java package `com.oveduumnakal.goatindicators` and class names are unchanged).

## Next action

Add `banner.png` / `icon.png` art, then cut Release 1.0 (tag `R-1.0` once the
`Release 1.0` milestone has zero open issues). The overlay is verified live and
CI is proven green (see Status), so nothing else blocks a release.

## Status

- **Build GREEN**: `./gradlew build` passes compile, tests, `check-style.py`,
  javadoc, no warnings.
- **In-game discovery DONE** — real ids confirmed, overlay verified live.
- **GitHub repo LIVE**: https://github.com/Oveduumnakal/Goat-Pit-Indicators-Plugin
  — `main` pushed, 11 labels, Discussions on, `Release 1.0` milestone.
- **Version 1.0** in `runelite-plugin.properties`, matching the milestone.
- **CI proven green end to end**: PR #2 ran `build` + `conventions` both green,
  then was closed as out of scope (see Not done #1). No open PRs or issues.

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

1. **First real PR still to merge.** The PR flow is proven (PR #2 opened from a
   feature branch with `Closes #1` + milestone, both checks green) but that PR
   was a needs-spikes **notification** the account holder ruled **out of scope**,
   so it was closed and issue #1 closed as not planned — no notification code is
   on `main`. Next genuinely-new change gets the first merged PR. Convention:
   PR body needs `Closes #<issue>` and a milestone or `pr-checks.yml` fails.
2. **No `banner.png` / `icon.png`** — README omits image refs so nothing is
   broken; add art before a wider release.
3. **Client relaunch etiquette** — the account holder asked not to restart their
   running client without a say-so. Build locally; only `./gradlew run` on
   request. (Kill stale clients with `pkill -9 java` — several piled up earlier.)

## Repo conventions (inherited from Stockpile)

- Branches: `type/type-issue#-short-title`, e.g. `feature/feature-1-pit-overlay`.
- PR body must contain `Closes #<issue>` and carry a milestone.
- Releases: tag `R-<version>`; the release workflow needs a matching
  `Release <version>` milestone with zero open issues.
