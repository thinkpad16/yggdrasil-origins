# Changelog

All notable changes to Yggdrasil Origins. Dates are the release date, newest first.

## Unreleased

Work since 1.0.0. Minecraft 1.21.1, Fabric.

### Content

- **25 origins and 55 paths**, 346 abilities. Umbral (Nichnytsia) was removed outright, with
  its three paths and every ability behind them.
- **The Beaten Track** (human) is no longer tied to the block underfoot: trodden ground around
  the player stacks three times — a trail, a road, a paved street — up to +9% movement speed
  and +12% roll distance. Raw stone and andesite left the road tag, so a cave is not a road.
- **The pack** (werewolf) reads at a glance: the leader gains +3% rogue and +4% warrior weapon
  damage per pack member within 20 blocks, three at most; a follower sees their leader out to
  30 blocks. The leader's lone-beast penalty is half what it was.
- **Bloodscent** (orc) has no visual at all: instead of making the wounded glow through walls,
  a wounded creature within 10 blocks gives the orc +5% movement speed and +6% warrior weapon
  damage.

### Looks

- **Customisation is colours only.** Players pick the colour of each element they wear and
  nothing else. The full editor is kept in the tree, behind `VisualStyle.TRIM_ENABLED`, and
  every other setting is dropped wherever a look is read — so a look saved while it was on,
  or sent by a changed client, cannot move an element.
- **Elements sit on the body.** Nine of them hovered a fraction above the skin, which read in
  game as a plate floating off a shoulder; all of them now reach into it. Elements armour does
  not take off stand clear of the armour shell, and worn pieces — bracer, armband, shoulder
  plates — are taken off by it instead.
- **Nothing is washed out by clouds.** The lit passes wrote colour but no depth, so clouds,
  drawn after the entities, were painted over those pixels. They are drawn as a solid cutout
  at full brightness now.
- **Element size is reined in** to 0.80-1.10 of the original, because growing one was what
  pushed it off the body in the first place.
- Amberborn lost its vest and its back slab: what is left is the amber that broke through.
  Mara's braid, the orc's war paint, the dragonborn's crest and the human's headband are
  elements an origin offers rather than wears.
- 57 elements, each belonging to exactly one origin family.

### Checks

- The element check now counts `CubeDeformation`, and fails the build on an element that does
  not touch the body, a piece that can never be seen under the second skin layer, an element
  armour would swallow, two marks of one origin inside each other, an element named in the data
  but gone from the code, and an atlas region nothing draws from.
- `check_symmetry` runs as part of `checkData`.

## 1.0.0 — 2026-11-05

First public release. Minecraft 1.21.1, Fabric.

### Content

- **50 origins** in two layers: 22 races plus Human in the class layer, 27 paths in the path
  layer. Human is a true vanilla baseline — no abilities, no attribute changes, no visual parts.
- **249 abilities** as plain JSON data, covering attributes, passives, periodic actions, combat
  reactions, damage modifiers, food, effects, blocks and item restrictions.
- **Impact tiers match the numbers.** Every origin was measured: the tier reflects the actual
  sum of its modifiers, and no origin has a positive net — risks outweigh the gain everywhere.
  4 low, 6 medium, 12 high, 1 neutral.

### Systems

- **Own power engine.** No Origins, no Apoli: the only hard dependencies are Fabric Loader,
  Minecraft and Fabric API. The engine keeps a prepared per-player layout, so a server tick
  costs about 5 microseconds on average — 0.01% of the tick budget.
- **Class looks with matching hitboxes.** Height, build, tint and up to 27 visual parts, driven
  from a single source so the model and the collision box can never drift apart. Every class
  fits a two-block gap in both width and height.
- **Picker screen** with search, impact filter, a live portrait and a read-only "My Class"
  reference on the L key.
- **Ability bars** for cooldowns and resources, animated and toggleable per player.
- **Admin tooling**: `/ygg doctor` (health report), `/ygg stats` (class spread), `/ygg perf`
  (engine cost), `/ygg debug` (per-player engine state). Every `/ygg` subcommand accepts any
  selector and returns a count usable in `execute store result score`.

### Compatibility

- The RPG Series stack (`spell_power`, `spell_engine`, `mrpg_lib`, `attributes_ld`,
  `combat_roll`, `ranged_weapon_api`, `critical_strike`) is a **soft** dependency. Without it
  the mod boots on plain Fabric, keeps 143 of 249 abilities working, and says so plainly at
  startup and in `/ygg doctor` — missing attributes are reported as "install the mod", never as
  a broken install.
- If Origins is installed alongside, a built-in datapack silences its default origins so players
  are not asked twice. The "My Class" key is L, because Origins already owns O.
- All mixins target vanilla only, never another mod.

### Localisation

- Every player-facing string is a translation key; the mod ships `en_us` and `uk_ua`.
- Anyone can reword the mod or add a language with a resource pack, without touching the jar.
  The build fails if a class or ability is missing a key in any shipped language.
