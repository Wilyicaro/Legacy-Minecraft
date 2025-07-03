package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
//? forge {
/*import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderTooltipEvent;
*///?} else if neoforge {
/*import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
*///?}
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.List;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Shadow public abstract int guiWidth();

    @Shadow @Final private Minecraft minecraft;

    @Shadow public abstract int guiHeight();
    //? if forge || neoforge {
    /*@Shadow(remap = false) private ItemStack tooltipStack;
    *///?}

    @Unique
    GuiGraphics self(){
        return (GuiGraphics) (Object) this;
    }

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
    private void renderItemDecorationsHead(Font font, ItemStack itemStack, int i, int j, String string, CallbackInfo ci){
        Legacy4JClient.legacyFont = false;
    }
    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void renderItemDecorationsTail(Font font, ItemStack itemStack, int i, int j, String string, CallbackInfo ci){
        Legacy4JClient.legacyFont = true;
    }
    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void renderTooltipInternal(Font font, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner clientTooltipPositioner,/*? if >=1.21.2 {*/ ResourceLocation location,/*?}*/ CallbackInfo ci){
        if (!LegacyOptions.legacyItemTooltips.get()) return;
        ci.cancel();
        if (list.isEmpty()) return;
        //? if forge {
        /*RenderTooltipEvent.Pre preEvent = ForgeHooksClient.onRenderTooltipPre(this.tooltipStack, self(), i, j, this.guiWidth(), this.guiHeight(), list, font, clientTooltipPositioner);
         *///?} else if neoforge {
        /*RenderTooltipEvent.Pre preEvent = ClientHooks.onRenderTooltipPre(this.tooltipStack, self(), i, j, this.guiWidth(), this.guiHeight(), list, font, clientTooltipPositioner);
         *///?}
        //? if neoforge || forge {
        /*if (preEvent.isCanceled()) {
            return;
        }
        font = preEvent.getFont();
        i = preEvent.getX();
        j = preEvent.getY();
        *///?}
        int k = 0;
        int l = 0;

        for (ClientTooltipComponent tooltipComponent : list) {
            k = Math.max(tooltipComponent.getWidth(font),k);
            l+= tooltipComponent.getHeight(/*? if >=1.21.2 {*/font/*?}*/);
        }

        Vector2ic vector2ic = clientTooltipPositioner.positionTooltip(this.guiWidth(), this.guiHeight(), i, j, (int) (k* LegacyRenderUtil.getTextScale()), (int) (l* LegacyRenderUtil.getTextScale()));
        int p = vector2ic.x();
        int q = vector2ic.y();
        self().pose().pushMatrix();
        if (p == (int)Legacy4JClient.controllerManager.getPointerX() && q == (int)Legacy4JClient.controllerManager.getPointerY()) self().pose().translate((float) (Legacy4JClient.controllerManager.getPointerX() - i), (float) (Legacy4JClient.controllerManager.getPointerY() - j));
        LegacyRenderUtil.renderPointerPanel(self(),p - Math.round(5 * LegacyRenderUtil.getTextScale()),q - Math.round(9 * LegacyRenderUtil.getTextScale()),Math.round((k + 11) *  LegacyRenderUtil.getTextScale()),Math.round((l + 13) * LegacyRenderUtil.getTextScale()));
        self().pose().translate(p, q);
        FactoryScreenUtil.disableDepthTest();
        self().pose().scale(LegacyRenderUtil.getTextScale(), LegacyRenderUtil.getTextScale());
        int s = 0;

        int t;
        ClientTooltipComponent tooltipComponent;
        for(t = 0; t < list.size(); ++t) {
            tooltipComponent = list.get(t);
            tooltipComponent.renderText(self(), font, 0, s);
            s += tooltipComponent.getHeight(/*? if >=1.21.2 {*/font/*?}*/);
        }

        s = 0;

        for(t = 0; t < list.size(); ++t) {
            tooltipComponent = list.get(t);
            tooltipComponent.renderImage(font, 0, s,/*? if >=1.21.2 {*/k, l,/*?}*/ self());
            s += tooltipComponent.getHeight(/*? if >=1.21.2 {*/font/*?}*/);
        }
        FactoryScreenUtil.enableDepthTest();
        self().pose().popMatrix();
    }

    @WrapOperation(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;submitItem(Lnet/minecraft/client/gui/render/state/GuiItemRenderState;)V"))
    private void renderItem(GuiRenderState instance, GuiItemRenderState arg, Operation<Void> original, @Nullable LivingEntity livingEntity, @Nullable Level level, ItemStack itemStack, int i, int j){
        float g = (float)itemStack.getPopTime() - FactoryAPIClient.getGamePartialTick(true);
        if (g > 0.0F && (minecraft.screen == null || minecraft.screen instanceof LegacyMenuAccess<?> m && m.allowItemPopping())) {
            float h = 1.0F + g / 5.0F;
            self().pose().pushMatrix();
            self().pose().translate((float)(i + 8), (float)(j + 12));
            self().pose().scale(1.0F / h, (h + 1.0F) / 2.0F);
            self().pose().translate((float)(-(i + 8)), (float)(-(j + 12)));
            original.call(instance, arg);
            self().pose().popMatrix();
            if (minecraft.player != null  && !minecraft.player.getInventory().getNonEquipmentItems().contains(itemStack)) itemStack.setPopTime(itemStack.getPopTime() - 1);
        } else original.call(instance, arg);
    }

    @Redirect(method = /*? if <1.21.2 {*//*"renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"*//*?} else {*/"renderItemCount"/*?}*/, at = @At(value = "INVOKE", target = "Ljava/lang/String;valueOf(I)Ljava/lang/String;"))
    private String renderItemDecorationsTail(int i, Font font, ItemStack itemStack){
        return i > itemStack.getMaxStackSize() && LegacyOptions.legacyOverstackedItems.get() ? I18n.get("legacy.container.overstack",itemStack.getMaxStackSize()) : String.valueOf(i);
    }
}
