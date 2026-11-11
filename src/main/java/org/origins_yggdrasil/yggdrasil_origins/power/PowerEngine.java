package org.origins_yggdrasil.yggdrasil_origins.power;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Our own power engine. A type it does not know is reported by {@code /ygg doctor}
 * and its power simply does not run, instead of breaking the rest of the class.
 * <p>
 * RPG-stack attributes are a soft dependency: a missing attribute means the modifier is
 * skipped with a warning, while the power and the mod keep working.
 */
public final class PowerEngine {

    public static final PowerEngine INSTANCE = new PowerEngine();

    private record Mod(Holder<Attribute> attribute, ResourceLocation id, double amount,
                       AttributeModifier.Operation operation) { }

    private sealed interface B { }

    private record Attr(List<Mod> mods, Predicate<ServerPlayer> cond, int tickRate) implements B { }
    private record Eff(Holder<MobEffect> effect, int amplifier, Predicate<ServerPlayer> cond) implements B { }
    private record Immune(List<Holder<MobEffect>> effects) implements B { }
    private record FireImmune() implements B { }
    private record Burn(int interval, int seconds, Predicate<ServerPlayer> cond) implements B { }
    private record Over(int interval, Actions.Action action, Predicate<ServerPlayer> cond) implements B { }
    private record Armor(Map<EquipmentSlot, Predicate<ItemStack>> slots) implements B { }
    private record NoUse(Predicate<ItemStack> item) implements B { }
    private record Cool(int ticks) implements B { }
    private record Res(int min, int max, int start) implements B { }
    /** The "hud_render" flag from the data: such a bar is shown to the player. */
    private record Hud() implements B { }

    /** How noticeable the player is: <1 means noticed later, >1 means sooner. */
    private record Notice(double multiplier) implements B { }

    /** Reaction to a hit taken: an action on self or on the attacker. */
    private record WhenHit(Predicate<DamageSource> damage, Predicate<ServerPlayer> self, int cooldown,
                           Actions.Action action, boolean toAttacker) implements B { }

    /** Reaction to a hit dealt: an action on the target. */
    private record OnHit(Predicate<LivingEntity> target, int cooldown, Actions.Action action) implements B { }

    private record NoDeath(Predicate<DamageSource> damage, Predicate<ServerPlayer> self,
                           Actions.Action action) implements B { }

    private enum DmgKind { TAKEN, DEALT, PROJECTILE }

    private record VMod(String operation, double amount) { }

    private record Food(Predicate<ItemStack> item, List<VMod> nutrition, List<VMod> saturation,
                        boolean preventEffects, Actions.Action action) implements B { }
    private record EffectTime(List<Holder<MobEffect>> effects, List<VMod> mods, boolean amplifier) implements B { }
    private record Exhaust(List<VMod> mods) implements B { }
    private record Harvest(Predicate<BlockState> block, boolean allow) implements B { }
    private record NoRegen() implements B { }
    private record Fluid(boolean lava, Predicate<ServerPlayer> cond) implements B { }
    private record Glow(Predicate<LivingEntity> target) implements B { }
    private record NoWaterPush(Predicate<ServerPlayer> cond) implements B { }
    /** Moves through lava as if it were water, and sees through it. */
    private record LavaBorn(boolean swim, boolean sight) implements B { }

    /** Night vision strength 0..1 - vanilla has none, a client mixin scales the light. */
    private record Vision(float strength) implements B { }

    /**
     * Swim speed. Deliberately NOT a real attribute: the player attribute supplier is built
     * once at startup, and if another mod touches it before our registration runs, the
     * attribute simply would not exist. We compute it ourselves, so load order cannot bite.
     */
    private record Swim(double amount, boolean multiplied) implements B { }
    private record OnBlockBreak(Predicate<BlockState> block, boolean onlyHarvested,
                                Actions.Action action) implements B { }

    private record ModDmg(DmgKind kind, Predicate<DamageSource> damage, Predicate<LivingEntity> target,
                          Predicate<ServerPlayer> self, List<VMod> mods,
                          Actions.Action selfAction, Actions.Action targetAction) implements B { }

    /**
     * A prepared layout of the player powers. Walking every power each tick and each hit
     * and running instanceof on it is expensive; this is sorted once, when powers are granted.
     */
    private static final class Cache {

        /** What has to tick, with its interval ready (0 - every tick). */
        private record Timed(ResourceLocation power, B behaviour, int interval) { }

        /** A conditional attribute remembers whether it is applied, to avoid pointless map work. */
        private static final class CondAttr {
            private final Attr attr;
            /** Which power it belongs to, so the client can be told what is in force. */
            private final ResourceLocation power;
            private boolean applied;

            private CondAttr(ResourceLocation power, Attr attr) {
                this.power = power;
                this.attr = attr;
            }
        }

        private final List<Timed> timed = new ArrayList<>();
        private final List<CondAttr> condAttrs = new ArrayList<>();
        private final List<Immune> immunes = new ArrayList<>();
        private final List<NoUse> noUse = new ArrayList<>();
        private final List<Food> foods = new ArrayList<>();
        private final List<EffectTime> effectTimes = new ArrayList<>();
        private final List<Exhaust> exhausts = new ArrayList<>();
        private final List<Harvest> harvests = new ArrayList<>();
        private final List<Fluid> fluids = new ArrayList<>();
        private final List<ModDmg> taken = new ArrayList<>();
        private final List<ModDmg> dealt = new ArrayList<>();
        private boolean lavaSwim;
        private boolean lavaSight;
        private final List<Map.Entry<ResourceLocation, WhenHit>> whenHit = new ArrayList<>();
        private final List<Map.Entry<ResourceLocation, OnHit>> onHit = new ArrayList<>();
        private final List<NoDeath> noDeath = new ArrayList<>();
        private final List<Map.Entry<ResourceLocation, OnBlockBreak>> onBreak = new ArrayList<>();
        private final List<ResourceLocation> hudPowers = new ArrayList<>();

        private boolean fireImmune;
        private boolean noRegen;
        private boolean noWaterPush;
        /** The same, but only while a condition holds - a ward can switch it off. */
        private final List<Predicate<ServerPlayer>> conditionalWaterPush = new ArrayList<>();
        private boolean glow;
        private boolean lavaWalk;
        private boolean waterWalk;
        private boolean healthMod;
        private float nightVision = 1f;
        private double swim = 1.0D;
        private double notice = 1.0D;
        private boolean empty = true;
    }

