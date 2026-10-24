package com.celestialwrath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Utilitaires d'effets : particules visibles de très loin, sons, raycast sûr. */
public final class Fx {
    private Fx() {}

    public static final double VIEW = 380;

    /** Envoie des particules "forcées" (visibles >32 blocs) aux joueurs proches. */
    public static void particle(ServerLevel l, ParticleOptions p, double x, double y, double z,
                                int count, double dx, double dy, double dz, double speed) {
        for (ServerPlayer pl : l.players()) {
            if (pl.distanceToSqr(x, y, z) > VIEW * VIEW) continue;
            // Signature 1.21.4+ : (player, particle, overrideLimiter, alwaysShow, x,y,z, count, dx,dy,dz, speed)
            l.sendParticles(pl, p, true, true, x, y, z, count, dx, dy, dz, speed);
        }
    }

    public static void sound(ServerLevel l, double x, double y, double z, SoundEvent s, float vol, float pitch) {
        l.playSound(null, x, y, z, s, SoundSource.AMBIENT, vol, pitch);
    }

    public static void announce(ServerLevel l, Vec3 c, double range, String msg) {
        for (ServerPlayer p : l.players()) {
            if (p.distanceToSqr(c) < range * range) p.displayClientMessage(Component.literal(msg), true);
        }
    }

    /** Éblouissement : effet Darkness aux joueurs proches. */
    public static void blind(ServerLevel l, Vec3 c, double range, int ticks) {
        for (ServerPlayer p : l.players()) {
            if (p.distanceToSqr(c) < range * range) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ticks, 0, false, false));
            }
        }
    }

    public static int groundY(ServerLevel l, double x, double z, int fallback) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        if (!l.hasChunkAt(new BlockPos(ix, 0, iz))) return fallback;
        return l.getHeight(Heightmap.Types.MOTION_BLOCKING, ix, iz);
    }

    public static void groundRing(ServerLevel l, ParticleOptions p, double cx, double cz,
                                  double r, int points, double yOff, int fallbackY) {
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            double x = cx + Math.cos(a) * r, z = cz + Math.sin(a) * r;
            particle(l, p, x, groundY(l, x, z, fallbackY) + yOff, z, 1, 0, 0, 0, 0);
        }
    }

    public static void column(ServerLevel l, ParticleOptions p, double x, double y0, double y1, double z,
                              double step, int count, double spread) {
        for (double y = y0; y <= y1; y += step) {
            particle(l, p, x, y, z, count, spread, step * 0.5, spread, 0);
        }
    }

    public static void sphere(ServerLevel l, ParticleOptions p, Vec3 c, double r, int n) {
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < n; i++) {
            double y = 1 - 2.0 * (i + 0.5) / n;
            double rr = Math.sqrt(1 - y * y);
            double th = golden * i;
            particle(l, p, c.x + Math.cos(th) * rr * r, c.y + y * r, c.z + Math.sin(th) * rr * r, 1, 0, 0, 0, 0);
        }
    }

    /** Raycast manuel qui ne charge jamais de chunk. */
    public static BlockPos raycast(ServerLevel l, double range, ServerPlayer p) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (double t = 1; t <= range; t += 0.5) {
            BlockPos bp = BlockPos.containing(eye.add(look.scale(t)));
            if (!l.hasChunkAt(bp)) return null;
            BlockState s = l.getBlockState(bp);
            if (!s.isAir() && s.getFluidState().isEmpty()) return bp;
        }
        return null;
    }
}
