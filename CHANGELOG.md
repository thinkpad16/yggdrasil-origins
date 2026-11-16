# Changelog

All notable changes to Yggdrasil Origins. Dates are the release date, newest first.

## Unreleased

Work since 1.0.0. Minecraft 1.21.1, Fabric.

### Content

- **25 origins and 51 paths**, 334 abilities. Umbral (Nichnytsia) was removed outright, with
  its three paths and every ability behind them.
- **The Beaten Track** (human) is no longer tied to the block underfoot: trodden ground around
  the player stacks three times — a trail, a road, a paved street — up to +9% movement speed
  and +12% roll distance. Raw stone and andesite left the road tag, so a cave is not a road.
- **The pack** (werewolf) reads at a glance: the leader gains +3% rogue and +4% warrior weapon
  damage per pack member within 20 blocks, three at most; a follower sees their leader out to
  30 blocks. The leader's lone-beast penalty is half what it was.
- **The mara**: Waking Dread gives +5% dagger damage, down from +12%. Unseen works in dusk as well as darkness (light 7 or lower, was 3 or lower, which open night never reaches), and after she hits or is hit she cannot vanish for 30 seconds.
- **Trodden Road** (human path) is gone; Levy is the human's only path.
- **Mermaid Ring** (mara path) is gone; Veil is the mara's only path.
- **The serpentfolk**: Serpent Blood gives +5% rogue weapon and dagger damage, down from +11%.
- **The werewolf**: Night Hunter works by night only (wherever he stands) and is small: +10% movement speed, +3% crit chance, +5% rogue weapon damage, no warrior bonus. Claws give +3% dagger damage, down from +32%. By day in shade neither Night Hunter nor Sun-Weary applies.
- **The dragonborn**: Dragon Scales give +4 armor, down from +8.
- **The halfling**: Sling gives +5% dagger damage, down from +14%.
- **Shieldbreaker** (orc path) is gone; Bloodscent is the orc's only path.
- **Bloodscent** (orc) has no visual at all: instead of making the wounded glow through walls,
  a wounded creature within 10 blocks gives the orc +5% movement speed and +6% warrior weapon
  damage.
- **The vortan fights with Forcemaster knuckles.** Knuckles count as swords in that mod, so Dead Steel used to cut his damage by 46% with them; knuckles are now exempt. Empty Palm became Fist in Iron (+10% movement speed with knuckles in hand; the flat +4.5 unarmed damage is gone), Fists became Knuckles, and Iron Shirt holds with knuckles as well as with empty hands.
- **The mara**: Waking Dread gives +5% dagger damage, down from +12%. Unseen works in dusk as well as darkness (light 7 or lower, was 3 or lower, which open night never reaches), and after she hits or is hit she cannot vanish for 30 seconds.
- **Trodden Road** (human path) is gone; Levy is the human's only path.
- **Mermaid Ring** (mara path) is gone; Veil is the mara's only path.
- **The serpentfolk**: Serpent Blood gives +5% rogue weapon and dagger damage, down from +11%.
- **The werewolf**: Night Hunter works by night only (wherever he stands) and is small: +10% movement speed, +3% crit chance, +5% rogue weapon damage, no warrior bonus. Claws give +3% dagger damage, down from +32%. By day in shade neither Night Hunter nor Sun-Weary applies.
- **The dragonborn**: Dragon Scales give +4 armor, down from +8.
- **The halfling**: Sling gives +5% dagger damage, down from +14%.
- **Shieldbreaker** (orc path) is gone; Bloodscent is the orc's only path.
- **Bloodscent** (orc): the scent and Finish the Bloodied trigger below 10% health instead of 50%, and Finish the Bloodied gives +5% damage, down from +15%.
- **The orc**: Bloodlust starts below 20% health instead of 50% and gives +10% lifesteal and +10% warrior weapon damage, down from +49% and +37%.
- **Moon paths** (lunaris): Alone (Bond) is -1.5 hearts and Low Water (Tide) -0.5 hearts instead of +18% and +8% damage taken.
- **The vortan**: Fists give +5% attack speed, down from +13%, and the description now shows the real +30% knuckles damage. No attack-speed bonus goes above +8%.
- **Iron Burden** (ferrine path) is gone; Retribution is the ferrine's only path.
- **Retribution** (ferrine): Anvil is -4 hearts, up from -0.5; Retribution Spikes now deal 3% of his own max health per hit instead of 3% of the damage taken.
- **The sylph**: No Sky Above slows her by 10% instead of cutting air spell power by 25%.
- **Stonecaller** (geonite): Heart of the Rock gives +8% earth spell power, up from +5%.
- **The geonite** no longer sees through lava (Glow of the Rock is gone), and Unyielding reflects 5% of damage, down from 13%.
- **The dwarf** no longer sees through lava; Eyes for the Deep is gone. **Forgehand** now costs
  -10% movement speed, up from -8%.
- **Oreseeker** (dwarf) no longer stacks more digging on a race that already digs fast. Under
  stone he gets +8% movement speed, +15% axe damage and +2 armor, mirroring what the open sky
  takes from him; the new Ore Sense gives +25% block break speed with ore within 5 blocks.
