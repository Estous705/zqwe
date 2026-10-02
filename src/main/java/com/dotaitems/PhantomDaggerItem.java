package com.dotaitems;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PhantomDaggerItem extends SwordItem {
    public PhantomDaggerItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.dotaitems.phantom_assassin_dagger.tooltip1").formatted(Formatting.DARK_RED));
        tooltip.add(Text.translatable("item.dotaitems.phantom_assassin_dagger.tooltip2").formatted(Formatting.GRAY));
    }
}
