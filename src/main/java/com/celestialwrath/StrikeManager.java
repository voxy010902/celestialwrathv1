package com.celestialwrath;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

final class StrikeManager {
    private StrikeManager() {}

    private static final int MAX_MAIN = 3;
    private static final List<Strike> ACTIVE = new ArrayList<>();
    private static final List<Strike> PENDING = new ArrayList<>();
    private static int mainCount = 0;

    static boolean canStart() { return mainCount < MAX_MAIN; }

    static void start(DesignatorItem.Mode mode, ServerLevel l, BlockPos target) {
        Strike s = switch (mode) {
            case LANCE -> new BeamStrike(l, target);
            case STORM -> new MeteorStormStrike(l, target);
            case SINGULARITY -> new SingularityStrike(l, target);
        };
        mainCount++;
        PENDING.add(s);
    }

    /** Lance un météore qui touchera (ix, iz) dans ~60 ticks. */
    static void meteor(ServerLevel l, double ix, double iz, float power, boolean big) {
        RandomSource r = l.getRandom();
        double iy = Fx.groundY(l, ix, iz, 64);
        Vec3 vel = new Vec3(0.9 + (r.nextDouble() - 0.5) * 0.3, -2.4, 0.5 + (r.nextDouble() - 0.5) * 0.3);
        Vec3 start = new Vec3(ix, iy, iz).subtract(vel.scale(60));
        PENDING.add(new Meteor(l, start, vel, power, big));
    }

    static void tick() {
        if (!PENDING.isEmpty()) { ACTIVE.addAll(PENDING); PENDING.clear(); }
        List<Strike> snapshot = new ArrayList<>(ACTIVE);
        for (Strike s : snapshot) {
            boolean done;
            try {
                done = s.tick();
            } catch (Exception e) {
                e.printStackTrace();
                done = true;
            }
            if (done) {
                ACTIVE.remove(s);
                if (!(s instanceof Meteor)) mainCount = Math.max(0, mainCount - 1);
            }
        }
    }

    static void clear() { ACTIVE.clear(); PENDING.clear(); mainCount = 0; }
}
