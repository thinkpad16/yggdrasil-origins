package org.origins_yggdrasil.yggdrasil_origins.clazz;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

import java.util.Set;

/**
 * Works out whether a power is a gain, a cost or neither, reading nothing but the power's own
 * JSON. The picker paints abilities from this: green for a gain, red for a cost, amber when a
 * power does both or does something that is simply not a number.
 * <p>
 * The sign alone is not enough - on a handful of attributes a bigger number is worse for the
 * player ("damage taken +10%" is a penalty), so those are listed and read inverted.
 */
public final class PowerSense {

    public static final int GAIN = 1;
    public static final int COST = -1;
    public static final int NEUTRAL = 0;

    /** Attributes where a higher value hurts the player. */
    private static final Set<String> HIGHER_IS_WORSE = Set.of(
        "spell_engine:damage_taken",
        "minecraft:generic.burning_time",
        "minecraft:generic.fall_damage_multiplier",
        "minecraft:generic.gravity",
        "combat_roll:recharge",
        "ranged_weapon:pull_time");

    /** Power types that help the player whatever their numbers say. */
    private static final Set<String> ALWAYS_GAIN = Set.of(
        "effect_immunity", "fire_immunity", "night_vision", "walk_on_fluid", "ignore_water",
        "entity_glow", "invisibility", "status_effect", "prevent_death");

    /** Power types that cost the player whatever their numbers say. */
    private static final Set<String> ALWAYS_COST = Set.of(
        "burn", "disable_regen", "prevent_item_use", "restrict_armor");

    /** Power types that carry no verdict of their own: timers, counters, bookkeeping. */
    private static final Set<String> ALWAYS_NEUTRAL = Set.of("cooldown", "resource", "nature");

    /** Power types judged by which effects they touch rather than by their own sign. */
    private static final Set<String> EFFECT_SCALING = Set.of(
        "modify_status_effect_duration", "modify_status_effect_amplifier");

    private PowerSense() { }

    public static int of(JsonObject json) {
        if (json == null) return NEUTRAL;
        return merge(senses(json));
    }

    /** Every verdict found anywhere in this power, folded into one. */
    private static int merge(int[] tally) {
        boolean gain = tally[0] > 0;
        boolean cost = tally[1] > 0;
        if (gain == cost) return NEUTRAL;   // both, or neither
        return gain ? GAIN : COST;
    }

    /** tally[0] counts gains, tally[1] counts costs. */
    private static int[] senses(JsonObject json) {
        int[] tally = new int[2];
        walk(json, type(json), tally);
        return tally;
    }

    private static String type(JsonObject json) {
        if (!json.has("type") || !json.get("type").isJsonPrimitive()) return "";
        String raw = json.get("type").getAsString();
        int colon = raw.indexOf(':');
        return colon < 0 ? raw : raw.substring(colon + 1);
    }

    private static void note(int[] tally, int sense) {
        if (sense == GAIN) tally[0]++;
        else if (sense == COST) tally[1]++;
    }

