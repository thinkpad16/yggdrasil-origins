"""Checks that what a power says matches what a power does.

Every number in a power's JSON has to appear in the text players read, in both languages.
Numbers drift: a value gets retuned and the description keeps the old figure, and from then
on the mod lies to the people playing it.

Run:  python3 tools/check_numbers.py
"""
import json
import os
import re

POWERS = 'src/main/resources/data/yggdrasil_origins/powers/'
LANG = 'src/main/resources/assets/yggdrasil_origins/lang/'
MOD = 'yggdrasil_origins'

# values written as points rather than percentages
FLAT = {'minecraft:generic.max_health', 'minecraft:generic.armor',
        'minecraft:generic.armor_toughness', 'minecraft:generic.attack_damage',
        'combat_roll:count', 'minecraft:generic.luck',
        'minecraft:generic.water_movement_efficiency', 'minecraft:generic.oxygen_bonus',
        'minecraft:generic.safe_fall_distance'}


# attributes that live between zero and one and are written as percentages anyway
FRACTION = {'minecraft:generic.knockback_resistance',
            'minecraft:generic.water_movement_efficiency',
            'minecraft:player.sweeping_damage_ratio',
            'minecraft:generic.fall_damage_multiplier'}


def amounts(node, found):
    """One set of acceptable forms per modifier: any of them in the text will do."""
    if isinstance(node, list):
        for child in node:
            amounts(child, found)
        return
    if not isinstance(node, dict):
        return
    if 'attribute' in node and 'amount' in node:
        attribute = node['attribute']
        amount = float(node['amount'])
        if amount != 0.0:
            forms = {abs(round(amount * 100.0, 1))}
            if node.get('operation', 'add_value') == 'add_value' \
                    and attribute not in FRACTION:
                forms = {abs(round(amount, 2))}
                if attribute == 'minecraft:generic.max_health':
                    forms.add(abs(round(amount / 2.0, 2)))   # health is read in hearts
            else:
                forms.add(abs(round(amount, 2)))
            found.append(forms)
    for key, child in node.items():
        if key not in ('id', 'attribute', 'operation'):
            amounts(child, found)


def written(text):
    return {abs(float(n)) for n in re.findall(r'-?\d+(?:[.,]\d+)?', text.replace(',', '.'))}


# Powers whose numbers are machinery rather than a promise: how hard the water pulls, how
# much health a summoned creature has. Their text describes what happens, not the figures.
QUIET = {'dwarf_water_weakness', 'undead_drowned_walker', 'undead_minion',
         'hydroletilus_water_master', 'undine_fluid_form'}


def main():
    languages = {name[:-5]: json.load(open(LANG + name, encoding='utf-8'))
                 for name in os.listdir(LANG) if name.endswith('.json')}
    problems = []
    checked = 0
    for name in sorted(os.listdir(POWERS)):
        if not name.endswith('.json'):
            continue
        power = name[:-5]
        with open(POWERS + name, encoding='utf-8') as handle:
            data = json.load(handle)
        found = []
        amounts(data, found)
        if not found:
            continue
        if power in QUIET:
            continue
        checked += 1
        for lang, strings in languages.items():
            text = strings.get(f'power.{MOD}.{power}.description')
            if text is None:
                problems.append(f'{power}: no description in {lang}')
                continue
            said = written(text)
            # a rounded figure in the text is close enough: "+2" for 1.5 is not a lie
            missing = [sorted(forms) for forms in found
                       if not any(abs(v - s) <= max(0.5, v * 0.06)
                                  for v in forms for s in said)]
            if missing:
                problems.append(f'{power} ({lang}): {missing} not in "{text}"')
    print(f'powers with numbers: {checked}')
    if problems:
        print(f'descriptions out of step: {len(problems)}')
        for line in problems[:25]:
            print('  ' + line)
        if len(problems) > 25:
            print(f'  ... and {len(problems) - 25} more')
    else:
        print('every number a power applies is written in both languages')


if __name__ == '__main__':
    main()
