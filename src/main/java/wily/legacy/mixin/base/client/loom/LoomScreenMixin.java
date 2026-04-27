package wily.legacy.mixin.base.client.loom;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.MutablePIPRenderState;
import wily.legacy.client.screen.LegacyScrollRenderer;
import wily.legacy.client.screen.LegacyScroller;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.List;

import static wily.legacy.util.LegacySprites.*;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> {

@Shadow
    private boolean hasMaxPatterns;

    @Shadow
    private boolean displayPatterns;


    @Shadow
    private int startRow;

    @Shadow
    private BannerFlagModel flag;

    @Shadow
    private boolean scrolling;
    @Shadow
    private @Nullable /*? if <1.20.5 {*//*List<Pair<Holder<BannerPattern>, DyeColor>>*//*?} else {*/ BannerPatternLayers/*?}*/ resultBannerPatterns;
    private final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();

    public LoomScreenMixin(LoomMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Shadow
    protected abstract int totalRowCount();

    @Shadow
    @Inject(method = "init", at = @At("HEAD"))
    public void init(CallbackInfo ci) {
        boolean sd = LegacyOptions.getUIMode().isSD();
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageWidth(sd ? 130 : 215);
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageHeight(sd ? 144 : 217);
        inventoryLabelX = sd ? 7 : 14;
        inventoryLabelY = sd ? 71 : 104;
        titleLabelX = sd ? 7 : 14;
        titleLabelY = sd ? 5 : 10;
        int slotsSize = sd ? 13 : 21;
        LegacySlotDisplay defaultSlotsDisplay = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return slotsSize;
            }
        };
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, sd ? 7 : 19, sd ? 27 : 41, new LegacySlotDisplay() {
                    public int getWidth() {
                        return sd ? 13 : 23;
                    }

                    @Override
                    public Identifier getIconSprite() {
                        return s.hasItem() ? null : BANNER_SLOT;
                    }
                });
            } else if (i == 1) {
                LegacySlotDisplay.override(s, sd ? 22 : 45, sd ? 27 : 41, new LegacySlotDisplay() {
                    public int getWidth() {
                        return sd ? 13 : 23;
                    }

                    @Override
                    public Identifier getIconSprite() {
                        return s.hasItem() ? null : DYE_SLOT;
                    }
                });
            } else if (i == 2) {
                LegacySlotDisplay.override(s, sd ? 14 : 32, sd ? 44 : 66, new LegacySlotDisplay() {
                    public int getWidth() {
                        return sd ? 13 : 23;
                    }

                    @Override
                    public Identifier getIconSprite() {
                        return s.hasItem() ? null : PATTERN_SLOT;
                    }
                });
            } else if (i == 3) {
                LegacySlotDisplay.override(s, sd ? 105 : 166, sd ? 50 : 75, new LegacySlotDisplay() {
                    public int getWidth() {
                        return sd ? 21 : 32;
                    }
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 81 : 115) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultSlotsDisplay);
            } else {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsSize, sd ? 126 : 185, defaultSlotsDisplay);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        super.extractBackground(GuiGraphicsExtractor, i, j, f);
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        int patternsPanelSize = sd ? 51 : 75;
        int patternsPanelX = leftPos + (sd ? 36 : 72);
        int patternsPanelY = topPos + (sd ? 16 : 18);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, patternsPanelX, patternsPanelY, patternsPanelSize, patternsPanelSize);
        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(0.5f, 0);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + (sd ? 104 : 164), topPos + (sd ? 5 : 7), sd ? 21 : 32, sd ? 42 : 64);
        GuiGraphicsExtractor.pose().popMatrix();
        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(patternsPanelX + patternsPanelSize + 2.5f, patternsPanelY);
        if (displayPatterns && menu.getSelectablePatterns().size() > 4) {
            if (startRow != totalRowCount() - 4)
                scrollRenderer.renderScroll(GuiGraphicsExtractor, ScreenDirection.DOWN, 0, patternsPanelSize + 4);
            if (startRow > 0)
                scrollRenderer.renderScroll(GuiGraphicsExtractor, ScreenDirection.UP, 0, -11);
        } else FactoryGuiGraphics.of(GuiGraphicsExtractor).setBlitColor(1.0f, 1.0f, 1.0f, 0.5f);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, 0, 0, 13, patternsPanelSize);
        GuiGraphicsExtractor.pose().translate(-2f, -1f + (menu.getSelectablePatterns().size() > 4 && displayPatterns ? (patternsPanelSize - LegacyScroller.SCROLLER_HEIGHT_OFFSET) * startRow / (totalRowCount() - 4) : 0));
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.PANEL, 0, 0, 16, 16);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).clearBlitColor();
        GuiGraphicsExtractor.pose().popMatrix();
        if (this.resultBannerPatterns != null && !this.hasMaxPatterns) {
            GuiGraphicsExtractor.pose().pushMatrix();
            GuiGraphicsExtractor.pose().translate(0.5f, 0.0f);
            DyeColor dyeColor = ((BannerItem) menu.getResultSlot().getItem().getItem()).getColor();
            GuiGraphicsExtractor.bannerPattern(this.flag, dyeColor, this.resultBannerPatterns, leftPos, topPos, leftPos + (sd ? 230 : 360), topPos + (sd ? 46 : 69));
            GuiGraphicsExtractor.pose().popMatrix();
        } else if (this.hasMaxPatterns) {
            FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LOOM_ERROR, leftPos + menu.slots.get(3).x - 5, topPos + menu.slots.get(3).y - 5, 26, 26);
        }
        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(patternsPanelX + 1.5f, patternsPanelY + 1.5f);
        if (this.displayPatterns) {
            int patternButtonSize = sd ? 12 : 18;
            int patternDisplayWidth = sd ? 5 : 7;
            int patternDisplayHeight = sd ? 10 : 15;
            List<Holder<BannerPattern>> list = this.menu.getSelectablePatterns();
            block0:
            for (int p = 0; p < 4; ++p) {
                for (int q = 0; q < 4; ++q) {
                    int r = p + this.startRow;
                    int s = r * 4 + q;
                    if (s >= list.size()) break block0;
                    int t = q * patternButtonSize;
                    int u = p * patternButtonSize;
                    FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(s == menu.getSelectedBannerPatternIndex() ? BUTTON_SLOT_SELECTED : (LegacyRenderUtil.isMouseOver(i, j, patternsPanelX + 1.5f + t, patternsPanelY + 1.5f + u, patternButtonSize, patternButtonSize) ? BUTTON_SLOT_HIGHLIGHTED : BUTTON_SLOT), t, u, patternButtonSize, patternButtonSize);

                    GuiGraphicsExtractor.pose().pushMatrix();
                    GuiGraphicsExtractor.pose().translate(t + (patternButtonSize - patternDisplayWidth) / 2f, u + (patternButtonSize - patternDisplayHeight) / 2f);
                    GuiGraphicsExtractor.bannerPattern(this.flag, DyeColor.GRAY, new BannerPatternLayers(List.of(new BannerPatternLayers.Layer(list.get(s), DyeColor.WHITE))), 0, 0, patternDisplayWidth, patternDisplayHeight);
                    GuiGraphicsExtractor.pose().popMatrix();
                }
            }
        }
        GuiGraphicsExtractor.pose().popMatrix();
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j) {
        LegacyFontUtil.applySDFont(b -> super.extractLabels(GuiGraphicsExtractor, i, j));
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void mouseClicked(MouseButtonEvent event, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        boolean sd = LegacyOptions.getUIMode().isSD();
        int patternsPanelSize = sd ? 51 : 75;
        int patternsPanelX = leftPos + (sd ? 36 : 72);
        int patternsPanelY = topPos + (sd ? 16 : 18);
        int patternButtonSize = sd ? 12 : 18;
        this.scrolling = false;
        if (this.displayPatterns) {
            double j = patternsPanelX + 1.5;
            double k = patternsPanelY + 1.5;
            for (int l = 0; l < 4; ++l) {
                for (int m = 0; m < 4; ++m) {
                    double f = event.x() - (j + m * patternButtonSize);
                    double g = event.y() - (k + l * patternButtonSize);
                    int n = l + this.startRow;
                    int o = n * 4 + m;
                    if (!(f >= 0.0) || !(g >= 0.0) || !(f < patternButtonSize) || !(g < patternButtonSize) || !menu.clickMenuButton(this.minecraft.player, o))
                        continue;
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0f));
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, o);
                    cir.setReturnValue(true);
                    return;
                }
            }
            if (LegacyRenderUtil.isMouseOver(event.x(), event.y(), patternsPanelX + patternsPanelSize + 2.5f, patternsPanelY, 13, patternsPanelSize))
                this.scrolling = true;
        }
        cir.setReturnValue(super.mouseClicked(event, bl));
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    public void mouseDragged(MouseButtonEvent event, double f, double g, CallbackInfoReturnable<Boolean> cir) {
        boolean sd = LegacyOptions.getUIMode().isSD();
        int patternsPanelSize = sd ? 51 : 75;
        int patternsPanelY = topPos + (sd ? 16 : 18);
        int j = this.totalRowCount() - 4;
        if (this.scrolling && this.displayPatterns && j > 0) {
            int oldRow = startRow;
            this.startRow = (int) Math.max(Math.round(j * Math.min(1, (event.y() - patternsPanelY) / patternsPanelSize)), 0);
            if (oldRow != startRow) {
                scrollRenderer.updateScroll(oldRow - startRow > 0 ? ScreenDirection.UP : ScreenDirection.DOWN);
            }
            cir.setReturnValue(true);
            return;
        }
        cir.setReturnValue(super.mouseDragged(event, f, g));
    }

    @Redirect(method = "mouseScrolled", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/LoomScreen;startRow:I"))
    private void mouseScrolled(LoomScreen instance, int value) {
        if (startRow != value) {
            scrollRenderer.updateScroll(startRow - value > 0 ? ScreenDirection.UP : ScreenDirection.DOWN);
            startRow = value;
        }
    }
}
