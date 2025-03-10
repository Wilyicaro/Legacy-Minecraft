package wily.legacy.mixin.base;

import net.minecraft.advancements.critereon.ChangeDimensionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.config.LegacyWorldOptions;
import wily.legacy.util.LegacyComponents;

@Mixin(ChangeDimensionTrigger.class)
public class ChangeDimensionTriggerMixin {
    @Inject(method = "trigger", at = @At("RETURN"))
    public void trigger(ServerPlayer serverPlayer, ResourceKey<Level> resourceKey, ResourceKey<Level> resourceKey2, CallbackInfo ci) {
        if (resourceKey2.equals(Level.END)){
            serverPlayer.serverLevel().getServer().getPlayerList().broadcastSystemMessage(LegacyComponents.getEnteredDimensionMessage(serverPlayer.getDisplayName(), Level.END), false);
            if (/*? if <1.21.1 {*//*((EntityAccessor)serverPlayer).getPortalEntrancePos()*//*?} else {*/serverPlayer.portalProcess/*?}*/ != null){
                BlockPos entryPos = /*? if <1.21.1 {*//*((EntityAccessor)serverPlayer).getPortalEntrancePos()*//*?} else {*/serverPlayer.portalProcess.getEntryPosition()/*?}*/;
                if (LegacyWorldOptions.usedEndPortalPositions.get().stream().noneMatch(pos-> pos.inRange(entryPos))){
                    LegacyWorldOptions.usedEndPortalPositions.get().add(new LegacyWorldOptions.UsedEndPortalPos(entryPos, serverPlayer.getUUID()));
                    LegacyWorldOptions.usedEndPortalPositions.save();
                }
            }
        } else if (resourceKey.equals(Level.END)) {
            serverPlayer.serverLevel().getServer().getPlayerList().broadcastSystemMessage(LegacyComponents.getLeftDimensionMessage(serverPlayer.getDisplayName(), Level.END), false);
        }
    }
}
