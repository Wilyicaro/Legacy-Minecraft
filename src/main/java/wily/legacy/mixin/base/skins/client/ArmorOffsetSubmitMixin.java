package wily.legacy.mixin.base.skins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
//? if >=26.2 {
import net.minecraft.client.renderer.rendertype.RenderType;
//?} else {
/*import net.minecraft.client.renderer.SubmitNodeStorage;
*///?}
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.skins.client.render.ArmorOffsetRenderContext;

//? if >=26.2 {
@Mixin(ModelFeatureRenderer.Submit.class)
//?} else {
/*@Mixin(SubmitNodeStorage.ModelSubmit.class)
*///?}
public abstract class ArmorOffsetSubmitMixin implements ArmorOffsetRenderContext.SubmitAccess {
    @Unique
    private ArmorOffsetRenderContext.Offsets consoleskins$armorOffsets;

    @Override
    public ArmorOffsetRenderContext.Offsets consoleskins$getArmorOffsets() {
        return consoleskins$armorOffsets;
    }

    //? if >=26.2 {
    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void consoleskins$captureArmorOffsets(RenderType renderType, PoseStack.Pose pose, Model<?> model, Object state,
                                                  int lightCoords, int overlayCoords, int tintedColor,
                                                  TextureAtlasSprite sprite, PoseStack.Pose sheetedDecalPose,
                                                  CallbackInfo ci) {
        consoleskins$armorOffsets = ArmorOffsetRenderContext.submitOffsets();
    }
    //?} else {
    /*@Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void consoleskins$captureArmorOffsets(PoseStack.Pose pose, Model<?> model, Object state,
                                                  int light, int overlay, int color,
                                                  TextureAtlasSprite sprite, int outlineColor,
                                                  ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                                                  CallbackInfo ci) {
        consoleskins$armorOffsets = ArmorOffsetRenderContext.submitOffsets();
    }
    *///?}
}
