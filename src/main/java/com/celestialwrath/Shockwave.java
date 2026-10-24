package com.celestialwrath;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Onde de choc : anneau au sol qui s'étend, projette et blesse tout sur son passage. */
final class Shockwave {
    private final ServerLevel l;
    private final Vec3 o;
    private final double maxR, speed;
    private final float maxDmg;
    private double r = 0;

    Shockwave(ServerLevel l, Vec3 origin, double maxR, float maxDmg, double speed) {
        this.l = l; this.o = origin; this.maxR = maxR; this.maxDmg = maxDmg; this.speed = speed;
    }

    /** @return false quand l'onde est terminée. */
    boolean tick() {
        double prev = r;
        r += speed;
        if (r > maxR) return false;

        int pts = (int) Math.min(420, Math.max(32, 2 * Math.PI * r / 1.1));
        int fb = (int) o.y;
        Fx.groundRing(l, ParticleTypes.CLOUD, o.x, o.z, r, pts, 1.0, fb);
        Fx.groundRing(l, ParticleTypes.FLAME, o.x, o.z, r * 0.97, pts / 2, 0.4, fb);
        Fx.groundRing(l, ParticleTypes.LARGE_SMOKE, o.x, o.z, r * 0.9, pts / 3, 2.0, fb);

        AABB box = new AABB(o, o).inflate(r + 2, 80, r + 2);
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, box)) {
            double dx = e.getX() - o.x, dz = e.getZ() - o.z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < prev || d >= r) continue;
            double f = 1 - d / maxR;
            e.hurtServer(l, l.damageSources().explosion(null, null), (float) (maxDmg * f * f + 2));
            double len = Math.max(d, 0.001);
            e.knockback(f * 5.0 + 0.5, -dx / len, -dz / len);
            e.push(0, 0.5 + f * 1.3, 0);
            e.igniteForSeconds((float) (10 * f));
        }
        return true;
    }
}
