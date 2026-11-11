package org.origins_yggdrasil.yggdrasil_origins.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;

/** Mod items. Only one so far: the orb of origins, which clears the choice. */
public final class YggItems {

    public static final ResourceLocation CLASS_ORB_ID =
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "class_orb");

    public static final Item CLASS_ORB = new ClassOrbItem(new Item.Properties().stacksTo(16));

    private YggItems() { }

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, CLASS_ORB_ID, CLASS_ORB);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
            .register(entries -> entries.accept(CLASS_ORB));
    }
}
