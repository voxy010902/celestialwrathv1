package com.celestialwrath;

import net.minecraft.server.level.ServerLevel;

abstract class Strike {
    protected final ServerLevel level;
    protected int age = 0;
    Strike(ServerLevel level) { this.level = level; }
    /** @return true quand la frappe est terminée. */
    abstract boolean tick();
}
