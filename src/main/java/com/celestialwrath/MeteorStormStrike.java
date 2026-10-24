package com.celestialwrath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Tempête de Météores : zone d'alerte rouge, pluie continue, gros bolides périodiques. */
final class MeteorStormStrike extends Strike {
    private static final int WARN = 50, DURATION = 320, END = WARN + DURATION + 80;
    private static final double R = 55;

    private final Vec3 c;
    private final int gy;

    MeteorStormStrike(ServerLevel l, BlockPos target) {
        super(l);
        this.c = Vec3.atBottomCenterOf(target.above());
        this.gy = target.getY() + 1;
    }

    @Override
    boolean tick() {
        RandomSource rng = level.getRandom();
        if (age == 0) {
            Fx.announce(level, c, 400, "☄ TEMPÊTE DE MÉTEORES — METTEZ-VOUS À L'ABRI !");
            Fx.sound(level, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, 16f, 0.5f);
        }
        if (age < WARN + DURATION && age % 2 == 0) {
            double pulse = 1 + 0.04 * Math.sin(age * 0.3);
            Fx.groundRing(level, ParticleTypes.FLAME, c.x, c.z, R * pulse, 140, 0.4, gy);
            Fx.groundRing(level, ParticleTypes.LARGE_SMOKE, c.x, c.z, R * 0.5 * pulse, 70, 0.4, gy);
        }
        if (age >= WARN && age < WARN + DURATION) {
            if (rng.nextFloat() < 0.45f) spawn(rng, 3.5f, false);
            if ((age - WARN) % 50 == 25) spawn(rng, 14f, true);
            if (age % 40 == 0) Fx.sound(level, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, 60f, 0.5f);
        }
        age++;
        return age >= END;
    }

    private void spawn(RandomSource rng, float power, boolean big) {
        double a = rng.nextDouble() * 2 * Math.PI, d = Math.sqrt(rng.nextDouble()) * R;
        StrikeManager.meteor(level, c.x + Math.cos(a) * d, c.z + Math.sin(a) * d, power, big);
    }
}
