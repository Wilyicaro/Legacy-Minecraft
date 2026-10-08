package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.GuiItemAtlas;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiItemAtlas.class)
public abstract class GuiItemAtlasMixin {
    @Shadow
    @Final
    private int textureSize;

    @ModifyVariable(method = {"<init>", "computeTextureSizeFor"}, at = @At("HEAD"), name = "slotTextureSize")
    private static int legacy$padSlot(int slotTextureSize) {
        return slotTextureSize + 2;
    }

    @WrapOperation(method = "drawToSlot", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void legacy$keepItemSize(PoseStack poseStack, float x, float y, float z, Operation<Void> original) {
        original.call(poseStack, x - 2, y + 2, z - 2);
    }

    @WrapOperation(method = "drawToSlot", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableScissorForRenderTypeDraws(IIII)V"))
    private void legacy$keepGutterClear(int x, int y, int width, int height, Operation<Void> original) {
        original.call(x + 1, y + 1, width - 2, height - 2);
    }

    @ModifyReturnValue(method = "getOrUpdate", at = @At("RETURN"))
    private GuiItemAtlas.SlotView legacy$sampleInsideGutter(GuiItemAtlas.SlotView slot) {
        if (slot == null) return null;
        float gutter = 1.0f / textureSize;
        return new GuiItemAtlas.SlotView(slot.textureView(), slot.u0() + gutter, slot.v0() - gutter, slot.u1() - gutter, slot.v1() + gutter);
    }
}
