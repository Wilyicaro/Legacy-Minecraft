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
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.client.screen.LegacyIconHolder;

import java.util.function.Consumer;

public class ScreenUtil {
    public static final ResourceLocation GUI_ATLAS = new ResourceLocation("textures/atlas/gui.png");
    private static final Minecraft mc = Minecraft.getInstance();
    public static long lastHotbarSelectionChange = -1;
    protected static LogoRenderer logoRenderer = new LogoRenderer(false);
    public static LegacyIconHolder iconHolderRenderer = new LegacyIconHolder();
    public static final ResourceLocation MINECRAFT = new ResourceLocation(Legacy4J.MOD_ID, "title/minecraft");
    public static final ResourceLocation PANORAMA_DAY = new ResourceLocation(Legacy4J.MOD_ID, "textures/gui/title/panorama_day.png");
    public static final ResourceLocation PANORAMA_NIGHT = new ResourceLocation(Legacy4J.MOD_ID, "textures/gui/title/panorama_night.png");
    public static final ResourceLocation SAVE_CHEST = new ResourceLocation(Legacy4J.MOD_ID,"hud/save_chest");
    public static final ResourceLocation SAVE_ARROW = new ResourceLocation(Legacy4J.MOD_ID,"hud/save_arrow");
    public static final ResourceLocation LOADING_BLOCK = new ResourceLocation(Legacy4J.MOD_ID,"widget/loading_block");
    public static final ResourceLocation POINTER_PANEL = new ResourceLocation(Legacy4J.MOD_ID,"tiles/pointer_panel");
    public static final ResourceLocation PANEL = new ResourceLocation(Legacy4J.MOD_ID,"tiles/panel");
    public static final ResourceLocation PANEL_RECESS = new ResourceLocation(Legacy4J.MOD_ID,"tiles/panel_recess");
    public static final ResourceLocation PANEL_TRANSLUCENT_RECESS = new ResourceLocation(Legacy4J.MOD_ID,"tiles/panel_translucent_recess");
    public static final ResourceLocation ENTITY_PANEL = new ResourceLocation(Legacy4J.MOD_ID,"tiles/entity_panel");
    public static final ResourceLocation SQUARE_RECESSED_PANEL = new ResourceLocation(Legacy4J.MOD_ID,"tiles/square_recessed_panel");
    public static final ResourceLocation SQUARE_ENTITY_PANEL = new ResourceLocation(Legacy4J.MOD_ID,"tiles/square_entity_panel");
    public static void renderPointerPanel(GuiGraphics graphics, int x, int y, int width, int height){
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        graphics.blitSprite(POINTER_PANEL,x,y,width,height);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    public static void renderPanel(GuiGraphics graphics, int x, int y, int width, int height, float dp){
      renderTiles(PANEL,graphics,x,y,width,height,dp);
    }
    public static void renderPanelRecess(GuiGraphics graphics, int x, int y, int width, int height, float dp){
        renderTiles(PANEL_RECESS,graphics,x,y,width,height,dp);
    }
    public static void renderPanelTranslucentRecess(GuiGraphics graphics, int x, int y, int width, int height, float dp){
        RenderSystem.enableBlend();
        renderTiles(PANEL_TRANSLUCENT_RECESS,graphics,x,y,width,height,dp);
        RenderSystem.disableBlend();
    }
    public static void renderEntityPanel(GuiGraphics graphics, int x, int y, int width, int height, float dp){
        renderTiles(ENTITY_PANEL,graphics,x,y,width,height,dp);
    }
    public static void renderSquareEntityPanel(GuiGraphics graphics, int x, int y, int width, int height, float dp){
        renderTiles(SQUARE_ENTITY_PANEL,graphics,x,y,width,height,dp);
    }
    public static void renderSquareRecessedPanel(GuiGraphics graphics, int x, int y, int width, int height, float dp){
        renderTiles(SQUARE_RECESSED_PANEL,graphics,x,y,width,height,dp);
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
        graphics.blitSprite(SAVE_CHEST,x * 2,y * 2,48,48);
        graphics.pose().popPose();
        graphics.pose().pushPose();
        double heightAnim = (Util.getMillis() / 50D) % 11;
        graphics.pose().translate(x + 5.5,y - 8 - (heightAnim > 5 ? 10 - heightAnim : heightAnim),0);
        graphics.blitSprite(SAVE_ARROW,0,0,13,16);
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
    public static void renderDefaultBackground(GuiGraphics guiGraphics, boolean loading, boolean title){
        if (mc.level == null || loading)
            renderPanoramaBackground(guiGraphics, loading && getActualLevelNight());
        else mc.screen.renderTransparentBackground(guiGraphics);
        if (title) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites().textureAtlas.texturesByName.get(MINECRAFT);
            if (sprite == null)
                logoRenderer.renderLogo(guiGraphics, mc.screen == null ? 0 : mc.screen.width, 1.0F);
            else try (SpriteContents contents = sprite.contents()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate((guiGraphics.guiWidth() - 285.5f) / 2, 30,0);
                guiGraphics.pose().scale(0.5f,0.5f,0.5f);
                    guiGraphics.blitSprite(MINECRAFT,(guiGraphics.guiWidth() - contents.width() / 2) / 2, 30, 571,138);
                guiGraphics.pose().popPose();
            }

        }
    }
    public static void renderPanoramaBackground(GuiGraphics guiGraphics, boolean isNight){
        RenderSystem.depthMask(false);
        Minecraft.getInstance().getTextureManager().getTexture(isNight ? PANORAMA_NIGHT : PANORAMA_DAY).setFilter(true, false);
        guiGraphics.blit(isNight ? PANORAMA_NIGHT : PANORAMA_DAY, 0, 0, mc.options.panoramaSpeed().get().floatValue() * Util.getMillis() / 66.32f, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), guiGraphics.guiHeight() * 820/144, guiGraphics.guiHeight());
        RenderSystem.depthMask(true);
    }
    public static void drawOutlinedString(GuiGraphics graphics, Font font, Component component, int x, int y, int color, int outlineColor, float outline) {
        drawStringOutline(graphics,font,component,x,y,outlineColor,outline);
        graphics.drawString(font,component, x, y, color,false);

    }
    public static void drawStringOutline(GuiGraphics graphics, Font font, Component component, int x, int y, int outlineColor, float outline) {
        outline/=2;
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
    public static void applyHUDScale(GuiGraphics graphics, Consumer<Integer> applyWidth, Consumer<Integer> applyHeight){
        graphics.pose().scale(3f / getHUDScale(), 3f / getHUDScale() ,3f / getHUDScale());
        applyHeight.accept((int) (mc.getWindow().getGuiScaledHeight() * getHUDScale()/3));
        applyWidth.accept((int) (mc.getWindow().getGuiScaledWidth() * getHUDScale()/3));
    }
    public static void resetHUDScale(GuiGraphics graphics, Consumer<Integer> applyWidth, Consumer<Integer> applyHeight){
        graphics.pose().scale(getHUDScale()/3f,getHUDScale()/3f,getHUDScale()/3f);
        applyHeight.accept(mc.getWindow().getGuiScaledHeight());
        applyWidth.accept(mc.getWindow().getGuiScaledWidth());
    }
    public static boolean hasClassicCrafting(){
        return getLegacyOptions().classicCrafting().get();
    }
    public static float getHUDScale(){
        return Math.max(1.5f,4 - getLegacyOptions().hudScale().get());
    }
    public static float getHUDSize(){
        return 3f / ScreenUtil.getHUDScale()* (mc.gameMode.canHurtPlayer() ? 68 : 41);
    }
    public static double getHUDDistance(){
        return -getLegacyOptions().hudDistance().value*(22.5D + (getLegacyOptions().inGameTooltips().get() ? 17.5D : 0));
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
        return (getLegacyOptions().forceYellowText().get() || hasProgrammerArt()) && !forceWhite ? 0xFFFF00 : 0xFFFFFF;
    }
    public static int getDefaultTextColor(){
        return getDefaultTextColor(false);
    }
    public static boolean hasProgrammerArt(){
        return mc.getResourcePackRepository().getSelectedPacks().stream().anyMatch(p->p.getId().equals("programmer_art"));
    }
    public static void playSimpleUISound(SoundEvent sound, float grave, float volume){
        playSimpleUISound(sound,grave,volume,false);
    }
    public static void playSimpleUISound(SoundEvent sound, float grave, float volume,boolean pauseRepeatedSounds){
        if (pauseRepeatedSounds) mc.getSoundManager().stop(sound.getLocation(), SoundSource.MASTER);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, grave, volume));
    }
    public static void playSimpleUISound(SoundEvent sound, float grave){
        playSimpleUISound(sound,grave,false);
    }
    public static void playSimpleUISound(SoundEvent sound, float grave, boolean pauseRepeatedSounds){
        if (pauseRepeatedSounds)mc.getSoundManager().stop(sound.getLocation(), SoundSource.MASTER);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, grave));
    }
    public static void addTip(Entity entity){
        if (hasTip(entity.getType())) LegacyTipManager.tips.add(new LegacyTip(entity.getType().getDescription(), ScreenUtil.getTip(entity.getType())));
        else if (entity.getPickResult() != null && !entity.getPickResult().isEmpty() && hasTip(entity.getPickResult())) addTip(entity.getPickResult());
    }
    public static void addTip(EntityType<?> entityType){
        if (hasTip(entityType)) LegacyTipManager.tips.add(new LegacyTip(entityType.getDescription(), ScreenUtil.getTip(entityType)));
    }
    public static void addCustomTip(Component title, Component tip, ItemStack stack, long time){
        LegacyTipManager.tips.add((title.getString().isEmpty() && tip.getString().isEmpty() && !stack.isEmpty() ?  new LegacyTip(stack) : new LegacyTip(title,tip).itemStack(stack)).disappearTime(time));
    }
    public static void addTip(ItemStack stack){
        if (hasTip(stack)) LegacyTipManager.tips.add(new LegacyTip(stack));
    }
    public static Component getTip(ItemStack item){
        return hasValidTipOverride(item) ? LegacyTipOverride.getOverride(item) : Component.translatable(getTipId(item));
    }
    public static Component getTip(EntityType<?> type){
        return hasValidTipOverride(type) ? LegacyTipOverride.getOverride(type) : Component.translatable(getTipId(type));
    }
    public static boolean hasTip(ItemStack item){
        return hasTip(getTipId(item)) || hasValidTipOverride(item);
    }
    public static boolean hasValidTipOverride(ItemStack item){
        return !LegacyTipOverride.getOverride(item).getString().isEmpty() && hasTip(((TranslatableContents)LegacyTipOverride.getOverride(item).getContents()).getKey());
    }
    public static boolean hasValidTipOverride(EntityType<?> type){
        return !LegacyTipOverride.getOverride(type).getString().isEmpty() && hasTip(((TranslatableContents)LegacyTipOverride.getOverride(type).getContents()).getKey());
    }
    public static boolean hasTip(String s){
        return Language.getInstance().has(s);
    }
    public static boolean hasTip(EntityType<?> s){
        return hasTip(getTipId(s)) || hasValidTipOverride(s);
    }
    public static String getTipId(ItemStack item){
        return item.getDescriptionId() + ".tip";
    }
    public static String getTipId(EntityType<?> item){
        return item.getDescriptionId() + ".tip";
    }
    public static Component getTip(ResourceLocation location){
        return Component.translatable(location.toLanguageKey() +".tip");
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
                graphics.blitSprite(LOADING_BLOCK, x+ (i <= 2 ? i : i >= 4 ? i == 7 ? 0 : 6 - i : 2) * 27, y + (i <= 2 ? 0 : i == 3 || i == 7 ? 1 : 2)* 27, 21, 21);
            }
        }
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f,1.0f);
    }
    public static void renderScrollingString(GuiGraphics guiGraphics, Font font, Component component, int j, int k, int l, int m, int n, boolean shadow) {
        renderScrollingString(guiGraphics,font,component.getVisualOrderText(),j,k,l,m,n,shadow);
    }
    public static void renderScrollingString(GuiGraphics guiGraphics, Font font, FormattedCharSequence charSequence, int j, int k, int l, int m, int n, boolean shadow) {
        int o = font.width(charSequence);
        int p = (k + m - font.lineHeight) / 2 + 1;
        int q = l - j;
        if (o > q) {
            int r = o - q;
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
        guiGraphics.pose().mulPoseMatrix(new Matrix4f().scaling(size / h, size / h, -size / h));
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

    public static float getTextScale(){
        return getLegacyOptions().legacyItemTooltips().get() ? Math.max(0.75f,Math.min((float) Math.sqrt(1280f / mc.getWindow().getScreenWidth() * 720f / mc.getWindow().getScreenHeight()),1.5f)) : 1.0f;
    }


}
