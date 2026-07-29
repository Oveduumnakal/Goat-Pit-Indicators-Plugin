# In-game discovery — goat pit ids

**Status: NOT YET DONE.** This file is the template to fill in during a
developer-mode session at a goat pit. Until it is filled in, the plugin runs on
the structural heuristics described in `GoatIds.java`, two of which are unverified
guesses (see the bottom of this file).

## How to run

```
./gradlew run
```

RuneLite launches in developer mode with the plugin loaded. Log in, enable
**Goat Indicators** in the config panel, and travel to a goat pit. Open **Dev
Tools** (the wrench icon added in developer mode).

## What to capture

### 1. Pit object id(s) and location

Dev Tools → **Scene** → hover or right-click the pit tile.

| State | Object id | Notes |
|---|---|---|
| Empty | | |
| Partially full | | |
| Full | | |

- South-west `WorldPoint` of the pit: `___`
- Footprint size (2x2 / 3x3): `___`
- Does the object id change per state, or is it one id with impostors? `___`

→ If ids are stable, put them in `GoatIds.PIT_OBJECT_IDS` to pin detection.

### 2. Goat count source

Dev Tools → **Varbits**. Add or remove a goat and watch for a varbit that steps
0 → 20.

- Count varbit id: `___`
- Steps 0 → 20 as goats are added? `___`
- Is it the varbit the object composition declares (`getVarbitId()`)? `___`

→ If the count varbit is **not** the object's own declared varbit, set
`GoatIds.COUNT_VARBIT_OVERRIDE` to it.
→ If no varbit moves at all, the plugin falls back to counting goat NPCs on the
footprint; record that here and leave `COUNT_VARBIT_OVERRIDE = -1`.

### 3. Spikes state

Remove and re-add spikes.

- Does a varbit flip? Which id: `___`
- Or does the pit object id / composition change instead? `___`
- Does the "Add spikes" menu action appear only when unspiked? `___`

→ If a varbit flips, set `GoatIds.SPIKES_VARBIT_OVERRIDE` to it (0 = unspiked).
→ Otherwise the action-based heuristic in `spikedFromActions` stands.

### 4. Per-player or shared

- Is the count per-player (instanced / VarPlayer) or shared across everyone? `___`

## Heuristics currently unverified

Confirm or replace these two during the session — they are the most likely
things to be wrong in-game:

1. **The object's declared varbit equals the goat count.** Assumed by `readCount`.
2. **An "Add spikes" action means the pit is unspiked.** Assumed by
   `spikedFromActions` / `readSpiked`.
