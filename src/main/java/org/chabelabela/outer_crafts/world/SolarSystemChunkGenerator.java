package org.chabelabela.outer_crafts.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.phys.Vec3;
import org.chabelabela.outer_crafts.physics.CelestialBody;
import org.chabelabela.outer_crafts.registry.CelestialBodyRegistry;
import org.chabelabela.outer_crafts.world.planet.PlanetGenerator;
import org.chabelabela.outer_crafts.world.planet.PlanetGenerators;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SolarSystemChunkGenerator extends ChunkGenerator {

    public static final MapCodec<SolarSystemChunkGenerator> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(SolarSystemChunkGenerator::getBiomeSource)
            ).apply(instance, SolarSystemChunkGenerator::new));

    public SolarSystemChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender, RandomState randomState,
            StructureManager structureManager, ChunkAccess chunk
    ) {
        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        int minY = chunk.getMinY();
        int maxY = minY + chunk.getHeight();

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;

        Map<String, Vec3> positions = CelestialBodyRegistry.resolveAllPositions(0);

        for (var entry : CelestialBodyRegistry.allBodies().entrySet()) {
            CelestialBody body = entry.getValue();
            Vec3 center = positions.get(body.id());
            if (center == null || body.radius() <= 0) continue;

            PlanetGenerator generator = PlanetGenerators.get(body.id());
            if (generator == null) continue;

            double maxRadius = body.radius() * 1.15;
            if (generator.hasAtmosphere()) {
                maxRadius += generator.atmosphereThickness();
            }

            double chunkCenterX = startX + 8.0;
            double chunkCenterZ = startZ + 8.0;
            double horizontalDist = Math.sqrt(
                    (chunkCenterX - center.x) * (chunkCenterX - center.x) +
                    (chunkCenterZ - center.z) * (chunkCenterZ - center.z)
            );
            if (horizontalDist > maxRadius + 12.0) continue;

            generatePlanetBlocks(chunk, body, center, generator, startX, startZ, minY, maxY);
        }

        return CompletableFuture.completedFuture(chunk);
    }

    private void generatePlanetBlocks(
            ChunkAccess chunk, CelestialBody body, Vec3 center,
            PlanetGenerator generator, int startX, int startZ, int minY, int maxY
    ) {
        double baseRadius = body.radius();
        long seed = Double.doubleToLongBits(center.x * 341873128712L + center.z * 132897987541L);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) {
            int worldX = startX + dx;
            for (int dz = 0; dz < 16; dz++) {
                int worldZ = startZ + dz;

                double surfaceNoise = computeSurfaceNoise(worldX, worldZ, seed);

                for (int worldY = minY; worldY < maxY; worldY++) {
                    double localX = worldX - center.x;
                    double localY = worldY - center.y;
                    double localZ = worldZ - center.z;

                    double distFromCenter = Math.sqrt(
                            localX * localX + localY * localY + localZ * localZ
                    );

                    double effectiveRadius = generator.getEffectiveRadius(baseRadius, surfaceNoise);

                    if (distFromCenter > effectiveRadius) {
                        continue;
                    }

                    double normalizedDistance = distFromCenter / effectiveRadius;

                    RandomSource random = RandomSource.create(
                            seed ^ ((long) worldX * 6364136223846793005L)
                                 ^ ((long) worldY * 1442695040888963407L)
                                 ^ ((long) worldZ * 3037000493L)
                    );

                    BlockState block = generator.getBlockAt(
                            normalizedDistance, surfaceNoise,
                            localX, localY, localZ, random
                    );

                    if (block != null) {
                        pos.set(worldX, worldY, worldZ);
                        chunk.setBlockState(pos, block);
                    }
                }
            }
        }
    }

    private double computeSurfaceNoise(int x, int z, long seed) {
        double noise = 0;
        double amplitude = 1.0;
        double frequency = 0.02;
        double totalAmplitude = 0;

        for (int octave = 0; octave < 4; octave++) {
            noise += amplitude * hashNoise(x * frequency, z * frequency, seed + octave);
            totalAmplitude += amplitude;
            amplitude *= 0.5;
            frequency *= 2.0;
        }

        return noise / totalAmplitude;
    }

    private double hashNoise(double x, double z, long seed) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        double fx = x - ix;
        double fz = z - iz;

        double sx = fx * fx * (3 - 2 * fx);
        double sz = fz * fz * (3 - 2 * fz);

        double n00 = hashPoint(ix, iz, seed);
        double n10 = hashPoint(ix + 1, iz, seed);
        double n01 = hashPoint(ix, iz + 1, seed);
        double n11 = hashPoint(ix + 1, iz + 1, seed);

        double nx0 = n00 + sx * (n10 - n00);
        double nx1 = n01 + sx * (n11 - n01);

        return nx0 + sz * (nx1 - nx0);
    }

    private double hashPoint(int x, int z, long seed) {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h ^= z * 0x6C62272E07BB0142L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h & 0xFFFFFFF) / (double) 0x8000000 - 1.0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types types,
                             LevelHeightAccessor heightAccessor, RandomState randomState) {
        Map<String, Vec3> positions = CelestialBodyRegistry.resolveAllPositions(0);
        for (var entry : CelestialBodyRegistry.allBodies().entrySet()) {
            CelestialBody body = entry.getValue();
            Vec3 center = positions.get(body.id());
            if (center == null || body.radius() <= 0) continue;

            double dx = x - center.x;
            double dz = z - center.z;
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);

            if (horizontalDist < body.radius()) {
                double verticalExtent = Math.sqrt(
                        body.radius() * body.radius() - horizontalDist * horizontalDist
                );
                return (int) (center.y + verticalExtent);
            }
        }
        return heightAccessor.getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z,
                                     LevelHeightAccessor heightAccessor, RandomState randomState) {
        int minY = heightAccessor.getMinY();
        int height = heightAccessor.getHeight();
        BlockState[] states = new BlockState[height];

        for (int i = 0; i < height; i++) {
            states[i] = Blocks.AIR.defaultBlockState();
        }

        return new NoiseColumn(minY, states);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk) {
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk,
                                     StructureManager structureManager) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return -64;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Outer Crafts Solar System");

        Map<String, Vec3> positions = CelestialBodyRegistry.resolveAllPositions(0);
        String nearest = "deep space";
        double nearestDist = Double.MAX_VALUE;

        for (var entry : CelestialBodyRegistry.allBodies().entrySet()) {
            Vec3 center = positions.get(entry.getKey());
            if (center == null) continue;
            double dist = center.distanceTo(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entry.getKey();
            }
        }

        info.add("Nearest body: " + nearest + " (%.0f blocks)".formatted(nearestDist));
    }
}
