package wily.legacy.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOption;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    private static final Map<TextureAtlasSprite, Map<String,ResourceLocation>> spriteTilesCache = new ConcurrentHashMap<>();
    @Shadow @Final private PoseStack pose;

    @Shadow public abstract PoseStack pose();

    @Shadow public abstract int guiWidth();

    @Shadow @Final private Minecraft minecraft;

    @Shadow public abstract void blit(ResourceLocation resourceLocation, int i, int j, float f, float g, int k, int l, int m, int n);

    @Shadow public abstract void blit(ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n);

    @Shadow @Final private MultiBufferSource.BufferSource bufferSource;

    @Shadow public abstract int guiHeight();

    GuiGraphics self(){
        return (GuiGraphics) (Object) this;
    }

    @ModifyVariable(method = "enableScissor", at = @At(value = "HEAD"), index = 1, argsOnly = true)
    private int enableScissor(int value){
        return value + Math.round(LegacyTipManager.getTipXDiff());
    }
    @ModifyVariable(method = "enableScissor", at = @At(value = "HEAD"), index = 3, argsOnly = true)
    private int enableScissorXW(int value){
        return value + Math.round(LegacyTipManager.getTipXDiff());
    }

    @Inject(method = "bufferSource", at = @At("HEAD"), cancellable = true)
    private void bufferSource(CallbackInfoReturnable<MultiBufferSource.BufferSource> cir){
        if (Legacy4JClient.guiBufferSourceOverride != null) cir.setReturnValue(Legacy4JClient.guiBufferSourceOverride);
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
    private void renderTooltipInternal(Font font, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner clientTooltipPositioner, CallbackInfo ci){
        if (!LegacyOption.legacyItemTooltips.get()) return;
        ci.cancel();
        if (list.isEmpty()) return;
        int k = 0;
        int l = 0;

        for (ClientTooltipComponent tooltipComponent : list) {
            k = Math.max(tooltipComponent.getWidth(font),k);
            l+= tooltipComponent.getHeight();
        }

        Vector2ic vector2ic = clientTooltipPositioner.positionTooltip(this.guiWidth(), this.guiHeight(), i, j, (int) (k*ScreenUtil.getTextScale()), (int) (l*ScreenUtil.getTextScale()));
        int p = vector2ic.x();
        int q = vector2ic.y();
        this.pose.pushPose();
        if (p == (int)Legacy4JClient.controllerManager.getPointerX() && q == (int)Legacy4JClient.controllerManager.getPointerY()) this.pose.translate(Legacy4JClient.controllerManager.getPointerX() - i, Legacy4JClient.controllerManager.getPointerY() - j,0.0f);
        ScreenUtil.renderPointerPanel(self(),p - Math.round(5 * ScreenUtil.getTextScale()),q - Math.round(9 * ScreenUtil.getTextScale()),Math.round((k + 11) *  ScreenUtil.getTextScale()),Math.round((l + 13) * ScreenUtil.getTextScale()));
        this.pose.translate(p, q, 400.0F);
        this.pose.scale(ScreenUtil.getTextScale(), ScreenUtil.getTextScale(),1.0f);
        int s = 0;

        int t;
        ClientTooltipComponent tooltipComponent;
        for(t = 0; t < list.size(); ++t) {
            tooltipComponent = list.get(t);
            tooltipComponent.renderText(font, 0, s, this.pose.last().pose(), this.bufferSource);
            s += tooltipComponent.getHeight();
        }

        s = q;

        for(t = 0; t < list.size(); ++t) {
            tooltipComponent = list.get(t);
            tooltipComponent.renderImage(font, 0, s, self());
            s += tooltipComponent.getHeight();
        }

        this.pose.popPose();
    }

    @Inject(method = "blitTiledSprite",at = @At("HEAD"), cancellable = true)
    private void blitTiledSprite(TextureAtlasSprite textureAtlasSprite, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s, CallbackInfo ci) {
        ci.cancel();
        if (l <= 0 || m <= 0 ) {
            return;
        }
        if (p <= 0 || q <= 0) {
            throw new IllegalArgumentException("Tiled sprite texture size must be positive, got " + p + "x" + q);
        }

        ResourceLocation tile = spriteTilesCache.computeIfAbsent(textureAtlasSprite, sp-> new ConcurrentHashMap<>()).computeIfAbsent("tile_" + n + "x" + o + "_" + p + "x" + q,(string)->{
            try {
                TextureAtlas atlas = (TextureAtlas) minecraft.getTextureManager().getTexture(textureAtlasSprite.atlasLocation());
                Optional<ResourceLocation> opt = atlas.texturesByName.entrySet().stream().filter(e-> e.getValue() == textureAtlasSprite).findFirst().map(Map.Entry::getKey);
                if (opt.isPresent()) {
                    NativeImage image = NativeImage.read(minecraft.getResourceManager().getResourceOrThrow(opt.get().withPath("textures/gui/sprites/" +opt.get().getPath() + ".png")).open());
                    int width = (int)Math.ceil(p * image.getWidth() / (double) r);
                    int height = (int)Math.ceil(q * image.getHeight() / (double) s);
                    NativeImage tileImage = new NativeImage(width, height, false);
                    image.copyRect(tileImage,  n * image.getWidth() / r, o * image.getHeight() / s, 0, 0, width, height, false, false);
                    return minecraft.getTextureManager().register("tile", new DynamicTexture(tileImage));
                }
            } catch (IOException e) {
                Legacy4J.LOGGER.warn(e.getMessage());
            }
            return null;
        });
        blit(tile,i,j,Math.min(n,p),Math.min(o,q),l,m,p,q);
    }
    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void renderItem(LivingEntity livingEntity, Level level, ItemStack itemStack, int i, int j, int k, int l, CallbackInfo ci){
        float g = (float)itemStack.getPopTime() - minecraft.getTimer().getGameTimeDeltaTicks();
        if (g > 0.0F && (minecraft.screen == null || minecraft.screen instanceof EffectRenderingInventoryScreen<?>)) {
            float h = 1.0F + g / 5.0F;
            pose().translate((float)(i + 8), (float)(j + 12), 0.0F);
            pose().scale(1.0F / h, (h + 1.0F) / 2.0F, 1.0F);
            pose().translate((float)(-(i + 8)), (float)(-(j + 12)), 0.0F);
            if (minecraft.player != null  && !minecraft.player.getInventory().items.contains(itemStack)) itemStack.setPopTime(itemStack.getPopTime() - 1);
        }
    }
    @ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0), index = 2)
    private float renderTooltipInternal(float z){
        return 800;
    }
}
