package wily.legacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import wily.factoryapi.util.FactoryScreenUtil;

public class LegacyGamma {
    //? if <1.21.2 {
    public static PostChain postEffect;
    //?}

    public static PostChain getPostEffect(){
        //? if <1.21.2 {
        return postEffect;
        //?} else {
        /*return Minecraft.getInstance().getShaderManager().getPostChain(LegacyResourceManager.GAMMA_LOCATION, LevelTargetBundle.MAIN_TARGETS);
         *///?}
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        PostChain gammaEffect = getPostEffect();
        if (gammaEffect != null && LegacyOptions.displayLegacyGamma.get()) {
            float gamma = LegacyOptions.legacyGamma.get().floatValue();
            graphics.flush();
            FactoryScreenUtil.enableBlend();
            FactoryScreenUtil.disableDepthTest();
            float tweakedGamma = gamma * 1.5f + 0.5f;
            //? if <1.21.5 {
            gammaEffect.passes.forEach(p-> p./*? if <1.21.2 {*/getEffect/*?} else {*//*getShader*//*?}*/().safeGetUniform("gamma").set(tweakedGamma));
            //?}
            gammaEffect.process(/*? if <1.21.2 {*/partialTick/*?} else {*//*Minecraft.getInstance().getMainRenderTarget(), Minecraft.getInstance().gameRenderer.resourcePool*//*?}*//*? if >1.21.4 {*//*, pass-> pass.setUniform("gamma", tweakedGamma)*//*?}*/);
            FactoryScreenUtil.enableDepthTest();
            FactoryScreenUtil.disableBlend();
        }
    }
}