- **The elf's Finesse** no longer boosts daggers; rogue weapon and archer melee damage stay +14%.
- **Fragility costs hearts, not damage taken.** The 25 always-on penalties that raised damage
  taken now lower max health instead, by the same effective amount: +X% damage taken became
  20 × X / (1 + X) health, rounded to the half heart, at least half a heart. Penalties that only
  apply under a condition (sun, snow, sprinting, the Nether, after a hit) still raise damage taken,
  so a condition switching on does not strip hearts.
- **One heart penalty per origin.** Elf, halfling and tiefling each had two abilities taking hearts;
  each now has one. Bodily Vulnerability is folded into the elf's Frail Frame (-4.5 hearts),
  Easily Thrown gives its hearts to Small Stature (-2.5) and Cursed Flesh to Demonic Blood (-1.5).
- **Venomstring** (elf) now has a cost: Thin String, -8% ranged damage. Light Hand is +3% rogue
  weapon damage, down from +6%.
- **Second Voice** (cantharis): Strained Voice is -3 hearts, up from -1.5.
- **Swarm Sense** (cantharis): Alone no longer cuts warrior damage; it slows by 10% instead of 5%.
- **Current** (undine): Thin Blood is -4 hearts and -2 armor, up from -2 hearts.
- **Potions answer conditional debuffs.** A potion now switches off a debuff that hangs on the surroundings, without granting the matching bonus: Water Breathing for undine Out of Element, Drying Out (renamed from the second "Evaporation") and Pearl Out of Water, and for the blazing, dwarf and ferrine water weaknesses; Invisibility for the sun debuffs of lunaris, mara, werewolf and Tide; Night Vision for aurelite Light Starved and Dawnsong After Dark; Slow Falling for sylph No Sky Above.
- **The aurelite**: Light Starved slows by 10% and cuts paladin damage by 6% (the data had -95% on paladin damage and -6% on speed, the reverse of its description). Sun-Weary's description now lists the numbers the data uses.
- **Pyromancer** (blazing): Kindling is -4 hearts and no longer takes armor.
- **Second Wind** (half-giant) no longer takes the roll away: Heavy Roll gives -15% roll distance
  and +30% roll recharge instead.
- **The undead**: Scythe and Guard is gone, Grave Reaper is halved (sickle +6.5%, rogue weapon +5%,
  axe +4.5%), and the new Stale Blood halves how long beneficial potions last on him, Fire
  Resistance included. Fire Resistance still switches Sunburn off entirely while it lasts.
- **Reaper** (undead): Harvest heals 10% of the damage the hit dealt instead of half a heart,
  and Dull Scythe became Withered, -3 hearts instead of -15% damage to healthy targets. The
  `heal` action takes `fraction_of_damage` for this.
- **No ability undoes another.** The angel gave +2 hearts and took 2.5; the orc gave 3.5 and took 2.
  Each now has one value, and it is a gain: the angel's Blessing is +2 hearts and the orc's Deep
  Reserves +4, while Mortal Frame and No Retreat no longer take hearts at all.

### Picker

- **The path step names the origin plainly**: the header reads "Path for the origin Ferrine" instead of "Path · Ferrine", which looked like the path was called Ferrine.
- **Ability texts read like a card.** The effect comes first with every number drawn bright, and
  the flavour line follows it in italics and quotes, a size smaller and in a muted shade of the
  ability's colour - on the same line when it fits, wrapping below when it does not. In the lang
  files the flavour is whatever follows the last `\n` of a description; 246 abilities have one.

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
- **The bar packet goes out once per tick** per player, however many powers moved together -
  a cooldown or resource change now waits for the end of the tick too.
- **Every player ticks on their own phase**: once-a-second work, scans included, no longer
  lands on the same tick for the whole server.
- **A class is granted in one go**, and the picker's class list is written once per set of
  choices, not for every player on every join, respawn and reload. A player's looks packet is
  written once, not for every pair of eyes that sees them.
- **No modifier trace that grows forever**: a conditional attribute switching on again no longer
  records itself a second time.

### Fixes

- **`moving` works on the server.** It read the player's delta movement, which stays near zero server-side, so "standing still" was always true: Unseen ignored steps, and the stand-still buffs (Resin Set, Bulwark Rooted, Rootbound Hold) never switched off. It now compares positions between checks.
- Six ability descriptions showed old heart counts after hearts were merged into one penalty per origin: Small and Easily Thrown (halfling), Frail (elf), Demon Blood and Cursed Flesh (tiefling), Hollow Frame (cantharis).
- Two Temptation descriptions carried a raw line break inside the lang string; it is an escaped \n now.
- **`modify_food` changes nutrition and saturation for players.** The hunger bar was fed before
  the change reached the food, so only `prevent_effects` worked. A stew still leaves its bowl.
- **Scans stop counting early again**: the numbers they count to were wiped right after loading.
  They now stop one past the largest number asked about, which answers every comparison.
