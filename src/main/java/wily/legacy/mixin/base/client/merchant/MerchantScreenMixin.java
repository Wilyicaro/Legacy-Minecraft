package wily.legacy.mixin.base.client.merchant;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.LegacyMerchantScreen;
import wily.legacy.client.screen.LegacyScrollRenderer;
import wily.legacy.inventory.LegacyMerchantOffer;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {

@Unique
    private static final LegacySlotDisplay SLOTS_DISPLAY = new LegacySlotDisplay() {
        public int getWidth() {
            return 23;
        }
    };
    @Unique
    private static final LegacySlotDisplay SD_SLOTS_DISPLAY = new LegacySlotDisplay() {
        public int getWidth() {
            return 15;
        }
    };
    @Shadow
    @Final
    private static Component DEPRECATED_TOOLTIP;
    @Shadow
    @Final
    private static Component TRADES_LABEL;
    @Unique
    private final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    @Shadow
    private int scrollOff;
    @Shadow
    private int shopItem;

    @Shadow
    private boolean isDragging;

    public MerchantScreenMixin(MerchantMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Shadow
    protected abstract void extractAndDecorateCostA(GuiGraphicsExtractor arg, ItemStack arg2, ItemStack arg3, int i, int j);

    @Shadow
    protected abstract void extractButtonArrows(GuiGraphicsExtractor arg, MerchantOffer arg2, int i, int j);

    @Shadow
    protected abstract void postButtonClick();

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageWidth(sd ? 215 : 330);
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageHeight(sd ? 134 : 202);
        inventoryLabelX = sd ? 92 : 131;
        inventoryLabelY = sd ? 56 : 85;
        int slotsSize = sd ? 13 : 21;
        LegacySlotDisplay defaultDisplay = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return slotsSize;
            }
        };
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, sd ? 110 : 165, sd ? 29 : 44, sd ? SD_SLOTS_DISPLAY : SLOTS_DISPLAY);
            } else if (i == 1) {
                LegacySlotDisplay.override(s, sd ? 130 : 195, sd ? 29 : 44, sd ? SD_SLOTS_DISPLAY : SLOTS_DISPLAY);
            } else if (i == 2) {
                LegacySlotDisplay.override(s, sd ? 172 : 258, sd ? 26 : 39, new LegacySlotDisplay() {
                    public int getWidth() {
                        return sd ? 21 : 32;
                    }
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, (sd ? 93 : 132) + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 65 : 98) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, (sd ? 93 : 132) + s.getContainerSlot() * slotsSize, sd ? 110 : 166, defaultDisplay);
            }
        }
    }

    @Inject(method = "extractContents", at = @At("HEAD"), cancellable = true)
    public void extractContents(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        super.extractContents(GuiGraphicsExtractor, i, j, f);

        MerchantOffers merchantOffers = this.menu.getOffers();

        if (!merchantOffers.isEmpty()) {
            boolean sd = LegacyOptions.getUIMode().isSD();

            float lx = leftPos + (sd ? 5.5f : 8.5f);
            float ly = topPos + (sd ? 15.5f : 22.5f);

            int buttonWidth = sd ? 67 : 102;
            int buttonHeight = sd ? 12 : 18;
            int costAX = sd ? 6 : 10;
            int costBX = sd ? 23 : 35;
            int resultX = sd ? 45 : 68;
            int padLockX = sd ? 34 : 52;
            int arrowX = sd ? -26 : -4;
            int arrowY = sd ? -2 : 1;
            int itemSize = sd ? 10 : 16;
            float itemScale = itemSize / 16f;
            int offerY = (buttonHeight - itemSize) / 2;

            GuiGraphicsExtractor.pose().pushMatrix();
            GuiGraphicsExtractor.pose().translate(lx, ly);
            for (int index = 0; index < 9; index++) {
                if (index + scrollOff >= merchantOffers.size()) break;
                FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(index + scrollOff == shopItem ? LegacySprites.BUTTON_SLOT_SELECTED : LegacyRenderUtil.isMouseOver(i, j, lx, ly + index * buttonHeight, buttonWidth, buttonHeight) ? LegacySprites.BUTTON_SLOT_HIGHLIGHTED : LegacySprites.BUTTON_SLOT, 0, 0, buttonWidth, buttonHeight);
                MerchantOffer merchantOffer = merchantOffers.get(index + scrollOff);
                ItemStack baseCostA = merchantOffer.getBaseCostA();
                ItemStack costA = merchantOffer.getCostA();
                ItemStack costB = merchantOffer.getCostB();
                ItemStack result = merchantOffer.getResult();
                GuiGraphicsExtractor.pose().pushMatrix();
                GuiGraphicsExtractor.pose().translate(costAX, offerY);
                GuiGraphicsExtractor.pose().scale(itemScale);
                this.extractAndDecorateCostA(GuiGraphicsExtractor, costA, baseCostA, 0, 0);
                GuiGraphicsExtractor.pose().popMatrix();
                if (!costB.isEmpty()) {
                    GuiGraphicsExtractor.pose().pushMatrix();
                    GuiGraphicsExtractor.pose().translate(costBX, offerY);
                    GuiGraphicsExtractor.pose().scale(itemScale);
                    GuiGraphicsExtractor.fakeItem(costB, 0, 0);
                    GuiGraphicsExtractor.itemDecorations(this.font, costB, 0, 0);
                    GuiGraphicsExtractor.pose().popMatrix();
                }
                this.extractButtonArrows(GuiGraphicsExtractor, merchantOffer, arrowX, arrowY);
                if (((LegacyMerchantOffer) merchantOffer).getRequiredLevel() > menu.getTraderLevel())
                    FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.PADLOCK, padLockX, offerY, itemSize, itemSize);
                GuiGraphicsExtractor.pose().pushMatrix();
                GuiGraphicsExtractor.pose().translate(resultX, offerY);
                GuiGraphicsExtractor.pose().scale(itemScale);
                GuiGraphicsExtractor.fakeItem(result, 0, 0);
                GuiGraphicsExtractor.itemDecorations(this.font, result, 0, 0);
                GuiGraphicsExtractor.pose().popMatrix();
                GuiGraphicsExtractor.pose().translate(0, buttonHeight);
            }
            GuiGraphicsExtractor.pose().popMatrix();

            for (int index = 0; index < 9; index++) {
                if (index + scrollOff >= merchantOffers.size()) break;
                MerchantOffer merchantOffer = merchantOffers.get(index + scrollOff);
                int diffY = index * buttonHeight;
                if (LegacyRenderUtil.isMouseOver(i, j, lx + costAX, ly + diffY + offerY, itemSize, itemSize))
                    GuiGraphicsExtractor.setTooltipForNextFrame(font, merchantOffer.getCostA(), i, j);
                else if (!merchantOffer.getCostB().isEmpty() && LegacyRenderUtil.isMouseOver(i, j, lx + costBX, ly + diffY + offerY, itemSize, itemSize))
                    GuiGraphicsExtractor.setTooltipForNextFrame(font, merchantOffer.getCostB(), i, j);
                else if (LegacyRenderUtil.isMouseOver(i, j, lx + resultX, ly + diffY + offerY, itemSize, itemSize))
                    GuiGraphicsExtractor.setTooltipForNextFrame(font, merchantOffer.getResult(), i, j);
            }

            MerchantOffer merchantOffer = merchantOffers.get(this.shopItem);
            if (shopItem - scrollOff < 9 && shopItem - scrollOff >= 0 && merchantOffer.isOutOfStock() && this.isHovering((int) lx, (int) ly + buttonHeight * (shopItem - scrollOff), buttonWidth, buttonHeight, i, j) && this.menu.canRestock()) {
                GuiGraphicsExtractor.setTooltipForNextFrame(this.font, DEPRECATED_TOOLTIP, i, j);
            }
        }

        this.extractTooltip(GuiGraphicsExtractor, i, j);
    }

    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    public void extractLabels(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, CallbackInfo ci) {
        ci.cancel();
        LegacyFontUtil.applySDFont(sd -> {
            int k = this.menu.getTraderLevel();

            int titleY = sd ? 6 : 10;
            int rightHalf = sd ? 126 : 189;

            if (k > 0 && k <= 5 && this.menu.showProgressBar()) {
                Component component = LegacyMerchantScreen.getMerchantTile(this.title, k);
                GuiGraphicsExtractor.text(this.font, component, inventoryLabelX + (rightHalf - this.font.width(component)) / 2, titleY, CommonColor.GRAY_TEXT.get(), false);
            } else {
                GuiGraphicsExtractor.text(this.font, this.title, inventoryLabelX + (rightHalf - this.font.width(title)) / 2, titleY, CommonColor.GRAY_TEXT.get(), false);
            }

            GuiGraphicsExtractor.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, CommonColor.GRAY_TEXT.get(), false);
            GuiGraphicsExtractor.text(this.font, TRADES_LABEL, (sd ? 4 : 7) + ((sd ? 70 : 105) - this.font.width(TRADES_LABEL)) / 2, titleY, CommonColor.GRAY_TEXT.get(), false);
        });
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void mouseClicked(MouseButtonEvent event, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        this.isDragging = false;

        boolean sd = LegacyOptions.getUIMode().isSD();

        float lx = leftPos + (sd ? 5.5f : 8.5f);
        float ly = topPos + (sd ? 15.5f : 22.5f);

        int buttonWidth = sd ? 67 : 102;
        int buttonHeight = sd ? 12 : 18;
        for (int index = 0; index < 9; index++) {
            boolean hovered = false;
            if (index + scrollOff >= this.menu.getOffers().size() || (hovered = LegacyRenderUtil.isMouseOver(event.x(), event.y(), lx, ly + index * buttonHeight, buttonWidth, buttonHeight))) {
                if (hovered) {
                    LegacySoundUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);

                    if (shopItem != index + scrollOff) {
                        shopItem = index + scrollOff;
                        postButtonClick();
                    }

                    cir.setReturnValue(true);
                    return;
                }
                break;
            }

        }

        int scrollX = leftPos + (sd ? 76 : 115);
        int scrollY = topPos + (sd ? 14 : 21);
        int scrollHeight = sd ? 110 : 165;

        if (this.menu.getOffers().size() > 9 && LegacyRenderUtil.isMouseOver(event.x(), event.y(), scrollX, scrollY, 13, scrollHeight))
            this.isDragging = true;

        cir.setReturnValue(super.mouseClicked(event, bl));
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    public void mouseDragged(MouseButtonEvent mouseButtonEvent, double d, double e, CallbackInfoReturnable<Boolean> cir) {
        if (this.isDragging) {
            boolean sd = LegacyOptions.getUIMode().isSD();
            int scrollY = topPos + (sd ? 14 : 21);
            int scrollHeight = sd ? 110 : 165;
            int oldScroll = scrollOff;
            this.scrollOff = (int) Math.round(Math.max(0, Math.min((mouseButtonEvent.y() - scrollY) / scrollHeight, menu.getOffers().size() - 9)));
            if (scrollOff != oldScroll)
                scrollRenderer.updateScroll(oldScroll - scrollOff > 0 ? ScreenDirection.UP : ScreenDirection.DOWN);
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(super.mouseDragged(mouseButtonEvent, d, e));
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    public void mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g, CallbackInfoReturnable<Boolean> cir) {
        if (menu.getOffers().size() > 9) {
            int j = menu.getOffers().size() - 9;
            int oldScroll = scrollOff;
            this.scrollOff = Mth.clamp((int) ((double) this.scrollOff - Math.signum(g)), 0, j);
            if (scrollOff != oldScroll)
                scrollRenderer.updateScroll(oldScroll - scrollOff > 0 ? ScreenDirection.UP : ScreenDirection.DOWN);
        }

        cir.setReturnValue(true);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.isSelection() && shopItem + scrollOff < menu.getOffers().size()) {
            LegacySoundUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
            postButtonClick();
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        super.extractBackground(GuiGraphicsExtractor, i, j, f);
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();

        boolean sd = LegacyOptions.getUIMode().isSD();

        int panelRecessX = leftPos + (sd ? 4 : 7);
        int panelRecessY = topPos + (sd ? 14 : 21);
        int panelRecessWidth = sd ? 70 : 105;
        int panelRecessHeight = sd ? 110 : 165;

        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, panelRecessX, panelRecessY, panelRecessWidth, panelRecessHeight);
        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(leftPos + (sd ? 150 : 219.5f), topPos + (sd ? 28 : 42.4f));
        if (!sd)
            GuiGraphicsExtractor.pose().scale(1.5f, 1.5f);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(sd ? LegacySprites.SMALL_ARROW : LegacySprites.ARROW, 0, 0, sd ? 16 : 22, sd ? 14 : 16);
        GuiGraphicsExtractor.pose().popMatrix();

        int scrollX = leftPos + (sd ? 76 : 115);
        int scrollY = topPos + (sd ? 14 : 21);
        int scrollHeight = sd ? 110 : 165;

        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(scrollX, scrollY);
        if (menu.getOffers().size() > 9) {
            if (scrollOff != menu.getOffers().size() - 9)
                scrollRenderer.renderScroll(GuiGraphicsExtractor, ScreenDirection.DOWN, 0, 169);
            if (scrollOff > 0)
                scrollRenderer.renderScroll(GuiGraphicsExtractor, ScreenDirection.UP, 0, -11);
        } else FactoryGuiGraphics.of(GuiGraphicsExtractor).setBlitColor(1.0f, 1.0f, 1.0f, 0.5f);
        FactoryScreenUtil.enableBlend();
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, 0, 0, 13, scrollHeight);
        GuiGraphicsExtractor.pose().translate(-2f, -1f + (menu.getOffers().size() > 9 ? 151.5f * scrollOff / (menu.getOffers().size() - 9) : 0));
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.PANEL, 0, 0, 16, 16);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).setBlitColor(1.0f, 1.0f, 1.0f, 1.0f);
        FactoryScreenUtil.disableBlend();
        GuiGraphicsExtractor.pose().popMatrix();

        if (!this.menu.showProgressBar())
            return;

        int k = this.menu.getTraderLevel();
        int l = this.menu.getTraderXp();
        if (k >= 5)
            return;

        float progressBarX = leftPos + (sd ? 96 : 144.5f);
        float progressBarY = topPos + (sd ? 14 : 21);
        int progressBarWidth = sd ? 107 : 161;
        int progressBarHeight = sd ? 3 : 4;

        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(progressBarX, progressBarY);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.EXPERIENCE_BAR_BACKGROUND, 0, 0, 0, progressBarWidth, progressBarHeight);
        int m = VillagerData.getMinXpPerLevel(k);
        if (l < m || !VillagerData.canLevelUp(k)) {
            GuiGraphicsExtractor.pose().popMatrix();
            return;
        }
        float v = progressBarWidth / (float) (VillagerData.getMaxXpPerLevel(k) - m);
        int o = Math.min(Mth.floor(v * (float) (l - m)), progressBarWidth);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.EXPERIENCE_BAR_CURRENT, progressBarWidth, progressBarHeight, 0, 0, 0, 0, 0, o, progressBarHeight);
        int p = menu.getFutureTraderXp();
        if (p > 0) {
            int q = Math.min(Mth.floor((float) p * v), progressBarWidth - o);
            FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.EXPERIENCE_BAR_RESULT, progressBarWidth, progressBarHeight, o, 0, o, 0, 0, q, progressBarHeight);
        }
        GuiGraphicsExtractor.pose().popMatrix();
    }
}
