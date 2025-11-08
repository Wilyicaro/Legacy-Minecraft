package wily.legacy.mixin.base.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import wily.legacy.client.screen.CreativeModeScreen;

@Mixin(MultiPlayerGameMode.class)
public class MultiplayerGameModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    //? if >=1.21.2 {
    @ModifyVariable(method = "handleCreativeModeItemDrop", at = @At("STORE"))
    public boolean handleCreativeModeItemDrop(boolean original) {
        return original && !(minecraft.screen instanceof CreativeModeScreen);
    }
    //?}
}
