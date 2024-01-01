package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.init.LegacyOptions;

@Mixin(MinecraftServer.class)
public class MinecraftServerClientMixin {
    @Redirect(method = "tickServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;tickCount:I", opcode = Opcodes.GETFIELD))
    private int runTick(MinecraftServer instance){
        int interval = ((LegacyOptions) Minecraft.getInstance().options).autoSaveInterval().get();
        return interval > 0 ? instance.getTickCount() / interval : 1;
    }
}
