package wily.legacy.util;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.client.screen.LegacyIconHolder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ScreenUtil {
    public static final ResourceLocation GUI_ATLAS = FactoryAPI.createVanillaLocation("textures/atlas/gui.png");
    private static final Minecraft mc = Minecraft.getInstance();
    public static long lastHotbarSelectionChange = -1;
    public static long animatedCharacterTime;
    public static long remainingAnimatedCharacterTime;
    protected static LogoRenderer logoRenderer = new LogoRenderer(false);
    public static LegacyIconHolder iconHolderRenderer = new LegacyIconHolder();
    public static final ResourceLocation MINECRAFT = Legacy4J.createModLocation( "textures/gui/title/minecraft.png");
    public static final ResourceLocation PANORAMA_DAY = Legacy4J.createModLocation( "textures/gui/title/panorama_day.png");
    public static final ResourceLocation PANORAMA_NIGHT = Legacy4J.createModLocation( "textures/gui/title/panorama_night.png");
    public static final ResourceLocation MENU_BACKGROUND = Legacy4J.createModLocation( "textures/gui/menu_background.png");

    public static void renderPointerPanel(GuiGraphics graphics, int x, int y, int width, int height){
        RenderSystem.enableBlend();
        FactoryGuiGraphics.of(graphics).disableDepthTest();
        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.POINTER_PANEL,x,y,width,height);
        FactoryGuiGraphics.of(graphics).enableDepthTest();
        RenderSystem.disableBlend();
    }
    public static void renderPanelTranslucentRecess(GuiGraphics graphics, int x, int y, int width, int height){
        RenderSystem.enableBlend();
        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.PANEL_TRANSLUCENT_RECESS,x,y,width,height);
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
        FactoryGuiGraphics.of(graphics).blitSprite(location,0,0, (int) (width * dp), (int) (height * dp));
        graphics.pose().popPose();
        GlStateManager._texParameter(3553, 10241, 9728);
    }
    public static void drawAutoSavingIcon(GuiGraphics graphics,int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F,0.5F,1);
        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SAVE_CHEST,x * 2,y * 2,48,48);
        graphics.pose().popPose();
        graphics.pose().pushPose();
        double heightAnim = (Util.getMillis() / 50D) % 11;
        graphics.pose().translate(x + 5.5,y - 8 - (heightAnim > 5 ? 10 - heightAnim : heightAnim),0);
        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SAVE_ARROW,0,0,13,16);
        graphics.pose().popPose();
    }
    public static void renderDefaultBackground(UIDefinition.Accessor accessor, GuiGraphics guiGraphics){
        renderDefaultBackground(accessor, guiGraphics, true);
    }
    public static void renderDefaultBackground(UIDefinition.Accessor accessor, GuiGraphics guiGraphics, boolean title){
        renderDefaultBackground(accessor, guiGraphics, false, title, true);
    }
    public static boolean getActualLevelNight(){
        return (mc.getSingleplayerServer() != null&& mc.getSingleplayerServer().overworld() != null && mc.getSingleplayerServer().overworld().isNight()) || (mc.level!= null && mc.level.isNight());
    }
    public static void renderDefaultBackground(UIDefinition.Accessor accessor, GuiGraphics guiGraphics, boolean forcePanorama, boolean title, boolean username){
        if (mc.level == null || accessor.getBoolean("forcePanorama",forcePanorama))
            renderPanoramaBackground(guiGraphics, forcePanorama && getActualLevelNight());
        else /*? if <=1.20.1 {*//*renderTransparentBackground(guiGraphics)*//*?} else {*/accessor.getScreen().renderTransparentBackground(guiGraphics)/*?}*/;
        if (accessor.getBoolean("hasTitle", title)) {
            if (Minecraft.getInstance().getResourceManager().getResource(MINECRAFT).isEmpty())
                logoRenderer.renderLogo(guiGraphics, guiGraphics.guiWidth(), 1.0F);
            else {
                RenderSystem.enableBlend();
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate((guiGraphics.guiWidth() - 285.5f) / 2, 30,0);
                guiGraphics.pose().scale(0.5f,0.5f,0.5f);
                FactoryGuiGraphics.of(guiGraphics).blit(MINECRAFT,0, 0,0,0, 571,138,571,138);
                guiGraphics.pose().popPose();
                RenderSystem.disableBlend();
            }
        }
        if (accessor.getBoolean("hasUsername", username)) renderUsername(guiGraphics);
    }

    public static void renderTransparentBackground(GuiGraphics graphics){
        RenderSystem.enableBlend();
        FactoryGuiGraphics.of(graphics).blit(ScreenUtil.MENU_BACKGROUND,0,0,0,0,graphics.guiWidth(),graphics.guiHeight(),graphics.guiWidth(),graphics.guiHeight());
        RenderSystem.disableBlend();
    }

    public static void renderUsername(GuiGraphics graphics){
        if (mc.level != null) return;
        String username = MCAccount.isOfflineUser() ? I18n.get("legacy.menu.offline_user",mc.getUser().getName()) : mc.getUser().getName();
        graphics.drawString(mc.font, username, graphics.guiWidth() - 33 - mc.font.width(username), graphics.guiHeight() - 27, 0xFFFFFF);
    }
    public static void renderPanoramaBackground(GuiGraphics guiGraphics, boolean isNight){
        RenderSystem.depthMask(false);
        guiGraphics.blit(/*? if >=1.21.2 {*/RenderType::guiTexturedOverlay,/*?}*/ isNight ? PANORAMA_NIGHT : PANORAMA_DAY, 0, 0, mc.options.panoramaSpeed().get().floatValue() * Util.getMillis() / 66.32f, 1, guiGraphics.guiWidth(), guiGraphics.guiHeight() + 2, guiGraphics.guiHeight() * 820/144, guiGraphics.guiHeight() + 2);
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
        FactoryGuiGraphics.of(graphics).setColor(1.0f,1.0f,1.0f,getHUDOpacity());
        //? if >=1.21.2 {
        graphics.flush();
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f,getHUDOpacity());
        //?}
        graphics.pose().translate(0,getHUDDistance(),0);
        RenderSystem.enableBlend();
    }
    public static void finalizeHUDRender(GuiGraphics graphics){
        graphics.pose().popPose();
        FactoryGuiGraphics.of(graphics).setColor(1.0f,1.0f,1.0f,1.0f);
        //? if >=1.21.2 {
        graphics.flush();
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f,1.0f);
        //?}
        RenderSystem.disableBlend();
    }
    public static boolean hasClassicCrafting(){
        return !FactoryAPIClient.hasModOnServer || LegacyOption.classicCrafting.get();
    }
    public static float getHUDScale(){
        return Math.max(1.5f,4 - LegacyOption.hudScale.get());
    }
    public static float getHUDSize(){
        return 6 + 3f / ScreenUtil.getHUDScale() * (35 + (mc.gameMode.canHurtPlayer() ?  Math.max(2,Mth.ceil((Math.max(mc.player.getAttributeValue(Attributes.MAX_HEALTH), Math.max(mc.gui.displayHealth, mc.player.getHealth())) + mc.player.getAbsorptionAmount()) / 20f) + (mc.player.getArmorValue() > 0 ? 1 : 0))* 10 : 0));
    }
    public static double getHUDDistance(){
        return -LegacyOption.hudDistance.get()*(22.5D + (LegacyOption.inGameTooltips.get() ? 17.5D : 0));
    }
    public static float getHUDOpacity(){
        float f = (Util.getMillis() - lastHotbarSelectionChange)/ 1200f;
        return getInterfaceOpacity() <= 0.8f ?Math.min(0.8f,getInterfaceOpacity() + (1 -getInterfaceOpacity()) * (f >= 3f ? Math.max(4 - f,0) : 1)) : getInterfaceOpacity();
    }
    public static boolean hasTooltipBoxes(){
        return LegacyOption.tooltipBoxes.get();
    }
    public static boolean hasTooltipBoxes(UIDefinition.Accessor accessor){
        return hasTooltipBoxes() && accessor.getBoolean("hasTooltipBox",true);
    }
    public static float getInterfaceOpacity(){
        return LegacyOption.hudOpacity.get().floatValue();
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

    public static void drawGenericLoading(GuiGraphics graphics,int x, int y) {
        RenderSystem.enableBlend();
        for (int i = 0; i < 8; i++) {
            int v = (i + 1) * 100;
            int n = (i + 3) * 100;
            float l = (Util.getMillis() / 4f) % 1000;
            float alpha = l >= v - 100  ? (l <= v ? l / v: (n - l) / 200f) : 0;
            if (alpha > 0) {
                FactoryGuiGraphics.of(graphics).setColor(1.0f, 1.0f, 1.0f, alpha);
                FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.LOADING_BLOCK, x+ (i <= 2 ? i : i >= 4 ? i == 7 ? 0 : 6 - i : 2) * 27, y + (i <= 2 ? 0 : i == 3 || i == 7 ? 1 : 2)* 27, 21, 21);
            }
        }
        RenderSystem.disableBlend();
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
            guiGraphics.enableScissor(j, k, l, m);
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

        FactoryGuiGraphics.of(graphics).pushBufferSource(BufferSourceWrapper.translucent(FactoryGuiGraphics.of(graphics).getBufferSource()));
        graphics.flush();
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f,alpha);
        RenderSystem.enableBlend();
        render.accept(true);
        RenderSystem.disableBlend();
        graphics.flush();
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f,1.0f);
        FactoryGuiGraphics.of(graphics).popBufferSource();
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
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }
    public static void renderEntityInInventoryFollowsMouse(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, float f, float g, float h, LivingEntity livingEntity) {
        float n = (float)(i + k) / 2.0f;
        float o = (float)(j + l) / 2.0f;
        guiGraphics.enableScissor(i, j, k, l);
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

    public static int getStandardHeight(){
        return Math.round(mc.getWindow().getHeight() / 180f) * 180;
    }

    public static float getTextScale(){
        return LegacyOption.legacyItemTooltipScaling.get() ? Math.max(2/3f,Math.min(720f/getStandardHeight(),4/3f)) : 1.0f;
    }

    public static float getChatSafeZone(){
        return 29 * LegacyOption.hudDistance.get().floatValue();
    }

    public static Component getDimensionName(ResourceKey<Level> dimension){
        String s = dimension.location().toLanguageKey("dimension");
        return Component.translatable(LegacyTipManager.hasTip(s) ? s : "dimension.minecraft");
    }

    public static int getSelectedItemTooltipLines(){
        return LegacyOption.selectedItemTooltipLines.get() == 0 ? 0 : LegacyOption.selectedItemTooltipLines.get() + (LegacyOption.itemTooltipEllipsis.get() ? 1 : 0);
    }

    public static void renderAnimatedCharacter(GuiGraphics guiGraphics){
        if (mc.getCameraEntity() instanceof LivingEntity character) {
            boolean hasRemainingTime = character.isSprinting() || character.isCrouching() || character.isFallFlying() || character.isVisuallySwimming() || !(character instanceof Player);
            if (LegacyOption.animatedCharacter.get() && (hasRemainingTime || character instanceof Player p && p.getAbilities().flying) && !character.isSleeping()) {
                ScreenUtil.animatedCharacterTime = Util.getMillis();
                ScreenUtil.remainingAnimatedCharacterTime = hasRemainingTime ? 450 : 0;
            }
            if (Util.getMillis() - ScreenUtil.animatedCharacterTime <= ScreenUtil.remainingAnimatedCharacterTime) {
                float xRot = character.getXRot();
                float xRotO = character.xRotO;
                if (!character.isFallFlying()) character.setXRot(character.xRotO = -2.5f);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(32f, character.isFallFlying() ? 44 : 18,0);
                ScreenUtil.applyHUDScale(guiGraphics);
                float f = LegacyOption.smoothAnimatedCharacter.get() ? FactoryAPIClient.getPartialTick() : 0;
                ScreenUtil.renderEntity(guiGraphics, 10f, (character.isFallFlying() ? -character.getViewXRot(f) / 180 * 40 : 36), 12, f,new Vector3f(), new Quaternionf().rotationXYZ(-5* Mth.PI/180f, (165 -Mth.lerp(f, character.yBodyRotO, character.yBodyRot)) * Mth.PI/180f, Mth.PI), null, character);
                guiGraphics.pose().popPose();
                character.setXRot(xRot);
                character.xRotO = xRotO;
            }
        }
    }

    public static int colorFromFloat(float r, float g, float b, float a) {
        return (int)(a * 255f) << 24 | (int)(r * 255f) << 16 | (int)(g * 255f) << 8 | (int)(b * 255f);
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
            ScreenUtil.renderPointerPanel(guiGraphics,x,y,bl ? 129 : 28, 28);
            if (bl) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(x + 25, y + 7,0);
                Legacy4JClient.applyFontOverrideIf(mc.getWindow().getHeight() <= 720, LegacyIconHolder.MOJANGLES_11_FONT, b->{
                    Component effect = getEffectName(mobEffectInstance);
                    if (!b) guiGraphics.pose().scale(2/3f,2/3f,2/3f);
                    guiGraphics.drawString(mc.font, effect, 0, 0, 0xFFFFFF);
                    guiGraphics.pose().translate(0, 10 * (b ? 1 : 1.5f),0);
                    guiGraphics.drawString(mc.font, MobEffectUtil.formatDuration(mobEffectInstance, 1.0f/*? if >1.20.2 {*/, mc.level.tickRateManager().tickrate()/*?}*/), 0,0, 0x7F7F7F);
                });
                guiGraphics.pose().popPose();
            }
            FactoryGuiGraphics.of(guiGraphics).blit(x + (bl ? 3 : 5), y + 5, 0, 18, 18, mc.getMobEffectTextures().get(mobEffectInstance.getEffect()));
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
                guiGraphics.renderTooltip(mc.font, list, Optional.empty(), mouseX, mouseY);
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

    public static List<Component> getTooltip(ItemStack stack){
        return stack.getTooltipLines(/*? if >1.20.5 {*/Item.TooltipContext.of(mc.level),/*?}*/ mc.player, TooltipFlag.NORMAL);
    }
}
