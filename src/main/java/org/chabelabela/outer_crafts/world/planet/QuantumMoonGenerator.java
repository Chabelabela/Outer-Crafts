package org.chabelabela.outer_crafts.world.planet;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class QuantumMoonGenerator implements PlanetGenerator {

    @Override
    public BlockState getBlockAt(double normalizedDistance, double surfaceNoise,
                                 double localX, double localY, double localZ,
                                 RandomSource random) {
        if (normalizedDistance < 0.5) {
            return random.nextFloat() < 0.05f
                    ? Blocks.AMETHYST_BLOCK.defaultBlockState()
                    : Blocks.STONE.defaultBlockState();
        } else if (normalizedDistance < 0.85) {
            float roll = random.nextFloat();
            if (roll < 0.03f) return Blocks.BUDDING_AMETHYST.defaultBlockState();
            if (roll < 0.3f) return Blocks.TUFF.defaultBlockState();
            if (roll < 0.5f) return Blocks.DIORITE.defaultBlockState();
            return Blocks.STONE.defaultBlockState();
        } else {
            float roll = random.nextFloat();
            if (roll < 0.05f) return Blocks.AMETHYST_BLOCK.defaultBlockState();
            if (roll < 0.1f) return Blocks.CALCITE.defaultBlockState();
            if (roll < 0.3f) return Blocks.SMOOTH_STONE.defaultBlockState();
            if (roll < 0.5f) return Blocks.TUFF.defaultBlockState();
            return Blocks.DIORITE.defaultBlockState();
        }
    }

    @Override
    public double getEffectiveRadius(double baseRadius, double surfaceNoise) {
        return baseRadius * (1.0 + surfaceNoise * 0.02);
    }
}
