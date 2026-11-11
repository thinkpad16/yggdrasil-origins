package org.origins_yggdrasil.yggdrasil_origins.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;

/**
 * The two ways to hold a racial curse at bay: drink the draught, or carry the gem.
 * <p>
 * Neither cancels a curse - the data keeps a weaker version of it running either way. Neither
 * carries a recipe on purpose: how a server hands these out is the server's business.
 */
public final class WardItems {

    /** Eight minutes, so one draught covers a trip rather than a single fight. */
    private static final int WARD_TICKS = 20 * 60 * 8;

    /** A marker effect: the powers look for it, it does nothing on its own. */
    public static Holder<MobEffect> ward;
    public static Item wardGem;
    public static Item wardingDraught;

    private WardItems() { }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, path);
    }

    /** An item that explains itself in one grey line. */
    private static class Explained extends Item {

        private final String key;

        private Explained(Properties properties, String key) {
            super(properties);
            this.key = key;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context,
                                    java.util.List<net.minecraft.network.chat.Component> tooltip,
                                    net.minecraft.world.item.TooltipFlag flag) {
            tooltip.add(net.minecraft.network.chat.Component.translatable(key)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    /** Drinks like a potion and leaves a bottle behind. */
    private static final class Draught extends Explained {

        private Draught(Properties properties, String key) {
            super(properties, key);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            ItemStack rest = super.finishUsingItem(stack, level, entity);
            return rest.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : rest;
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.DRINK;
        }

        @Override
        public SoundEvent getDrinkingSound() {
            return SoundEvents.GENERIC_DRINK;
        }
    }

    public static void register() {
        // the effect must exist before the draught that grants it is built
        ward = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id("warded"),
            new MobEffect(MobEffectCategory.BENEFICIAL, 0xC9A227) { });

        wardGem = new Explained(new Item.Properties().stacksTo(16),
            "item.yggdrasil_origins.ward_gem.tooltip");
        wardingDraught = new Draught(new Item.Properties()
            .stacksTo(16)
            .food(new FoodProperties.Builder()
                .alwaysEdible()
                .nutrition(0)
                .saturationModifier(0f)
                .effect(new MobEffectInstance(ward, WARD_TICKS), 1.0F)
                .build()),
            "item.yggdrasil_origins.warding_draught.tooltip");

        Registry.register(BuiltInRegistries.ITEM, id("ward_gem"), wardGem);
        Registry.register(BuiltInRegistries.ITEM, id("warding_draught"), wardingDraught);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(wardGem);
            entries.accept(wardingDraught);
        });
    }
}
