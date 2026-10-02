package com.dotaitems;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;

public class DotaItems implements ModInitializer {
    public static final String MOD_ID = "dotaitems";

    @Override
    public void onInitialize() {
        ModItems.init();
        BlinkDaggerItem.registerDamageLockout();
        CoupDeGrace.register();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(ModItems.BLINK_DAGGER);
            entries.add(ModItems.PHANTOM_ASSASSIN_DAGGER);
        });
    }
}
