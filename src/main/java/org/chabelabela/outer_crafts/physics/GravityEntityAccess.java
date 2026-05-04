package org.chabelabela.outer_crafts.physics;

import net.minecraft.world.phys.Vec3;

public interface GravityEntityAccess {
    Vec3 outerCrafts$getLocalUp();
    String outerCrafts$getDominantBodyId();
    boolean outerCrafts$isOnCelestialSurface();
}
