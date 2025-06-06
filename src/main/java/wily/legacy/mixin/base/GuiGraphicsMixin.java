package wily.legacy.mixin.base;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
import wily.legacy.util.ScreenUtil;

import java.util.List;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Shadow @Final private PoseStack pose;

    @Shadow public abstract PoseStack pose();

    @Shadow public abstract int guiWidth();

    @Shadow @Final private Minecraft minecraft;

    @Shadow @Final private MultiBufferSource.BufferSource bufferSource;

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
    @Inject(method = "renderTooltipInternal", at = @At("HEAD"), cancellable = true)
    private void renderTooltipInternal(Font font, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner clientTooltipPositioner,/*? if >=1.21.2 {*/ /*ResourceLocation location,*//*?}*/ CallbackInfo ci){
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
            l+= tooltipComponent.getHeight(/*? if >=1.21.2 {*//*font*//*?}*/);
        }

        Vector2ic vector2ic = clientTooltipPositioner.positionTooltip(this.guiWidth(), this.guiHeight(), i, j, (int) (k*ScreenUtil.getTextScale()), (int) (l*ScreenUtil.getTextScale()));
        int p = vector2ic.x();
        int q = vector2ic.y();
        this.pose.pushPose();
        this.pose.translate(0, 0, 800.0F);
        if (p == (int)Legacy4JClient.controllerManager.getPointerX() && q == (int)Legacy4JClient.controllerManager.getPointerY()) this.pose.translate(Legacy4JClient.controllerManager.getPointerX() - i, Legacy4JClient.controllerManager.getPointerY() - j,0.0f);
        ScreenUtil.renderPointerPanel(self(),p - Math.round(5 * ScreenUtil.getTextScale()),q - Math.round(9 * ScreenUtil.getTextScale()),Math.round((k + 11) *  ScreenUtil.getTextScale()),Math.round((l + 13) * ScreenUtil.getTextScale()));
        this.pose.translate(p, q, 0.0F);
        FactoryScreenUtil.disableDepthTest();
        this.pose.scale(ScreenUtil.getTextScale(), ScreenUtil.getTextScale(),1.0f);
        int s = 0;

        int t;
        ClientTooltipComponent tooltipComponent;
        for(t = 0; t < list.size(); ++t) {
            tooltipComponent = list.get(t);
            tooltipComponent.renderText(font, 0, s, this.pose.last().pose(), this.bufferSource);
            s += tooltipComponent.getHeight(/*? if >=1.21.2 {*//*font*//*?}*/);
        }

        s = 0;

        for(t = 0; t < list.size(); ++t) {
            tooltipComponent = list.get(t);
            tooltipComponent.renderImage(font, 0, s,/*? if >=1.21.2 {*//*k, l,*//*?}*/ self());
            s += tooltipComponent.getHeight(/*? if >=1.21.2 {*//*font*//*?}*/);
        }
        FactoryScreenUtil.enableDepthTest();
        this.pose.popPose();
    }

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void renderItem(LivingEntity livingEntity, Level level, ItemStack itemStack, int i, int j, int k, int l, CallbackInfo ci){
        float g = (float)itemStack.getPopTime() - FactoryAPIClient.getGamePartialTick(true);
        if (g > 0.0F && (minecraft.screen == null || minecraft.screen instanceof LegacyMenuAccess<?> m && m.allowItemPopping())) {
            float h = 1.0F + g / 5.0F;
            pose().translate((float)(i + 8), (float)(j + 12), 0.0F);
            pose().scale(1.0F / h, (h + 1.0F) / 2.0F, 1.0F);
            pose().translate((float)(-(i + 8)), (float)(-(j + 12)), 0.0F);
            if (minecraft.player != null  && !minecraft.player.getInventory()./*? if <1.21.5 {*/items/*?} else {*//*getNonEquipmentItems()*//*?}*/.contains(itemStack)) itemStack.setPopTime(itemStack.getPopTime() - 1);
        }
    }
    @ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0), index = 2)
    private float renderTooltipInternal(float z){
        return 800;
    }

    @Redirect(method = /*? if <1.21.2 {*/"renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"/*?} else {*//*"renderItemCount"*//*?}*/, at = @At(value = "INVOKE", target = "Ljava/lang/String;valueOf(I)Ljava/lang/String;"))
    private String renderItemDecorationsTail(int i, Font font, ItemStack itemStack){
        return i > itemStack.getMaxStackSize() && LegacyOptions.legacyOverstackedItems.get() ? I18n.get("legacy.container.overstack",itemStack.getMaxStackSize()) : String.valueOf(i);
    }
}
