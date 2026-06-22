package wily.legacy.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class LegacyDragonEggTeleportParticles {
    private static final int MAX_PENDING_AGE = 4;
    private static final int MAX_HORIZONTAL_OFFSET = 16;
    private static final int MAX_VERTICAL_OFFSET = 8;
    private static final int PARTICLE_COUNT = 128;
    private static final List<PendingDragonEggUpdate> PENDING_UPDATES = new ArrayList<>();

    private LegacyDragonEggTeleportParticles() {
    }

    public static void handleBlockUpdate(ClientLevel level, BlockPos blockPos, BlockState oldState, BlockState newState) {
        boolean wasDragonEgg = oldState.is(Blocks.DRAGON_EGG);
        boolean isDragonEgg = newState.is(Blocks.DRAGON_EGG);
        if (wasDragonEgg == isDragonEgg) {
            pruneStaleUpdates(level, level.getGameTime());
            return;
        }

        long gameTime = level.getGameTime();
        PendingDragonEggUpdate currentUpdate = new PendingDragonEggUpdate(level, blockPos.immutable(), isDragonEgg, gameTime);
        pruneStaleUpdates(level, gameTime);

        PendingDragonEggUpdate match = findMatchingUpdate(currentUpdate);
        if (match != null) {
            PENDING_UPDATES.remove(match);
            BlockPos sourcePos = currentUpdate.isPlacement ? match.blockPos : currentUpdate.blockPos;
            BlockPos destinationPos = currentUpdate.isPlacement ? currentUpdate.blockPos : match.blockPos;
            spawnTeleportParticles(level, sourcePos, destinationPos);
            return;
        }

        PENDING_UPDATES.add(currentUpdate);
    }

    private static void pruneStaleUpdates(ClientLevel level, long gameTime) {
        Iterator<PendingDragonEggUpdate> iterator = PENDING_UPDATES.iterator();
        while (iterator.hasNext()) {
            PendingDragonEggUpdate update = iterator.next();
            if (update.level != level || gameTime - update.gameTime > MAX_PENDING_AGE) {
                iterator.remove();
            }
        }
    }

    private static PendingDragonEggUpdate findMatchingUpdate(PendingDragonEggUpdate currentUpdate) {
        PendingDragonEggUpdate bestMatch = null;
        double bestDistance = Double.MAX_VALUE;
        for (PendingDragonEggUpdate pendingUpdate : PENDING_UPDATES) {
            if (pendingUpdate.level != currentUpdate.level || pendingUpdate.isPlacement == currentUpdate.isPlacement) {
                continue;
            }
            if (!isPossibleTeleportPair(pendingUpdate.blockPos, currentUpdate.blockPos)) {
                continue;
            }

            double distance = pendingUpdate.blockPos.distSqr(currentUpdate.blockPos);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = pendingUpdate;
            }
        }
        return bestMatch;
    }

    private static boolean isPossibleTeleportPair(BlockPos firstPos, BlockPos secondPos) {
        return Math.abs(firstPos.getX() - secondPos.getX()) <= MAX_HORIZONTAL_OFFSET
            && Math.abs(firstPos.getY() - secondPos.getY()) <= MAX_VERTICAL_OFFSET
            && Math.abs(firstPos.getZ() - secondPos.getZ()) <= MAX_HORIZONTAL_OFFSET;
    }

    private static void spawnTeleportParticles(ClientLevel level, BlockPos sourcePos, BlockPos destinationPos) {
        RandomSource random = level.random;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double progress = random.nextDouble();
            float velocityX = (random.nextFloat() - 0.5F) * 0.2F;
            float velocityY = (random.nextFloat() - 0.5F) * 0.2F;
            float velocityZ = (random.nextFloat() - 0.5F) * 0.2F;
            double x = Mth.lerp(progress, destinationPos.getX(), sourcePos.getX()) + random.nextDouble() - 0.5D + 0.5D;
            double y = Mth.lerp(progress, destinationPos.getY(), sourcePos.getY()) + random.nextDouble() - 0.5D;
            double z = Mth.lerp(progress, destinationPos.getZ(), sourcePos.getZ()) + random.nextDouble() - 0.5D + 0.5D;
            level.addParticle(ParticleTypes.PORTAL, x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    private record PendingDragonEggUpdate(ClientLevel level, BlockPos blockPos, boolean isPlacement, long gameTime) {
    }
}
