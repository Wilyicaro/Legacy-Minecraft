package wily.legacy.util.client;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if forge {
/*import net.minecraftforge.client.extensions.common.IClientItemExtensions;
 *///?} else if neoforge {
/*import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
 *///?}
//? if >=1.21.2 {
//?}
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.GuiAccessor;
import wily.factoryapi.base.client.MinecraftAccessor;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.ColorUtil;
import wily.factoryapi.util.FactoryGuiElement;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.client.screen.ConfirmationScreen;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.client.screen.LegacyScreen;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.network.TopMessage;
import wily.legacy.util.LegacySprites;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static wily.legacy.client.screen.ControlTooltip.MORE;

public class LegacyRenderUtil {
    public static final ResourceLocation GUI_ATLAS = FactoryAPI.createVanillaLocation("textures/atlas/gui.png");
    private static final Minecraft mc = Minecraft.getInstance();
    public static long lastHotbarSelectionChange = -1;
    public static long animatedCharacterTime;
    public static long remainingAnimatedCharacterTime;
    public static int lastHotbarSelection = -1;
    public static long lastGui = -1;
    protected static final LogoRenderer logoRenderer = new LogoRenderer(false);
    public static final PanoramaRenderer panoramaRenderer = /*? if <1.20.5 {*//*new PanoramaRenderer(TitleScreen.CUBE_MAP)*//*?} else {*/LegacyScreen.PANORAMA_RENDERER/*?}*/;
    public static final LegacyIconHolder iconHolderRenderer = new LegacyIconHolder();
    public static final ResourceLocation MINECRAFT = Legacy4J.createModLocation( "textures/gui/title/minecraft.png");
    public static final ResourceLocation PANORAMA_DAY = Legacy4J.createModLocation( "textures/gui/title/panorama_day.png");
    public static final ResourceLocation PANORAMA_NIGHT = Legacy4J.createModLocation( "textures/gui/title/panorama_night.png");
    public static final ResourceLocation MENU_BACKGROUND = Legacy4J.createModLocation( "textures/gui/menu_background.png");
    public static final ResourceLocation LOADING_BACKGROUND = Legacy4J.createModLocation( "textures/gui/loading_background.png");

    public static final Bearer<Integer> actualPlayerTabWidth = Bearer.of(0);
    public static final Bearer<Integer> actualPlayerTabHeight = Bearer.of(0);

    public static void updateAnimatedCharacterTime(long remainingTime){
        animatedCharacterTime = Util.getMillis();
        remainingAnimatedCharacterTime = remainingTime;
    }

    public static void renderPointerPanel(GuiGraphics graphics, int x, int y, int width, int height){
        blitTranslucentOverlaySprite(graphics, LegacySprites.POINTER_PANEL, x, y, width, height);
    }

    public static void blitTranslucentOverlaySprite(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height){
        FactoryGuiGraphics.of(graphics).disableDepthTest();
        blitTranslucentSprite(graphics, sprite, x, y, width,height);
        FactoryGuiGraphics.of(graphics).enableDepthTest();
    }

    public static void blitTranslucentSprite(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height){
        FactoryScreenUtil.enableBlend();
        FactoryGuiGraphics.of(graphics).blitSprite(sprite, x, y, width,height);
        FactoryScreenUtil.disableBlend();
    }

    public static void renderPanelTranslucentRecess(GuiGraphics graphics, int x, int y, int width, int height){
        blitTranslucentSprite(graphics, LegacySprites.PANEL_TRANSLUCENT_RECESS, x, y, width, height);
    }

    public static void drawAutoSavingIcon(GuiGraphics graphics,int x, int y) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F,0.5F);
        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SAVE_CHEST,x * 2,y * 2,48,48);
        graphics.pose().popMatrix();
        graphics.pose().pushMatrix();
        float heightAnim = (Util.getMillis() / 50f) % 11;
        graphics.pose().translate(x + 5.5f, y - 8 - (heightAnim > 5 ? 10 - heightAnim : heightAnim));
        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SAVE_ARROW,0,0,13,16);
        graphics.pose().popMatrix();
    }

    public static void renderDefaultBackground(UIAccessor accessor, GuiGraphics guiGraphics){
        renderDefaultBackground(accessor, guiGraphics, true);
    }

    public static void renderDefaultBackground(UIAccessor accessor, GuiGraphics guiGraphics, boolean title){
        renderDefaultBackground(accessor, guiGraphics, false, title, true);
    }

    public static boolean isVisualNight(){
        return mc.level != null && mc.level./*? if <1.21.5 {*//*isNight*//*?} else {*/isDarkOutside/*?}*/();
    }

    public static void renderDefaultBackground(UIAccessor accessor, GuiGraphics guiGraphics, boolean forcePanorama, boolean title, boolean username){
        if (mc.level == null || accessor.getBoolean("forcePanorama", forcePanorama)) {
           renderPanorama(guiGraphics);
        }else /*? if <=1.20.1 {*//*renderTransparentBackground(guiGraphics)*//*?} else {*/accessor.getScreen().renderTransparentBackground(guiGraphics)/*?}*/;
        if (accessor.getBoolean("hasTitle", title)) renderLogo(guiGraphics);
        if (accessor.getBoolean("hasUsername", username)) renderUsername(guiGraphics);
    }

    public static void renderLogo(GuiGraphics guiGraphics){
        logoRenderer.renderLogo(guiGraphics, guiGraphics.guiWidth(), 1.0F);
    }

    public static void renderLegacyLogo(GuiGraphics guiGraphics){
        FactoryScreenUtil.enableBlend();
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((guiGraphics.guiWidth() - 285.5f) / 2, 30);
        guiGraphics.pose().scale(0.5f,0.5f);
        FactoryGuiGraphics.of(guiGraphics).blit(mc.getResourceManager().getResource(MINECRAFT).isPresent() ? MINECRAFT : ControlType.getActiveType().getMinecraftLogo(),0,0,0,0,571,138,571,138);
        guiGraphics.pose().popMatrix();
        FactoryScreenUtil.disableBlend();
    }

    public static void renderTransparentBackground(GuiGraphics graphics){
        FactoryScreenUtil.enableBlend();
        FactoryGuiGraphics.of(graphics).blit(LegacyRenderUtil.MENU_BACKGROUND,0,0,0,0,graphics.guiWidth(),graphics.guiHeight(),graphics.guiWidth(),graphics.guiHeight());
        FactoryScreenUtil.disableBlend();
    }

    public static void renderUsername(GuiGraphics graphics){
        if (mc.level != null) return;
        String username = MCAccount.isOfflineUser() ? I18n.get("legacy.menu.offline_user",mc.getUser().getName()) : mc.getUser().getName();
        graphics.drawString(mc.font, username, graphics.guiWidth() - 33 - mc.font.width(username), graphics.guiHeight() - 27, 0xFFFFFF);
    }

    public static void renderPanorama(GuiGraphics guiGraphics){
        panoramaRenderer.render(guiGraphics, guiGraphics.guiWidth(), guiGraphics.guiHeight(), true);
    }

    public static void renderLegacyPanorama(GuiGraphics guiGraphics){
        renderLegacyPanorama(guiGraphics, isVisualNight());
    }

    public static void renderLegacyPanorama(GuiGraphics guiGraphics, boolean isNight){
        FactoryGuiGraphics.of(guiGraphics).disableDepthTest();
        FactoryGuiGraphics.of(guiGraphics).blit(isNight ? PANORAMA_NIGHT : PANORAMA_DAY, 0, 0, mc.options.panoramaSpeed().get().floatValue() * Util.getMillis() * guiGraphics.guiHeight() / 360 / 66.32f, 1, guiGraphics.guiWidth(), guiGraphics.guiHeight() + 2, guiGraphics.guiHeight() * 820/144, guiGraphics.guiHeight() + 2);
        FactoryGuiGraphics.of(guiGraphics).enableDepthTest();
    }

    public static void drawOutlinedString(GuiGraphics graphics, Font font, Component component, int x, int y, int color, int outlineColor, float outline) {
        drawOutlinedString(graphics, font, component.getVisualOrderText(), x, y, color, outlineColor, outline);
    }

    public static void drawOutlinedString(GuiGraphics graphics, Font font, FormattedCharSequence formattedCharSequence, int x, int y, int color, int outlineColor, float outline) {
        drawStringOutline(graphics,font,formattedCharSequence, x,y,outlineColor,outline);
        graphics.drawString(font,formattedCharSequence, x, y, color,false);
    }

    public static void drawStringOutline(GuiGraphics graphics, Font font, FormattedCharSequence formattedCharSequence, int x, int y, int outlineColor, float outline) {
        float[] translations = new float[]{0,outline,-outline};
        for (float t : translations) {
            for (float t1 : translations) {
                if (t != 0 || t1 != 0) {
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(t,t1);
                    graphics.drawString(font, formattedCharSequence, x, y, outlineColor, false);
                    graphics.pose().popMatrix();
                }
            }
        }
    }

     public static boolean isMouseOver(double mouseX, double mouseY, double x, double y, int width, int height){
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static void applyHUDScale(GuiGraphics graphics){
        graphics.pose().scale(3f / getHUDScale(), 3f / getHUDScale());
    }

    public static void prepareHUDRender(GuiGraphics graphics){
        graphics.pose().pushMatrix();
        FactoryGuiGraphics.of(graphics).setColor(1.0f,1.0f,1.0f, getHUDOpacity());
        graphics.pose().translate(0, getHUDDistance());
        FactoryScreenUtil.enableBlend();
    }

    public static void finalizeHUDRender(GuiGraphics graphics){
        graphics.pose().popMatrix();
        FactoryGuiGraphics.of(graphics).setColor(1.0f,1.0f,1.0f,1.0f);
        FactoryScreenUtil.disableBlend();
    }

    public static boolean hasClassicCrafting(){
        return LegacyOptions.classicCrafting.get();
    }

    public static boolean hasMixedCrafting(){
        return (LegacyOptions.forceMixedCrafting.get() || !Legacy4JClient.hasModOnServer()) && !LegacyOptions.classicCrafting.get();
    }

    public static float getHUDScale(){
        return Math.max(1.5f, 4 - LegacyOptions.hudScale.get());
    }

    public static float getHUDSize(){
        return 6 + 3f / LegacyRenderUtil.getHUDScale() * (35 + (mc.gameMode.canHurtPlayer() ?  Math.max(2,Mth.ceil((Math.max(mc.player.getAttributeValue(Attributes.MAX_HEALTH), Math.max(mc.gui.displayHealth, mc.player.getHealth())) + mc.player.getAbsorptionAmount()) / 20f) + (mc.player.getArmorValue() > 0 ? 1 : 0))* 10 : 0));
    }

    public static float getHUDDistance(){
        return -LegacyOptions.hudDistance.get().floatValue() * (22.5f + (LegacyOptions.inGameTooltips.get() ? 17.5f : 0));
    }

    public static float getHUDOpacity(){
        float f = (Util.getMillis() - lastHotbarSelectionChange)/ 1200f;
        return getInterfaceOpacity() <= 0.8f ?Math.min(0.8f,getInterfaceOpacity() + (1 -getInterfaceOpacity()) * (f >= 3f ? Math.max(4 - f,0) : 1)) : getInterfaceOpacity();
    }

    public static boolean hasTooltipBoxes(){
        return LegacyOptions.tooltipBoxes.get();
    }

    public static boolean hasTooltipBoxes(UIAccessor accessor){
        return hasTooltipBoxes() && accessor.getBoolean("hasTooltipBox",true);
    }

    public static float getInterfaceOpacity(){
        return LegacyOptions.hudOpacity.get().floatValue();
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
        mc.getSoundManager().play(new SimpleSoundInstance(sound./*? if <1.21.2 {*//*getLocation*//*?} else {*/location/*?}*/(), SoundSource.MASTER, volume,pitch + (randomPitch ? (source.nextFloat() - 0.5f) / 10 : 0), source, false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
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

    public static void playBackSound(){
        if (LegacyOptions.backSound.get()) LegacyRenderUtil.playSimpleUISound(LegacyRegistries.BACK.get(),1.0f);
    }

    public static void drawGenericLoading(GuiGraphics graphics, int x, int y) {
        drawGenericLoading(graphics, x, y, 21, 6);
    }

    public static void drawGenericLoading(GuiGraphics graphics, int x, int y, int blockSize, int blockDistance) {
        int blockD = (blockSize + blockDistance);
        for (int i = 0; i < 8; i++) {
            int v = (i + 1) * 100;
            int n = (i + 3) * 100;
            float l = (Util.getMillis() / 4f) % 1000;
            float alpha = l >= v - 100  ? (l <= v ? l / v: (n - l) / 200f) : 0;
            if (alpha > 0) {
                FactoryGuiGraphics.of(graphics).setColor(1.0f, 1.0f, 1.0f, alpha, true);
                FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.LOADING_BLOCK, x + (i <= 2 ? i : i >= 4 ? i == 7 ? 0 : 6 - i : 2) * blockD, y + (i <= 2 ? 0 : i == 3 || i == 7 ? 1 : 2) * blockD, blockSize, blockSize);
            }
        }
        FactoryGuiGraphics.of(graphics).clearColor();
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
            FactoryGuiGraphics.of(guiGraphics).enableScissor(j, k, l, m);
            guiGraphics.drawString(font, charSequence, j - (int)g, p, n,shadow && CommonValue.WIDGET_TEXT_SHADOW.get());
            guiGraphics.disableScissor();
        } else {
            guiGraphics.drawString(font, charSequence, j, p, n,shadow && CommonValue.WIDGET_TEXT_SHADOW.get());
        }
    }

    public static void secureTranslucentRender(GuiGraphics graphics, boolean translucent, float alpha, Consumer<Boolean> render){
        if (!translucent){
            render.accept(false);
            return;
        }

        //FactoryGuiGraphics.of(graphics).pushBufferSource(BufferSourceWrapper.translucent(FactoryGuiGraphics.of(graphics).getBufferSource()));
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f,alpha);
        FactoryScreenUtil.enableBlend();
        render.accept(true);
        FactoryScreenUtil.disableBlend();
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f,1.0f);
        //FactoryGuiGraphics.of(graphics).popBufferSource();
    }

    public static boolean isHovering(Slot slot, int leftPos, int topPos,  double d, double e) {
        LegacyIconHolder holder = LegacyRenderUtil.iconHolderRenderer.slotBounds(slot);
        int width = holder.getWidth();
        int height = holder.getHeight();
        double xCorner = holder.getXCorner() + holder.offset.x();
        double yCorner = holder.getYCorner() + holder.offset.y();
        return (d -= leftPos) >= xCorner && d < (xCorner + width) && (e -= topPos) >= yCorner && e < (yCorner + height);
    }

    public static void renderEntity(GuiGraphics guiGraphics, float x, float y, int size, float partialTicks, Vector3f vector3f, Quaternionf quaternionf, @Nullable Quaternionf quaternionf2, Entity entity) {
        renderEntity(guiGraphics,x,y,size,partialTicks,vector3f,quaternionf,quaternionf2,entity,false);
    }

    public static void renderEntity(GuiGraphics guiGraphics, int x, int y, int size, float partialTicks, Vector3f vector3f, Quaternionf quaternionf, @Nullable Quaternionf quaternionf2, Entity entity, boolean forceSize) {
        InventoryScreen.renderEntityInInventory(guiGraphics, x, y, size, partialTicks, vector3f, quaternionf, quaternionf2, entity, forceSize);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y, 50.0);
        float h = forceSize ? Math.max(1f,Math.max(entity.getBbWidth(), entity.getBbHeight())) : 1;
        guiGraphics.pose()./*? if <1.20.5 {*//*mulPoseMatrix*//*?} else {*/mulPose/*?}*/(new Matrix4f().scaling(size / h, size / h, -size / h));
        guiGraphics.pose().translate(vector3f.x, vector3f.y, vector3f.z);
        guiGraphics.pose().mulPose(quaternionf);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (quaternionf2 != null) {
            quaternionf2.conjugate();
            entityRenderDispatcher.overrideCameraOrientation(quaternionf2);
        }

        entityRenderDispatcher.setRenderShadow(false);
        entityRenderDispatcher.render(entity, 0.0, 0.0, 0.0, /*? if <1.21.2 {*//*0.0f,*//*?}*/ partialTicks, guiGraphics.pose(), FactoryGuiGraphics.of(guiGraphics).getBufferSource(), 0xF000F0);
        guiGraphics.flush();
        entityRenderDispatcher.setRenderShadow(true);
        guiGraphics.pose().popMatrix();
        Lighting.setupFor3DItems();
    }

    public static void renderEntityInInventoryFollowsMouse(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, float f, float g, float h, LivingEntity livingEntity) {
        float n = (float)(i + k) / 2.0f;
        float o = (float)(j + l) / 2.0f;
        FactoryGuiGraphics.of(guiGraphics).enableScissor(i, j, k, l);
        float p = (float)Math.atan((n - g) / 40.0f);
        float q = (float)Math.atan((o - h) / 40.0f);
        Quaternionf quaternionf = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf quaternionf2 = new Quaternionf().rotateX(q * 20.0f * ((float)Math.PI / 180));
        quaternionf.mul(quaternionf2);
        float r = livingEntity.yBodyRot;
        float s = livingEntity.getYRot();
        float t = livingEntity.getXRot();
        float u = livingEntity.yHeadRotO;
        float v = livingEntity.yHeadRot;
        livingEntity.yBodyRot = 180.0f + p * 20.0f;
        livingEntity.setYRot(180.0f + p * 40.0f);
        livingEntity.setXRot(-q * 20.0f);
        livingEntity.yHeadRot = livingEntity.getYRot();
        livingEntity.yHeadRotO = livingEntity.getYRot();
        Vector3f vector3f = new Vector3f(0.0f, livingEntity.getBbHeight() / 2.0f + f, 0.0f);
        renderEntity(guiGraphics, n, o, m,1, vector3f, quaternionf, quaternionf2, livingEntity);
        livingEntity.yBodyRot = r;
        livingEntity.setYRot(s);
        livingEntity.setXRot(t);
        livingEntity.yHeadRotO = u;
        livingEntity.yHeadRot = v;
        guiGraphics.disableScissor();
    }

    public static void renderLocalPlayerHead(GuiGraphics guiGraphics, int x, int y, int size) {
        if (mc.player == null) return;
        PlayerFaceRenderer.draw(guiGraphics, mc.player./*? if >1.20.1 {*/getSkin/*?} else {*//*getSkinTextureLocation*//*?}*/(), x, y, size);
    }

    public static double getInterfaceResolution(){
        return 1.5 - LegacyOptions.interfaceResolution.get();
    }

    public static double getGuiScale(){
        int h = (LegacyOptions.autoResolution.get() ? LegacyRenderUtil.getStandardHeight() : mc.getWindow().getHeight());
        return h / 360d * LegacyRenderUtil.getTweakedHeightScale(h) * CommonValue.SCALE_MULTIPLIER.get();
    }

    public static double getTweakedHeightScale(int height) {
        if (LegacyOptions.autoResolution.get()){
            if (height == 1080) return 0.999623452;
            else if (height % 720 != 0) return 1.001d;

            return 1d;
        }
        return getInterfaceResolution();
    }

    public static int getStandardHeight(){
        return Math.round(mc.getWindow().getHeight() / 180f) * 180;
    }

    public static boolean is720p(){
        return getStandardHeight() <= 720;
    }

    public static float getTextScale(){
        return LegacyOptions.legacyItemTooltipScaling.get() ? Math.max(2/3f,Math.min(720f/getStandardHeight(),4/3f)) : 1.0f;
    }

    public static float getChatSafeZone(){
        return 29 * LegacyOptions.hudDistance.get().floatValue();
    }

    public static int getSelectedItemTooltipLines(){
        return LegacyOptions.selectedItemTooltipLines.get() == 0 ? 0 : LegacyOptions.selectedItemTooltipLines.get() + (LegacyOptions.itemTooltipEllipsis.get() ? 1 : 0);
    }

    public static boolean canDisplayHUD(){
        int hudDelay = LegacyOptions.hudDelay.get();
        return mc.screen == null && (hudDelay == 0 || Util.getMillis() - lastGui > hudDelay);
    }

    public static void renderAnimatedCharacter(GuiGraphics guiGraphics){
        if (!LegacyOptions.animatedCharacter.get()) return;
        if (mc.getCameraEntity() instanceof LivingEntity character) {
            boolean hasRemainingTime = character.isSprinting() || character.isCrouching() || character.isFallFlying() || character.isVisuallySwimming() || !(character instanceof Player);
            if ((hasRemainingTime || character instanceof Player p && p.getAbilities().flying) && !character.isSleeping()) {
                LegacyRenderUtil.updateAnimatedCharacterTime(450);
            }
            if (Util.getMillis() - LegacyRenderUtil.animatedCharacterTime <= LegacyRenderUtil.remainingAnimatedCharacterTime) {
                float xRot = character.getXRot();
                float xRotO = character.xRotO;
                if (!character.isFallFlying()) character.setXRot(character.xRotO = -2.5f);
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(32f, character.isFallFlying() ? 44 : 18);
                LegacyRenderUtil.applyHUDScale(guiGraphics);
                float f = LegacyOptions.smoothAnimatedCharacter.get() ? FactoryAPIClient.getPartialTick() : 0;
                ClientEntityAccessor.of(character).setAllowDisplayFireAnimation(false);
                LegacyRenderUtil.renderEntity(guiGraphics, 10f, (character.isFallFlying() ? -character.getViewXRot(f) / 180 * 40 : 36), 12, f,new Vector3f(), new Quaternionf().rotationXYZ(-5* Mth.PI/180f, (165 -Mth.lerp(f, character.yBodyRotO, character.yBodyRot)) * Mth.PI/180f, Mth.PI), null, character);
                ClientEntityAccessor.of(character).setAllowDisplayFireAnimation(true);
                guiGraphics.pose().popMatrix();
                character.setXRot(xRot);
                character.xRotO = xRotO;
            }
        }
    }

    public static void renderContainerEffects(GuiGraphics guiGraphics, int leftPos, int topPos, int imageWidth, int imageHeight, int mouseX, int mouseY){
        int x = leftPos + imageWidth + 3;
        int l = guiGraphics.guiWidth() - x;
        Collection<MobEffectInstance> collection = mc.player.getActiveEffects();
        if (collection.isEmpty() || l < 32) {
            return;
        }
        boolean bl = l >= 129;
        int m = 31;
        if (imageHeight < collection.size() * 28) {
            m = imageHeight / collection.size();
        }
        List<MobEffectInstance> iterable = Ordering.natural().sortedCopy(collection);
        int y = topPos + imageHeight - 28;
        for (MobEffectInstance mobEffectInstance : iterable) {
            LegacyRenderUtil.renderPointerPanel(guiGraphics,x,y,bl ? 129 : 28, 28);
            if (bl) {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(x + 25, y + 7);
                Legacy4JClient.applyFontOverrideIf(is720p(), LegacyIconHolder.MOJANGLES_11_FONT, b->{
                    Component effect = getEffectName(mobEffectInstance);
                    if (!b) guiGraphics.pose().scale(2/3f,2/3f);
                    guiGraphics.drawString(mc.font, effect, 0, 0, 0xFFFFFF);
                    guiGraphics.pose().translate(0, 10 * (b ? 1 : 1.5f));
                    guiGraphics.drawString(mc.font, MobEffectUtil.formatDuration(mobEffectInstance, 1.0f/*? if >1.20.2 {*/, mc.level.tickRateManager().tickrate()/*?}*/), 0,0, 0x7F7F7F);
                });
                guiGraphics.pose().popMatrix();
            }
            FactoryGuiGraphics.of(guiGraphics).blitSprite(Gui.getMobEffectSprite(mobEffectInstance.getEffect()), x + (bl ? 3 : 5), y + 5,  18, 18);
            y -= m;
        }
        if (!bl && mouseX >= x && mouseX <= x + 28) {
            int n = topPos + imageHeight - 28;
            MobEffectInstance mobEffectInstance = null;
            for (MobEffectInstance mobEffectInstance2 : iterable) {
                if (mouseY >= n && mouseY <= n + m) {
                    mobEffectInstance = mobEffectInstance2;
                }
                n -= m;
            }
            if (mobEffectInstance != null) {
                List<Component> list = List.of(getEffectName(mobEffectInstance), MobEffectUtil.formatDuration(mobEffectInstance, 1.0f/*? if >1.20.2 {*/, mc.level.tickRateManager().tickrate()/*?}*/));
                guiGraphics.setTooltipForNextFrame(mc.font, list, Optional.empty(), mouseX, mouseY);
            }
        }
    }
    public static Component getEffectName(MobEffectInstance mobEffectInstance) {
        MutableComponent mutableComponent = mobEffectInstance.getEffect()/*? if >=1.20.5 {*/.value()/*?}*/.getDisplayName().copy();
        if (mobEffectInstance.getAmplifier() >= 1 && mobEffectInstance.getAmplifier() <= 9) {
            MutableComponent var10000 = mutableComponent.append(CommonComponents.SPACE);
            int var10001 = mobEffectInstance.getAmplifier();
            var10000.append(Component.translatable("enchantment.level." + (var10001 + 1)));
        }

        return mutableComponent;
    }

    public static List<Component> getTooltip(ItemStack stack) {
        return stack.getTooltipLines(/*? if >1.20.5 {*/Item.TooltipContext.of(mc.level),/*?}*/ mc.player, LegacyOptions.advancedHeldItemTooltip.get() ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);
    }

    public static void renderHUDTooltip(GuiGraphics guiGraphics, int shift){
        if (!FactoryGuiElement.SELECTED_ITEM_NAME.isVisible(FactoryScreenUtil.getGuiAccessor()) || LegacyRenderUtil.getSelectedItemTooltipLines() == 0) return;
        LegacyRenderUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0, guiGraphics.guiHeight() - Math.max(shift, LegacyRenderUtil.getHUDSize()));
        FactoryAPIClient.getProfiler().push("selectedItemName");
        if (GuiAccessor.getInstance().getToolHighlightTimer() > 0 && !GuiAccessor.getInstance().getLastToolHighlight().isEmpty()) {
            Font font = /*? if forge || neoforge {*//*Objects.requireNonNullElse(IClientItemExtensions.of(GuiAccessor.getInstance().getLastToolHighlight()).getFont(GuiAccessor.getInstance().getLastToolHighlight(), IClientItemExtensions.FontContext.SELECTED_ITEM_NAME), mc.font)*//*?} else {*/  mc.font/*?}*/;
            List<Component> tooltip = LegacyRenderUtil.getTooltip(GuiAccessor.getInstance().getLastToolHighlight());
            tooltip.removeIf(c->c.getString().isBlank());
            Object2IntMap<Component> tooltipLines = tooltip.stream().limit(LegacyRenderUtil.getSelectedItemTooltipLines()).map(c-> tooltip.indexOf(c) == LegacyRenderUtil.getSelectedItemTooltipLines() - 1 && LegacyOptions.itemTooltipEllipsis.get() ? MORE : c).collect(Collectors.toMap(Function.identity(),font::width,(a, b)->b, Object2IntLinkedOpenHashMap::new));
            int l = Math.min((int)((float)GuiAccessor.getInstance().getToolHighlightTimer() * 256.0f / 10.0f),255);
            if (l > 0) {
                int height = LegacyOptions.selectedItemTooltipSpacing.get() * (tooltipLines.size() -1);
                guiGraphics.pose().translate(0, -height);
                if (!mc.options.backgroundForChatOnly().get()) {
                    int backgroundWidth = tooltipLines.values().intStream().max().orElse(0) + 4;
                    int backgroundX = (guiGraphics.guiWidth() - backgroundWidth) / 2;
                    FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f,l / 255f);
                    LegacyRenderUtil.renderPointerPanel(guiGraphics, backgroundX, -4, backgroundWidth, height + 15);
                    FactoryGuiGraphics.of(guiGraphics).clearColor();
                }
                tooltipLines.forEach((mutableComponent, width) -> {
                    int x = (guiGraphics.guiWidth() - width) / 2;
                    guiGraphics.drawString(font, mutableComponent, x, 0, 0xFFFFFF + (l << 24));
                    guiGraphics.pose().translate(0, LegacyOptions.selectedItemTooltipSpacing.get());
                });
            }
        }
        FactoryAPIClient.getProfiler().pop();
        LegacyRenderUtil.finalizeHUDRender(guiGraphics);
    }

    public static void renderGuiEffects(GuiGraphics guiGraphics){
        Collection<MobEffectInstance> collection = mc.player.getActiveEffects();
        if (!FactoryGuiElement.EFFECTS.isVisible(FactoryScreenUtil.getGuiAccessor()) || collection.isEmpty()) {
            return;
        }
        int i = 0;
        int j = 0;
        float backAlpha = LegacyRenderUtil.getHUDOpacity();

        for (MobEffectInstance mobEffectInstance : Ordering.natural().reverse().sortedCopy(collection)) {
            Holder<MobEffect> mobEffect = /*? if <1.20.5 {*//*BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mobEffectInstance.getEffect())*//*?} else {*/mobEffectInstance.getEffect()/*?}*/;
            if (!mobEffectInstance.showIcon()) continue;
            int k = guiGraphics.guiWidth() - 55;
            int l = 18;
            if (mc.isDemo()) {
                l += 15;
            }
            if (mobEffect.value().isBeneficial()) {
                k -= 24 * ++i;
            } else {
                k -= 24 * ++j;
                l += 24;
            }
            float f = 1.0f;
            FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, backAlpha);
            LegacyRenderUtil.renderPointerPanel(guiGraphics, k, l, 24, 24);
            if (mobEffectInstance.endsWithin(200)){
                int m = mobEffectInstance.getDuration();
                f = Mth.clamp((float)m / 10.0f / 5.0f * 0.5f, 0.0f, 0.5f) + Mth.cos((float)m * (float)Math.PI / 5.0f) * Mth.clamp((10 - m / 20) / 10.0f * 0.25f, 0.0f, 0.25f);
            }
            FactoryScreenUtil.enableBlend();

            FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, f * backAlpha);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(Gui.getMobEffectSprite(mobEffect), k + 3, l + 3, 18, 18);
            FactoryScreenUtil.disableBlend();
        }
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void renderTopText(GuiGraphics guiGraphics, TopMessage topMessage, int y, float scale, int ticks) {
        if (topMessage != null && ticks < topMessage.ticksOnScreen()) {
            FactoryScreenUtil.disableDepthTest();
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, y);
            guiGraphics.pose().scale(scale, scale);
            if (topMessage.pulse()) {
                float pulse = Util.getMillis() / 400f % 1;
                pulse = 1 + (pulse > 0.5f ? 1 - pulse : pulse) / 10;
                guiGraphics.pose().scale(pulse, pulse);
            }
            guiGraphics.pose().translate(-mc.font.width(topMessage.message()) / 2f,0);
            float maxFade = Math.min(topMessage.ticksOnScreen(), 20);
            float fade = topMessage.fade() ? Math.min(1, (topMessage.ticksOnScreen() - (ticks + FactoryAPIClient.getPartialTick())) / maxFade) : 1;
            guiGraphics.drawString(mc.font, topMessage.message(),0,0, fade < 1 ? ColorUtil.withAlpha(topMessage.baseColor(), fade) : topMessage.baseColor(), topMessage.shadow());
            guiGraphics.pose().popMatrix();
            FactoryScreenUtil.enableDepthTest();
        }
    }

    public static float getFlyingViewRollingRotation(float original){
        if (LegacyOptions.flyingViewRolling.get() && mc.player != null && mc.player.isFallFlying()){
            float f = FactoryAPIClient.getGamePartialTick(false);
            Vec3 vec3 = mc.player.getViewVector(f);
            Vec3 vec32 = mc.player.getDeltaMovementLerped(f);
            double d = vec32.horizontalDistanceSqr();
            double e = vec3.horizontalDistanceSqr();
            if (d > 0.0 && e > 0.0) {
                int dir = (int) -Math.signum(vec32.x * vec3.z - vec32.z * vec3.x);
                float z = (float) (Math.min(Math.PI / 8, Math.acos((vec32.x * vec3.x + vec32.z * vec3.z) / Math.sqrt(d * e)) / 2.5));
                if (z > 0) return dir*z;
            }
        }
        return original;
    }

    public static void renderGameOverlay(GuiGraphics graphics){
        if (!MinecraftAccessor.getInstance().hasGameLoaded()) return;
        float partialTick = FactoryAPIClient.getPartialTick();
        boolean canRenderElement = (mc.screen != null || !mc.options.hideGui);
        LegacyTip tip = LegacyTipManager.getActualTip();
        if ((!LegacyTipManager.tips.isEmpty() || tip != null) && canRenderElement) {
            if (tip == null) tip = LegacyTipManager.updateTip();
            tip.setX(graphics.guiWidth() - tip.getWidth() - 30);
            tip.render(graphics, 0, 0, partialTick);
            if (tip.visibility == Toast.Visibility.HIDE) LegacyTipManager.updateTip();
        }

        if (mc.options.showAutosaveIndicator().get() && canRenderElement && (mc.gui.autosaveIndicatorValue > 0 || mc.gui.lastAutosaveIndicatorValue > 0) && Mth.clamp(Mth.lerp(FactoryAPIClient.getPartialTick(), mc.gui.lastAutosaveIndicatorValue, mc.gui.autosaveIndicatorValue), 0.0f, 1.0f) > 0.02) {
            FactoryScreenUtil.disableDepthTest();
            LegacyRenderUtil.drawAutoSavingIcon(graphics, graphics.guiWidth() - 66, 44);
            FactoryScreenUtil.enableDepthTest();
        }

        if (GLFW.glfwGetInputMode(mc.getWindow().getWindow(),GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_HIDDEN && !Legacy4JClient.controllerManager.isCursorDisabled && !LegacyOptions.hasSystemCursor()) {
            FactoryScreenUtil.disableDepthTest();
            FactoryScreenUtil.enableBlend();
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) (Legacy4JClient.controllerManager.getPointerX() + LegacyTipManager.getTipXDiff()), (float) Legacy4JClient.controllerManager.getPointerY());
            FactoryGuiGraphics.of(graphics).blitSprite(mc.getWindow().getScreenWidth() >= 1920 ? LegacySprites.POINTER : LegacySprites.SMALL_POINTER, -8, -8, 16, 16);
            graphics.pose().popMatrix();
            FactoryScreenUtil.disableBlend();
            FactoryScreenUtil.enableDepthTest();
        }

        PostChain gammaEffect = Legacy4JClient.getGammaEffect();
        if (gammaEffect != null && LegacyOptions.displayLegacyGamma.get()) {
            float gamma = LegacyOptions.legacyGamma.get().floatValue();
            FactoryScreenUtil.enableBlend();
            FactoryScreenUtil.disableDepthTest();
            float tweakedGamma = gamma >= 0.5f ? (gamma - 0.5f) * 1.12f + 1.08f : gamma * 0.96f + 0.6f;
            //? if <1.21.5 {
            /*gammaEffect.passes.forEach(p-> p./^? if <1.21.2 {^//^getEffect^//^?} else {^/getShader/^?}^/().safeGetUniform("gamma").set(tweakedGamma));
            *///?}
            gammaEffect.process(mc.getMainRenderTarget(), mc.gameRenderer.resourcePool);
            FactoryScreenUtil.enableDepthTest();
            FactoryScreenUtil.disableBlend();
        }
    }

    public static Screen getInitialScreen(){
        TitleScreen titleScreen = new TitleScreen(LegacyOptions.titleScreenFade.get());
        if (LegacyOptions.skipInitialSaveWarning.get()){
            return titleScreen;
        } else return ConfirmationScreen.createSaveInfoScreen(titleScreen);
    }

    public static ScreenDirection getScreenDirection(double x, double y) {
        if (Math.abs(x) > Math.abs(y)) {
            if (x > 0)
                return ScreenDirection.RIGHT;
            else if (x < 0)
                return ScreenDirection.LEFT;
        }
        if (y > 0)
            return ScreenDirection.DOWN;
        else if (y < 0)
            return ScreenDirection.UP;
        return null;
    }

    public static void setSoundInstanceVolume(SoundInstance soundInstance, float volume) {
        SoundManagerAccessor.of(mc.getSoundManager()).setVolume(soundInstance, volume);
    }
}
