package wily.legacy.mixin.base.client;

//? if >=1.20.5 {

import net.minecraft.client.DeltaTracker;
//?}
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.LegacyOptions;

@Mixin(GameRenderer.class)
public class GuiGameRendererMixin {
    //? if <1.20.5 {
    /*@Shadow
    private int itemActivationTicks;

    @Shadow private float itemActivationOffX;

    @Shadow @Nullable private ItemStack itemActivationItem;

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V"))
    private boolean render(Gui instance, GuiGraphicsExtractor GuiGraphicsExtractor, float f){
        if (this.itemActivationItem != null && this.itemActivationTicks > 0 && Legacy4JClient.itemActivationRenderReplacement != null) {
            float g = ((float)40 - this.itemActivationTicks + f) / 40.0F;
            float h = g * g;
            float l = g * h;
            float m = 10.25F * l * h - 24.95F * h * h + 25.5F * l - 13.8F * h + 4.0F * g;
            float n = m * 3.1415927F;
            float o = this.itemActivationOffX * (float)(GuiGraphicsExtractor.guiWidth() / 4);
            float p = this.itemActivationOffX * (float)(GuiGraphicsExtractor.guiHeight() / 4);
            FactoryScreenUtil.enableDepthTest();
            RenderSystem.disableCull();
            GuiGraphicsExtractor.pose().pushMatrix();
            GuiGraphicsExtractor.pose().translate((float)(GuiGraphicsExtractor.guiWidth() / 2) + o * Mth.abs(Mth.sin(n * 2.0F)), (float)(GuiGraphicsExtractor.guiHeight() / 2) + p * Mth.abs(Mth.sin(n * 2.0F)), -50.0F);
            float q = 50.0F + 175.0F * Mth.sin(n);
            GuiGraphicsExtractor.pose().scale(q, -q, q);
            GuiGraphicsExtractor.pose().mulPose(Axis.YP.rotationDegrees(900.0F * Mth.abs(Mth.sin(n))));
            GuiGraphicsExtractor.pose().mulPose(Axis.XP.rotationDegrees(6.0F * Mth.cos(g * 8.0F)));
            GuiGraphicsExtractor.pose().mulPose(Axis.ZP.rotationDegrees(6.0F * Mth.cos(g * 8.0F)));
            Legacy4JClient.itemActivationRenderReplacement.extractRenderState(GuiGraphicsExtractor,0,0,f);
            GuiGraphicsExtractor.pose().popMatrix();
        }
        return LegacyOptions.displayHUD.get();
    }
    *///?} else {
    @WrapWithCondition(method = "extractGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private boolean render(Gui instance, GuiGraphicsExtractor GuiGraphicsExtractor, DeltaTracker deltaTracker) {
        return LegacyOptions.displayHUD.get();
    }
    //?}
}
