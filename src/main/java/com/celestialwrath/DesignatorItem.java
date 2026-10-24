package com.celestialwrath;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Clic droit = tir. Accroupi + clic droit = changer de mode. */
public class DesignatorItem extends Item {

    public enum Mode {
        LANCE("Lance Orbitale"),
        STORM("Tempête de Météores"),
        SINGULARITY("Singularité");

        public final String label;
        Mode(String label) { this.label = label; }
        Mode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private static final Map<UUID, Mode> MODES = new HashMap<>();
    private static final double RANGE = 320;

    public DesignatorItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        Mode mode = MODES.getOrDefault(sp.getUUID(), Mode.LANCE);

        if (sp.isShiftKeyDown()) {
            mode = mode.next();
            MODES.put(sp.getUUID(), mode);
            sp.displayClientMessage(Component.literal("Mode : " + mode.label), true);
            return InteractionResult.SUCCESS;
        }

        BlockPos target = Fx.raycast(sl, RANGE, sp);
        if (target == null) {
            sp.displayClientMessage(Component.literal("Aucune cible valide."), true);
            return InteractionResult.FAIL;
        }
        if (!StrikeManager.canStart()) {
            sp.displayClientMessage(Component.literal("Trop de frappes en cours."), true);
            return InteractionResult.FAIL;
        }

        StrikeManager.start(mode, sl, target);
        sp.getCooldowns().addCooldown(sp.getItemInHand(hand), 20 * 45);
        return InteractionResult.SUCCESS;
    }
}
