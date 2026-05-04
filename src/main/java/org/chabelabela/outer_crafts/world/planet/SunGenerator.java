package org.chabelabela.outer_crafts.world.planet;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class SunGenerator implements PlanetGenerator {

    @Override
    public BlockState getBlockAt(double normalizedDistance, double surfaceNoise,
                                 double localX, double localY, double localZ,
                                 RandomSource random) {
        if (normalizedDistance < 0.3) {
            return Blocks.LAVA.defaultBlockState();
        } else if (normalizedDistance < 0.7) {
            return Blocks.MAGMA_BLOCK.defaultBlockState();
        } else if (normalizedDistance < 0.95) {
            return random.nextFloat() < 0.15f
                    ? Blocks.SHROOMLIGHT.defaultBlockState()
                    : Blocks.GLOWSTONE.defaultBlockState();
        } else {
            return random.nextFloat() < 0.3f
                    ? Blocks.GLOWSTONE.defaultBlockState()
                    : null;
        }
    }

    @Override
    public double getEffectiveRadius(double baseRadius, double surfaceNoise) {
        return baseRadius * (1.0 + surfaceNoise * 0.08);
    }
}
