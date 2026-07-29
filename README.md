<h1 align="center">Goat Pit Indicators</h1>

Goat Pit Indicators is a RuneLite plugin that shows how full a goat pit is at a glance, and reminds you when an emptied pit still needs its spikes put back. The plugin draws the pit's state directly on the pit itself: a coloured fill and an `X / 20` count you can read from across the area, so you never have to walk over and count by hand.

## Features

- **See how full a pit is without walking to it**

  Every goat pit in view carries a live `X / 20` count and a colour fill that reads at a glance — green when the pit is full, a neutral fill while it is filling up.

- **Never forget the spikes**

  When a pit is empty and its spikes have not been re-added, the fill turns red and an "Add Spikes" prompt appears on the pit. An empty pit that still has its spikes is left neutral, so the warning only shows when it actually matters. You can also switch on a notification that fires the moment a nearby pit loses its spikes.

- **Make it yours**

  The full, partial, and needs-spikes fill colours, the count label, the "Add Spikes" prompt, and the draw distance are all configurable. Turn the whole overlay off from the config panel when you don't need it.

## Configuration

| Setting | What it does | Default |
|---|---|---|
| Show overlay | Master toggle for the on-pit overlay | On |
| Show count | Show the `X / 20` count label | On |
| Show "Add Spikes" | Show the spikes reminder at `0 / 20` | On |
| Full: outline only | Draw a full pit as outline only, no fill | Off |
| Needs spikes: outline only | Draw a spikes-needed pit as outline only, no fill | Off |
| Full colour | Fill when the pit is full | Green |
| Partly full colour | Midpoint of the outline gradient | Gold |
| Needs-spikes colour | Fill/outline when the pit needs spikes | Red |
| Label colour | Colour of the count and "Add Spikes" text | White |
| Needs-spikes notification | Notify when a nearby pit loses its spikes | Off |
| Max draw distance | How far away pits still draw (tiles) | 32 |

## Links

- [Report a bug](https://github.com/Oveduumnakal/Goat-Pit-Indicators-Plugin/issues/new?template=bug_report.yml)
- [Request a feature](https://github.com/Oveduumnakal/Goat-Pit-Indicators-Plugin/issues/new?template=feature_request.yml)
- [Buy me a coffee](https://buymeacoffee.com/oveduumnakal)
