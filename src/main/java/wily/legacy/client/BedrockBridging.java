package wily.legacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import wily.legacy.mixin.base.BlockItemAccessor;

public final class BedrockBridging {
    private BedrockBridging() {
    }

    public static @Nullable BlockHitResult findHit(Minecraft minecraft, InteractionHand hand) {
        if (!LegacyOptions.bedrockBridging.get() || minecraft.player == null || minecraft.level == null
                || minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.MISS) return null;
        var player = minecraft.player;
        var level = minecraft.level;
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator() || player.isPassenger() || !(stack.getItem() instanceof BlockItem item)
                || stack.isEmpty() || player.getCooldowns().isOnCooldown(stack)) return null;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        if (look.y >= 0) return null;
        double reach = player.blockInteractionRange();
        int belowFeet = Mth.floor(player.getY() - 0.001);
        for (int y = belowFeet; y >= belowFeet - 2; y--) {
            double distance = (y + 1 - eye.y) / look.y;
            if (distance < 0 || distance > reach) continue;
            Vec3 aimed = eye.add(look.scale(distance));
            BlockPos target = BlockPos.containing(aimed.x, y, aimed.z);
            if (level.isOutsideBuildHeight(target) || !level.getWorldBorder().isWithinBounds(target)) continue;

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos support = target.relative(direction);
                if (level.getBlockState(support).getCollisionShape(level, support).isEmpty()) continue;
                Direction face = direction.getOpposite();
                Vec3 location = Vec3.atCenterOf(support).relative(face, 0.5);
                if (eye.distanceToSqr(location) > reach * reach) continue;
                BlockHitResult hit = new BlockHitResult(location, face, support, false);
                BlockPlaceContext context = item.updatePlacementContext(new BlockPlaceContext(player, hand, stack, hit));
                if (context != null && context.getClickedPos().equals(target) && context.canPlace()
                        && ((BlockItemAccessor) item).getPlacementBlockState(context) != null) return hit;
            }
        }
        return null;
    }
}
