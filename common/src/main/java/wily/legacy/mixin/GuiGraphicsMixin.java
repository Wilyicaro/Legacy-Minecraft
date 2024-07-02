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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
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
    @Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawManaged(Ljava/lang/Runnable;)V", shift = At.Shift.AFTER))
    private void renderTooltipInternal(Font font, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner clientTooltipPositioner, CallbackInfo ci){
        pose.pushPose();
        pose.scale(ScreenUtil.getTextScale(), ScreenUtil.getTextScale(),1.0f);
    }
    @Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void renderTooltipInternalPop(Font font, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner clientTooltipPositioner, CallbackInfo ci){
        pose.popPose();
    }
    @Redirect(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"))
    private Vector2ic renderTooltipInternal(ClientTooltipPositioner instance, int guiWidth, int guiHeight, int x, int y, int width, int height){
        return instance.positionTooltip(guiWidth(),guiHeight,x,y, (int) (width * ScreenUtil.getTextScale()), (int) (height * ScreenUtil.getTextScale()));
    }
    @Redirect(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;renderImage(Lnet/minecraft/client/gui/Font;IILnet/minecraft/client/gui/GuiGraphics;)V"))
    private void renderTooltipInternal(ClientTooltipComponent instance, Font font, int i, int j, GuiGraphics guiGraphics){
        instance.renderImage(font, (int)(i / ScreenUtil.getTextScale()),(int)(j / ScreenUtil.getTextScale()),guiGraphics);
    }
    @Redirect(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;renderText(Lnet/minecraft/client/gui/Font;IILorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"))
    private void renderTooltipInternal(ClientTooltipComponent instance, Font font, int i, int j, Matrix4f matrix4f, MultiBufferSource.BufferSource bufferSource){
        instance.renderText(font, (int)(i / ScreenUtil.getTextScale()),(int)(j / ScreenUtil.getTextScale()),matrix4f,bufferSource);
    }
    @Redirect(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;getHeight()I", ordinal = 1))
    private int renderTooltipInternal(ClientTooltipComponent instance){
        return Math.round(instance.getHeight() * ScreenUtil.getTextScale());
    }
    @Redirect(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;getHeight()I", ordinal = 2))
    private int renderTooltipInternalImageHeight(ClientTooltipComponent instance){
        return Math.round(instance.getHeight() * ScreenUtil.getTextScale());
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
}
