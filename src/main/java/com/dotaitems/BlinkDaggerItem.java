package com.dotaitems;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Blink Dagger: телепорт на 120 блоков по направлению взгляда.
 * Канон: перезарядка 15 c; получение урона от врага блокирует блинк на 3 c.
 */
public class BlinkDaggerItem extends Item {
    public static final double RANGE = 120.0;
    public static final int COOLDOWN_TICKS = 15 * 20;
    public static final int DAMAGE_LOCKOUT_TICKS = 3 * 20;

    public BlinkDaggerItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        ItemCooldownManager cd = user.getItemCooldownManager();
        if (cd.isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }
        if (world.isClient || !(world instanceof ServerWorld sw)) {
            return TypedActionResult.success(stack);
        }

        Vec3d dest = findDestination(sw, user);
        if (dest == null) {
            user.sendMessage(Text.translatable("message.dotaitems.blink_blocked").formatted(Formatting.RED), true);
            return TypedActionResult.fail(stack);
        }

        Vec3d from = user.getPos();
        Vec3d dir = dest.subtract(from);

        // эффекты в точке старта
        burst(sw, from);
        // след между точками
        int steps = (int) Math.min(60, dir.length() / 2);
        for (int i = 1; i < steps; i++) {
            Vec3d p = from.add(dir.multiply(i / (double) steps)).add(0, 1.0, 0);
            sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.15, 0.3, 0.15, 0.02);
        }

        user.requestTeleport(dest.x, dest.y, dest.z);
        user.fallDistance = 0;

        // эффекты в точке прибытия
        burst(sw, dest);

        cd.set(this, COOLDOWN_TICKS);
        return TypedActionResult.success(stack, true);
    }

    private static void burst(ServerWorld w, Vec3d p) {
        w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y + 1.0, p.z, 45, 0.3, 0.7, 0.3, 0.09);
        w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 1.0, p.z, 25, 0.4, 0.8, 0.4, 0.25);
        w.spawnParticles(ParticleTypes.END_ROD, p.x, p.y + 1.0, p.z, 10, 0.2, 0.6, 0.2, 0.05);
        w.playSound(null, p.x, p.y, p.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.25f);
        w.playSound(null, p.x, p.y, p.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.8f, 1.6f);
    }

    /** Ищет ближайшую свободную точку на луче взгляда (до 120 блоков), не телепортируя в стены. */
    @Nullable
    private static Vec3d findDestination(ServerWorld world, PlayerEntity user) {
        Vec3d start = user.getEyePos();
        Vec3d dir = user.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(RANGE));

        BlockHitResult hit = world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, user));
        double maxDist = hit.getType() == HitResult.Type.MISS ? RANGE : start.distanceTo(hit.getPos());

        float eye = user.getEyeHeight(user.getPose());
        for (double d = maxDist; d >= 1.0; d -= 0.25) {
            Vec3d feet = start.add(dir.multiply(d)).subtract(0, eye, 0);
            if (feet.y < world.getBottomY() || feet.y > world.getTopY() - 2) continue;
            if (!world.isChunkLoaded(BlockPos.ofFloored(feet))) continue;
            Box box = user.getDimensions(user.getPose()).getBoxAt(feet);
            if (world.isSpaceEmpty(user, box)) {
                return feet;
            }
        }
        return null;
    }

    /** Канон: урон от врага отключает Blink Dagger на 3 секунды. */
    public static void registerDamageLockout() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player && amount > 0 && source.getAttacker() != null
                    && source.getAttacker() != player) {
                ItemCooldownManager cd = player.getItemCooldownManager();
                if (!cd.isCoolingDown(ModItems.BLINK_DAGGER)) {
                    cd.set(ModItems.BLINK_DAGGER, DAMAGE_LOCKOUT_TICKS);
                }
            }
            return true;
        });
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.dotaitems.blink_dagger.tooltip1").formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("item.dotaitems.blink_dagger.tooltip2").formatted(Formatting.GRAY));
    }
}
