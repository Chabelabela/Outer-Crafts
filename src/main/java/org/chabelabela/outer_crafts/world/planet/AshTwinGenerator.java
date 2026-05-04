package org.chabelabela.outer_crafts.world.planet;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class AshTwinGenerator implements PlanetGenerator {

    @Override
    public BlockState getBlockAt(double normalizedDistance, double surfaceNoise,
                                 double localX, double localY, double localZ,
                                 RandomSource random) {
        if (normalizedDistance < 0.3) {
            float roll = random.nextFloat();
            if (roll < 0.05f) return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            if (roll < 0.1f) return Blocks.SMOOTH_STONE.defaultBlockState();
            return Blocks.STONE.defaultBlockState();
        } else if (normalizedDistance < 0.6) {
            float roll = random.nextFloat();
            if (roll < 0.08f) return Blocks.CHISELED_SANDSTONE.defaultBlockState();
            if (roll < 0.12f) return Blocks.CUT_SANDSTONE.defaultBlockState();
            return Blocks.SANDSTONE.defaultBlockState();
        } else if (normalizedDistance < 0.85) {
            return random.nextFloat() < 0.1f
                    ? Blocks.RED_SAND.defaultBlockState()
                    : Blocks.SAND.defaultBlockState();
        } else {
            return Blocks.SAND.defaultBlockState();
        }
    }

    @Override
    public double getEffectiveRadius(double baseRadius, double surfaceNoise) {
        return baseRadius * (1.0 + surfaceNoise * 0.04);
    }
}
