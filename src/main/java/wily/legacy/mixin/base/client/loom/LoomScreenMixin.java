package wily.legacy.mixin.base.client.loom;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
//? if >=1.20.5 {
import net.minecraft.world.level.block.entity.BannerPatternLayers;
//?}
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.screen.LegacyScrollRenderer;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.List;

import static wily.legacy.util.LegacySprites.*;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> {



    @Shadow private boolean hasMaxPatterns;

    @Shadow private boolean displayPatterns;


    @Shadow private int startRow;

    @Shadow protected abstract void renderPattern(GuiGraphics guiGraphics, Holder<BannerPattern> holder, int i, int j);

    @Shadow private ModelPart flag;

    @Shadow private boolean scrolling;

    @Shadow protected abstract int totalRowCount();

    @Shadow private @Nullable /*? if <1.20.5 {*//*List<Pair<Holder<BannerPattern>, DyeColor>>*//*?} else {*/BannerPatternLayers/*?}*/ resultBannerPatterns;
    private LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();

    public LoomScreenMixin(LoomMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        imageWidth = 215;
        imageHeight = 217;
        inventoryLabelX = 14;
        inventoryLabelY = 104;
        titleLabelX = 14;
        titleLabelY = 10;
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, 19, 41,new LegacySlotDisplay(){
                    public int getWidth() {
                        return 23;
                    }

                    @Override
                    public ResourceLocation getIconSprite() {
                        return s.hasItem() ? null : BANNER_SLOT;
                    }
                });
            } else if (i == 1) {
                LegacySlotDisplay.override(s, 45, 41,new LegacySlotDisplay(){
                    public int getWidth() {
                        return 23;
                    }
                    @Override
                    public ResourceLocation getIconSprite() {
                        return s.hasItem() ? null : DYE_SLOT;
                    }
                });
            } else if (i == 2) {
                LegacySlotDisplay.override(s, 32, 66,new LegacySlotDisplay(){
                    public int getWidth() {
                        return 23;
                    }
                    @Override
                    public ResourceLocation getIconSprite() {
                        return s.hasItem() ? null : PATTERN_SLOT;
                    }
                });
            } else if (i == 3) {
                LegacySlotDisplay.override(s,166, 75,new LegacySlotDisplay(){
                    public int getWidth() {
                        return 32;
                    }
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, 14 + (s.getContainerSlot() - 9) % 9 * 21,115 + (s.getContainerSlot() - 9) / 9 * 21);
            } else {
                LegacySlotDisplay.override(s, 14 + s.getContainerSlot() * 21,185);
            }
        }
        this.flag = this.minecraft.getEntityModels().bakeLayer(ModelLayers./*? if <1.21.4 {*/BANNER/*?} else {*//*STANDING_BANNER_FLAG*//*?}*/).getChild("flag");
    }
    @Redirect(method = "renderPattern", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 1))
    private void renderPattern(PoseStack instance, float posestack$pose, float f, float f1) {
        instance.scale(1,-1,-1);
    }
    @Redirect(method = "renderPattern", at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;"))
    private PoseStack renderPattern(GuiGraphics graphics) {
        return graphics.pose();
    }
    //? if >1.20.1 {
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
    //?} else {
    /*@Override
    public void renderBackground(GuiGraphics guiGraphics) {
    }
    *///?}

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos, imageWidth,imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 72,  topPos+ 18, 75, 75);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 164.5f, topPos + 7, 0.0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,0,  0, 32, 64);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 149.5, topPos + 18, 0f);
        if (displayPatterns && menu.getSelectablePatterns().size() > 4) {
            if (startRow != totalRowCount() - 4)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN, 0, 79);
            if (startRow > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.UP,0,-11);
        }else FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,0.5f);
        FactoryScreenUtil.enableBlend();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,0, 0,13,75);
        guiGraphics.pose().translate(-2f, -1f + (menu.getSelectablePatterns().size() > 4 && displayPatterns ?  61.5f * startRow  / (totalRowCount() - 4) : 0), 0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL,0,0, 16,16);
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,1.0f);
        FactoryScreenUtil.disableBlend();
        guiGraphics.pose().popPose();
        Lighting.setupForFlatItems();
        if (this.resultBannerPatterns != null && !this.hasMaxPatterns) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(leftPos + 168.5f, topPos + 69, 0.0f);
            guiGraphics.pose().scale(24.0f, -24.0f, 1.0f);
            guiGraphics.pose().translate(0.5f, 0.5f, 0.5f);
            guiGraphics.pose().scale(1f, -1f, -1f);
            this.flag.xRot = 0.0f;
            this.flag.y = -32.0f;
            //? if >=1.20.5
            DyeColor dyeColor = ((BannerItem)menu.getResultSlot().getItem().getItem()).getColor();
            BannerRenderer.renderPatterns(guiGraphics.pose(), FactoryGuiGraphics.of(guiGraphics).getBufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY, this.flag, ModelBakery.BANNER_BASE, true/*? if >=1.20.5 {*/, dyeColor/*?}*/, this.resultBannerPatterns);
            guiGraphics.pose().popPose();
            guiGraphics.flush();
        } else if (this.hasMaxPatterns) {
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LOOM_ERROR, leftPos + menu.slots.get(3).x - 5, topPos + menu.slots.get(3).y - 5, 26, 26);
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 73.5f,topPos + 19.5f,0);
        if (this.displayPatterns) {
            List<Holder<BannerPattern>> list = this.menu.getSelectablePatterns();
            block0: for (int p = 0; p < 4; ++p) {
                for (int q = 0; q < 4; ++q) {
                    int r = p + this.startRow;
                    int s = r * 4 + q;
                    if (s >= list.size()) break block0;
                    int t = q * 18;
                    int u = p * 18;
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(s == menu.getSelectedBannerPatternIndex() ? BUTTON_SLOT_SELECTED : (ScreenUtil.isMouseOver(i,j,leftPos + 73.5f + t,topPos + 19.5f + u,18,18)? BUTTON_SLOT_HIGHLIGHTED : BUTTON_SLOT), t, u, 18, 18);
                    guiGraphics.pose().pushPose();
                    this.renderPattern(guiGraphics, list.get(s), t + 3, u + 3);
                    guiGraphics.pose().popPose();
                }
            }
        }
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }
    @Inject(method = "mouseClicked",at = @At("HEAD"), cancellable = true)
    public void mouseClicked(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        this.scrolling = false;
        if (this.displayPatterns) {
            double j = leftPos + 73.5f;
            double k = topPos + 19.5f;
            for (int l = 0; l < 4; ++l) {
                for (int m = 0; m < 4; ++m) {
                    double f = d - (j + m * 18);
                    double g = e - (k + l * 18);
                    int n = l + this.startRow;
                    int o = n * 4 + m;
                    if (!(f >= 0.0) || !(g >= 0.0) || !(f < 18.0) || !(g < 18.0) || !menu.clickMenuButton(this.minecraft.player, o)) continue;
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0f));
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, o);
                    cir.setReturnValue(true);
                    return;
                }
            }
            if (ScreenUtil.isMouseOver(d,e,leftPos+ 149.5,topPos + 18,13,75)) this.scrolling = true;
        }
        cir.setReturnValue(super.mouseClicked(d, e, i));
    }
    @Inject(method = "mouseDragged",at = @At("HEAD"), cancellable = true)
    public void mouseDragged(double d, double e, int i, double f, double g, CallbackInfoReturnable<Boolean> cir) {
        int j = this.totalRowCount() - 4;
        if (this.scrolling && this.displayPatterns && j > 0) {
            int oldRow = startRow;
            this.startRow = (int) Math.max(Math.round(j * Math.min(1,(e - (topPos + 18)) / 75)), 0);
            if (oldRow != startRow){
                scrollRenderer.updateScroll(oldRow - startRow > 0 ? ScreenDirection.UP : ScreenDirection.DOWN);
            }
            cir.setReturnValue(true);
            return;
        }
        cir.setReturnValue(super.mouseDragged(d, e, i, f, g));
    }
    @Redirect(method = "mouseScrolled",at = @At(value = "FIELD",target = "Lnet/minecraft/client/gui/screens/inventory/LoomScreen;startRow:I"))
    private void mouseScrolled(LoomScreen instance, int value){
        if (startRow!= value){
            scrollRenderer.updateScroll(startRow - value > 0 ? ScreenDirection.UP : ScreenDirection.DOWN);
            startRow = value;
        }
    }
}
