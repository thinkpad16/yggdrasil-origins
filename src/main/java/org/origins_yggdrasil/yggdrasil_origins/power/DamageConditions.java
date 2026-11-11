package org.origins_yggdrasil.yggdrasil_origins.power;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;
import java.util.function.Predicate;

/** Damage source conditions: {@code apoli:type}, {@code apoli:projectile}, {@code apoli:fire}, tags. */
public final class DamageConditions {

    private DamageConditions() { }

    public static Predicate<DamageSource> parse(JsonElement element) {
        if (element == null) return source -> true;
        if (!element.isJsonObject()) return null;
        JsonObject json = element.getAsJsonObject();
        boolean inverted = json.has("inverted") && json.get("inverted").getAsBoolean();
        Predicate<DamageSource> base = base(Conditions.strip(
            json.has("type") ? json.get("type").getAsString() : ""), json);
        if (base == null) return null;
        return inverted ? base.negate() : base;
    }

    /** The weapon in the attacker's main hand (an arrow carries no enchantment of its own). */
    private static Optional<ItemStack> weapon(DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker
            ? Optional.of(attacker.getMainHandItem()) : Optional.empty();
    }

    private static Predicate<DamageSource> base(String path, JsonObject json) {
        switch (path) {
            case "constant": {
                boolean value = json.get("value").getAsBoolean();
                return source -> value;
            }
            case "all_of", "any_of": {
                List<Predicate<DamageSource>> parts = new ArrayList<>();
                for (JsonElement child : json.getAsJsonArray("conditions")) {
                    Predicate<DamageSource> part = parse(child);
                    if (part == null) return null;
                    parts.add(part);
                }
                boolean all = path.equals("all_of");
                return source -> all
                    ? parts.stream().allMatch(part -> part.test(source))
                    : parts.stream().anyMatch(part -> part.test(source));
            }
            case "type": {
                ResourceLocation id = ResourceLocation.parse(json.get("damage_type").getAsString());
                return source -> source.typeHolder().unwrapKey()
                    .map(key -> key.location().equals(id)).orElse(false);
            }
            case "in_tag": {
                TagKey<DamageType> tag = TagKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.parse(json.get("tag").getAsString()));
                return source -> source.is(tag);
            }
            case "fire":
                return source -> source.is(DamageTypeTags.IS_FIRE);
            case "projectile":
                return source -> source.is(DamageTypeTags.IS_PROJECTILE);
            case "explosive":
                return source -> source.is(DamageTypeTags.IS_EXPLOSION);
            case "from_falling":
                return source -> source.is(DamageTypeTags.IS_FALL);
            case "bypasses_armor":
                return source -> source.is(DamageTypeTags.BYPASSES_ARMOR);
            case "attacker_enchantment": {
                // "Smite against the undead": a hit from a weapon with a given enchantment.
                // The level comes from the world registry: in 1.21 enchantments are a dynamic
                // registry, and holders in the item component do not always carry an unwrapped key.
                ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.parse(json.get("enchantment").getAsString()));
                int min = json.has("min_level") ? json.get("min_level").getAsInt() : 1;
                return source -> {
                    if (!(source.getEntity() instanceof LivingEntity attacker)) return false;
                    ItemStack stack = attacker.getMainHandItem();
                    if (stack.isEmpty()) return false;
                    return attacker.level().registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolder(key)
                        .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack) >= min)
                        .orElse(false);
                };
            }
            case "attacker_item": {
                // "Cold iron against the fey": a hit from a weapon of a given item or tag
                Predicate<ItemStack> item = Conditions.item(json.get("item_condition"));
                if (item == null) return null;
                return source -> weapon(source).map(item::test).orElse(false);
            }
            default:
                return null;
        }
    }
}
