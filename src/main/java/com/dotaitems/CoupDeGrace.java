package com.dotaitems;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Vector3f;

/**
 * Coup de Grace: 27% шанс нанести 450% урона.
 * Идея: перехватываем обычный удар (ALLOW_DAMAGE), при крите отменяем его
 * и сразу наносим тот же удар с множителем 4.5 (броня и защита считаются как обычно).
 */
public final class CoupDeGrace {
    public static final float CHANCE = 0.27f;
    public static final float MULTIPLIER = 4.5f;

    private static boolean applying = false;

    private CoupDeGrace() {}

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((target, source, amount) -> {
            if (applying || amount <= 0) return true;
            if (!(source.getAttacker() instanceof LivingEntity attacker)) return true;
            if (source.getSource() != attacker) return true; // только ближний бой
            ItemStack weapon = attacker.getMainHandStack();
            if (!weapon.isOf(ModItems.PHANTOM_ASSASSIN_DAGGER)) return true;
            if (!(target.getWorld() instanceof ServerWorld world)) return true;
            if (target.timeUntilRegen > 10) return true; // удар всё равно заблокируют кадры неуязвимости
            if (attacker.getRandom().nextFloat() >= CHANCE) return true;

            float critDamage = amount * MULTIPLIER;
            boolean hurt;
            applying = true;
            try {
                hurt = target.damage(source, critDamage);
            } finally {
                applying = false;
            }
            if (hurt) {
                weapon.damage(1, attacker, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
                critEffects(world, target, attacker, critDamage);
            }
            return false; // обычный удар заменён критическим
        });
    }

    private static void critEffects(ServerWorld w, LivingEntity target, LivingEntity attacker, float dmg) {
        double x = target.getX(), y = target.getBodyY(0.6), z = target.getZ();
        w.spawnParticles(new DustParticleEffect(new Vector3f(0.75f, 0.02f, 0.08f), 1.6f), x, y, z, 40, 0.35, 0.45, 0.35, 0.05);
        w.spawnParticles(ParticleTypes.CRIT, x, y, z, 30, 0.4, 0.5, 0.4, 0.45);
        w.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 10, 0.3, 0.4, 0.3, 0.2);
        w.spawnParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0, 0, 0);
        w.playSound(null, x, y, z, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.2f, 0.7f);
        w.playSound(null, x, y, z, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.35f, 1.9f);
        w.playSound(null, x, y, z, SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.3f, 1.4f);
        if (attacker instanceof ServerPlayerEntity p) {
            p.sendMessage(Text.translatable("message.dotaitems.crit", Math.round(dmg))
                    .formatted(Formatting.DARK_RED, Formatting.BOLD), true);
        }
    }
}