    private Map<ResourceLocation, List<B>> supported = Map.of();
    private final Map<UUID, Set<ResourceLocation>> granted = new HashMap<>();
    private final Map<UUID, Cache> caches = new HashMap<>();
    /** Exactly which modifiers we applied to this player, so they come off without a registry walk. */
    private final Map<UUID, List<Mod>> appliedMods = new HashMap<>();
    /** Who was already swept fully this session (leftovers from older versions). */
    private final Set<UUID> deepSwept = new HashSet<>();
    private final Set<ResourceLocation> missingAttributes = new HashSet<>();
    /** An attribute no installed mod provides -> the powers that refer to it. */
    private final Map<ResourceLocation, Set<ResourceLocation>> missingUsers = new LinkedHashMap<>();
    /** Power -> the type the engine did not recognise. */
    private final Map<ResourceLocation, String> unsupportedPowers = new LinkedHashMap<>();
    /**
     * Powers that parsed fine but ended up empty because every attribute they touch belongs to
     * a mod that is not installed. Telling these apart from a genuinely unknown type matters:
     * the first is "install the mod", the second is "the data or the engine is wrong".
     */
    private final Map<ResourceLocation, Integer> attributeStarvedPowers = new LinkedHashMap<>();
    private int skippedForCurrentPower;
    private ResourceLocation parsing;
    private record HitKey(UUID player, ResourceLocation power) { }

    private final Map<HitKey, Long> hitCooldowns = new HashMap<>();
    private Set<ResourceLocation> knownPowers = Set.of();
    private int unsupported;
    private long ticks;

    /** Load counters for /ygg perf. */
    private long tickNanos;
    private long tickSamples;
    private long peakNanos;
    private long actionsRun;

    private PowerEngine() { }

    // ------------------------------------------------------------------ loading

    public void load(Map<ResourceLocation, JsonObject> powers) {
        Map<ResourceLocation, List<B>> parsed = new LinkedHashMap<>();
        missingAttributes.clear();
        missingUsers.clear();
        unsupportedPowers.clear();
        attributeStarvedPowers.clear();
        unsupported = 0;
        for (Map.Entry<ResourceLocation, JsonObject> entry : powers.entrySet()) {
            List<B> behaviours;
            parsing = entry.getKey();
            skippedForCurrentPower = 0;
            try {
                behaviours = parse(entry.getValue());
            } catch (Exception e) {
                behaviours = null;  // foreign datapacks take liberties - such a power is skipped
            }
            if (behaviours == null || behaviours.isEmpty()) {
                unsupported++;
                if (skippedForCurrentPower > 0) {
                    attributeStarvedPowers.put(entry.getKey(), skippedForCurrentPower);
                } else {
                    JsonElement type = entry.getValue().get("type");
                    unsupportedPowers.put(entry.getKey(), type == null ? "no type" : type.getAsString());
                }
                continue;
            }
            parsed.put(entry.getKey(), List.copyOf(behaviours));
        }
        supported = Map.copyOf(parsed);
        knownPowers = Set.copyOf(powers.keySet());
        granted.clear();
        caches.clear();
        // appliedMods is deliberately kept: it records the modifiers physically written onto
        // players, and a reload does not undo those. Dropping it here would leave the old
        // numbers on everyone who is online - an edit would appear to do nothing.
        deepSwept.clear();
        if (unsupported > 0 || !missingAttributes.isEmpty()) {
            YggdrasilOrigins.LOGGER.warn("Engine: {} powers unrecognised{}", unsupported,
                missingAttributes.isEmpty() ? "" : (", attributes skipped: " + missingAttributes));
        }
    }

