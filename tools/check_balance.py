"""Scores every origin and path so no choice is plainly better than the others.

A score is in "points", where one point is roughly one percent of combat strength: a
percent of movement, a point of armour, a percent of weapon damage. Gains are positive,
costs negative, and a power that only applies while some condition holds counts for less,
because it is only worth what it is worth while that condition holds.

Run:  python3 tools/check_balance.py [--all]
"""
import json
import os
import sys

DATA = 'src/main/resources/data/yggdrasil_origins/'
ORIGINS = DATA + 'origins/'
POWERS = DATA + 'powers/'

# what one percent (or one point, for flat values) of an attribute is worth
WEIGHT = {
    'minecraft:generic.movement_speed': 1.0,
    # a point of health is a twentieth of the pool, and staying alive outweighs everything
    'minecraft:generic.max_health': 2.5,          # per point, i.e. half a heart
    'minecraft:generic.armor': 1.5,               # per point
    'minecraft:generic.armor_toughness': 0.8,
    'minecraft:generic.knockback_resistance': 0.3,
    'minecraft:generic.attack_speed': 1.0,
    'minecraft:generic.attack_damage': 1.0,
    'minecraft:generic.burning_time': -0.2,
    'minecraft:generic.fall_damage_multiplier': -0.3,
    'minecraft:generic.gravity': -0.3,
    'minecraft:generic.step_height': 0.2,
    'minecraft:generic.luck': 0.3,
    'minecraft:generic.safe_fall_distance': 0.3,
    'minecraft:generic.scale': 0.2,
    'spell_engine:damage_taken': -1.3,
    'spell_engine:evasion_chance': 1.2,
    'spell_engine:healing_taken': 0.8,
    'spell_engine:critical_chance': 1.4,
    'spell_engine:critical_damage': 0.8,
    'spell_power:haste': 0.8,
    'spell_power:healing': 0.8,
    'mrpg_lib:tenacity': 0.6,
    'mrpg_lib:poison_chance': 0.5,
    'mrpg_lib:damage_reflect_modifier': 0.4,
    'combat_roll:distance': 0.3,
    'combat_roll:count': 2.0,
    'combat_roll:recharge': -0.4,
    'attributes_ld:shield_bonus': 0.5,
    'ranged_weapon:pull_time': -0.6,
    'minecraft:generic.water_movement_efficiency': 3.0,
    'minecraft:generic.oxygen_bonus': 2.0,
    'minecraft:generic.submerged_mining_speed': 0.4,
    'minecraft:generic.block_break_speed': 0.4,
    'minecraft:player.sweeping_damage_ratio': 0.3,
    'mrpg_lib:lifesteal_modifier': 0.5,
}

# A weapon class or a magic school is only worth something to the player who uses it, and
# nobody uses them all. Counting them in full made an origin that cannot swing a claymore
# look crippled when the penalty costs its players nothing.
CLASS_PREFIX = ('attributes_ld:', 'spell_power:', 'ranged_weapon:', 'mrpg_lib:')
CLASS_SHARE = 0.35
UNIVERSAL_SCHOOLS = {'spell_power:haste'}

# Work, not fighting. Mining faster is worth having and worth nothing in a duel, so these
# count for a fraction of what a point of armour counts for.
UTILITY = {
    'minecraft:player.block_break_speed', 'minecraft:player.submerged_mining_speed',
    'minecraft:player.mining_efficiency', 'minecraft:player.sneaking_speed',
    'minecraft:generic.block_break_speed', 'minecraft:generic.submerged_mining_speed',
    'minecraft:generic.movement_efficiency', 'minecraft:generic.step_height',
    'minecraft:generic.scale', 'minecraft:generic.luck',
    'minecraft:generic.water_movement_efficiency', 'minecraft:generic.oxygen_bonus',
    'minecraft:generic.safe_fall_distance', 'minecraft:player.sweeping_damage_ratio',
}
UTILITY_SHARE = 0.15

# anything not named above: damage schools and weapon damage are the common case
DEFAULT_WEIGHT = 0.9
# attributes that are flat points rather than percentages
FLAT = {'minecraft:generic.max_health', 'minecraft:generic.armor',
        'minecraft:generic.armor_toughness', 'minecraft:generic.attack_damage',
        'combat_roll:count', 'minecraft:generic.luck', 'minecraft:generic.safe_fall_distance',
        'minecraft:generic.water_movement_efficiency', 'minecraft:generic.oxygen_bonus'}


def relevance(attribute):
    """How much of an attribute any one player actually feels in a fight."""
    if attribute in UNIVERSAL_SCHOOLS:
        return 1.0
    if attribute in UTILITY:
        return UTILITY_SHARE
    return CLASS_SHARE if attribute.startswith(CLASS_PREFIX) else 1.0

