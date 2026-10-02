package com.dotaitems;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
    public static final Item BLINK_DAGGER = register("blink_dagger",
            new BlinkDaggerItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));

    // База как у алмазного меча (7 урона). 450% крит считается от фактического урона удара.
    public static final Item PHANTOM_ASSASSIN_DAGGER = register("phantom_assassin_dagger",
            new PhantomDaggerItem(ToolMaterials.DIAMOND, 3, -1.6f, new FabricItemSettings().rarity(Rarity.EPIC)));

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(DotaItems.MOD_ID, name), item);
    }

    public static void init() {}
}
