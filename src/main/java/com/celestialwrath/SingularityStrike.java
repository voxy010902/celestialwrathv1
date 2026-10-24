package com.celestialwrath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Singularité : un trou noir suspendu aspire terrain et entités, puis s'effondre en implosion. */
final class SingularityStrike extends Strike {
    private static final int PULL = 130, AFTER = 120;

    private final BlockPos bp;
    private final Vec3 ground, core;
    private final int gy;
    private final List<FallingBlockEntity> debris = new ArrayList<>();
    private final List<Shockwave> waves = new ArrayList<>();
    private Crater crater;
    private boolean finished;

    SingularityStrike(ServerLevel l, BlockPos target) {
        super(l);
        this.bp = target;
        this.ground = Vec3.atBottomCenterOf(target.above());
        this.core = ground.add(0, 22, 0);
        this.gy = target.getY() + 1;
    }

    @Override
    boolean tick() {
        RandomSource rng = level.getRandom();
        if (age == 0) {
            Fx.announce(level, ground, 400, "● SINGULARITÉ DÉTECTÉE — FUYEZ !");
            Fx.sound(level, core.x, core.y, core.z, SoundEvents.ENDER_DRAGON_GROWL, 100f, 0.5f);
        }
        if (age < PULL) pull(rng);
        else {
            if (age == PULL) collapse();
            afterglow();
        }
        waves.removeIf(w -> !w.tick());
        if (crater != null && !finished && crater.step(2600)) { crater.finish(); finished = true; }
        age++;
        return age >= PULL + AFTER;
    }

    private void pull(RandomSource rng) {
        double g = age / (double) PULL;
        double cr = 1.2 + g * 4.5;

        // Noyau noir + sphère d'horizon
        Fx.particle(level, ParticleTypes.SQUID_INK, core.x, core.y, core.z, 45, cr * 0.6, cr * 0.6, cr * 0.6, 0.01);
        Fx.sphere(level, ParticleTypes.PORTAL, core, cr, 30);
        // Disque d'accrétion : 3 bras spiralés
        for (int arm = 0; arm < 3; arm++) {
            for (int j = 0; j < 26; j++) {
                double t = j / 26.0, rad = 3 + t * 20;
                double ang = age * 0.3 + arm * 2.094 + t * 6;
                var type = t < 0.35 ? ParticleTypes.END_ROD : t < 0.7 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.PORTAL;
                Fx.particle(level, type, core.x + Math.cos(ang) * rad, core.y + Math.sin(ang * 0.5) * rad * 0.12,
                        core.z + Math.sin(ang) * rad, 1, 0, 0, 0, 0);
            }
        }
        if (age % 5 == 0) Fx.particle(level, ParticleTypes.ELECTRIC_SPARK, core.x, core.y, core.z, 40, cr * 2, cr * 2, cr * 2, 0.2);
        if (age % 15 == 0) Fx.sound(level, core.x, core.y, core.z, SoundEvents.BEACON_AMBIENT, 20f, (float) (0.5 + g * 0.4));
        if (age == PULL - 40) Fx.blind(level, ground, 150, 60);

        // Aspiration des entités
        AABB box = new AABB(core, core).inflate(48);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            Vec3 d = core.subtract(e.position());
            double dist = d.length();
            if (dist < 1) continue;
            e.knockback((0.08 + g * 0.35) * Math.min(1.0, 22 / dist), -d.x, -d.z);
            e.push(0, 0.04 + g * 0.05, 0);
            if (dist < 8) e.hurtServer(level, level.damageSources().explosion(null, null), 3f);
        }

        // Arrachage du terrain
        if (age % 2 == 0 && age < PULL - 15 && debris.size() < 140) {
            for (int i = 0; i < 3; i++) {
                double a = rng.nextDouble() * 2 * Math.PI, rad = 5 + rng.nextDouble() * 22;
                double x = ground.x + Math.cos(a) * rad, z = ground.z + Math.sin(a) * rad;
                BlockPos p = new BlockPos((int) Math.floor(x), Fx.groundY(level, x, z, gy) - 1, (int) Math.floor(z));
                if (!level.hasChunkAt(p)) continue;
                BlockState s = level.getBlockState(p);
                if (s.isAir() || s.getDestroySpeed(level, p) < 0 || !s.getFluidState().isEmpty()) continue;
                FallingBlockEntity fb = FallingBlockEntity.fall(level, p, s);
                fb.setNoGravity(true);
                debris.add(fb);
            }
        }
        // Les débris spiralent vers le noyau
        Iterator<FallingBlockEntity> it = debris.iterator();
        while (it.hasNext()) {
            FallingBlockEntity fb = it.next();
            if (fb.isRemoved()) { it.remove(); continue; }
            Vec3 to = core.subtract(fb.position());
            double dist = to.length();
            if (dist < 3) { fb.discard(); it.remove(); continue; }
            Vec3 n = to.scale(1 / dist);
            fb.setDeltaMovement(n.scale(0.45).add(new Vec3(-n.z, 0.2, n.x).scale(0.4)));
        }
    }

    private void collapse() {
        for (FallingBlockEntity fb : debris) if (!fb.isRemoved()) fb.discard();
        debris.clear();
        Fx.sound(level, core.x, core.y, core.z, SoundEvents.WITHER_SPAWN, 200f, 0.4f);
        Fx.sound(level, core.x, core.y, core.z, SoundEvents.DRAGON_FIREBALL_EXPLODE, 200f, 0.4f);
        Fx.sound(level, core.x, core.y, core.z, SoundEvents.LIGHTNING_BOLT_THUNDER, 200f, 0.5f);
        Fx.blind(level, ground, 200, 160);
        level.explode(null, core.x, core.y, core.z, 12f, false, Level.ExplosionInteraction.NONE);
        Fx.particle(level, ParticleTypes.EXPLOSION_EMITTER, core.x, core.y, core.z, 8, 10, 6, 10, 0);
        crater = new Crater(level, bp, 28);
        waves.add(new Shockwave(level, ground, 130, 60f, 3.5));
        for (int i = 0; i < 14; i++) {
            RandomSource r = level.getRandom();
            double a = r.nextDouble() * 2 * Math.PI, d = 10 + r.nextDouble() * 50;
            StrikeManager.meteor(level, ground.x + Math.cos(a) * d, ground.z + Math.sin(a) * d, 4f, false);
        }
    }

    private void afterglow() {
        int ba = age - PULL;
        if (ba < 40) {
            double r = ba * 3.5;
            Fx.sphere(level, ParticleTypes.END_ROD, core, r, 220);
            Fx.sphere(level, ParticleTypes.SOUL_FIRE_FLAME, core, r * 0.8, 150);
        }
        if (ba < 25) {
            Fx.column(level, ParticleTypes.END_ROD, ground.x, ground.y, core.y, ground.z, 1.5, 6, 3.0 * (1 - ba / 25.0));
            Fx.particle(level, ParticleTypes.EXPLOSION, ground.x, ground.y + 1, ground.z, 6, 6, 2, 6, 0);
        }
        if (ba < 120) {
            Fx.particle(level, ParticleTypes.LARGE_SMOKE, ground.x, ground.y + ba * 0.7, ground.z, 20, 5, 2, 5, 0.03);
        }
    }
}
