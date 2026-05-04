package org.chabelabela.outer_crafts.world.planet;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class HollowsLanternGenerator implements PlanetGenerator {

    @Override
    public BlockState getBlockAt(double normalizedDistance, double surfaceNoise,
                                 double localX, double localY, double localZ,
                                 RandomSource random) {
        if (normalizedDistance < 0.4) {
            return Blocks.LAVA.defaultBlockState();
        } else if (normalizedDistance < 0.7) {
            float roll = random.nextFloat();
            if (roll < 0.3f) return Blocks.MAGMA_BLOCK.defaultBlockState();
            return Blocks.BASALT.defaultBlockState();
        } else {
            float roll = random.nextFloat();
            if (roll < 0.1f) return Blocks.LAVA.defaultBlockState();
            if (roll < 0.25f) return Blocks.MAGMA_BLOCK.defaultBlockState();
            if (roll < 0.5f) return Blocks.BASALT.defaultBlockState();
            return Blocks.BLACKSTONE.defaultBlockState();
        }
    }

    @Override
    public double getEffectiveRadius(double baseRadius, double surfaceNoise) {
        return baseRadius * (1.0 + surfaceNoise * 0.1);
    }
}