# how much of a conditioned power's value survives, by how often the condition holds
CONDITION = {
    'sneaking': 0.35, 'sprinting': 0.4, 'moving': 0.5, 'swimming': 0.3, 'submerged_in': 0.35,
    'in_rain': 0.25, 'in_snow': 0.25, 'in_thunderstorm': 0.15, 'on_fire': 0.2,
    'dimension': 0.25, 'exposed_to_sun': 0.45, 'exposed_to_sky': 0.6, 'brightness': 0.5,
    'time_of_day': 0.5, 'entity_in_radius': 0.5, 'block_in_radius': 0.5, 'on_block': 0.4,
    'equipped_item': 0.6, 'inventory_item': 0.5, 'relative_health': 0.4, 'health': 0.4,
    'power_active': 0.5, 'status_effect': 0.4, 'fall_distance': 0.25, 'invisible': 0.3,
    'armor_value': 0.6, 'class_in_radius': 0.5, 'living': 1.0, 'constant': 1.0,
}
# powers that are not numbers at all: a flat worth, by type
FLAT_POWER = {
    'apoli:fire_immunity': 4.0, 'apoli:effect_immunity': 3.0, 'apoli:night_vision': 2.5,
    'apoli:walk_on_fluid': 3.0, 'apoli:ignore_water': 2.0, 'apoli:invisibility': 5.0,
    'apoli:prevent_death': 6.0, 'apoli:entity_glow': 1.5, 'apoli:lava_born': 4.0,
    'apoli:burn': -3.0, 'apoli:disable_regen': -4.0, 'apoli:prevent_item_use': -3.0,
    'apoli:restrict_armor': -4.0, 'apoli:modify_harvest': 1.0, 'apoli:status_effect': 2.0,
    'apoli:action_over_time': 1.5, 'apoli:action_when_hit': 2.0,
    'apoli:target_action_on_hit': 2.0, 'apoli:action_on_block_break': 1.0,
    'apoli:modify_food': 1.0, 'apoli:modify_exhaustion': 1.0, 'apoli:resource': 0.0,
    'apoli:cooldown': 0.0, 'apoli:nature': 0.0, 'apoli:modify_damage_taken': -0.0,
}


def read(path):
    with open(path, encoding='utf-8') as handle:
        return json.load(handle)


def discount(node, depth=0):
    """How much of a power survives its condition, if it has one."""
    if not isinstance(node, dict) or depth > 4:
        return 1.0
    condition = node.get('condition')
    if condition is None:
        return 1.0
    if isinstance(condition, dict):
        kind = condition.get('type', '').split(':')[-1]
        if kind in ('all_of', 'any_of'):
            parts = condition.get('conditions') or []
            values = [CONDITION.get((p.get('type', '').split(':')[-1]), 0.5)
                      for p in parts if isinstance(p, dict)]
            if not values:
                return 0.5
            # all_of is as rare as its rarest clause, any_of as common as its commonest
            return min(values) if kind == 'all_of' else max(values)
        return CONDITION.get(kind, 0.5)
    return 0.5


def score_power(power):
    """Points this power is worth, and whether it was numbers or a flat judgement."""
    kind = power.get('type', '')
    if kind == 'apoli:multiple':
        total = 0.0
        for key, child in power.items():
            if key != 'type' and isinstance(child, dict):
                total += score_power(child)
        return total
    share = discount(power)
    if kind in ('apoli:attribute', 'apoli:conditioned_attribute'):
        total = 0.0
        for mod in power.get('modifiers') or ([power['modifier']] if 'modifier' in power else []):
            attribute = mod.get('attribute', '')
            amount = float(mod.get('amount', 0.0))
            weight = WEIGHT.get(attribute, DEFAULT_WEIGHT)
            units = amount if attribute in FLAT else amount * 100.0
            total += units * weight * relevance(attribute)
        return total * share
    if kind == 'apoli:modify_damage_taken':
        amount = float((power.get('modifier') or {}).get('amount', 0.0))
        return -amount * 100.0 * 1.3 * share
    if kind in ('apoli:modify_damage_dealt', 'apoli:modify_projectile_damage'):
        amount = float((power.get('modifier') or {}).get('amount', 0.0))
        return amount * 100.0 * 0.9 * share
    return FLAT_POWER.get(kind, 0.0) * share


def main():
    powers = {name[:-5]: read(POWERS + name) for name in os.listdir(POWERS)
              if name.endswith('.json')}
    rows = []
    for name in sorted(os.listdir(ORIGINS)):
        if not name.endswith('.json'):
            continue
        origin = read(ORIGINS + name)
        total = 0.0
        for power in origin.get('powers') or []:
            key = power.split(':')[-1]
            if key in powers:
                total += score_power(powers[key])
        rows.append((name[:-5], round(total, 1), len(origin.get('powers') or [])))

    paths = [r for r in rows if r[0].startswith('path_')]
    bases = [r for r in rows if not r[0].startswith('path_')]
    show_all = '--all' in sys.argv

    for title, group, span in (('origins', bases, 14.0), ('paths', paths, 9.0)):
        values = sorted(r[1] for r in group)
        middle = values[len(values) // 2] if values else 0.0
        print(f'\n{title}: {len(group)}, middle {middle:+.1f} points, '
              f'range {values[0]:+.1f} to {values[-1]:+.1f}')
        off = [r for r in group if abs(r[1] - middle) > span]
        for name, score, count in sorted(group if show_all else off, key=lambda r: r[1]):
            mark = '  ' if abs(score - middle) <= span else '!!'
            print(f'  {mark} {name:34s} {score:+7.1f}  ({count} powers)')
        if not off and not show_all:
            print('   every one of them sits within '
                  f'{span:.0f} points of the middle')


if __name__ == '__main__':
    main()
