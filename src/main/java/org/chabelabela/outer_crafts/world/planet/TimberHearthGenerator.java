package org.chabelabela.outer_crafts.world.planet;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class TimberHearthGenerator implements PlanetGenerator {

    @Override
    public BlockState getBlockAt(double normalizedDistance, double surfaceNoise,
                                 double localX, double localY, double localZ,
                                 RandomSource random) {
        if (normalizedDistance < 0.2) {
            return switch (random.nextInt(5)) {
                case 0 -> Blocks.RAW_IRON_BLOCK.defaultBlockState();
                case 1 -> Blocks.RAW_COPPER_BLOCK.defaultBlockState();
                default -> Blocks.DEEPSLATE.defaultBlockState();
            };
        } else if (normalizedDistance < 0.7) {
            return random.nextFloat() < 0.02f
                    ? Blocks.LAVA.defaultBlockState()
                    : Blocks.DEEPSLATE.defaultBlockState();
        } else if (normalizedDistance < 0.92) {
            float roll = random.nextFloat();
            if (roll < 0.02f) return Blocks.COAL_ORE.defaultBlockState();
            if (roll < 0.035f) return Blocks.IRON_ORE.defaultBlockState();
            if (roll < 0.04f) return Blocks.COPPER_ORE.defaultBlockState();
            return Blocks.STONE.defaultBlockState();
        } else if (normalizedDistance < 0.97) {
            return random.nextFloat() < 0.3f
                    ? Blocks.COARSE_DIRT.defaultBlockState()
                    : Blocks.DIRT.defaultBlockState();
        } else {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
    }

    @Override
    public double getEffectiveRadius(double baseRadius, double surfaceNoise) {
        return baseRadius * (1.0 + surfaceNoise * 0.06);
    }

    @Override
    public boolean hasAtmosphere() { return true; }

    @Override
    public double atmosphereThickness() { return 20.0; }
}
