package wily.legacy.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.WorldData;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyWorldSettings;

@Mixin(MinecraftServer.class)
public abstract class ClientMinecraftServerMixin {

    @Shadow public abstract boolean isSingleplayerOwner(GameProfile gameProfile);

    @Shadow @Final protected WorldData worldData;

    @Redirect(method = "tickServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;tickCount:I", opcode = Opcodes.GETFIELD, ordinal = 1))
    private int isUnderSpawnProtection(MinecraftServer instance){
        int interval = ((LegacyOptions) Minecraft.getInstance().options).autoSaveInterval().get();
        return interval > 0 ? instance.getTickCount() / interval : 1;
    }
    @Inject(method = "isUnderSpawnProtection", at = @At(value = "HEAD"), cancellable = true)
    private void isUnderSpawnProtection(ServerLevel serverLevel, BlockPos blockPos, Player player, CallbackInfoReturnable<Boolean> cir){
        if (!isSingleplayerOwner(player.getGameProfile()) && !((LegacyWorldSettings)worldData).trustPlayers()) {
            cir.setReturnValue(true);
        }
    }
}
