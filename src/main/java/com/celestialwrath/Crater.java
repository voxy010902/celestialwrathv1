package com.celestialwrath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Cratère creusé progressivement (évite le lag), avec débris projetés et fond de magma. */
final class Crater {
    private static final int MAX_DEBRIS = 160;

    private final ServerLevel level;
    private final BlockPos center;
    private final RandomSource rng;
    private final List<BlockPos> blocks = new ArrayList<>();
    private final List<int[]> floors = new ArrayList<>();
    private int idx = 0, debris = 0;

    Crater(ServerLevel level, BlockPos c, int radius) {
        this.level = level; this.center = c; this.rng = level.getRandom();
        int depthMax = Math.max(4, (int) (radius * 0.6));
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius) continue;
                double t = d / radius;
                int depth = (int) Math.round(depthMax * (1 - t * t));
                if (t > 0.7) depth += rng.nextInt(3) - 1;
                int up = (int) Math.round(radius * 0.6 * (1 - t)) + 3;
                int bottom = c.getY() - depth;
                for (int y = c.getY() + up; y >= bottom; y--) {
                    blocks.add(new BlockPos(c.getX() + dx, y, c.getZ() + dz));
                }
                floors.add(new int[]{c.getX() + dx, bottom - 1, c.getZ() + dz, (int) (t * 100)});
            }
        }
        blocks.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
    }

    /** @return true quand tout est creusé. */
    boolean step(int n) {
        int done = 0;
        while (idx < blocks.size() && done < n) {
            BlockPos p = blocks.get(idx++);
            done++;
            if (!level.hasChunkAt(p)) continue;
            BlockState s = level.getBlockState(p);
            if (s.isAir() || s.getDestroySpeed(level, p) < 0) continue;
            if (debris < MAX_DEBRIS && rng.nextInt(400) == 0 && s.getFluidState().isEmpty()) {
                throwDebris(p, s);
                debris++;
                continue;
            }
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        return idx >= blocks.size();
    }

    private void throwDebris(BlockPos p, BlockState s) {
        FallingBlockEntity fb = FallingBlockEntity.fall(level, p, s);
        Vec3 dir = new Vec3(p.getX() - center.getX(), 0, p.getZ() - center.getZ());
        dir = dir.lengthSqr() < 0.01
                ? new Vec3(rng.nextDouble() - 0.5, 0, rng.nextDouble() - 0.5).normalize()
                : dir.normalize();
        double sp = 0.6 + rng.nextDouble() * 1.4;
        fb.setDeltaMovement(dir.x * sp, 0.8 + rng.nextDouble() * 1.6, dir.z * sp);
    }

    /** Fond du cratère : lave au centre, magma, roche brûlée, obsidienne et feu sur les bords. */
    void finish() {
        for (int[] f : floors) {
            BlockPos p = new BlockPos(f[0], f[1], f[2]);
            if (!level.hasChunkAt(p)) continue;
            BlockState cur = level.getBlockState(p);
            if (cur.isAir() || cur.getDestroySpeed(level, p) < 0) continue;
            int t = f[3];
            BlockState ns;
            if (t < 16) ns = Blocks.LAVA.defaultBlockState();
            else if (t < 45) ns = rng.nextInt(10) < 6 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
            else if (t < 85) ns = rng.nextInt(4) == 0 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
            else ns = cur;
            if (ns != cur) level.setBlock(p, ns, Block.UPDATE_ALL);
            if (t >= 45 && t < 95 && rng.nextInt(7) == 0) {
                BlockPos above = p.above();
                if (level.getBlockState(above).isAir()) level.setBlock(above, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
