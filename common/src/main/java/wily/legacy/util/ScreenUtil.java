package wily.legacy.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.client.screen.LegacyIconHolder;

import java.util.function.Consumer;

import static wily.legacy.Legacy4JClient.isModEnabledOnServer;

public class ScreenUtil {
    public static final ResourceLocation GUI_ATLAS = ResourceLocation.withDefaultNamespace("textures/atlas/gui.png");
    private static final Minecraft mc = Minecraft.getInstance();
    public static long lastHotbarSelectionChange = -1;
    public static long animatedCharacterTime;
    public static long remainingAnimatedCharacterTime;
    protected static LogoRenderer logoRenderer = new LogoRenderer(false);
    public static LegacyIconHolder iconHolderRenderer = new LegacyIconHolder();
    public static final ResourceLocation MINECRAFT = ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID, "textures/gui/title/minecraft.png");
    public static final ResourceLocation PANORAMA_DAY = ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID, "textures/gui/title/panorama_day.png");
    public static final ResourceLocation PANORAMA_NIGHT = ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID, "textures/gui/title/panorama_night.png");
    public static final ResourceLocation MENU_BACKGROUND = ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID, "textures/gui/menu_background.png");

    public static void renderPointerPanel(GuiGraphics graphics, int x, int y, int width, int height){
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        graphics.blitSprite(LegacySprites.POINTER_PANEL,x,y,width,height);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    public static void renderPanelTranslucentRecess(GuiGraphics graphics, int x, int y, int width, int height){
        RenderSystem.enableBlend();
        graphics.blitSprite(LegacySprites.PANEL_TRANSLUCENT_RECESS,x,y,width,height);
        RenderSystem.disableBlend();
    }
    public static void renderTiles(ResourceLocation location,GuiGraphics graphics, int x, int y, int width, int height, float dp){
        mc.getTextureManager().getTexture(GUI_ATLAS).bind();
        GlStateManager._texParameter(3553, 10241, 9729);
        //GlStateManager._texParameter(3553, 10240, 9729);
        graphics.pose().pushPose();
        graphics.pose().translate(x,y,0);
        if (dp != 1.0)
            graphics.pose().scale(1/dp,1/dp,1/dp);
        graphics.blitSprite(location,0,0, (int) (width * dp), (int) (height * dp));
        graphics.pose().popPose();
        GlStateManager._texParameter(3553, 10241, 9728);
    }
    public static void drawAutoSavingIcon(GuiGraphics graphics,int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F,0.5F,1);
        graphics.blitSprite(LegacySprites.SAVE_CHEST,x * 2,y * 2,48,48);
        graphics.pose().popPose();
        graphics.pose().pushPose();
        double heightAnim = (Util.getMillis() / 50D) % 11;
        graphics.pose().translate(x + 5.5,y - 8 - (heightAnim > 5 ? 10 - heightAnim : heightAnim),0);
        graphics.blitSprite(LegacySprites.SAVE_ARROW,0,0,13,16);
        graphics.pose().popPose();
    }
    public static void renderDefaultBackground(GuiGraphics guiGraphics){
        renderDefaultBackground(guiGraphics,false,true);
    }
    public static void renderDefaultBackground(GuiGraphics guiGraphics, boolean title){
        renderDefaultBackground(guiGraphics,false,title);
    }
    public static boolean getActualLevelNight(){
        return (mc.getSingleplayerServer() != null&& mc.getSingleplayerServer().overworld() != null && mc.getSingleplayerServer().overworld().isNight()) || (mc.level!= null && mc.level.isNight());
    }
    public static void renderDefaultBackground(GuiGraphics guiGraphics, boolean forcePanorama, boolean title){
        if (mc.level == null || forcePanorama)
            renderPanoramaBackground(guiGraphics, forcePanorama && getActualLevelNight());
        else mc.screen.renderTransparentBackground(guiGraphics);
        if (title) {
            if (Minecraft.getInstance().getResourceManager().getResource(MINECRAFT).isEmpty())
                logoRenderer.renderLogo(guiGraphics, mc.screen == null ? 0 : mc.screen.width, 1.0F);
            else {
                RenderSystem.enableBlend();
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate((guiGraphics.guiWidth() - 285.5f) / 2, 30,0);
                guiGraphics.pose().scale(0.5f,0.5f,0.5f);
                guiGraphics.blit(MINECRAFT,0, 0,0,0, 571,138,571,138);
                guiGraphics.pose().popPose();
                RenderSystem.disableBlend();
            }

        }
    }
    public static void renderPanoramaBackground(GuiGraphics guiGraphics, boolean isNight){
        RenderSystem.depthMask(false);
        Minecraft.getInstance().getTextureManager().getTexture(isNight ? PANORAMA_NIGHT : PANORAMA_DAY).setFilter(true, false);
        guiGraphics.blit(isNight ? PANORAMA_NIGHT : PANORAMA_DAY, 0, 0, mc.options.panoramaSpeed().get().floatValue() * Util.getMillis() / 66.32f, 1, guiGraphics.guiWidth(), guiGraphics.guiHeight() + 2, guiGraphics.guiHeight() * 820/144, guiGraphics.guiHeight() + 2);
        RenderSystem.depthMask(true);
    }
    public static void drawOutlinedString(GuiGraphics graphics, Font font, Component component, int x, int y, int color, int outlineColor, float outline) {
        drawStringOutline(graphics,font,component,x,y,outlineColor,outline);
        graphics.drawString(font,component, x, y, color,false);

    }
    public static void drawStringOutline(GuiGraphics graphics, Font font, Component component, int x, int y, int outlineColor, float outline) {
        float[] translations = new float[]{0,outline,-outline};
        for (float t : translations) {
            for (float t1 : translations) {
                if (t != 0 || t1 != 0) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(t,t1,0F);
                    graphics.drawString(font, component, x, y, outlineColor, false);
                    graphics.pose().popPose();
                }
            }
        }
    }
    public static boolean isMouseOver(double mouseX, double mouseY, double x, double y, int width, int height){
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    public static void applyHUDScale(GuiGraphics graphics){
        graphics.pose().scale(3f / getHUDScale(), 3f / getHUDScale() ,3f / getHUDScale());
    }
    public static void prepareHUDRender(GuiGraphics graphics){
        graphics.pose().pushPose();
        graphics.setColor(1.0f,1.0f,1.0f,getHUDOpacity());
        graphics.pose().translate(0,getHUDDistance(),0);
        RenderSystem.enableBlend();
    }
    public static void finishHUDRender(GuiGraphics graphics){
        graphics.pose().popPose();
        graphics.setColor(1.0f,1.0f,1.0f,1.0f);
        RenderSystem.disableBlend();
    }
    public static boolean hasClassicCrafting(){
        return !isModEnabledOnServer() || getLegacyOptions().classicCrafting().get();
    }
    public static float getHUDScale(){
        return Math.max(1.5f,4 - getLegacyOptions().hudScale().get());
    }
    public static float getHUDSize(){
        return 6 + 3f / ScreenUtil.getHUDScale() * (35 + (mc.gameMode.canHurtPlayer() ?  Math.max(2,Mth.ceil((Math.max(mc.player.getAttributeValue(Attributes.MAX_HEALTH), Math.max(mc.gui.displayHealth, mc.player.getHealth())) + mc.player.getAbsorptionAmount()) / 20f) + (mc.player.getArmorValue() > 0 ? 1 : 0))* 10 : 0));
    }
    public static double getHUDDistance(){
        return -getLegacyOptions().hudDistance().get()*(22.5D + (getLegacyOptions().inGameTooltips().get() ? 17.5D : 0));
    }
    public static float getHUDOpacity(){
        float f = (Util.getMillis() - lastHotbarSelectionChange)/ 1200f;
        return getInterfaceOpacity() <= 0.8f ?Math.min(0.8f,getInterfaceOpacity() + (1 -getInterfaceOpacity()) * (f >= 3f ? Math.max(4 - f,0) : 1)) : getInterfaceOpacity();
    }
    public static boolean hasTooltipBoxes(){
        return getLegacyOptions().tooltipBoxes().get();
    }
    public static float getInterfaceOpacity(){
        return getLegacyOptions().hudOpacity().get().floatValue();
    }
    public static int getDefaultTextColor(boolean forceWhite){
        return !forceWhite ? CommonColor.HIGHLIGHTED_WIDGET_TEXT.get() : CommonColor.WIDGET_TEXT.get();
    }
    public static int getDefaultTextColor(){
        return getDefaultTextColor(false);
    }
    public static boolean hasProgrammerArt(){
        return mc.getResourcePackRepository().getSelectedPacks().stream().anyMatch(p->p.getId().equals("programmer_art"));
    }
    public static void playSimpleUISound(SoundEvent sound, float volume, float pitch, boolean randomPitch){
        RandomSource source = SoundInstance.createUnseededRandom();
        mc.getSoundManager().play(new SimpleSoundInstance(sound.getLocation(), SoundSource.MASTER, volume,pitch + (randomPitch ? (source.nextFloat() - 0.5f) / 10 : 0), source, false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
    }
    public static void playSimpleUISound(SoundEvent sound, float pitch, boolean randomPitch){
        playSimpleUISound(sound,1.0f, pitch,randomPitch);
    }
    public static void playSimpleUISound(SoundEvent sound, float pitch){
        playSimpleUISound(sound, pitch,false);
    }
    public static void playSimpleUISound(SoundEvent sound, boolean randomPitch){
        playSimpleUISound(sound,1.0f,randomPitch);
    }

    public static LegacyOptions getLegacyOptions(){
        return (LegacyOptions) mc.options;
    }

    public static void drawGenericLoading(GuiGraphics graphics,int x, int y) {
        RenderSystem.enableBlend();
        for (int i = 0; i < 8; i++) {
            int v = (i + 1) * 100;
            int n = (i + 3) * 100;
            float l = (Util.getMillis() / 4f) % 1000;
            float alpha = l >= v - 100  ? (l <= v ? l / v: (n - l) / 200f) : 0;
            if (alpha > 0) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
                graphics.blitSprite(LegacySprites.LOADING_BLOCK, x+ (i <= 2 ? i : i >= 4 ? i == 7 ? 0 : 6 - i : 2) * 27, y + (i <= 2 ? 0 : i == 3 || i == 7 ? 1 : 2)* 27, 21, 21);
            }
        }
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f,1.0f);
    }
    public static void renderScrollingString(GuiGraphics guiGraphics, Font font, Component component, int j, int k, int l, int m, int n, boolean shadow) {
        renderScrollingString(guiGraphics,font,component.getVisualOrderText(),j,k,l,m,n,shadow);
    }
    public static void renderScrollingString(GuiGraphics guiGraphics, Font font, FormattedCharSequence charSequence, int j, int k, int l, int m, int n, boolean shadow) {
        renderScrollingString(guiGraphics,font,charSequence,j,k,l,m,n,shadow,font.width(charSequence));
    }
    public static void renderScrollingString(GuiGraphics guiGraphics, Font font, FormattedCharSequence charSequence, int j, int k, int l, int m, int n, boolean shadow, int stringWidth) {
        int p = (k + m - font.lineHeight) / 2 + 1;
        int q = l - j;
        if (stringWidth > q) {
            int r = stringWidth - q;
            double d = (double) Util.getMillis() / 1000.0;
            double e = Math.max((double)r * 0.5, 3.0);
            double f = Math.sin(1.5707963267948966 * Math.cos(Math.PI * 2 * d / e)) / 2.0 + 0.5;
            double g = Mth.lerp(f, 0.0, r);
            guiGraphics.enableScissor(j, k, l, m);
            guiGraphics.drawString(font, charSequence, j - (int)g, p, n,shadow);
            guiGraphics.disableScissor();
        } else {
            guiGraphics.drawString(font, charSequence, j, p, n,shadow);
        }
    }
    public static void secureTranslucentRender(GuiGraphics graphics, boolean translucent, float alpha, Consumer<Boolean> render){
        if (!translucent){
            render.accept(false);
            return;
        }
        Legacy4JClient.guiBufferSourceOverride = BufferSourceWrapper.translucent(graphics.bufferSource());
        graphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        RenderSystem.enableBlend();
        render.accept(true);
        RenderSystem.disableBlend();
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        Legacy4JClient.guiBufferSourceOverride = null;
    }
    public static boolean isHovering(Slot slot,int leftPos, int topPos,  double d, double e) {
        LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(slot);
        int width = holder.getWidth();
        int height = holder.getHeight();
        double xCorner = holder.getXCorner() + holder.offset.x();
        double yCorner = holder.getYCorner() + holder.offset.y();
        return (d -= leftPos) >= xCorner && d < (xCorner + width) && (e -= topPos) >= yCorner && e < (yCorner + height);
    }
    public static void renderEntity(GuiGraphics guiGraphics, float x, float y, int size, float partialTicks, Vector3f vector3f, Quaternionf quaternionf, @Nullable Quaternionf quaternionf2, Entity entity) {
        renderEntity(guiGraphics,x,y,size,partialTicks,vector3f,quaternionf,quaternionf2,entity,false);
    }
    public static void renderEntity(GuiGraphics guiGraphics, float x, float y, int size, float partialTicks, Vector3f vector3f, Quaternionf quaternionf, @Nullable Quaternionf quaternionf2, Entity entity, boolean forceSize) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 50.0);
        float h = forceSize ? Math.max(1f,Math.max(entity.getBbWidth(), entity.getBbHeight())) : 1;
        guiGraphics.pose().mulPose(new Matrix4f().scaling(size / h, size / h, -size / h));
        guiGraphics.pose().translate(vector3f.x, vector3f.y, vector3f.z);
        guiGraphics.pose().mulPose(quaternionf);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (quaternionf2 != null) {
            quaternionf2.conjugate();
            entityRenderDispatcher.overrideCameraOrientation(quaternionf2);
        }
        entityRenderDispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> entityRenderDispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, partialTicks, guiGraphics.pose(), guiGraphics.bufferSource(), 0xF000F0));
        guiGraphics.flush();
        entityRenderDispatcher.setRenderShadow(true);
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }
    public static int getStandardHeight(){
        return Math.round(mc.getWindow().getHeight() / 180f) * 180;
    }
    public static float getTextScale(){
        return getLegacyOptions().legacyItemTooltips().get() ? Math.max(2/3f,Math.min(720f/getStandardHeight(),4/3f)) : 1.0f;
    }

    public static float getChatSafeZone(){
        return 29 * ScreenUtil.getLegacyOptions().hudDistance().get().floatValue();
    }

    public static Component getDimensionName(ResourceKey<Level> dimension){
        String s = dimension.location().toLanguageKey("dimension");
        return Component.translatable(LegacyTipManager.hasTip(s) ? s : "dimension.minecraft");
    }
}
