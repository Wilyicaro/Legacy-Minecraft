package wily.legacy.mixin.base.client;

//? if >=1.20.5 {
import net.minecraft.client.DeltaTracker;
//?}
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.LegacyOptions;

@Mixin(GameRenderer.class)
public class GuiGameRendererMixin {
    //? if <1.20.5 {
    /*@Shadow
    private int itemActivationTicks;

    @Shadow private float itemActivationOffX;

    @Shadow @Nullable private ItemStack itemActivationItem;

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V"))
    private boolean render(Gui instance, GuiGraphics guiGraphics, float f){
        if (this.itemActivationItem != null && this.itemActivationTicks > 0 && Legacy4JClient.itemActivationRenderReplacement != null) {
            float g = ((float)40 - this.itemActivationTicks + f) / 40.0F;
            float h = g * g;
            float l = g * h;
            float m = 10.25F * l * h - 24.95F * h * h + 25.5F * l - 13.8F * h + 4.0F * g;
            float n = m * 3.1415927F;
            float o = this.itemActivationOffX * (float)(guiGraphics.guiWidth() / 4);
            float p = this.itemActivationOffX * (float)(guiGraphics.guiHeight() / 4);
            FactoryScreenUtil.enableDepthTest();
            RenderSystem.disableCull();
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float)(guiGraphics.guiWidth() / 2) + o * Mth.abs(Mth.sin(n * 2.0F)), (float)(guiGraphics.guiHeight() / 2) + p * Mth.abs(Mth.sin(n * 2.0F)), -50.0F);
            float q = 50.0F + 175.0F * Mth.sin(n);
            guiGraphics.pose().scale(q, -q, q);
            guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(900.0F * Mth.abs(Mth.sin(n))));
            guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(6.0F * Mth.cos(g * 8.0F)));
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(6.0F * Mth.cos(g * 8.0F)));
            Legacy4JClient.itemActivationRenderReplacement.render(guiGraphics,0,0,f);
            guiGraphics.pose().popMatrix();
        }
        return LegacyOptions.displayHUD.get();
    }
    *///?} else {
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private boolean render(Gui instance, GuiGraphics guiGraphics, DeltaTracker deltaTracker){
        return LegacyOptions.displayHUD.get();
    }
    //?}
}
