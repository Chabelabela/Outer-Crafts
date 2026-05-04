package org.chabelabela.outer_crafts.world.planet;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class DarkBrambleGenerator implements PlanetGenerator {

    @Override
    public BlockState getBlockAt(double normalizedDistance, double surfaceNoise,
                                 double localX, double localY, double localZ,
                                 RandomSource random) {
        if (normalizedDistance < 0.3) {
            return random.nextFloat() < 0.005f
                    ? Blocks.COBWEB.defaultBlockState()
                    : null;
        } else if (normalizedDistance < 0.5) {
            float roll = random.nextFloat();
            if (roll < 0.08f) return Blocks.COBWEB.defaultBlockState();
            if (roll < 0.12f) return Blocks.DARK_OAK_WOOD.defaultBlockState();
            if (roll < 0.15f) return Blocks.DARK_OAK_LEAVES.defaultBlockState();
            return null;
        } else if (normalizedDistance < 0.8) {
            float roll = random.nextFloat();
            if (roll < 0.05f) return null;
            if (roll < 0.25f) return Blocks.DARK_OAK_LEAVES.defaultBlockState();
            if (roll < 0.35f) return Blocks.MANGROVE_ROOTS.defaultBlockState();
            return Blocks.DARK_OAK_WOOD.defaultBlockState();
        } else {
            float roll = random.nextFloat();
            if (roll < 0.03f) return null;
            if (roll < 0.15f) return Blocks.DARK_OAK_LEAVES.defaultBlockState();
            if (roll < 0.3f) return Blocks.MANGROVE_ROOTS.defaultBlockState();
            return Blocks.DARK_OAK_WOOD.defaultBlockState();
        }
    }

    @Override
    public double getEffectiveRadius(double baseRadius, double surfaceNoise) {
        return baseRadius * (1.0 + surfaceNoise * 0.12);
    }
}
