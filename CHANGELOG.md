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
- Amberborn lost its vest, its back slab and the shards behind the shoulders: what is left is
  the amber that broke through the collarbone and the forearms.
  Mara's braid, the orc's war paint, the dragonborn's crest and the human's headband are
  elements an origin offers rather than wears.
- **Every element is painted its own way.** The atlas used to hand one pattern to a dozen
  regions: eleven marks were "plain cloth", seven were "strands", seven were "scales". Each
  element now has a pattern written for it and for the origin that wears it - an orc's ivory
  is chipped, a werewolf's enamel is clean, a dwarf's beard is caught in metal bands, a
  viper's back runs in diamonds while a dragon's is rounds, a halfling knits and a Cossack
  embroiders. Three marks that borrowed another origin's texture - a half-giant's brow, a
  halfling's hair, a werewolf's fangs - were given regions of their own, so nothing a class
  wears looks like anything another class wears.
- **Every element moves its own way too.** Four tails used to share one wave with different
  numbers; now a wolf wags on its own clock and lifts the tail as the stride lengthens, a
  devil's whip hangs still and snaps from root to tip, a serpent's never stops and rolls as
  each bend passes, and a dragon's answers the stride late and heavy. The three rings of
  light were one orbit; now fire climbs where it stands, aurelite's dust drifts on its own
  ellipses and ice holds a rigid formation that turns as one. A halfling's curls bounce on
  the stride and a cantharis lifts its shell when it runs. No two elements share an idle
  rhythm any more, so two origins standing together never sway in time.
- **More to look at**: a half-giant's brow grew a boss over each eye, a serpentfolk hood a
  stiff rib and a scalloped edge, an undead spur two smaller breaks beside it, and a
  ferrine's studs now sit on a strap that runs the length of the spine.
- Every element belongs to exactly one origin family.

### Server cost and stability

- **One look instead of three.** Powers that ask the same question about the ground, the
  creatures or the players around them now share one answer: it is kept per player while they
  stand on the same block, and it is a count, so the tiers of one path read a single scan.
  Counting stops at the largest number any power asks about.
- **Block scans walk chunk by chunk** - one chunk lookup instead of one per block - and never
  load a chunk that is not in memory: a bonus is not worth generating terrain for.
- **Counting the pack reads the level's player list** rather than querying entities in a box,
  and asks whether a player wears a class without copying their choices first.
- **A power that throws is switched off**, loudly, instead of costing the server its tick -
  while ticking, on a hit, on a kill saved from death and on a broken block. `/ygg perf` says
  how many were put aside, and what looking around costs.
- **The bar packet goes out once per tick** per player, however many powers moved together.

### Fixes

- **Arm marks sit on slim arms.** A slim skin's arm is three pixels wide, not four, and the
  whole missing pixel comes off its outer side - so every piece cut for the wide arm hung a
  pixel off the sleeve. Geonite's stone fists, the dwarf's vambrace, the dragonborn's claws,
  the human's armband and amberborn's spurs are brought the full pixel in now (they were only
  brought half of it), in the third-person body and in the hand its owner sees. The ferrine's
  shoulder plates and the angel's pauldrons rest on the arms rather than the body, so they
  follow it in too.
- **The brow and the hair tuft are painted from the player's own skin again.** Giving them
  regions in the atlas sent their UVs off a 64x64 skin, which is why they came out
  see-through. `check_uv` now fails the build on any skin-mesh piece whose UV leaves the
  skin, or any prop whose UV leaves the atlas.
- The werewolf's teeth are gone; it keeps its ears and its tail. 56 elements.

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