    private List<B> parse(JsonObject json) {
        String type = Conditions.strip(json.has("type") ? json.get("type").getAsString() : "");
        Predicate<ServerPlayer> cond = null;
        if (json.has("condition")) {
            cond = Conditions.entity(json.get("condition"));
            if (cond == null) return null;
        }
        int tickRate = json.has("tick_rate") ? Math.max(1, json.get("tick_rate").getAsInt()) : 10;

        switch (type) {
            case "attribute", "conditioned_attribute": {
                List<B> out = new ArrayList<>();
                List<Mod> mods = mods(json);
                if (!mods.isEmpty()) out.add(new Attr(mods, cond, tickRate));
                // swimming bypasses the attribute registry; it does not support a condition yet
                for (Swim swim : swims(json)) out.add(swim);
                return out;
            }
            case "multiple": {
                List<B> all = new ArrayList<>();
                for (Map.Entry<String, JsonElement> child : json.entrySet()) {
                    if (!child.getValue().isJsonObject()) continue;
                    if (child.getKey().equals("condition")) continue;
                    List<B> parts = parse(child.getValue().getAsJsonObject());
                    if (parts == null) return null;
                    all.addAll(parts);
                }
                return all;
            }
            case "night_vision": {
                Holder<MobEffect> effect = effect("minecraft:night_vision");
                if (effect == null) return null;
                float strength = json.has("strength")
                    ? Math.max(0f, Math.min(1f, json.get("strength").getAsFloat()))
                    : 1f;
                return strength >= 1f
                    ? List.of(new Eff(effect, 0, cond))
                    : List.of(new Eff(effect, 0, cond), new Vision(strength));
            }
            case "invisibility": {
                Holder<MobEffect> effect = effect("minecraft:invisibility");
                return effect == null ? null : List.of(new Eff(effect, 0, cond));
            }
            case "status_effect": {
                // "effect" is what we want, "fallback" is what we settle for: the RPG Series
                // effects are richer than the vanilla ones, but the mod must still work without
                // them, so a power names both and the first one actually registered wins.
                Holder<MobEffect> effect = json.has("effect")
                    ? effect(json.get("effect").getAsString()) : null;
                if (effect == null && json.has("fallback")) {
                    effect = effect(json.get("fallback").getAsString());
                }
                if (effect == null) return null;
                if (YggdrasilOrigins.LOGGER.isDebugEnabled()) {
                    YggdrasilOrigins.LOGGER.debug("status_effect uses {}",
                        effect.unwrapKey().map(key -> key.location().toString()).orElse("?"));
                }
                int amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
                return List.of(new Eff(effect, amplifier, cond));
            }
            case "effect_immunity": {
                List<Holder<MobEffect>> list = new ArrayList<>();
                if (json.has("effect")) {
                    Holder<MobEffect> one = effect(json.get("effect").getAsString());
                    if (one == null) return null;
                    list.add(one);
                }
                if (json.has("effects")) {
                    for (JsonElement element : json.getAsJsonArray("effects")) {
                        Holder<MobEffect> one = effect(element.getAsString());
                        if (one == null) return null;
                        list.add(one);
                    }
                }
                return list.isEmpty() ? null : List.of(new Immune(list));
            }
            case "fire_immunity":
                return List.of(new FireImmune());
            case "burn": {
                int interval = json.has("interval") ? json.get("interval").getAsInt() : 20;
                int seconds = json.has("burn_duration") ? json.get("burn_duration").getAsInt() : 1;
                return List.of(new Burn(Math.max(1, interval), seconds, cond));
            }
            case "action_over_time": {
                int interval = json.has("interval") ? Math.max(1, json.get("interval").getAsInt()) : 20;
                Actions.Action action = Actions.parse(json.get("entity_action"));
                if (action == null) return null;
                return List.of(new Over(interval, action, cond));
            }
            case "restrict_armor": {
                Map<EquipmentSlot, Predicate<ItemStack>> slots = new EnumMap<>(EquipmentSlot.class);
                for (Map.Entry<String, EquipmentSlot> pair : Map.of(
                        "head", EquipmentSlot.HEAD, "chest", EquipmentSlot.CHEST,
                        "legs", EquipmentSlot.LEGS, "feet", EquipmentSlot.FEET).entrySet()) {
                    if (!json.has(pair.getKey())) continue;
                    Predicate<ItemStack> item = Conditions.item(json.get(pair.getKey()));
                    if (item == null) return null;
                    slots.put(pair.getValue(), item);
                }
                return slots.isEmpty() ? null : List.of(new Armor(slots));
            }
            case "prevent_item_use": {
                Predicate<ItemStack> item = Conditions.item(json.get("item_condition"));
                return item == null ? null : List.of(new NoUse(item));
            }
            case "action_when_hit", "attacker_action_when_hit": {
                boolean toAttacker = type.equals("attacker_action_when_hit");
                Predicate<DamageSource> damage = DamageConditions.parse(json.get("damage_condition"));
                if (damage == null) return null;
                Actions.Action action = Actions.parse(
                    json.has("entity_action") ? json.get("entity_action") : json.get("bientity_action"));
                if (action == null) return null;
                int cooldown = json.has("cooldown") ? json.get("cooldown").getAsInt() : 0;
                return List.of(new WhenHit(damage, cond, cooldown, action, toAttacker));
            }
            case "target_action_on_hit": {
                Predicate<LivingEntity> target = Conditions.living(json.get("target_condition"));
                if (target == null) return null;
                Actions.Action action = Actions.parse(json.get("entity_action"));
                if (action == null) return null;
                int cooldown = json.has("cooldown") ? json.get("cooldown").getAsInt() : 0;
                return List.of(new OnHit(target, cooldown, action));
            }
            case "prevent_death": {
                Predicate<DamageSource> damage = DamageConditions.parse(json.get("damage_condition"));
                if (damage == null) return null;
                Actions.Action action = json.has("entity_action")
                    ? Actions.parse(json.get("entity_action"))
                    : (owner, victim) -> { };
                if (action == null) return null;
                return List.of(new NoDeath(damage, cond, action));
            }
            case "modify_damage_taken", "modify_damage_dealt", "modify_projectile_damage": {
                DmgKind kind = switch (type) {
                    case "modify_damage_taken" -> DmgKind.TAKEN;
                    case "modify_projectile_damage" -> DmgKind.PROJECTILE;
                    default -> DmgKind.DEALT;
                };
                List<VMod> mods = new ArrayList<>();
                if (json.has("modifier")) mods.add(vmod(json.getAsJsonObject("modifier")));
                if (json.has("modifiers")) {
                    json.getAsJsonArray("modifiers").forEach(e -> mods.add(vmod(e.getAsJsonObject())));
                }
                Predicate<DamageSource> damage = DamageConditions.parse(json.get("damage_condition"));
                if (damage == null) return null;
                Predicate<LivingEntity> target = Conditions.living(json.get("target_condition"));
                if (target == null) return null;
                Actions.Action selfAction = json.has("self_action") ? Actions.parse(json.get("self_action")) : null;
                if (json.has("self_action") && selfAction == null) return null;
                Actions.Action targetAction = json.has("target_action") ? Actions.parse(json.get("target_action")) : null;
                if (json.has("target_action") && targetAction == null) return null;
                return List.of(new ModDmg(kind, damage, target, cond, List.copyOf(mods), selfAction, targetAction));
            }
            case "modify_food": {
                Predicate<ItemStack> item = json.has("item_condition")
                    ? Conditions.item(json.get("item_condition"))
                    : stack -> true;
                if (item == null) return null;
                List<VMod> nutrition = vmods(json, "food_modifier", "food_modifiers");
                List<VMod> saturation = vmods(json, "saturation_modifier", "saturation_modifiers");
                boolean prevent = json.has("prevent_effects") && json.get("prevent_effects").getAsBoolean();
                Actions.Action action = json.has("entity_action") ? Actions.parse(json.get("entity_action")) : null;
                if (json.has("entity_action") && action == null) return null;
                return List.of(new Food(item, nutrition, saturation, prevent, action));
            }
            case "modify_status_effect_duration", "modify_status_effect_amplifier": {
                List<Holder<MobEffect>> effects = new ArrayList<>();
                if (json.has("status_effect")) {
                    Holder<MobEffect> one = effect(json.get("status_effect").getAsString());
                    if (one == null) return null;
                    effects.add(one);
                }
                if (json.has("status_effects")) {
                    for (JsonElement element : json.getAsJsonArray("status_effects")) {
                        Holder<MobEffect> one = effect(element.getAsString());
                        if (one != null) effects.add(one);
                    }
                }
                if (effects.isEmpty()) return null;
                return List.of(new EffectTime(effects, vmods(json, "modifier", "modifiers"),
                    type.endsWith("amplifier")));
            }
            case "modify_exhaustion":
                return List.of(new Exhaust(vmods(json, "modifier", "modifiers")));
            case "modify_harvest": {
                Predicate<BlockState> block = Conditions.block(json.get("block_condition"));
                if (block == null) return null;
                boolean allow = !json.has("allow") || json.get("allow").getAsBoolean();
                return List.of(new Harvest(block, allow));
            }
            case "disable_regen":
                return List.of(new NoRegen());
            case "ignore_water":
                return List.of(new NoWaterPush(cond));
            case "action_on_block_break": {
                Predicate<BlockState> block = json.has("block_condition")
                    ? Conditions.block(json.get("block_condition"))
                    : state -> true;
                if (block == null) return null;
                Actions.Action action = Actions.parse(json.get("entity_action"));
                if (action == null) return null;
                boolean onlyHarvested = json.has("only_when_harvested")
                    && json.get("only_when_harvested").getAsBoolean();
                return List.of(new OnBlockBreak(block, onlyHarvested, action));
            }
            case "walk_on_fluid": {
                ResourceLocation fluid = ResourceLocation.parse(json.get("fluid").getAsString());
                return List.of(new Fluid(fluid.getPath().contains("lava"), cond));
            }
            case "lava_born": {
                boolean swim = !json.has("swim") || json.get("swim").getAsBoolean();
                boolean sight = !json.has("sight") || json.get("sight").getAsBoolean();
                return swim || sight ? List.of(new LavaBorn(swim, sight)) : null;
            }
            case "entity_glow": {
                Predicate<LivingEntity> target = Conditions.living(json.get("entity_condition"));
                return target == null ? null : List.of(new Glow(target));
            }
            case "nature": {
                List<B> out = new ArrayList<>();
                if (json.has("notice")) out.add(new Notice(json.get("notice").getAsDouble()));
                return out.isEmpty() ? null : out;
            }
            case "cooldown": {
                int length = json.has("cooldown") ? json.get("cooldown").getAsInt() : 20;
                return json.has("hud_render")
                    ? List.of(new Cool(length), new Hud())
                    : List.of(new Cool(length));
            }
            case "resource": {
                int min = json.has("min") ? json.get("min").getAsInt() : 0;
                int max = json.has("max") ? json.get("max").getAsInt() : 1;
                int start = json.has("start_value") ? json.get("start_value").getAsInt() : min;
                return json.has("hud_render")
                    ? List.of(new Res(min, max, start), new Hud())
                    : List.of(new Res(min, max, start));
            }
            default:
                return null;
        }
    }

