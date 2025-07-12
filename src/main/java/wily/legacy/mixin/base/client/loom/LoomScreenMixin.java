package wily.legacy.mixin.base.client.loom;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.render.state.pip.GuiBannerResultRenderState;import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.MutablePIPRenderState;import wily.legacy.client.screen.LegacyScrollRenderer;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.List;

import static wily.legacy.util.LegacySprites.*;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> {



    @Shadow private boolean hasMaxPatterns;

    @Shadow private boolean displayPatterns;


    @Shadow private int startRow;

    @Shadow private ModelPart flag;

    @Shadow private boolean scrolling;

    @Shadow protected abstract int totalRowCount();

    @Shadow private @Nullable /*? if <1.20.5 {*//*List<Pair<Holder<BannerPattern>, DyeColor>>*//*?} else {*/BannerPatternLayers/*?}*/ resultBannerPatterns;

    @Shadow protected abstract void renderBannerOnButton(GuiGraphics arg, int i, int j, TextureAtlasSprite arg2);

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
        this.flag = this.minecraft.getEntityModels().bakeLayer(ModelLayers./*? if <1.21.4 {*//*BANNER*//*?} else {*/STANDING_BANNER_FLAG/*?}*/).getChild("flag");
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class), leftPos, topPos, imageWidth, imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 72,  topPos + 18, 75, 75);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(0.5f, 0);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + 164,  topPos + 7, 32, 64);
        guiGraphics.pose().popMatrix();
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(leftPos + 149.5f, topPos + 18);
        if (displayPatterns && menu.getSelectablePatterns().size() > 4) {
            if (startRow != totalRowCount() - 4)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN, 0, 79);
            if (startRow > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.UP,0,-11);
        } else FactoryGuiGraphics.of(guiGraphics).setBlitColor(1.0f,1.0f,1.0f,0.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,0, 0,13,75);
        guiGraphics.pose().translate(-2f, -1f + (menu.getSelectablePatterns().size() > 4 && displayPatterns ?  61.5f * startRow  / (totalRowCount() - 4) : 0));
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL,0,0, 16,16);
        FactoryGuiGraphics.of(guiGraphics).clearBlitColor();
        guiGraphics.pose().popMatrix();
        if (this.resultBannerPatterns != null && !this.hasMaxPatterns) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(0.5f, 0.0f);
            DyeColor dyeColor = ((BannerItem)menu.getResultSlot().getItem().getItem()).getColor();
            GuiBannerResultRenderState renderState = new GuiBannerResultRenderState(this.flag, dyeColor, this.resultBannerPatterns, leftPos, topPos, leftPos + 360, topPos + 69, guiGraphics.scissorStack.peek());
            MutablePIPRenderState.of(renderState).setScale(24);
            MutablePIPRenderState.of(renderState).setPose(guiGraphics.pose());
            guiGraphics.guiRenderState.submitPicturesInPictureState(renderState);
            guiGraphics.pose().popMatrix();
        } else if (this.hasMaxPatterns) {
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LOOM_ERROR, leftPos + menu.slots.get(3).x - 5, topPos + menu.slots.get(3).y - 5, 26, 26);
        }
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(leftPos + 73.5f,topPos + 19.5f);
        if (this.displayPatterns) {
            List<Holder<BannerPattern>> list = this.menu.getSelectablePatterns();
            block0: for (int p = 0; p < 4; ++p) {
                for (int q = 0; q < 4; ++q) {
                    int r = p + this.startRow;
                    int s = r * 4 + q;
                    if (s >= list.size()) break block0;
                    int t = q * 18;
                    int u = p * 18;
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(s == menu.getSelectedBannerPatternIndex() ? BUTTON_SLOT_SELECTED : (LegacyRenderUtil.isMouseOver(i,j,leftPos + 73.5f + t,topPos + 19.5f + u,18,18) ? BUTTON_SLOT_HIGHLIGHTED : BUTTON_SLOT), t, u, 18, 18);

                    TextureAtlasSprite sprite = Sheets.getBannerMaterial(list.get(s)).sprite();
                    guiGraphics.pose().pushMatrix();
                    guiGraphics.pose().translate(t + 5.5f, u + 1.5f);
                    float u0 = sprite.getU0();
                    float g = u0 + (sprite.getU1() - sprite.getU0()) * 21.0F / 64.0F;
                    float h = sprite.getV1() - sprite.getV0();
                    float k = sprite.getV0() + h / 64.0F;
                    float l = k + h * 40.0F / 64.0F;
                    guiGraphics.fill(0, 0, 7, 15, DyeColor.GRAY.getTextureDiffuseColor());
                    guiGraphics.blit(sprite.atlasLocation(), 0, 0, 7, 15, u0, g, k, l);
                    guiGraphics.pose().popMatrix();
                }
            }
        }
        guiGraphics.pose().popMatrix();
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
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
            if (LegacyRenderUtil.isMouseOver(d,e,leftPos+ 149.5,topPos + 18,13,75)) this.scrolling = true;
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
