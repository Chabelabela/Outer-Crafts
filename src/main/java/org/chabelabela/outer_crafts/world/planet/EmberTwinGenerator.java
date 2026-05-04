package org.chabelabela.outer_crafts.world.planet;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class EmberTwinGenerator implements PlanetGenerator {

    @Override
    public BlockState getBlockAt(double normalizedDistance, double surfaceNoise,
                                 double localX, double localY, double localZ,
                                 RandomSource random) {
        if (normalizedDistance < 0.25) {
            return random.nextFloat() < 0.4f
                    ? Blocks.LAVA.defaultBlockState()
                    : Blocks.MAGMA_BLOCK.defaultBlockState();
        } else if (normalizedDistance < 0.6) {
            float roll = random.nextFloat();
            if (roll < 0.3f) return Blocks.BASALT.defaultBlockState();
            if (roll < 0.5f) return Blocks.BLACKSTONE.defaultBlockState();
            return Blocks.DEEPSLATE.defaultBlockState();
        } else if (normalizedDistance < 0.85) {
            float roll = random.nextFloat();
            if (roll < 0.3f) return Blocks.RED_SANDSTONE.defaultBlockState();
            if (roll < 0.5f) return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            if (roll < 0.6f) return Blocks.RED_TERRACOTTA.defaultBlockState();
            return Blocks.TERRACOTTA.defaultBlockState();
        } else {
            float roll = random.nextFloat();
            if (roll < 0.4f) return Blocks.RED_SAND.defaultBlockState();
            if (roll < 0.6f) return Blocks.SAND.defaultBlockState();
            return Blocks.RED_SANDSTONE.defaultBlockState();
        }
    }

    @Override
    public double getEffectiveRadius(double baseRadius, double surfaceNoise) {
        return baseRadius * (1.0 + surfaceNoise * 0.08);
    }
}
