package com.celestialwrath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class Meteor extends Strike {
    private Vec3 pos;
    private final Vec3 vel;
    private final float power;
    private final boolean big;

    Meteor(ServerLevel l, Vec3 start, Vec3 vel, float power, boolean big) {
        super(l);
        this.pos = start; this.vel = vel; this.power = power; this.big = big;
    }

    @Override
    boolean tick() {
        if (age == 0) Fx.sound(level, pos.x, pos.y, pos.z, SoundEvents.FIRECHARGE_USE, big ? 40f : 8f, 0.6f);
        age++;
        for (int i = 0; i < 4; i++) {
            pos = pos.add(vel.scale(0.25));
            BlockPos bp = BlockPos.containing(pos);
            if (pos.y < -64 || !level.hasChunkAt(bp)) return true;
            if (!level.getBlockState(bp).isAir()) { impact(); return true; }
        }
        int n = big ? 40 : 10;
        Fx.particle(level, ParticleTypes.FLAME, pos.x, pos.y, pos.z, n, 0.6, 0.6, 0.6, 0.03);
        Fx.particle(level, ParticleTypes.LARGE_SMOKE, pos.x - vel.x * 0.6, pos.y - vel.y * 0.6, pos.z - vel.z * 0.6,
                n / 2, 0.5, 0.5, 0.5, 0.02);
        if (big) Fx.particle(level, ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 20, 1.2, 1.2, 1.2, 0.1);
        return false;
    }

    private void impact() {
        level.explode(null, pos.x, pos.y, pos.z, power, true, Level.ExplosionInteraction.TNT);
        Fx.particle(level, ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, big ? 3 : 1, 1, 1, 1, 0);
        Fx.particle(level, ParticleTypes.CLOUD, pos.x, pos.y + 1, pos.z, big ? 120 : 30, 2, 1, 2, 0.25);
    }
}
