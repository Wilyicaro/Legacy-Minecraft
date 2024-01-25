package wily.legacy.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.LegacyTipOverride;
import wily.legacy.client.screen.LegacyIconHolder;

import java.util.function.Consumer;

public class ScreenUtil {
    private static final Minecraft mc = Minecraft.getInstance();
    protected static LogoRenderer logoRenderer = new LogoRenderer(false);
    public static LegacyIconHolder iconHolderRenderer = new LegacyIconHolder();
    public static final ResourceLocation LOADING_BLOCK_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/loading_block");
    public static final ResourceLocation POINTER_PANEL_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"tiles/pointer_panel");
    public static final ResourceLocation PANEL_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"tiles/panel");
    public static final ResourceLocation PANEL_RECESS_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"tiles/panel_recess");
    public static final ResourceLocation ENTITY_PANEL_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"tiles/entity_panel");
    public static final ResourceLocation SQUARE_RECESSED_PANEL = new ResourceLocation(LegacyMinecraft.MOD_ID,"tiles/square_recessed_panel");
    public static final ResourceLocation SQUARE_ENTITY_PANEL = new ResourceLocation(LegacyMinecraft.MOD_ID,"tiles/square_entity_panel");
    public static void renderPointerPanel(GuiGraphics graphics, int x, int y, int width, int height){
        RenderSystem.disableDepthTest();
        renderTiles(POINTER_PANEL_SPRITE,graphics,x,y,width,height,2);
        RenderSystem.enableDepthTest();
    }
    public static void renderPanel(GuiGraphics graphics, int x, int y, int width, int height, float dp){
      renderTiles(PANEL_SPRITE,graphics,x,y,width,height,dp);
    }
    public static void renderPanelRecess(GuiGraphics graphics, int x, int y, int width, int height, float dp){
        renderTiles(PANEL_RECESS_SPRITE,graphics,x,y,width,height,dp);
    }
    public static void renderEntityPanel(GuiGraphics graphics, int x, int y, int width, int height, float dp){
        renderTiles(ENTITY_PANEL_SPRITE,graphics,x,y,width,height,dp);
    }
    public static void renderSquareEntityPanel(GuiGraphics graphics, int x, int y, int width, int height, float dp){
        renderTiles(SQUARE_ENTITY_PANEL,graphics,x,y,width,height,dp);
    }
    public static void renderSquareRecessedPanel(GuiGraphics graphics, int x, int y, int width, int height, float dp){
        renderTiles(SQUARE_RECESSED_PANEL,graphics,x,y,width,height,dp);
    }
    public static void renderTiles(ResourceLocation location,GuiGraphics graphics, int x, int y, int width, int height, float dp){
        //TextureAtlasSprite sprite = mc.getGuiSprites().getSprite(location);
        //mc.getTextureManager().getTexture(sprite.atlasLocation()).setFilter(true,false);
        RenderSystem.enableBlend();
        graphics.pose().pushPose();
        if (dp != 1.0)
            graphics.pose().scale(1/dp,1/dp,1/dp);
        graphics.pose().translate(dp* x,dp * y,0);
        graphics.blitSprite(location,0,0, (int) (width * dp), (int) (height * dp));
        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }
    public static void drawAutoSavingIcon(GuiGraphics graphics,int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F,0.5F,1);
        graphics.blitSprite(new ResourceLocation(LegacyMinecraft.MOD_ID,"hud/save_chest"),x * 2,y * 2,48,48);
        graphics.pose().popPose();
        graphics.pose().pushPose();
        double heightAnim = (Util.getMillis() / 80D) % 11;
        graphics.pose().translate(x + 5.5,y - 8 - (heightAnim > 5 ? 10 - heightAnim : heightAnim),0);
        graphics.blitSprite(new ResourceLocation(LegacyMinecraft.MOD_ID,"hud/save_arrow"),0,0,13,16);
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
        if (title)
            logoRenderer.renderLogo(guiGraphics,mc.screen == null ?  0: mc.screen.width,1.0F);
    }
    public static void renderPanoramaBackground(GuiGraphics guiGraphics, boolean isNight){
        RenderSystem.depthMask(false);
        ResourceLocation panorama = new ResourceLocation(LegacyMinecraft.MOD_ID, "textures/gui/title/panorama_" + (isNight ? "night" : "day") + ".png");
        Minecraft.getInstance().getTextureManager().getTexture(panorama).setFilter(true, false);
        guiGraphics.blit(panorama, 0, 0, mc.options.panoramaSpeed().get().floatValue() * Util.getMillis() / 66.32f, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), guiGraphics.guiHeight() * 820/144, guiGraphics.guiHeight());
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
    public static boolean isMouseOver(double x, double y, int width, int height,double mouseX, double mouseY){
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    public static void applyHUDScale(GuiGraphics graphics, Consumer<Integer> applyWidth, Consumer<Integer> applyHeight){
        graphics.pose().scale(1.5f,1.5f,1.5f);
        applyHeight.accept(mc.getWindow().getGuiScaledHeight() * 2/3);
        applyWidth.accept(mc.getWindow().getGuiScaledWidth() * 2/3);
    }
    public static void resetHUDScale(GuiGraphics graphics, Consumer<Integer> applyWidth, Consumer<Integer> applyHeight){
        graphics.pose().scale(2/3f,2/3f,2/3f);
        applyHeight.accept(mc.getWindow().getGuiScaledHeight());
        applyWidth.accept(mc.getWindow().getGuiScaledWidth());
    }
    public static double getHUDDistance(){
        return -((LegacyOptions)mc.options).hudDistance().value*(22.5D + (((LegacyOptions)mc.options).inGameTooltips().get() ? 17.5D : 0));
    }
    public static float getHUDOpacity(){
        return Math.max(Math.min(255f,mc.gui.toolHighlightTimer * 38.4f)/ 255f, getInterfaceOpacity());
    }
    public static float getInterfaceOpacity(){
        return ((LegacyOptions)mc.options).hudOpacity().get().floatValue();
    }
    public static void playSimpleUISound(SoundEvent sound, float grave){
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, grave));
    }
    public static void addTip(Entity entity){
        if (hasTip(entity.getType())) mc.getToasts().addToast(new LegacyTip(entity.getType().getDescription(), ScreenUtil.getTip(entity.getType())));
        else if (entity.getPickResult() != null && !entity.getPickResult().isEmpty() && hasTip(entity.getPickResult())) addTip(entity.getPickResult());
    }
    public static void addTip(ItemStack stack){
        if (hasTip(stack)) mc.getToasts().addToast(new LegacyTip(stack));
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


    public static void drawGenericLoading(GuiGraphics graphics,int x, int y) {
        RenderSystem.enableBlend();
        for (int i = 0; i < 8; i++) {
            int v = (i + 1) * 100;
            int n = (i + 3) * 100;
            float l = (Util.getMillis() / 4f) % 1000;
            float alpha = l >= v - 100  ? (l <= v ? l / v: (n - l) / 200f) : 0;
            if (alpha > 0) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
                graphics.blitSprite(LOADING_BLOCK_SPRITE, x+ (i <= 2 ? i : i >= 4 ? i == 7 ? 0 : 6 - i : 2) * 27, y + (i <= 2 ? 0 : i == 3 || i == 7 ? 1 : 2)* 27, 21, 21);
            }
        }
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f,1.0f);
    }

}
