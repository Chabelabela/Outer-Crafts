package org.chabelabela.outer_crafts.world.planet;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public interface PlanetGenerator {

    BlockState getBlockAt(double normalizedDistance, double surfaceNoise,
                          double localX, double localY, double localZ,
                          RandomSource random);

    default double getEffectiveRadius(double baseRadius, double surfaceNoise) {
        return baseRadius * (1.0 + surfaceNoise * 0.05);
    }

    default boolean hasAtmosphere() {
        return false;
    }

    default double atmosphereThickness() {
        return 0;
    }
}
