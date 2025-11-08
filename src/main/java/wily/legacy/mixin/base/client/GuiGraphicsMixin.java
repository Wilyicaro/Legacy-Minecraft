package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.client.LegacyGuiItemRenderer;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.MutablePIPRenderState;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.List;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    //? if forge || neoforge {
    /*@Shadow(remap = false) private ItemStack tooltipStack;
     *///?}

    @Unique
    GuiGraphics self() {
        return (GuiGraphics) (Object) this;
    }

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
    private void renderItemDecorationsHead(Font font, ItemStack itemStack, int i, int j, String string, CallbackInfo ci) {
        LegacyFontUtil.legacyFont = false;
    }

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void renderItemDecorationsTail(Font font, ItemStack itemStack, int i, int j, String string, CallbackInfo ci) {
        LegacyFontUtil.legacyFont = true;
    }

    @Inject(method = /*? if neoforge {*/ /*"renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/world/item/ItemStack;)V", remap = false*//*?} else {*/"renderTooltip"/*?}*/, at = @At("HEAD"), cancellable = true)
    private void renderTooltipInternal(Font font, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner clientTooltipPositioner, ResourceLocation location/*? if neoforge {*//*, ItemStack tooltipStack*//*?}*/, CallbackInfo ci) {
        if (!LegacyOptions.legacyItemTooltips.get()) return;
        ci.cancel();
        LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderTooltipInternal(self(), font, list, i, j, clientTooltipPositioner/*? if neoforge || forge {*//*, tooltipStack*//*?}*/));
    }

    @WrapOperation(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;submitItem(Lnet/minecraft/client/gui/render/state/GuiItemRenderState;)V"))
    private void renderItem(GuiRenderState instance, GuiItemRenderState arg, Operation<Void> original, @Nullable LivingEntity livingEntity, @Nullable Level level, ItemStack itemStack, int i, int j) {
        float g = (float) itemStack.getPopTime() - FactoryAPIClient.getGamePartialTick(true);
        if (g > 0.0F && (minecraft.screen == null || minecraft.screen instanceof LegacyMenuAccess<?> m && m.allowItemPopping())) {
            float h = 1.0F + g / 5.0F;
            self().pose().pushMatrix();
            self().pose().translate((float) (i + 8), (float) (j + 12));
            self().pose().scale(1.0F / h, (h + 1.0F) / 2.0F);
            self().pose().translate((float) (-(i + 8)), (float) (-(j + 12)));
            original.call(instance, arg);
            self().pose().popMatrix();
            if (minecraft.player != null && !minecraft.player.getInventory().getNonEquipmentItems().contains(itemStack))
                itemStack.setPopTime(itemStack.getPopTime() - 1);
        } else original.call(instance, arg);
    }

    @Redirect(method = "renderItemCount", at = @At(value = "INVOKE", target = "Ljava/lang/String;valueOf(I)Ljava/lang/String;"))
    private String renderItemDecorationsTail(int i, Font font, ItemStack itemStack) {
        return i > itemStack.getMaxStackSize() && LegacyOptions.legacyOverstackedItems.get() ? I18n.get("legacy.container.overstack", itemStack.getMaxStackSize()) : String.valueOf(i);
    }

    @ModifyReceiver(method = {"submitEntityRenderState", "submitBookModelRenderState", "submitBannerPatternRenderState"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;submitPicturesInPictureState(Lnet/minecraft/client/gui/render/state/pip/PictureInPictureRenderState;)V"))
    private GuiRenderState submitEntityRenderState(GuiRenderState instance, PictureInPictureRenderState pictureInPictureRenderState) {
        MutablePIPRenderState.of(pictureInPictureRenderState).setPose(self().pose());
        return instance;
    }

    @WrapOperation(method = {"renderItemBar", "renderItemCooldown"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lcom/mojang/blaze3d/pipeline/RenderPipeline;IIIII)V"))
    private void fill(GuiGraphics instance, RenderPipeline j, int i, int renderPipeline, int k, int l, int m, Operation<Void> original) {
        float opacity = LegacyGuiItemRenderer.OPACITY;
        if (opacity != 1f) m = ARGB.color((int) (ARGB.alpha(m) * opacity), ARGB.transparent(m));
        original.call(instance, j, i, renderPipeline, k, l, m);
    }

    @WrapOperation(method = "renderItemCount", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"))
    private void drawString(GuiGraphics instance, Font arg, String string, int i, int j, int k, boolean bl, Operation<Void> original) {
        float opacity = LegacyGuiItemRenderer.OPACITY;
        if (opacity != 1f) k = ARGB.color((int) (ARGB.alpha(k) * opacity), ARGB.transparent(k));
        original.call(instance, arg, string, i, j, k, bl);
    }
}
