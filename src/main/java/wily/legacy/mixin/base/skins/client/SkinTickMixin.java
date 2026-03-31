package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.skin.SkinSyncClient;

@Mixin(Minecraft.class)
public abstract class SkinTickMixin {
    @Unique
    private boolean legacy4j$wasInLevel = false;
    @Inject(method = "tick", at = @At("TAIL"))
    private void legacy4j$skinsTick(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        boolean inLevel = mc.level != null;
        if (legacy4j$wasInLevel && !inLevel) { SkinSyncClient.onClientDisconnect(); }
        legacy4j$wasInLevel = inLevel;
    }
}