    private static List<VMod> vmods(JsonObject json, String single, String plural) {
        List<VMod> mods = new ArrayList<>();
        if (json.has(single)) mods.add(vmod(json.getAsJsonObject(single)));
        if (json.has(plural)) json.getAsJsonArray(plural).forEach(e -> mods.add(vmod(e.getAsJsonObject())));
        return List.copyOf(mods);
    }

    private static VMod vmod(JsonObject json) {
        return new VMod(json.has("operation") ? json.get("operation").getAsString() : "multiply_base_additive",
            json.get("amount").getAsDouble());
    }

    private Holder<MobEffect> effect(String id) {
        Optional<Holder.Reference<MobEffect>> effect =
            BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id));
        return effect.orElse(null);
    }

    /** Name of the "attribute" we compute ourselves. */
    private static final ResourceLocation SWIM_SPEED =
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "swim_speed");

    private List<Swim> swims(JsonObject json) {
        List<Swim> out = new ArrayList<>();
        List<JsonObject> raw = new ArrayList<>();
        if (json.has("modifier")) raw.add(json.getAsJsonObject("modifier"));
        if (json.has("modifiers")) {
            json.getAsJsonArray("modifiers").forEach(element -> raw.add(element.getAsJsonObject()));
        }
        for (JsonObject modifier : raw) {
            if (!modifier.has("attribute")) continue;
            if (!SWIM_SPEED.equals(ResourceLocation.parse(modifier.get("attribute").getAsString()))) continue;
            String operation = modifier.has("operation") ? modifier.get("operation").getAsString() : "add_value";
            out.add(new Swim(modifier.get("amount").getAsDouble(), !operation.equals("add_value")));
        }
        return out;
    }

    private List<Mod> mods(JsonObject json) {
        List<Mod> mods = new ArrayList<>();
        if (json.has("modifier")) add(mods, json.getAsJsonObject("modifier"));
        if (json.has("modifiers")) {
            json.getAsJsonArray("modifiers").forEach(element -> add(mods, element.getAsJsonObject()));
        }
        return mods;
    }

    private void add(List<Mod> mods, JsonObject json) {
        if (!json.has("attribute") || !json.has("id")) return;
        ResourceLocation attributeId = ResourceLocation.parse(json.get("attribute").getAsString());
        if (SWIM_SPEED.equals(attributeId)) return;
        Optional<Holder.Reference<Attribute>> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId);
        if (attribute.isEmpty()) {
            if (missingAttributes.add(attributeId)) {
                YggdrasilOrigins.LOGGER.warn("Attribute {} is absent in this pack - modifier skipped", attributeId);
            }
            skippedForCurrentPower++;
            if (parsing != null) {
                missingUsers.computeIfAbsent(attributeId, id -> new LinkedHashSet<>()).add(parsing);
            }
            return;
        }
        mods.add(new Mod(attribute.get(),
            ResourceLocation.parse(json.get("id").getAsString()),
            json.get("amount").getAsDouble(),
            switch (json.get("operation").getAsString()) {
                case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                default -> AttributeModifier.Operation.ADD_VALUE;
            }));
    }

    // ------------------------------------------------------------------ reports

    /** How many power types the engine recognised - for the single log line. */
    public int grantedTypes() {
        return supported.size();
    }

    public boolean supports(ResourceLocation powerId) {
        return supported.containsKey(powerId);
    }

    public int cooldownLength(ResourceLocation power) {
        for (B behaviour : supported.getOrDefault(power, List.of())) {
            if (behaviour instanceof Cool cool) return cool.ticks();
        }
        return 0;
    }

    public int resourceStart(ResourceLocation power) {
        for (B behaviour : supported.getOrDefault(power, List.of())) {
            if (behaviour instanceof Res res) return res.start();
        }
        return 0;
    }

    public int resourceMin(ResourceLocation power) {
        for (B behaviour : supported.getOrDefault(power, List.of())) {
            if (behaviour instanceof Res res) return res.min();
        }
        return 0;
    }

    public int resourceMax(ResourceLocation power) {
        for (B behaviour : supported.getOrDefault(power, List.of())) {
            if (behaviour instanceof Res res) return res.max();
        }
        return 1;
    }

    /** Player powers that ask for a HUD bar. */
    public List<ResourceLocation> hudPowers(ServerPlayer player) {
        return cache(player).hudPowers;
    }

    public int clampResource(ResourceLocation power, int value) {
        for (B behaviour : supported.getOrDefault(power, List.of())) {
            if (behaviour instanceof Res res) return Math.max(res.min(), Math.min(res.max(), value));
        }
        return value;
    }

    /**
     * The conditional powers that are in force for this player at this moment. The screen
     * shows them, so that a path which only works in the forest says when it is working.
     */
    public List<ResourceLocation> activeConditions(ServerPlayer player) {
        Cache cache = caches.get(player.getUUID());
        if (cache == null || cache.condAttrs.isEmpty()) return List.of();
        List<ResourceLocation> on = new ArrayList<>(2);
        for (int i = 0; i < cache.condAttrs.size(); i++) {
            Cache.CondAttr state = cache.condAttrs.get(i);
            if (state.applied && !on.contains(state.power)) on.add(state.power);
        }
        return on;
    }

    private static final Cache EMPTY = new Cache();

    /** The "is applied" memory of a conditional attribute for this player. */
    private Cache.CondAttr state(ServerPlayer player, Attr attr) {
        Cache cache = caches.get(player.getUUID());
        if (cache == null) return null;
        for (int i = 0; i < cache.condAttrs.size(); i++) {
            if (cache.condAttrs.get(i).attr == attr) return cache.condAttrs.get(i);
        }
        return null;
    }

    /** The player layout; built once whenever the power set changes. */
    private Cache cache(ServerPlayer player) {
        Cache cache = caches.get(player.getUUID());
        return cache == null ? EMPTY : cache;
    }

    private void rebuild(ServerPlayer player) {
        Set<ResourceLocation> ids = granted.get(player.getUUID());
        if (ids == null || ids.isEmpty()) {
            caches.remove(player.getUUID());
            return;
        }
        Cache cache = new Cache();
        for (ResourceLocation id : ids) {
            for (B behaviour : supported.getOrDefault(id, List.of())) {
                cache.empty = false;
                sort(cache, id, behaviour);
            }
        }
        cache.hudPowers.sort(java.util.Comparator.comparing(ResourceLocation::toString));
        caches.put(player.getUUID(), cache);
    }

    /** Files a single behaviour onto the right cache shelf. */
    private void sort(Cache cache, ResourceLocation id, B behaviour) {
        if (behaviour instanceof Attr attr) {
            if (attr.cond() != null) {
                cache.condAttrs.add(new Cache.CondAttr(id, attr));
                cache.timed.add(new Cache.Timed(id, attr, attr.tickRate()));
            }
            for (Mod mod : attr.mods()) {
                if (mod.attribute().value() == net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH.value()) {
                    cache.healthMod = true;
                }
            }
            return;
        }
        if (behaviour instanceof Eff eff) {
            if (eff.cond() != null) cache.timed.add(new Cache.Timed(id, eff, 5));
            return;
        }
        if (behaviour instanceof Immune immune) {
            cache.immunes.add(immune);
            // safety net: another mod could apply the effect bypassing canBeAffected
            cache.timed.add(new Cache.Timed(id, immune, 20));
            return;
        }
        if (behaviour instanceof FireImmune) {
            cache.fireImmune = true;
            cache.timed.add(new Cache.Timed(id, behaviour, 5));
            return;
        }
        if (behaviour instanceof Burn burn) {
            cache.timed.add(new Cache.Timed(id, burn, burn.interval()));
            return;
        }
        if (behaviour instanceof Over over) {
            cache.timed.add(new Cache.Timed(id, over, over.interval()));
            return;
        }
        if (behaviour instanceof Armor) {
            cache.timed.add(new Cache.Timed(id, behaviour, 10));
            return;
        }
        if (behaviour instanceof NoUse noUse) {
            cache.noUse.add(noUse);
            return;
        }
        if (behaviour instanceof Food food) {
            cache.foods.add(food);
            return;
        }
        if (behaviour instanceof EffectTime time) {
            cache.effectTimes.add(time);
            return;
        }
        if (behaviour instanceof Exhaust exhaust) {
            cache.exhausts.add(exhaust);
            return;
        }
        if (behaviour instanceof Harvest harvest) {
            cache.harvests.add(harvest);
            return;
        }
        if (behaviour instanceof Fluid fluid) {
            cache.fluids.add(fluid);
            if (fluid.lava()) cache.lavaWalk = true;
            else cache.waterWalk = true;
            return;
        }
        if (behaviour instanceof NoRegen) {
            cache.noRegen = true;
            return;
        }
        if (behaviour instanceof LavaBorn lava) {
            if (lava.swim()) cache.lavaSwim = true;
            if (lava.sight()) cache.lavaSight = true;
            return;
        }
        if (behaviour instanceof NoWaterPush push) {
            if (push.cond() == null) cache.noWaterPush = true;
            else cache.conditionalWaterPush.add(push.cond());
            return;
        }
        if (behaviour instanceof Glow) {
            cache.glow = true;
            return;
        }
        if (behaviour instanceof Vision vision) {
            cache.nightVision = Math.min(cache.nightVision, vision.strength());
            return;
        }
        if (behaviour instanceof Swim swim) {
            cache.swim = swim.multiplied() ? cache.swim * (1.0D + swim.amount()) : cache.swim + swim.amount();
            if (cache.swim < 0.0D) cache.swim = 0.0D;
            return;
        }
        if (behaviour instanceof ModDmg mod) {
            if (mod.kind() == DmgKind.TAKEN) cache.taken.add(mod);
            else cache.dealt.add(mod);
            return;
        }
        if (behaviour instanceof WhenHit hit) {
            cache.whenHit.add(Map.entry(id, hit));
            return;
        }
        if (behaviour instanceof OnHit hit) {
            cache.onHit.add(Map.entry(id, hit));
            return;
        }
        if (behaviour instanceof NoDeath death) {
            cache.noDeath.add(death);
            return;
        }
        if (behaviour instanceof OnBlockBreak onBreak) {
            cache.onBreak.add(Map.entry(id, onBreak));
            return;
        }
        if (behaviour instanceof Notice notice) {
            cache.notice *= notice.multiplier();
            return;
        }
        if (behaviour instanceof Hud) {
            cache.hudPowers.add(id);
        }
    }

    /** Noticeability multiplier for {@code LivingEntity#getVisibilityPercent}. */
    public double noticeMultiplier(ServerPlayer player) {
        Cache cache = caches.get(player.getUUID());
        return cache == null ? 1.0D : cache.notice;
    }

    public int grantedCount(ServerPlayer player) {
        Set<ResourceLocation> ids = granted.get(player.getUUID());
        return ids == null ? 0 : ids.size();
    }

    /** true - the effect cannot be applied at all (immunity), not "we will remove it next tick". */
    public boolean immuneTo(ServerPlayer player, Holder<MobEffect> effect) {
        List<Immune> immunes = cache(player).immunes;
        for (int i = 0; i < immunes.size(); i++) {
            List<Holder<MobEffect>> effects = immunes.get(i).effects();
            for (int j = 0; j < effects.size(); j++) {
                if (effects.get(j).value() == effect.value()) return true;
            }
        }
        return false;
    }

    public boolean fireImmune(ServerPlayer player) {
        return cache(player).fireImmune;
    }

    public boolean preventsUse(ServerPlayer player, ItemStack stack) {
        List<NoUse> list = cache(player).noUse;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).item().test(stack)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ granting

    public void grant(ServerPlayer player, ResourceLocation powerId) {
        if (!supported.containsKey(powerId)) return;
        granted.computeIfAbsent(player.getUUID(), uuid -> new HashSet<>()).add(powerId);
        for (B behaviour : supported.get(powerId)) {
            if (behaviour instanceof Attr attr && attr.cond() == null) apply(player, attr.mods(), true);
            if (behaviour instanceof Eff eff && eff.cond() == null) addEffect(player, eff);
        }
        rebuild(player);
        clampHealth(player);
    }

    public void revokeAll(ServerPlayer player) {
        Set<ResourceLocation> ids = granted.get(player.getUUID());
        if (ids != null) {
            for (ResourceLocation id : ids) {
                for (B behaviour : supported.getOrDefault(id, List.of())) {
                    if (behaviour instanceof Eff eff) removeEffect(player, eff);
                }
            }
        }
        granted.remove(player.getUUID());
        caches.remove(player.getUUID());

        // remove exactly what we applied ourselves
        List<Mod> mine = appliedMods.remove(player.getUUID());
        if (mine != null) {
            for (Mod mod : mine) {
                AttributeInstance instance = player.getAttribute(mod.attribute());
                if (instance != null) instance.removeModifier(mod.id());
            }
        }

        // once per session we walk the registry fully, in case an older version of the mod
        // left modifiers written into the player data
        if (deepSwept.add(player.getUUID())) {
            for (Holder.Reference<Attribute> attribute : allAttributes()) {
                if (!player.getAttributes().hasAttribute(attribute)) continue;
                AttributeInstance instance = player.getAttribute(attribute);
                if (instance == null) continue;
                List<ResourceLocation> stale = instance.getModifiers().stream()
                    .map(AttributeModifier::id)
                    .filter(id -> id.getNamespace().equals(YggdrasilOrigins.MOD_ID))
                    .toList();
                stale.forEach(instance::removeModifier);
            }
        }
        clampHealth(player);
    }

    /** The full attribute list is cached: the registry does not change during a game. */
    private static List<Holder.Reference<Attribute>> attributeList;

    private static List<Holder.Reference<Attribute>> allAttributes() {
        if (attributeList == null) {
            attributeList = BuiltInRegistries.ATTRIBUTE.holders().toList();
        }
        return attributeList;
    }

    public void forget(UUID uuid) {
        granted.remove(uuid);
        caches.remove(uuid);
        appliedMods.remove(uuid);
        deepSwept.remove(uuid);
        hitCooldowns.keySet().removeIf(key -> key.player().equals(uuid));
        PowerState.forget(uuid);
    }

    private void apply(ServerPlayer player, List<Mod> mods, boolean on) {
        for (Mod mod : mods) {
            AttributeInstance instance = player.getAttribute(mod.attribute());
            if (instance == null) continue;
            if (on) {
                AttributeModifier existing = instance.getModifier(mod.id());
                if (existing == null || existing.amount() != mod.amount()) {
                    instance.addOrUpdateTransientModifier(
                        new AttributeModifier(mod.id(), mod.amount(), mod.operation()));
                }
                appliedMods.computeIfAbsent(player.getUUID(), uuid -> new ArrayList<>()).add(mod);
            } else {
                instance.removeModifier(mod.id());
            }
        }
    }

    private void addEffect(ServerPlayer player, Eff eff) {
        MobEffectInstance current = player.getEffect(eff.effect());
        if (current != null && current.isInfiniteDuration()) return;
        player.addEffect(new MobEffectInstance(eff.effect(), -1, eff.amplifier(), true, false, false));
    }

    private void removeEffect(ServerPlayer player, Eff eff) {
        MobEffectInstance current = player.getEffect(eff.effect());
        // foreign potions are left alone: our effects are infinite
        if (current != null && current.isInfiniteDuration()) player.removeEffect(eff.effect());
    }

    private void clampHealth(ServerPlayer player) {
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    // ------------------------------------------------------------------ hit reactions

    private boolean rateLimited(ServerPlayer player, ResourceLocation power, int cooldown) {
        if (cooldown <= 0) return false;
        HitKey key = new HitKey(player.getUUID(), power);
        long now = player.level().getGameTime();
        Long until = hitCooldowns.get(key);
        if (until != null && now < until) return true;
        hitCooldowns.put(key, now + cooldown);
        return false;
    }

    /** The player was hit. {@code taken} is the damage that actually went through. */
    public void onDamaged(ServerPlayer player, DamageSource source, float taken) {
        List<Map.Entry<ResourceLocation, WhenHit>> list = cache(player).whenHit;
        if (list.isEmpty()) return;
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        // a reflected hit can land on another player mid-loop, so the context nests
        float outerDamage = Actions.hitDamage;
        Actions.hitDamage = taken;
        try {
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = list.get(i).getKey();
                WhenHit hit = list.get(i).getValue();
                if (!hit.damage().test(source)) continue;
                if (hit.self() != null && !hit.self().test(player)) continue;
                if (hit.toAttacker() && attacker == null) continue;
                if (rateLimited(player, id, hit.cooldown())) continue;
                actionsRun++;
                hit.action().accept(player, hit.toAttacker() ? attacker : player);
            }
        } finally {
            Actions.hitDamage = outerDamage;
        }
    }

    /** The player dealt damage to a target. */
    public void onDealt(ServerPlayer player, LivingEntity target) {
        List<Map.Entry<ResourceLocation, OnHit>> list = cache(player).onHit;
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = list.get(i).getKey();
            OnHit hit = list.get(i).getValue();
            if (!hit.target().test(target)) continue;
            if (rateLimited(player, id, hit.cooldown())) continue;
            actionsRun++;
            hit.action().accept(player, target);
        }
    }

    /** true - death was cancelled (the player stays at 1 HP, a power action fired). */
    public boolean preventDeath(ServerPlayer player, DamageSource source) {
        List<NoDeath> list = cache(player).noDeath;
        for (int i = 0; i < list.size(); i++) {
            NoDeath noDeath = list.get(i);
            if (!noDeath.damage().test(source)) continue;
            if (noDeath.self() != null && !noDeath.self().test(player)) continue;
            player.setHealth(1.0F);
            noDeath.action().accept(player, player);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ damage modifiers

    private float applyMods(List<VMod> mods, float amount) {
        double addEarly = 0, additive = 0, multiplicative = 1, addLate = 0;
        for (VMod mod : mods) {
            switch (mod.operation()) {
                case "add_base_early" -> addEarly += mod.amount();
                case "multiply_base_multiplicative" -> multiplicative *= (1 + mod.amount());
                case "add_base_late" -> addLate += mod.amount();
                default -> additive += mod.amount();
            }
        }
        double value = (amount + addEarly) * (1 + additive) * multiplicative + addLate;
        return (float) Math.max(0, value);
    }

    /** Damage the player takes. */
    public float modifyIncoming(ServerPlayer player, DamageSource source, float amount) {
        List<ModDmg> list = cache(player).taken;
        if (list.isEmpty()) return amount;
        List<VMod> mods = null;
        for (int i = 0; i < list.size(); i++) {
            ModDmg mod = list.get(i);
            if (!mod.damage().test(source)) continue;
            if (mod.self() != null && !mod.self().test(player)) continue;
            if (mods == null) mods = new ArrayList<>(4);
            mods.addAll(mod.mods());
        }
        return mods == null ? amount : applyMods(mods, amount);
    }

    /** Damage the player deals to a target (arrows included). */
    public float modifyOutgoing(ServerPlayer player, LivingEntity target, DamageSource source,
                                float amount, boolean projectile) {
        List<ModDmg> list = cache(player).dealt;
        if (list.isEmpty()) return amount;
        List<VMod> mods = null;
        for (int i = 0; i < list.size(); i++) {
            ModDmg mod = list.get(i);
            if (!matches(mod, player, target, source, projectile)) continue;
            if (mods == null) mods = new ArrayList<>(4);
            mods.addAll(mod.mods());
        }
        return mods == null ? amount : applyMods(mods, amount);
    }

    private boolean matches(ModDmg mod, ServerPlayer player, LivingEntity target,
                            DamageSource source, boolean projectile) {
        boolean matchesKind = mod.kind() == DmgKind.DEALT
            || (mod.kind() == DmgKind.PROJECTILE && projectile);
        if (!matchesKind) return false;
        if (!mod.damage().test(source)) return false;
        if (!mod.target().test(target)) return false;
        return mod.self() == null || mod.self().test(player);
    }

    /**
     * Side actions from {@code modify_damage_dealt} fire only when the hit actually landed.
     * They used to run before the game decided whether there would be damage at all:
     * a hit into invulnerability frames or a shield still poisoned the target.
     */
    public void onDamageLanded(ServerPlayer player, LivingEntity target, DamageSource source, boolean projectile) {
        List<ModDmg> list = cache(player).dealt;
        for (int i = 0; i < list.size(); i++) {
            ModDmg mod = list.get(i);
            if (mod.selfAction() == null && mod.targetAction() == null) continue;
            if (!matches(mod, player, target, source, projectile)) continue;
            if (mod.selfAction() != null) mod.selfAction().accept(player, player);
            if (mod.targetAction() != null) mod.targetAction().accept(player, target);
        }
    }

    // ------------------------------------------------------------------ food, effects, blocks

    /** Changes nutrition and, if asked, strips the side effects of food. */
    public FoodProperties modifyFood(ServerPlayer player, ItemStack stack, FoodProperties props) {
        List<Food> list = cache(player).foods;
        if (list.isEmpty()) return props;
        List<VMod> nutrition = new ArrayList<>();
        List<VMod> saturation = new ArrayList<>();
        boolean prevent = false;
        for (Food food : list) {
            if (!food.item().test(stack)) continue;
            nutrition.addAll(food.nutrition());
            saturation.addAll(food.saturation());
            prevent |= food.preventEffects();
            if (food.action() != null) food.action().accept(player, player);
        }
        if (nutrition.isEmpty() && saturation.isEmpty() && !prevent) return props;
        FoodProperties.Builder builder = new FoodProperties.Builder()
            .nutrition(Math.round(applyMods(nutrition, props.nutrition())))
            .saturationModifier(applyMods(saturation, props.saturation()));
        if (props.canAlwaysEat()) builder.alwaysEdible();
        if (!prevent) props.effects().forEach(effect -> builder.effect(
            new MobEffectInstance(effect.effect()), effect.probability()));
        return builder.build();
    }

    /** Changes the duration or amplifier of an effect applied to the player. */
    public MobEffectInstance modifyEffect(ServerPlayer player, MobEffectInstance instance) {
        List<EffectTime> times = cache(player).effectTimes;
        if (times.isEmpty()) return instance;
        List<VMod> duration = new ArrayList<>();
        List<VMod> amplifier = new ArrayList<>();
        for (EffectTime time : times) {
            boolean matches = false;
            List<Holder<MobEffect>> effects = time.effects();
            for (int i = 0; i < effects.size() && !matches; i++) {
                matches = effects.get(i).value() == instance.getEffect().value();
            }
            if (!matches) continue;
            (time.amplifier() ? amplifier : duration).addAll(time.mods());
        }
        if (duration.isEmpty() && amplifier.isEmpty()) return instance;
        int newDuration = instance.isInfiniteDuration()
            ? instance.getDuration()
            : Math.max(1, Math.round(applyMods(duration, instance.getDuration())));
        int newAmplifier = Math.max(0, Math.round(applyMods(amplifier, instance.getAmplifier())));
        return new MobEffectInstance(instance.getEffect(), newDuration, newAmplifier,
            instance.isAmbient(), instance.isVisible(), instance.showIcon());
    }

    public float modifyExhaustion(ServerPlayer player, float amount) {
        List<Exhaust> list = cache(player).exhausts;
        if (list.isEmpty()) return amount;
        List<VMod> mods = new ArrayList<>(list.size());
        for (Exhaust exhaust : list) mods.addAll(exhaust.mods());
        return applyMods(mods, amount);
    }

    public boolean allowsHarvest(ServerPlayer player, BlockState state) {
        List<Harvest> list = cache(player).harvests;
        for (int i = 0; i < list.size(); i++) {
            Harvest harvest = list.get(i);
            if (harvest.allow() && harvest.block().test(state)) return true;
        }
        return false;
    }

    public boolean regenDisabled(ServerPlayer player) {
        return cache(player).noRegen;
    }

    public boolean walksOnFluid(ServerPlayer player, boolean lava) {
        Cache cache = cache(player);
        if (!(lava ? cache.lavaWalk : cache.waterWalk)) return false;
        List<Fluid> list = cache.fluids;
        for (int i = 0; i < list.size(); i++) {
            Fluid fluid = list.get(i);
            if (fluid.lava() == lava && (fluid.cond() == null || fluid.cond().test(player))) return true;
        }
        return false;
    }

    /** Whether such a power exists at all (no condition check) - for the client flags. */
    public boolean hasFluidWalk(ServerPlayer player, boolean lava) {
        Cache cache = cache(player);
        return lava ? cache.lavaWalk : cache.waterWalk;
    }

    /** Whether a power with this id exists in the data (regardless of engine support). */
    public boolean hasData(ResourceLocation id) {
        return knownPowers.contains(id);
    }

    /** The weakest night vision present; 1.0 means full (or none at all). */
    public float nightVisionStrength(ServerPlayer player) {
        return cache(player).nightVision;
    }

    /** Horizontal speed multiplier in water: 1.0 is normal. */
    public double swimMultiplier(ServerPlayer player) {
        return cache(player).swim;
    }

    /** Whether lava carries the player the way water does. */
    public boolean swimsInLava(ServerPlayer player) {
        return cache(player).lavaSwim;
    }

    /** Whether the player sees through lava instead of drowning in orange. */
    public boolean seesInLava(ServerPlayer player) {
        return cache(player).lavaSight;
    }

    public boolean ignoresWaterPush(ServerPlayer player) {
        Cache cache = cache(player);
        if (cache.noWaterPush) return true;
        for (int i = 0; i < cache.conditionalWaterPush.size(); i++) {
            if (cache.conditionalWaterPush.get(i).test(player)) return true;
        }
        return false;
    }

    /** The player broke a block - for {@code action_on_block_break}. */
    public void onBlockBroken(ServerPlayer player, BlockState state) {
        List<Map.Entry<ResourceLocation, OnBlockBreak>> list = cache(player).onBreak;
        for (int i = 0; i < list.size(); i++) {
            OnBlockBreak hit = list.get(i).getValue();
            if (!hit.block().test(state)) continue;
            if (hit.onlyHarvested() && !player.hasCorrectToolForDrops(state)) continue;
            hit.action().accept(player, player);
        }
    }

    public boolean glowsWounded(ServerPlayer player) {
        return cache(player).glow;
    }

    // ------------------------------------------------------------------ tick

    public void tick(MinecraftServer server) {
        ticks++;
        if (caches.isEmpty()) return;
        long started = System.nanoTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
        long spent = System.nanoTime() - started;
        tickNanos += spent;
        tickSamples++;
        if (spent > peakNanos) peakNanos = spent;
    }

    /** Engine work for a single player this tick. */
    public void tickPlayer(ServerPlayer player) {
        Cache cache = caches.get(player.getUUID());
        if (cache == null) return;

        List<Cache.Timed> timed = cache.timed;
        for (int i = 0; i < timed.size(); i++) {
            Cache.Timed entry = timed.get(i);
            if (entry.interval() > 1 && ticks % entry.interval() != 0) continue;
            tickBehaviour(player, entry.behaviour());
        }

        // clamping health only matters for someone whose powers change it
        if (cache.healthMod) clampHealth(player);
    }

    /** How many powers the engine can actually execute. */
    public int supportedCount() {
        return supported.size();
    }

    /** Attributes absent from this pack and the powers that counted on them. */
    public Map<ResourceLocation, Set<ResourceLocation>> missingAttributeUsers() {
        return Map.copyOf(missingUsers);
    }

    /** Powers whose type the engine did not recognise: they simply do not work. */
    public Map<ResourceLocation, String> unsupportedPowers() {
        return Map.copyOf(unsupportedPowers);
    }

    /** Powers left empty because the mods providing their attributes are not installed. */
    public Map<ResourceLocation, Integer> attributeStarvedPowers() {
        return Map.copyOf(attributeStarvedPowers);
    }

    /** The /ygg perf line: what the engine actually costs the server. */
    public String performance(MinecraftServer server) {
        double avgMicros = tickSamples == 0 ? 0 : tickNanos / (double) tickSamples / 1000.0;
        int players = server.getPlayerList().getPlayerCount();
        int timed = 0;
        for (Cache cache : caches.values()) timed += cache.timed.size();
        return String.format(
            "engine tick: average %.1f us, peak %.1f us over %d ticks (50000 us = a whole server tick)"
                + " | players %d, caches %d, ticking entries %d, actions fired %d"
                + " | hit cooldowns %d, modifier traces %d",
            avgMicros, peakNanos / 1000.0, tickSamples, players, caches.size(), timed, actionsRun,
            hitCooldowns.size(), appliedMods.size());
    }

    public void resetPerformance() {
        tickNanos = 0;
        tickSamples = 0;
        peakNanos = 0;
        actionsRun = 0;
    }

    private void tickBehaviour(ServerPlayer player, B behaviour) {
        if (behaviour instanceof Attr attr) {
            if (attr.cond() == null) return;
            boolean on = attr.cond().test(player);
            Cache.CondAttr state = state(player, attr);
            // nothing changed - leave it alone
            if (state != null && state.applied == on) return;
            apply(player, attr.mods(), on);
            if (state != null) {
                state.applied = on;
                // the screen says which conditional powers are in force, so it has to hear
                // about it the moment one turns on or off
                HudSync.send(player);
            }
            return;
        }
        if (behaviour instanceof Eff eff) {
            if (eff.cond() == null) return;
            if (eff.cond().test(player)) addEffect(player, eff);
            else removeEffect(player, eff);
            return;
        }
        if (behaviour instanceof Immune immune) {
            immune.effects().forEach(effect -> {
                if (player.hasEffect(effect)) player.removeEffect(effect);
            });
            return;
        }
        if (behaviour instanceof FireImmune) {
            if (player.getRemainingFireTicks() > 0) player.clearFire();
            return;
        }
        if (behaviour instanceof Burn burn) {
            if (burn.cond() == null || burn.cond().test(player)) player.igniteForSeconds(burn.seconds());
            return;
        }
        if (behaviour instanceof Over over) {
            if (over.cond() == null || over.cond().test(player)) over.action().accept(player, player);
            return;
        }
        if (behaviour instanceof Armor armor) {
            armor.slots().forEach((slot, forbidden) -> {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.isEmpty() || !forbidden.test(stack)) return;
                player.setItemSlot(slot, ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(stack);
            });
        }
    }
}
