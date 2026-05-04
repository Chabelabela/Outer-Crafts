package org.chabelabela.outer_crafts.world.dimension;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.world.SolarSystemChunkGenerator;

public final class OuterCraftsDimensions {

    private OuterCraftsDimensions() {}

    public static final ResourceKey<Level> SOLAR_SYSTEM_LEVEL =
            ResourceKey.create(
                    Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "solar_system")
            );

    public static final ResourceKey<DimensionType> SOLAR_SYSTEM_DIMENSION_TYPE =
            ResourceKey.create(
                    Registries.DIMENSION_TYPE,
                    Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "solar_system")
            );

    /** The Eye dimension — endgame destination. Dimension JSON not yet shipped. */
    public static final ResourceKey<Level> EYE_LEVEL =
            ResourceKey.create(
                    Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "the_eye")
            );

    public static final ResourceKey<DimensionType> EYE_DIMENSION_TYPE =
            ResourceKey.create(
                    Registries.DIMENSION_TYPE,
                    Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "the_eye")
            );

    /**
     * Called during {@code RegisterEvent} (mod bus) — before built-in registries freeze.
     * Registers the custom chunk generator codec so the dimension JSON can deserialize it.
     */
    public static void onRegister(RegisterEvent event) {
        event.register(
                BuiltInRegistries.CHUNK_GENERATOR.key(),
                Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "solar_system"),
                () -> SolarSystemChunkGenerator.CODEC
        );
        OuterCrafts.LOGGER.info("[Dimensions] Registered solar system chunk generator");
    }
}
