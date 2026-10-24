package com.celestialwrath;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class CelestialWrath implements ModInitializer {
    public static final String MOD_ID = "celestialwrath";
    public static Item DESIGNATOR;

    @Override
    public void onInitialize() {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "orbital_designator");
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        DESIGNATOR = Registry.register(BuiltInRegistries.ITEM, key,
                new DesignatorItem(new Item.Properties()
                        .setId(key)
                        .stacksTo(1)
                        .rarity(Rarity.EPIC)
                        .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(DESIGNATOR));

        ServerTickEvents.END_SERVER_TICK.register(server -> StrikeManager.tick());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> StrikeManager.clear());
    }
}
