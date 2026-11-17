package wily.legacy.client.control.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import wily.legacy.util.LegacyComponents;

public class HitPrediction {

    public static Component evaluate(Minecraft minecraft) {
        if (minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS && !minecraft.level.getWorldBorder().isWithinBounds(minecraft.hitResult.getLocation().x(), minecraft.hitResult.getLocation().z()))
            return null;

        Level level = minecraft.level;
        Player player = minecraft.player;
        ItemStack mainHandItem = player.getMainHandItem();

        if (minecraft.hitResult instanceof BlockHitResult r && r.getType() != HitResult.Type.MISS) {
            BlockState state = level.getBlockState(r.getBlockPos());
            if (state.getBlock() instanceof NoteBlock && !player.getAbilities().instabuild)
                return LegacyComponents.PLAY;
            else if ((player.getAbilities().instabuild || state.getBlock().defaultDestroyTime() >= 0 && !player.blockActionRestricted(level, r.getBlockPos(), minecraft.gameMode.getPlayerMode()) && player.getMainHandItem().canDestroyBlock(state, level, r.getBlockPos(), player)))
                return LegacyComponents.MINE;
        }
        if (minecraft.hitResult instanceof EntityHitResult r) {
            return mainHandItem.has(DataComponents.KINETIC_WEAPON) ? LegacyComponents.JAB : LegacyComponents.HIT;
        }
        return null;
    }
}
