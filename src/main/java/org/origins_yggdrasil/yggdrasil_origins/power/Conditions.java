package org.origins_yggdrasil.yggdrasil_origins.power;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Our own conditions. Returns {@code null} when a condition type is unsupported, which makes
 * the whole power unsupported - {@code /ygg doctor} then reports it instead of failing silently.
 */
public final class Conditions {

    private Conditions() { }

    // ------------------------------------------------------------------ entity

    public static Predicate<ServerPlayer> entity(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject json = element.getAsJsonObject();
        String type = json.has("type") ? json.get("type").getAsString() : "";
        boolean inverted = json.has("inverted") && json.get("inverted").getAsBoolean();
        Predicate<ServerPlayer> base = entityBase(type, json);
        if (base == null) return null;
        return inverted ? base.negate() : base;
    }

    private static Predicate<ServerPlayer> entityBase(String type, JsonObject json) {
        String path = strip(type);
        switch (path) {
            case "constant": {
                boolean value = json.get("value").getAsBoolean();
                return player -> value;
            }
            case "all_of": {
                List<Predicate<ServerPlayer>> parts = list(json.getAsJsonArray("conditions"));
                if (parts == null) return null;
                return player -> {
                    for (int i = 0; i < parts.size(); i++) {
                        if (!parts.get(i).test(player)) return false;
                    }
                    return true;
                };
            }
            case "any_of": {
                List<Predicate<ServerPlayer>> parts = list(json.getAsJsonArray("conditions"));
                if (parts == null) return null;
                return player -> {
                    for (int i = 0; i < parts.size(); i++) {
                        if (parts.get(i).test(player)) return true;
                    }
                    return false;
                };
            }
            case "brightness": {
                Compare compare = Compare.of(json);
                // getMaxLocalRawBrightness, not getRawBrightness: the raw value ignores the time
                // of day, so standing under an open sky at midnight still reads a full 15 and
                // nothing that waits for darkness would ever fire outdoors.
                return player -> compare.test(player.level()
                    .getMaxLocalRawBrightness(player.blockPosition()) / 15.0D);
            }
            case "relative_health": {
                Compare compare = Compare.of(json);
                return player -> compare.test(player.getHealth() / Math.max(1.0F, player.getMaxHealth()));
            }
            case "health": {
                Compare compare = Compare.of(json);
                return player -> compare.test(player.getHealth());
            }
            case "fall_distance": {
                Compare compare = Compare.of(json);
                return player -> compare.test(player.fallDistance);
            }
            case "time_of_day", "day_time": {
                Compare compare = Compare.of(json);
                return player -> compare.test(player.level().getDayTime() % 24000L);
            }
            case "exposed_to_sun":
                return player -> {
                    Level level = player.level();
                    return level.isDay() && !level.isRaining() && level.canSeeSky(player.blockPosition());
                };
            case "exposed_to_sky":
                return player -> player.level().canSeeSky(player.blockPosition());
            case "on_fire":
                return ServerPlayer::isOnFire;
            case "in_rain":
                return player -> player.level().isRainingAt(player.blockPosition());
            case "in_thunderstorm":
                return player -> player.level().isThundering()
                    && player.level().isRainingAt(player.blockPosition());
            case "in_snow":
                return player -> {
                    BlockPos pos = player.blockPosition();
                    Level level = player.level();
                    return (level.isRaining() && level.getBiome(pos).value().coldEnoughToSnow(pos))
                        || level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.SNOW)
                        || level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK);
                };
            case "submerged_in": {
                ResourceLocation fluid = ResourceLocation.parse(json.get("fluid").getAsString());
                boolean lava = fluid.getPath().contains("lava");
                return player -> player.isEyeInFluid(lava ? FluidTags.LAVA : FluidTags.WATER);
            }
            case "dimension": {
                ResourceLocation dimension = ResourceLocation.parse(json.get("dimension").getAsString());
                return player -> player.level().dimension().location().equals(dimension);
            }
            case "moving":
                return player -> player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5D;
            case "sneaking":
                return ServerPlayer::isShiftKeyDown;
            case "sprinting":
                return ServerPlayer::isSprinting;
            case "swimming":
                return ServerPlayer::isSwimming;
            case "invisible":
                return ServerPlayer::isInvisible;
            case "status_effect": {
                ResourceLocation id = ResourceLocation.parse(json.get("effect").getAsString());
                Optional<net.minecraft.core.Holder.Reference<MobEffect>> effect =
                    BuiltInRegistries.MOB_EFFECT.getHolder(id);
                if (effect.isEmpty()) return null;
                return player -> player.hasEffect(effect.get());
            }
            case "power_active": {
                ResourceLocation power = ResourceLocation.parse(json.get("power").getAsString());
                return player -> PowerState.ready(player, power);
            }
            case "resource": {
                ResourceLocation power = ResourceLocation.parse(json.get("resource").getAsString());
                Compare compare = Compare.of(json);
                return player -> compare.test(PowerState.resource(player, power));
            }
            case "equipped_item": {
                EquipmentSlot slot = slot(json.get("equipment_slot").getAsString());
                if (slot == null) return null;
                Predicate<ItemStack> item = item(json.get("item_condition"));
                if (item == null) return null;
                return player -> item.test(player.getItemBySlot(slot));
            }
            case "on_block": {
                Predicate<BlockState> block = json.has("block_condition")
                    ? block(json.get("block_condition"))
                    : state -> !state.isAir();
                if (block == null) return null;
                return player -> block.test(player.level().getBlockState(player.blockPosition().below()));
            }
            case "entity_in_radius": {
                double radius = json.get("radius").getAsDouble();
                Compare compare = Compare.of(json, ">=", 1);
                Predicate<LivingEntity> target = bientityTarget(json.get("bientity_condition"));
                if (target == null) return null;
                boolean sphere = isSphere(json);
                // a scent in the air is somebody else's: a power may ask not to smell itself
                boolean countSelf = !json.has("count_self") || json.get("count_self").getAsBoolean();
                double radiusSqr = radius * radius;
                return player -> {
                    AABB box = new AABB(player.blockPosition()).inflate(radius);
                    List<LivingEntity> found = player.level().getEntitiesOfClass(LivingEntity.class, box,
                        entity -> (countSelf || entity != player) && target.test(entity));
                    int count = found.size();
                    if (sphere) {
                        count = 0;
                        for (int i = 0; i < found.size(); i++) {
                            if (found.get(i).distanceToSqr(player) <= radiusSqr) count++;
                        }
                    }
                    return compare.test(count);
                };
            }
            case "inventory_item": {
                // anywhere the player can reach without opening a chest: hotbar, backpack,
                // armour slots and the off hand
                Predicate<ItemStack> item = item(json.get("item_condition"));
                if (item == null) return null;
                return player -> {
                    var inventory = player.getInventory();
                    for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                        ItemStack stack = inventory.getItem(slot);
                        if (!stack.isEmpty() && item.test(stack)) return true;
                    }
                    return false;
                };
            }
            case "class_in_radius": {
                // counts players nearby who carry a given origin - the pack needs to know
                // who else is running with it, and an entity type cannot answer that
                ResourceLocation wanted = ResourceLocation.parse(json.get("class").getAsString());
                double radius = json.get("radius").getAsDouble();
                Compare compare = Compare.of(json, ">=", 1);
                boolean countSelf = !json.has("count_self") || json.get("count_self").getAsBoolean();
                double radiusSqr = radius * radius;
                return player -> {
                    AABB box = new AABB(player.blockPosition()).inflate(radius);
                    List<ServerPlayer> found = player.level()
                        .getEntitiesOfClass(ServerPlayer.class, box, other ->
                            (countSelf || other != player)
                                && other.distanceToSqr(player) <= radiusSqr
                                && org.origins_yggdrasil.yggdrasil_origins.clazz.ClassManager
                                    .chosen(other).containsValue(wanted.toString()));
                    return compare.test(found.size());
                };
            }
            case "block_in_radius": {
                double radius = json.get("radius").getAsDouble();
                Compare compare = Compare.of(json, ">=", 1);
                Predicate<BlockState> block = block(json.get("block_condition"));
                if (block == null) return null;
                int r = (int) Math.ceil(radius);
                boolean sphere = isSphere(json);
                double radiusSqr = radius * radius;
                // for ">=" and ">" there is no point counting further once the condition holds:
                // a radius of 8 is nearly five thousand blocks per check
                boolean growing = compare.op().startsWith(">");
                // and the answer is kept until the player steps off the block they were on,
                // or a second passes: a radius of six is two thousand lookups, and nobody
                // notices terrain arriving a moment late. Ticking is single threaded, and a
                // weak key lets a player who logs out fall out of the map on its own.
                java.util.Map<ServerPlayer, long[]> seen = new java.util.WeakHashMap<>();
                return player -> {
                    long at = player.blockPosition().asLong();
                    long now = player.level().getGameTime();
                    long[] last = seen.get(player);
                    if (last != null && last[0] == at && now - last[1] < 20L) {
                        return last[2] != 0L;
                    }
                    BlockPos origin = player.blockPosition();
                    Level level = player.level();
                    int count = 0;
                    boolean answer = false;
                    for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
                        if (sphere && origin.distSqr(pos) > radiusSqr) continue;
                        if (!block.test(level.getBlockState(pos))) continue;
                        count++;
                        if (growing && compare.test(count)) {
                            answer = true;
                            break;
                        }
                    }
                    if (!answer) answer = compare.test(count);
                    seen.put(player, new long[] {at, now, answer ? 1L : 0L});
                    return answer;
                };
            }
            default:
                return null;
        }
    }

    private static List<Predicate<ServerPlayer>> list(JsonArray array) {
        if (array == null) return null;
        List<Predicate<ServerPlayer>> parts = new ArrayList<>();
        for (JsonElement element : array) {
            Predicate<ServerPlayer> part = entity(element);
            if (part == null) return null;
            parts.add(part);
        }
        return parts;
    }

    /** We only use {@code apoli:target_condition} with an entity type check. */
    private static Predicate<LivingEntity> bientityTarget(JsonElement element) {
        if (element == null || !element.isJsonObject()) return entity -> true;
        JsonObject json = element.getAsJsonObject();
        String path = strip(json.has("type") ? json.get("type").getAsString() : "");
        if (!path.equals("target_condition")) return null;
        JsonObject inner = json.getAsJsonObject("condition");
        String innerPath = strip(inner.has("type") ? inner.get("type").getAsString() : "");
        switch (innerPath) {
            case "relative_health", "health", "on_fire", "invisible", "status_effect",
                 "entity_tag", "in_entity_tag", "all_of", "any_of":
                // everything the plain living test already answers: how hurt a creature is,
                // what it is, what is on it. No reason for a radius to know less.
                return living(inner);
            case "entity_type": {
                ResourceLocation id = ResourceLocation.parse(inner.get("entity_type").getAsString());
                Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
                return entityType.<Predicate<LivingEntity>>map(t -> entity -> entity.getType() == t).orElse(null);
            }
            case "living":
                return entity -> true;
            default:
                return null;
        }
    }

    // ------------------------------------------------------------------ any living entity

    /** The subset of conditions that works for any living entity (for hit targets). */
    public static Predicate<LivingEntity> living(JsonElement element) {
        if (element == null) return entity -> true;
        if (!element.isJsonObject()) return null;
        JsonObject json = element.getAsJsonObject();
        boolean inverted = json.has("inverted") && json.get("inverted").getAsBoolean();
        Predicate<LivingEntity> base = livingBase(strip(json.has("type") ? json.get("type").getAsString() : ""), json);
        if (base == null) return null;
        return inverted ? base.negate() : base;
    }

    private static Predicate<LivingEntity> livingBase(String path, JsonObject json) {
        switch (path) {
            case "constant": {
                boolean value = json.get("value").getAsBoolean();
                return entity -> value;
            }
            case "living":
                return entity -> true;
            case "all_of", "any_of": {
                List<Predicate<LivingEntity>> parts = new ArrayList<>();
                for (JsonElement child : json.getAsJsonArray("conditions")) {
                    Predicate<LivingEntity> part = living(child);
                    if (part == null) return null;
                    parts.add(part);
                }
                boolean all = path.equals("all_of");
                return entity -> {
                    for (int i = 0; i < parts.size(); i++) {
                        if (parts.get(i).test(entity) != all) return !all;
                    }
                    return all;
                };
            }
            case "relative_health": {
                Compare compare = Compare.of(json);
                return entity -> compare.test(entity.getHealth() / Math.max(1.0F, entity.getMaxHealth()));
            }
            case "health": {
                Compare compare = Compare.of(json);
                return entity -> compare.test(entity.getHealth());
            }
            case "on_fire":
                return LivingEntity::isOnFire;
            case "invisible":
                return LivingEntity::isInvisible;
            case "status_effect": {
                ResourceLocation id = ResourceLocation.parse(json.get("effect").getAsString());
                Optional<net.minecraft.core.Holder.Reference<MobEffect>> effect =
                    BuiltInRegistries.MOB_EFFECT.getHolder(id);
                if (effect.isEmpty()) return null;
                return entity -> entity.hasEffect(effect.get());
            }
            case "entity_type": {
                ResourceLocation id = ResourceLocation.parse(json.get("entity_type").getAsString());
                Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
                return entityType.<Predicate<LivingEntity>>map(t -> entity -> entity.getType() == t).orElse(null);
            }
            case "entity_tag", "in_entity_tag": {
                // {"tag": "minecraft:undead"} - vanilla mob families: undead, arthropod, aquatic, illager
                TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE,
                    ResourceLocation.parse(json.get("tag").getAsString()));
                return entity -> entity.getType().is(tag);
            }
            default:
                return null;
        }
    }

    // ------------------------------------------------------------------ item

    public static Predicate<ItemStack> item(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject json = element.getAsJsonObject();
        boolean inverted = json.has("inverted") && json.get("inverted").getAsBoolean();
        Predicate<ItemStack> base = itemBase(strip(json.has("type") ? json.get("type").getAsString() : ""), json);
        if (base == null) return null;
        return inverted ? base.negate() : base;
    }

    private static Predicate<ItemStack> itemBase(String path, JsonObject json) {
        switch (path) {
            case "empty":
                return ItemStack::isEmpty;
            case "all_of", "any_of": {
                List<Predicate<ItemStack>> parts = new ArrayList<>();
                for (JsonElement element : json.getAsJsonArray("conditions")) {
                    Predicate<ItemStack> part = item(element);
                    if (part == null) return null;
                    parts.add(part);
                }
                boolean all = path.equals("all_of");
                return stack -> {
                    for (int i = 0; i < parts.size(); i++) {
                        if (parts.get(i).test(stack) != all) return !all;
                    }
                    return all;
                };
            }
            case "armor_value": {
                Compare compare = Compare.of(json);
                return stack -> stack.getItem() instanceof ArmorItem armor && compare.test(armor.getDefense());
            }
            case "ingredient": {
                JsonObject ingredient = json.getAsJsonObject("ingredient");
                if (ingredient.has("tag")) {
                    TagKey<Item> tag = TagKey.create(Registries.ITEM,
                        ResourceLocation.parse(ingredient.get("tag").getAsString()));
                    return stack -> stack.is(tag);
                }
                if (ingredient.has("item")) {
                    ResourceLocation id = ResourceLocation.parse(ingredient.get("item").getAsString());
                    return BuiltInRegistries.ITEM.getOptional(id)
                        .<Predicate<ItemStack>>map(item -> stack -> stack.is(item))
                        .orElse(null);
                }
                return null;
            }
            default:
                return null;
        }
    }

    // ------------------------------------------------------------------ block

    public static Predicate<BlockState> block(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject json = element.getAsJsonObject();
        boolean inverted = json.has("inverted") && json.get("inverted").getAsBoolean();
        String path = strip(json.has("type") ? json.get("type").getAsString() : "");
        Predicate<BlockState> base;
        switch (path) {
            case "in_tag": {
                TagKey<net.minecraft.world.level.block.Block> tag = TagKey.create(Registries.BLOCK,
                    ResourceLocation.parse(json.get("tag").getAsString()));
                base = state -> state.is(tag);
                break;
            }
            case "any_of", "all_of": {
                List<Predicate<BlockState>> parts = new ArrayList<>();
                for (JsonElement child : json.getAsJsonArray("conditions")) {
                    Predicate<BlockState> part = block(child);
                    if (part == null) return null;
                    parts.add(part);
                }
                boolean all = path.equals("all_of");
                base = state -> {
                    for (int i = 0; i < parts.size(); i++) {
                        if (parts.get(i).test(state) != all) return !all;
                    }
                    return all;
                };
                break;
            }
            default:
                return null;
        }
        return inverted ? base.negate() : base;
    }

    // ------------------------------------------------------------------ small helpers

    private static EquipmentSlot slot(String name) {
        return switch (name) {
            case "mainhand" -> EquipmentSlot.MAINHAND;
            case "offhand" -> EquipmentSlot.OFFHAND;
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    /** Apoli knows "cube" (default) and "sphere" - the radius is measured differently. */
    private static boolean isSphere(JsonObject json) {
        return json.has("shape")
            && json.get("shape").getAsString().toLowerCase(java.util.Locale.ROOT).contains("sphere");
    }

    static String strip(String type) {
        int colon = type.indexOf(':');
        return colon < 0 ? type : type.substring(colon + 1);
    }

    /** Comparison from JSON: {"comparison": ">=", "compare_to": 0.3}. */
    private record Compare(String op, double value) {

        static Compare of(JsonObject json) {
            return of(json, "==", 0);
        }

        static Compare of(JsonObject json, String defaultOp, double defaultValue) {
            String op = json.has("comparison") ? json.get("comparison").getAsString() : defaultOp;
            double value = json.has("compare_to") ? json.get("compare_to").getAsDouble() : defaultValue;
            return new Compare(op, value);
        }

        boolean test(double actual) {
            return switch (op) {
                case "<" -> actual < value;
                case "<=" -> actual <= value;
                case ">" -> actual > value;
                case ">=" -> actual >= value;
                case "!=" -> actual != value;
                default -> actual == value;
            };
        }
    }
}
