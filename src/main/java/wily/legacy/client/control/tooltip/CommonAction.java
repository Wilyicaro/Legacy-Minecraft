package wily.legacy.client.control.tooltip;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.control.LegacyKeyMapping;
import wily.legacy.util.LegacyComponents;

import java.util.function.Function;
import java.util.function.Supplier;

public class CommonAction {
    public static final ListMap<String, Supplier<Component>> commonActions = new ListMap<>();
    public static final Codec<Supplier<Component>> CODEC = commonActions.createCodec(Codec.STRING);
    public static final Supplier<Component> USE = registerCommonAction("use", UsePrediction::evaluate);
    public static final Supplier<Component> HIT = registerCommonAction("hit", HitPrediction::evaluate);
    public static final Supplier<Component> PICK = registerCommonAction("pick", CommonAction::getPickAction);
    public static final Supplier<Component> SHIFT = registerCommonAction("shift", CommonAction::getShiftAction);
    public static final Supplier<Component> JUMP = registerCommonAction("jump", mc -> mc.player.isUnderWater() ? LegacyComponents.SWIM_UP : null);
    public static final Supplier<Component> INVENTORY = registerCommonAction("inventory", mc -> !mc.gameMode.isServerControlledInventory() || !(mc.player.getVehicle() instanceof AbstractHorse h) || h.isTamed() ? LegacyKeyMapping.of(mc.options.keyInventory).getDisplayName() : null);

    public static Supplier<Component> registerCommonAction(String key, Function<Minecraft, Component> function) {
        Supplier<Component> supplier = () -> function.apply(Minecraft.getInstance());
        commonActions.put(key, supplier);
        return supplier;
    }

    static Component getPickAction(Minecraft minecraft) {
        ItemStack result;
        BlockState b;
        if ((minecraft.hitResult instanceof EntityHitResult r && (result = r.getEntity().getPickResult()) != null || minecraft.hitResult instanceof BlockHitResult h && h.getType() != HitResult.Type.MISS && !(result = (b = minecraft.level.getBlockState(h.getBlockPos()))/*? if <1.21.4 {*//*.getBlock()*//*?}*/.getCloneItemStack(minecraft.level, h.getBlockPos(),/*? if >=1.21.4 {*/true/*?} else {*//*b*//*?}*/)).isEmpty()) && (Legacy4JClient.playerHasInfiniteMaterials() || minecraft.player.getInventory().findSlotMatchingItem(result) != -1))
            return minecraft.hitResult instanceof EntityHitResult ? LegacyComponents.PICK_ENTITY : ((LegacyKeyMapping) minecraft.options.keyPickItem).getDisplayName();

        return null;
    }

    static Component getShiftAction(Minecraft minecraft) {
        if (minecraft.player.isPassenger()) {
            return minecraft.player.getVehicle() instanceof LivingEntity ? LegacyComponents.DISMOUNT : LegacyComponents.EXIT;
        }
        BlockPos playerPos = minecraft.player.blockPosition();
        boolean inOrOnScaffolding = minecraft.level.getBlockState(playerPos).is(Blocks.SCAFFOLDING) || minecraft.level.getBlockState(playerPos.below()).is(Blocks.SCAFFOLDING);
        boolean scaffoldBelow = minecraft.level.getBlockState(playerPos.below()).is(Blocks.SCAFFOLDING);
        boolean notOnGroundFloor = !minecraft.player.onGround() || scaffoldBelow;
        return (inOrOnScaffolding && notOnGroundFloor) ? LegacyComponents.HOLD_TO_DESCEND : null;
    }
}
