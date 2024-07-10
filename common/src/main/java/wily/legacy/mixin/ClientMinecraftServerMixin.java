package wily.legacy.mixin;

import net.minecraft.server.MinecraftServer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.util.ScreenUtil;

@Mixin(MinecraftServer.class)
public abstract class ClientMinecraftServerMixin {

    @Shadow private int ticksUntilAutosave;

    @Redirect(method = "tickServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;ticksUntilAutosave:I", opcode = Opcodes.GETFIELD, ordinal = 1))
    private int tickServer(MinecraftServer instance){
        return ScreenUtil.getLegacyOptions().autoSaveInterval().get() > 0 ? ScreenUtil.getLegacyOptions().autoSaveInterval().get() * ticksUntilAutosave : 1;
    }
}
