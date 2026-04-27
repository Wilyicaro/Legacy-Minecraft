package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
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

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    //? if forge || neoforge {
    /*@Shadow(remap = false) private ItemStack tooltipStack;
     *///?}

    @Unique
    GuiGraphicsExtractor self() {
        return (GuiGraphicsExtractor) (Object) this;
    }

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
    private void renderItemDecorationsHead(Font font, ItemStack itemStack, int i, int j, String string, CallbackInfo ci) {
        LegacyFontUtil.disableLegacyFont();
    }

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void renderItemDecorationsTail(Font font, ItemStack itemStack, int i, int j, String string, CallbackInfo ci) {
        LegacyFontUtil.enableLegacyFont();
    }

    @Inject(method = /*? if (forge || neoforge) && <26.1 {*/ /*"renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Lnet/minecraft/world/item/ItemStack;)V", remap = false*//*?} else if forge || neoforge {*/ /*"tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Lnet/minecraft/world/item/ItemStack;)V" *//*?} else {*/"tooltip"/*?}*/, at = @At("HEAD"), cancellable = true)
    private void renderTooltipInternal(Font font, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner clientTooltipPositioner, Identifier location/*? if forge || neoforge {*//*, ItemStack tooltipStack*//*?}*/, CallbackInfo ci) {
        if (!LegacyOptions.legacyItemTooltips.get()) return;
        ci.cancel();
        LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderTooltipInternal(self(), font, list, i, j, clientTooltipPositioner/*? if forge || neoforge {*//*, tooltipStack*//*?}*/));
    }

    @WrapOperation(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addItem(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;)V"))
    private void renderItem(GuiRenderState instance, GuiItemRenderState arg, Operation<Void> original, @Nullable LivingEntity livingEntity, @Nullable Level level, ItemStack itemStack, int i, int j, int k) {
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

    @Redirect(method = "itemCount", at = @At(value = "INVOKE", target = "Ljava/lang/String;valueOf(I)Ljava/lang/String;"))
    private String renderItemDecorationsTail(int i, Font font, ItemStack itemStack) {
        return i > itemStack.getMaxStackSize() && LegacyOptions.legacyOverstackedItems.get() ? I18n.get("legacy.container.overstack", itemStack.getMaxStackSize()) : String.valueOf(i);
    }

    @ModifyReceiver(method = {"entity", "book", "bannerPattern"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addPicturesInPictureState(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;)V"))
    private GuiRenderState submitEntityRenderState(GuiRenderState instance, PictureInPictureRenderState pictureInPictureRenderState) {
        MutablePIPRenderState.of(pictureInPictureRenderState).setPose(self().pose());
        return instance;
    }

    @WrapOperation(method = {"itemBar", "itemCooldown"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(Lcom/mojang/blaze3d/pipeline/RenderPipeline;IIIII)V"))
    private void fill(GuiGraphicsExtractor instance, RenderPipeline j, int i, int renderPipeline, int k, int l, int m, Operation<Void> original) {
        float opacity = LegacyGuiItemRenderer.OPACITY;
        if (opacity != 1f) m = ARGB.color((int) (ARGB.alpha(m) * opacity), ARGB.transparent(m));
        original.call(instance, j, i, renderPipeline, k, l, m);
    }

    @WrapOperation(method = "itemCount", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"))
    private void drawString(GuiGraphicsExtractor instance, Font arg, String string, int i, int j, int k, boolean bl, Operation<Void> original) {
        float opacity = LegacyGuiItemRenderer.OPACITY;
        if (opacity != 1f) k = ARGB.color((int) (ARGB.alpha(k) * opacity), ARGB.transparent(k));
        original.call(instance, arg, string, i, j, k, bl);
    }

    @Inject(method = "itemBar", at = @At("RETURN"))
    private void drawString(ItemStack itemStack, int i, int j, CallbackInfo ci) {
        LegacyRenderUtil.renderPotionLevel(self(), i, j, itemStack);
    }
}
