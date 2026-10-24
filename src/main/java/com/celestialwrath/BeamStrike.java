package com.celestialwrath;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Lance Orbitale : ciblage -> charge -> faisceau colossal -> cratère -> champignon -> météores. */
final class BeamStrike extends Strike {
    private static final int TARGET = 60, CHARGE = 40, IMPACT = TARGET + CHARGE, BEAM = 50, END = IMPACT + 260;
    private static final int RADIUS = 26, SKY = 280;

    private final BlockPos bp;
    private final Vec3 c;
    private final int gy;
    private Crater crater;
    private boolean finished;
    private final List<Shockwave> waves = new ArrayList<>();

    BeamStrike(ServerLevel l, BlockPos target) {
        super(l);
        this.bp = target;
        this.c = Vec3.atBottomCenterOf(target.above());
        this.gy = target.getY() + 1;
    }

    @Override
    boolean tick() {
        if (age == 0) {
            Fx.announce(level, c, 400, "☄ FRAPPE ORBITALE IMMINENTE — ÉVACUEZ LA ZONE !");
            Fx.sound(level, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, 16f, 0.6f);
        }
        if (age < TARGET) targeting();
        else if (age < IMPACT) charging();
        else {
            if (age == IMPACT) impact();
            if (age == IMPACT + 12) waves.add(new Shockwave(level, c, 80, 25f, 2.0));
            beam();
        }
        waves.removeIf(w -> !w.tick());
        if (crater != null && !finished && crater.step(2200)) { crater.finish(); finished = true; }
        age++;
        return age >= END;
    }

    private void targeting() {
        double r = 30 - age * 0.4;
        Fx.groundRing(level, ParticleTypes.SOUL_FIRE_FLAME, c.x, c.z, r, 72, 0.3, gy);
        Fx.groundRing(level, ParticleTypes.ELECTRIC_SPARK, c.x, c.z, r * 0.55, 40, 0.3, gy);
        for (int k = 0; k < 4; k++) {
            double a = age * 0.12 + k * Math.PI / 2;
            for (int j = 1; j <= 10; j++) {
                double d = r * j / 10.0, x = c.x + Math.cos(a) * d, z = c.z + Math.sin(a) * d;
                Fx.particle(level, ParticleTypes.END_ROD, x, Fx.groundY(level, x, z, gy) + 0.3, z, 1, 0, 0, 0, 0);
            }
        }
        Fx.column(level, ParticleTypes.END_ROD, c.x, c.y, c.y + SKY, c.z, 5, 1, 0.1);
        if (age % 10 == 0) Fx.sound(level, c.x, c.y, c.z, SoundEvents.BEACON_POWER_SELECT, 8f, 0.5f + age / 60f * 1.5f);
    }

    private void charging() {
        double t = (age - TARGET) / (double) CHARGE;
        double rad = 0.4 + t * 3.6;
        Fx.column(level, ParticleTypes.END_ROD, c.x, c.y, c.y + SKY, c.z, 3, 3, rad);
        Fx.column(level, ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.y + SKY, c.z, 6, 2, rad * 1.5);
        Fx.groundRing(level, ParticleTypes.SOUL_FIRE_FLAME, c.x, c.z, 14 * (1 - t) + 3, 60, 0.3, gy);
        Fx.sphere(level, ParticleTypes.END_ROD, new Vec3(c.x, c.y + SKY, c.z), 4 + t * 14, 60);
        if (age % 6 == 0) Fx.sound(level, c.x, c.y, c.z, SoundEvents.BEACON_AMBIENT, 12f, (float) (0.5 + t));
    }

    private void impact() {
        Fx.sound(level, c.x, c.y, c.z, SoundEvents.WITHER_SPAWN, 200f, 0.5f);
        Fx.sound(level, c.x, c.y, c.z, SoundEvents.DRAGON_FIREBALL_EXPLODE, 200f, 0.5f);
        Fx.sound(level, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, 200f, 0.6f);
        Fx.blind(level, c, 180, 140);
        level.explode(null, c.x, c.y + 1, c.z, 8f, false, Level.ExplosionInteraction.NONE);
        Fx.particle(level, ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 2, c.z, 6, 8, 3, 8, 0);
        crater = new Crater(level, bp, RADIUS);
        waves.add(new Shockwave(level, c, 120, 45f, 3.2));
    }

    private void beam() {
        RandomSource rng = level.getRandom();
        int ba = age - IMPACT;
        if (ba < BEAM) {
            double rad = 7 * (1 - ba / (double) BEAM * 0.6);
            Fx.column(level, ParticleTypes.END_ROD, c.x, c.y, c.y + SKY, c.z, 2, 10, rad);
            Fx.column(level, ParticleTypes.FLAME, c.x, c.y, c.y + SKY, c.z, 4, 4, rad * 1.3);
            Fx.column(level, ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.y + SKY, c.z, 3, 3, rad * 1.8);
            Fx.particle(level, ParticleTypes.LAVA, c.x, c.y + 1, c.z, 25, rad, 1, rad, 0);
            AABB box = new AABB(c.x - rad - 3, c.y - 12, c.z - rad - 3, c.x + rad + 3, c.y + SKY, c.z + rad + 3);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
                e.hurtServer(level, level.damageSources().explosion(null, null), 60f);
                e.igniteForSeconds(15);
            }
        }
        // Champignon atomique
        if (ba >= 8 && ba < 200) {
            double h = (ba - 8) * 0.9;
            double sw = 3 + h * 0.04;
            Fx.particle(level, ParticleTypes.LARGE_SMOKE, c.x, c.y + h * 0.5, c.z, 25, sw, h * 0.5, sw, 0.02);
            Fx.particle(level, ParticleTypes.FLAME, c.x, c.y + h * 0.3, c.z, 10, sw * 0.6, h * 0.3, sw * 0.6, 0.02);
            double cr = Math.min(34, 6 + h * 0.5);
            for (int i = 0; i < 48; i++) {
                double a = 2 * Math.PI * i / 48;
                Fx.particle(level, ParticleTypes.CLOUD, c.x + Math.cos(a) * cr, c.y + h, c.z + Math.sin(a) * cr, 2, 1, 0.6, 1, 0.05);
                if (i % 2 == 0) Fx.particle(level, ParticleTypes.LARGE_SMOKE, c.x + Math.cos(a) * cr * 0.6, c.y + h + 2, c.z + Math.sin(a) * cr * 0.6, 1, 1, 0.5, 1, 0.02);
            }
        }
        // Pluie de météores résiduelle
        if (ba >= 30 && ba < 130 && ba % 3 == 0) {
            for (int i = 0; i < 2; i++) {
                double a = rng.nextDouble() * 2 * Math.PI, d = 10 + rng.nextDouble() * 60;
                StrikeManager.meteor(level, c.x + Math.cos(a) * d, c.z + Math.sin(a) * d, 4f, false);
            }
        }
    }
}