- **Milk does not wash off an origin.** The endless effects a class gives pass through milk,
  `/effect clear` and a `clear_effect` action untouched, with no flicker; a potion of the same
  effect still washes off. The method is wrapped rather than cancelled, so other mods that keep
  their own effects through milk (Potions_LD's potion sickness) keep working beside it. Anything
  that removes one of them by name gets it back within a second.
- **Attribute powers with Apoli's wildcard or missing modifier id run.** `"id": "*:*"` threw
  while reading, so the whole power was reported as an unknown type - 71 of the Fallen Eden
  RPG pack's stat powers among them. Such a modifier now gets an id of our own.
- **`modify_harvest` with `"allow": false` forbids harvesting**, and wins over a power that
  allows it.
- **Marks that did not read.** Every origin was drawn with its real texture colours over a
  grey body and looked at one by one. Six came out too faint to tell apart from the skin at
  playing distance and were strengthened: blazing's embers are coals rather than specks and
  come in four sizes, a cantharis antenna is a stalk with a club at the tip and a knuckle
  midway instead of a hairline, an orc's tusks are half again as large, a dragonborn's crest
  stands a pixel taller, a half-giant's brow is a heavier shelf, and geonite's crystal is
  lighter and less gritty so it stops reading as mud.

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
- **The brow and the hair tuft read the right part of the skin.** At UV (0,0) their faces
  landed in the skin's unused corner - black on a downloaded skin, empty on a default one.
  The brow now takes the forehead and hairline, the tuft the top of the head.
- **The picker portrait is a whole figure in any game mode.** It shared the player's
  profile, so a spectator's portrait was a floating head with no class looks. It also wears
  the outer skin layer now, faces the viewer square-on, and keeps its wings folded.
- **Your own looks and hitbox survive death and a change of dimension.** The old body's
  unload made the client forget its own profile, which the server sends only on join.
- **A seated player sits in the seat**: the scaled hitbox dropped the vanilla riding point,
  so every scaled player rode half a block above a boat, cart or horse.
- **An elytra is not a chestplate**: it hides what is on the back, no longer the gorget,
  the sash or the arm pieces. Anything on the head (a pumpkin, a skull) covers head parts.
- **Parts flash red with the body when hurt**, and show on an invisible teammate whose
  body you can see.
- **Mirrored pairs mirror**: a roll, a variant tilt or a sideways nudge turns both ears,
  horns, wings or arm pieces outward together instead of one in and one out. The
  first-person hand no longer adds the chestplate's pixel to a bare arm.
- **Wings open at the same speed at any frame rate**, and a second draw in one frame (the
  shader shadow pass) no longer pushes them along.
- **Night-vision strength dims only the class's own night vision**, never a potion; lava
  sight is the vanilla fire-resistance view and never worse than what a spectator or a
  fire-resistant player already has; fluid walking agrees between client and server
  (sneaking sinks on both, and only water-tagged fluids count as water).
- **Leaving a server forgets its powers on the client**: lava walking, dimmed night vision
  and lava sight no longer follow the player to the next world.
- **The visuals key is the player's own.** It wrote the shared config, so in singleplayer
  and LAN it switched looks and hitboxes off on the server for everyone. It now has a
  setting of its own, `showVisuals`.

- **The picker stays open between steps.** Choosing an origin used to close the screen and
  open a new one for the path, so it blinked and the cursor jumped to the middle. The next step
  now arrives in the same screen, and it closes by itself when nothing is left to choose.
- **Search finds a name in English or Ukrainian**, whatever language the game is in.
- **Scroll bars can be clicked and dragged**, and the details bar no longer slips out of its frame
  when scrolled past the end.
- **The portrait turns when dragged** instead of spinning on its own, without the cape, and
  its elements move: the portrait never ticks, so shards, flames and ears used to stand still.
- **A mane is the colour of the player's hair**, read off their skin, instead of one brown for
  everybody.

- **The picker's heading follows the step**: the path step was still headed "Origin". It now
  reads "Path · <origin>", naming the origin the path is for.
- **Geonite's Stonecaller path is amethyst**: its stone shards and fists take a violet tint.

- **The portrait turns on its own again**, and holds still for five seconds after it was turned
  by hand.
- **Lightbearer's motes orbit clear of the body**: at 7 pixels out (5 front to back) they passed
  through the chest and shoulders; now 11.
- **The halfling scarf moves like a cape**: its ends hang from the nape and follow vanilla's cape
  motion - streaming back at speed, lifting in a fall, swinging on a turn, bobbing with the stride
  and lifting over a sneak - while the wrap and the knot stay on the neck.

### Engine

- **`combat_recently`** condition: the player hit or was hit by something living within the last `seconds`.
- **`daytime`** condition: true while the world has day, wherever the player stands.
- **`damage` action** takes `fraction_of_owner_max_health`: a share of the power owner's max health.
- **`fluid_height`** condition: how deep the player stands in water or lava.
- **`invulnerability`** power: damage matching its `damage_condition` does not land at all.
- **`damage_over_time`** power: `interval`, `onset_delay`, `damage` and `damage_easy`. A
  `damage_type` that is not registered (Origins' own leave with Origins) falls back to plain
  damage; `protection_enchantment` is not read.

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
