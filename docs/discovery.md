# In-game discovery — goat pit

Captured 2026-07-29 in a developer-mode session, via the plugin's own
`[goat-discovery]` logging (config toggle "Debug logging (developer)").

## Confirmed ids

| Thing | Id | Notes |
|---|---|---|
| Pit object | `19750` | See caveat below — its cache definition is empty. |
| Wyrmscraid Goat (NPC) | `16298` | The goat caught by the pit. |
| Ground item near pit | `62343` | Reported in-game; role not yet confirmed. |

## Caveat: object 19750 carries no state

Reading `getObjectDefinition(19750)` from the cache returns an empty shell:

```
object def: id=19750 name='null' declaredVarbit=-1 declaredVarPlayer=-1 actions=[] impostorIds=null
```

So the original heuristics **cannot work** on this pit:
- there is no declared varbit → the "object varbit = goat count" assumption fails;
- there are no menu actions → the "offers 'Add spikes' ⇒ unspiked" assumption fails.

All pit state is held in **VarPlayer 5706** instead.

## Pit state lives in VarPlayer 5706

Action-to-varbit mapping observed (spikes started present, empty pit):

| Action | 15724 | 15725 | 15726 |
|---|---|---|---|
| Login: spiked, empty | 1 | (0) | 1 |
| Caught a goat | (1) | 1 | 2 |
| Emptied the pit | 0 | 0 | 0 |
| Re-added spikes | 1 | (0) | 1 |

Parenthesised values were unchanged at that step.

Decoded meaning:
- **`15724` = spikes present.** `1` = spiked, `0` = needs spikes. Drops to 0 when
  the pit is emptied (spikes consumed), returns to 1 when spikes are re-added.
- **`15725` = goat caught.** `1` = a goat is in the trap awaiting collection.
- **`15726` = stage.** `0` = empty & unspiked, `1` = spiked & ready, `2` = goat
  caught. A superset of the two booleans above.
- `15727` = set to 1 at login, did not move afterwards — role unknown, ignored.

Lifetime tally (not pit state): varbit `11625` (varp `4923`) stepped 17 → 18 when
the pit was emptied — a running count of goats caught.

## Implication for the plugin

This pit is a **single-catch trap**, not a 0–20 fill. The `X / 20` count model in
the original plan does not apply. The overlay should instead show three states:

- **needs spikes** — `15724 == 0`
- **spiked, waiting for a goat** — `15724 == 1` and `15725 == 0`
- **goat caught, ready to collect** — `15725 == 1`

Open question still to confirm with the account holder: can the pit hold more than
one goat at once (is there any count), or is it strictly one-at-a-time?
