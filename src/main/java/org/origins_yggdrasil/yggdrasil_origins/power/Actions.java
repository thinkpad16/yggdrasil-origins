package org.origins_yggdrasil.yggdrasil_origins.power;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Our own entity actions. Returns {@code null} for an action type we do not implement,
 * which marks the whole power unsupported rather than half-running it.
 * <p>
 * An action gets the "actor" (the player owning the power) and the "target":
 * for hit reactions those differ, for periodic powers the target is the actor.
 */
public final class Actions {

    /** (power owner, action target) */
    public interface Action extends BiConsumer<ServerPlayer, LivingEntity> { }

    private Actions() { }

    /**
     * How much damage the hit currently being reacted to actually dealt. Set by the engine around
     * a hit reaction and zero outside one, so {@code fraction_of_damage_taken} has a value to use.
     */
    static float hitDamage = 0.0F;

    private static float fraction(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsFloat() : 0.0F;
    }

    public static Action parse(JsonElement element) {
        if (element == null) return null;
        if (element.isJsonArray()) {
            List<Action> list = new ArrayList<>();
            for (JsonElement child : element.getAsJsonArray()) {
                Action action = parse(child);
                if (action == null) return null;
                list.add(action);
            }
            return (owner, target) -> list.forEach(action -> action.accept(owner, target));
        }
        if (!element.isJsonObject()) return null;
        JsonObject json = element.getAsJsonObject();
        String type = Conditions.strip(json.has("type") ? json.get("type").getAsString() : "");
        switch (type) {
            case "nothing":
                return (owner, target) -> { };
            case "sequence", "and": {
                Action action = parse(json.get("actions"));
                return action;
            }
            case "random_chance": {
                float chance = json.get("chance").getAsFloat();
                Action action = parse(json.get("action"));
                if (action == null) return null;
                return (owner, target) -> {
                    if (owner.getRandom().nextFloat() < chance) action.accept(owner, target);
                };
            }
            case "target_action", "actor_action": {
                // in our code the action already arrives with the right target
                Action action = parse(json.get("action"));
                return action;
            }
            case "heal": {
                float amount = json.get("amount").getAsFloat();
                return (owner, target) -> target.heal(amount);
            }
            case "damage": {
                float amount = json.has("amount") ? json.get("amount").getAsFloat() : 0.0F;
                float ofMaxHealth = fraction(json, "fraction_of_max_health");
                float ofHit = fraction(json, "fraction_of_damage_taken");
                ResourceLocation typeId = json.has("damage_type")
                    ? ResourceLocation.parse(json.get("damage_type").getAsString())
                    : ResourceLocation.withDefaultNamespace("generic");
                return (owner, target) -> {
                    if (!(target.level() instanceof ServerLevel level)) return;
                    float total = amount
                        + ofMaxHealth * target.getMaxHealth()
                        + ofHit * hitDamage;
                    if (total <= 0.0F) return;
                    DamageSource source = level.damageSources().source(
                        net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DAMAGE_TYPE, typeId));
                    target.hurt(source, total);
                };
            }
            case "absorb": {
                float amount = json.has("amount") ? json.get("amount").getAsFloat() : 0.0F;
                float ofMaxHealth = fraction(json, "fraction_of_max_health");
                return (owner, target) -> {
                    float gain = amount + ofMaxHealth * target.getMaxHealth();
                    if (gain <= 0.0F) return;
                    // never stacks with a shield already standing - the larger one wins
                    target.setAbsorptionAmount(Math.max(target.getAbsorptionAmount(), gain));
                };
            }
            case "apply_effect": {
                List<MobEffectInstance> effects = effects(json);
                if (effects == null) return null;
                return (owner, target) -> effects.forEach(effect -> target.addEffect(new MobEffectInstance(effect)));
            }
            case "clear_effect": {
                if (!json.has("effect") && !json.has("effects")) {
                    return (owner, target) -> target.removeAllEffects();
                }
                List<Holder<MobEffect>> list = effectHolders(json);
                if (list == null) return null;
                return (owner, target) -> list.forEach(target::removeEffect);
            }
            case "play_sound": {
                ResourceLocation soundId = ResourceLocation.parse(json.get("sound").getAsString());
                Optional<Holder.Reference<SoundEvent>> sound = BuiltInRegistries.SOUND_EVENT.getHolder(soundId);
                if (sound.isEmpty()) {
                    YggdrasilOrigins.LOGGER.warn("Sound {} not found - action skipped", soundId);
                    return (owner, target) -> { };
                }
                float volume = json.has("volume") ? json.get("volume").getAsFloat() : 1.0F;
                float pitch = json.has("pitch") ? json.get("pitch").getAsFloat() : 1.0F;
                return (owner, target) -> target.level().playSound(null, target.blockPosition(),
                    sound.get().value(), SoundSource.PLAYERS, volume, pitch);
            }
            case "gain_air": {
                int value = json.get("value").getAsInt();
                return (owner, target) -> target.setAirSupply(
                    Math.min(target.getMaxAirSupply(), target.getAirSupply() + value));
            }
            case "give": {
                JsonObject stackJson = json.getAsJsonObject("stack");
                ResourceLocation itemId = ResourceLocation.parse(
                    stackJson.has("id") ? stackJson.get("id").getAsString() : stackJson.get("item").getAsString());
                Optional<Item> item = BuiltInRegistries.ITEM.getOptional(itemId);
                if (item.isEmpty()) return null;
                int count = stackJson.has("count") ? stackJson.get("count").getAsInt() : 1;
                return (owner, target) -> {
                    if (target instanceof ServerPlayer player) {
                        player.getInventory().placeItemBackInInventory(new ItemStack(item.get(), count));
                    }
                };
            }
            case "random_teleport": {
                int width = json.has("area_width") ? json.get("area_width").getAsInt() : 8;
                int height = json.has("area_height") ? json.get("area_height").getAsInt() : 8;
                int attempts = json.has("attempts") ? json.get("attempts").getAsInt() : 16;
                return (owner, target) -> {
                    ServerLevel level = (ServerLevel) target.level();
                    for (int i = 0; i < attempts; i++) {
                        double x = target.getX() + (target.getRandom().nextDouble() - 0.5) * width;
                        double z = target.getZ() + (target.getRandom().nextDouble() - 0.5) * width;
                        double y = target.getY() + (target.getRandom().nextDouble() - 0.5) * height;
                        BlockPos pos = BlockPos.containing(x, y, z);
                        if (!level.isLoaded(pos)) continue;
                        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
                        if (level.getBlockState(pos.below()).isAir()) continue;
                        target.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                        return;
                    }
                };
            }
            case "spawn_entity": {
                ResourceLocation typeId = ResourceLocation.parse(json.get("entity_type").getAsString());
                Optional<net.minecraft.world.entity.EntityType<?>> entityType =
                    BuiltInRegistries.ENTITY_TYPE.getOptional(typeId);
                if (entityType.isEmpty()) return null;
                Action spawned = json.has("entity_action") ? parse(json.get("entity_action")) : null;
                if (json.has("entity_action") && spawned == null) return null;
                List<JsonObject> attributes = new ArrayList<>();
                if (json.has("attributes")) {
                    json.getAsJsonArray("attributes").forEach(e -> attributes.add(e.getAsJsonObject()));
                }
                return (owner, target) -> {
                    if (!(target.level() instanceof ServerLevel level)) return;
                    net.minecraft.world.entity.Entity entity = entityType.get().spawn(
                        level, target.blockPosition(), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED);
                    if (entity == null) return;
                    if (entity instanceof LivingEntity living) {
                        for (JsonObject attributeJson : attributes) {
                            ResourceLocation attributeId =
                                ResourceLocation.parse(attributeJson.get("attribute").getAsString());
                            BuiltInRegistries.ATTRIBUTE.getHolder(attributeId).ifPresent(holder -> {
                                var instance = living.getAttribute(holder);
                                if (instance == null) return;
                                instance.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes
                                    .AttributeModifier(
                                    ResourceLocation.fromNamespaceAndPath("yggdrasil_origins",
                                        "spawned_" + attributeId.getPath().replace('.', '_')),
                                    attributeJson.get("amount").getAsDouble(),
                                    switch (attributeJson.get("operation").getAsString()) {
                                        case "add_multiplied_base" -> net.minecraft.world.entity.ai.attributes
                                            .AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                                        case "add_multiplied_total" -> net.minecraft.world.entity.ai.attributes
                                            .AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                                        default -> net.minecraft.world.entity.ai.attributes
                                            .AttributeModifier.Operation.ADD_VALUE;
                                    }));
                            });
                        }
                        living.setHealth(living.getMaxHealth());
                        // the minion goes for the same target, not for its master
                        if (living instanceof net.minecraft.world.entity.Mob mob && target != owner
                            && target instanceof LivingEntity victim) {
                            mob.setTarget(victim);
                        }
                        if (spawned != null) spawned.accept(owner, living);
                    }
                };
            }
            case "trigger_cooldown": {
                ResourceLocation power = ResourceLocation.parse(json.get("power").getAsString());
                return (owner, target) -> PowerState.triggerCooldown(owner, power);
            }
            case "change_resource": {
                ResourceLocation power = ResourceLocation.parse(json.get("resource").getAsString());
                int change = json.get("change").getAsInt();
                boolean set = json.has("operation") && json.get("operation").getAsString().equals("set");
                return (owner, target) -> {
                    if (set) PowerState.setResource(owner, power, change);
                    else PowerState.changeResource(owner, power, change);
                };
            }
            case "set_resource": {
                ResourceLocation power = ResourceLocation.parse(json.get("resource").getAsString());
                int value = json.get("value").getAsInt();
                return (owner, target) -> PowerState.setResource(owner, power, value);
            }
            default:
                return null;
        }
    }

    private static List<MobEffectInstance> effects(JsonObject json) {
        List<MobEffectInstance> out = new ArrayList<>();
        List<JsonObject> raw = new ArrayList<>();
        if (json.has("effect") && json.get("effect").isJsonObject()) raw.add(json.getAsJsonObject("effect"));
        if (json.has("effects")) {
            json.getAsJsonArray("effects").forEach(element -> raw.add(element.getAsJsonObject()));
        }
        for (JsonObject effectJson : raw) {
            String idString = effectJson.has("id")
                ? effectJson.get("id").getAsString()
                : effectJson.get("effect").getAsString();
            Optional<Holder.Reference<MobEffect>> effect =
                BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(idString));
            if (effect.isEmpty()) return null;
            int duration = effectJson.has("duration") ? effectJson.get("duration").getAsInt() : 100;
            int amplifier = effectJson.has("amplifier") ? effectJson.get("amplifier").getAsInt() : 0;
            out.add(new MobEffectInstance(effect.get(), duration, amplifier, false, false, true));
        }
        return out.isEmpty() ? null : out;
    }

    private static List<Holder<MobEffect>> effectHolders(JsonObject json) {
        List<String> ids = new ArrayList<>();
        if (json.has("effect")) ids.add(json.get("effect").getAsString());
        if (json.has("effects")) json.getAsJsonArray("effects").forEach(e -> ids.add(e.getAsString()));
        List<Holder<MobEffect>> out = new ArrayList<>();
        for (String id : ids) {
            Optional<Holder.Reference<MobEffect>> effect =
                BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id));
            if (effect.isEmpty()) return null;
            out.add(effect.get());
        }
        return out;
    }
}