    private static void walk(JsonElement element, String outerType, int[] tally) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) walk(child, outerType, tally);
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject json = element.getAsJsonObject();
        String type = type(json).isEmpty() ? outerType : type(json);

        // a condition says when a power applies, never whether it is good or bad
        if (json.has("attribute") && json.has("amount")) {
            note(tally, attributeSense(json));
        } else if (json.has("operation") && json.has("amount")) {
            note(tally, valueSense(type, json.get("amount").getAsDouble()));
        } else if (ALWAYS_GAIN.contains(type)) {
            note(tally, GAIN);
        } else if (ALWAYS_COST.contains(type)) {
            note(tally, COST);
        } else if (type.equals("modify_harvest")) {
            note(tally, json.has("allow") && !json.get("allow").getAsBoolean() ? COST : GAIN);
        } else if (EFFECT_SCALING.contains(type)) {
            note(tally, effectScalingSense(json));
            return;   // its own modifier block must not be read a second time
        } else if (type.equals("gain_air")) {
            // the same action gives air underwater and takes it away for a creature of flame
            note(tally, json.has("value") && json.get("value").getAsDouble() < 0 ? COST : GAIN);
        } else if (type.equals("heal") || type.equals("absorb")
                || type.equals("give") || type.equals("spawn_entity")) {
            note(tally, GAIN);
        } else if (type.equals("damage")) {
            // damage to somebody else is a gain; a power that burns its own owner is a cost
            note(tally, hurtsOwner(outerType) ? COST : GAIN);
        }

        if (ALWAYS_NEUTRAL.contains(type)) return;

        for (var entry : json.entrySet()) {
            if (entry.getKey().equals("condition") || entry.getKey().equals("conditions")
                || entry.getKey().equals("damage_condition") || entry.getKey().equals("target_condition")
                || entry.getKey().equals("item_condition") || entry.getKey().equals("block_condition")
                || entry.getKey().equals("bientity_condition")) {
                continue;
            }
            JsonElement child = entry.getValue();
            if (child.isJsonObject() || child.isJsonArray()) walk(child, type, tally);
        }
    }

    /** A periodic or self-targeted action hurts the owner; a hit reaction hits the attacker. */
    private static boolean hurtsOwner(String outerType) {
        return outerType.equals("action_over_time") || outerType.equals("action_when_hit");
    }

    private static int attributeSense(JsonObject json) {
        double amount = json.get("amount").getAsDouble();
        if (amount == 0.0D) return NEUTRAL;
        boolean better = amount > 0.0D;
        if (HIGHER_IS_WORSE.contains(json.get("attribute").getAsString())) better = !better;
        return better ? GAIN : COST;
    }

    /**
     * Stretching a good effect is a gain, stretching a bad one is a cost, and the sign of the
     * modifier flips both. A power that touches effects of both kinds gets no verdict.
     */
    private static int effectScalingSense(JsonObject json) {
        double amount = 0.0D;
        if (json.has("modifier") && json.getAsJsonObject("modifier").has("amount")) {
            amount = json.getAsJsonObject("modifier").get("amount").getAsDouble();
        } else if (json.has("modifiers")) {
            for (JsonElement child : json.getAsJsonArray("modifiers")) {
                if (child.isJsonObject() && child.getAsJsonObject().has("amount")) {
                    amount += child.getAsJsonObject().get("amount").getAsDouble();
                }
            }
        }
        if (amount == 0.0D) return NEUTRAL;

        boolean beneficial = false;
        boolean harmful = false;
        for (String id : effectIds(json)) {
            MobEffectCategory category = BuiltInRegistries.MOB_EFFECT
                .getOptional(ResourceLocation.parse(id))
                .map(MobEffect::getCategory)
                .orElse(MobEffectCategory.NEUTRAL);
            if (category == MobEffectCategory.BENEFICIAL) beneficial = true;
            if (category == MobEffectCategory.HARMFUL) harmful = true;
        }
        if (beneficial == harmful) return NEUTRAL;
        boolean better = amount > 0.0D;
        if (harmful) better = !better;
        return better ? GAIN : COST;
    }

    private static Set<String> effectIds(JsonObject json) {
        Set<String> ids = new java.util.LinkedHashSet<>();
        if (json.has("status_effect")) ids.add(json.get("status_effect").getAsString());
        if (json.has("status_effects")) {
            for (JsonElement child : json.getAsJsonArray("status_effects")) {
                ids.add(child.getAsString());
            }
        }
        return ids;
    }

    /** A bare {@code operation}/{@code amount} pair: damage and food modifiers. */
    private static int valueSense(String type, double amount) {
        if (amount == 0.0D) return NEUTRAL;
        boolean better = amount > 0.0D;
        // taking less damage and tiring less are gains, so these read inverted
        if (type.equals("modify_damage_taken") || type.equals("modify_exhaustion")) better = !better;
        return better ? GAIN : COST;
    }
}
