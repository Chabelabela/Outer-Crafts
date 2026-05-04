package org.chabelabela.outer_crafts.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.chabelabela.outer_crafts.quantum.QuantumObservation;

/**
 * Quantum Shard (3.2): an unstable rock that teleports to a different shard's
 * position when nobody is looking at <em>it</em>.
 *
 * <p>Implementation: each shard schedules a random tick on itself; on tick we
 * test observation; if unobserved we look around for another shard within a
 * search radius and swap positions with it.
 *
 * <p>Notes:
 * <ul>
 *   <li>Movement is reactive — only happens when the shard ticks AND nobody can see it.</li>
 *   <li>The search radius is local: shards form clusters; cross-cluster
 *       teleporting would be jarring.</li>
 *   <li>Uses {@link QuantumObservation} to share LOS code with the Quantum Moon.</li>
 * </ul>
 */
public class QuantumShardBlock extends Block {

    public static final MapCodec<QuantumShardBlock> CODEC = simpleCodec(QuantumShardBlock::new);

    /** How many blocks around itself a shard searches for a swap partner. */
    private static final int SEARCH_RADIUS = 12;

    /** LOS observation range for this shard. */
    private static final double OBSERVATION_RANGE = 64.0;

    /** Probability per random tick of attempting a swap when unobserved. */
    private static final float SWAP_CHANCE = 0.4f;

    public QuantumShardBlock(BlockBehaviour.Properties properties) {
        // randomTicks() is enabled here so vanilla schedules a tick for us.
        super(properties.randomTicks());
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() > SWAP_CHANCE) return;

        Vec3 center = Vec3.atCenterOf(pos);
        boolean observed = QuantumObservation.isObserved(level, center, 0.5, OBSERVATION_RANGE);
        if (observed) return;

        // Find another shard in our search neighbourhood
        BlockPos other = findSwapPartner(level, pos, random);
        if (other == null || other.equals(pos)) return;

        // Don't swap if a player can see EITHER position
        Vec3 otherCenter = Vec3.atCenterOf(other);
        if (QuantumObservation.isObserved(level, otherCenter, 0.5, OBSERVATION_RANGE)) return;

        BlockState ourState = level.getBlockState(pos);
        BlockState theirState = level.getBlockState(other);

        // Swap (atomic enough — both writes happen before the next render frame on clients)
        level.setBlockAndUpdate(pos, theirState);
        level.setBlockAndUpdate(other, ourState);
    }

    private BlockPos findSwapPartner(ServerLevel level, BlockPos centre, RandomSource random) {
        // Sample up to N candidate positions in the search box; first match wins.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < 16; attempt++) {
            int dx = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int dy = random.nextInt(SEARCH_RADIUS / 2 + 1) - SEARCH_RADIUS / 4;
            int dz = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            cursor.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);

            if (cursor.equals(centre)) continue;
            if (level.getBlockState(cursor).getBlock() instanceof QuantumShardBlock) {
                return cursor.immutable();
            }
        }
        return null;
    }
}
