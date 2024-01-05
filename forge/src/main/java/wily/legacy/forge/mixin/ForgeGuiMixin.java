package wily.legacy.forge.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.ScreenUtil;

@Mixin(ForgeGui.class)
public abstract class ForgeGuiMixin {

    @Inject(method = "setupOverlayRenderState", at = @At("RETURN"),remap = false)
    private void setupOverlayRenderState(boolean blend, boolean depthTest, CallbackInfo ci) {
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f, ScreenUtil.getHUDOpacity());
    }
}
