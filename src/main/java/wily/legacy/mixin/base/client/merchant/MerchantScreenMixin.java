package wily.legacy.mixin.base.client.merchant;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
import wily.legacy.util.ScreenUtil;

import static wily.legacy.util.LegacySprites.ARROW;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {

    private LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();

    @Shadow private int scrollOff;

    @Shadow @Final private static Component DEPRECATED_TOOLTIP;

    @Shadow protected abstract void renderAndDecorateCostA(GuiGraphics arg, ItemStack arg2, ItemStack arg3, int i, int j);

    @Shadow protected abstract void renderButtonArrows(GuiGraphics arg, MerchantOffer arg2, int i, int j);

    @Shadow private int shopItem;

    @Shadow private boolean isDragging;

    @Shadow protected abstract void postButtonClick();

    @Shadow @Final private static Component TRADES_LABEL;

    public MerchantScreenMixin(MerchantMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    private static final LegacySlotDisplay SLOTS_DISPLAY = new LegacySlotDisplay(){
        public int getWidth() {
            return 23;
        }
    };
    private static final LegacySlotDisplay SD_SLOTS_DISPLAY = new LegacySlotDisplay(){
        public int getWidth() {
            return 15;
        }
    };

    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        imageWidth = sd ? 215 : 330;
        imageHeight = sd ? 134 : 202;
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
                LegacySlotDisplay.override(s, sd ? 172 : 258, sd ? 26 : 39,new LegacySlotDisplay(){
                    public int getWidth() {return sd ? 21 : 32;}
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, (sd ? 93 : 132) + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 65 : 98) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, (sd ? 93 : 132) + s.getContainerSlot() * slotsSize, sd ? 110 : 166, defaultDisplay);
            }
        }
    }

    @Inject(method = "render",at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        super.render(guiGraphics, i, j, f);
        MerchantOffers merchantOffers = this.menu.getOffers();
        if (!merchantOffers.isEmpty()) {
            boolean sd = LegacyOptions.getUIMode().isSD();
            float lx = leftPos + (sd ? 5.5f : 8.5F);
            float ly = topPos + (sd ? 15.5f : 22.5F);
            int buttonWidth = sd ? 67 : 102;
            int buttonHeight = sd ? 12 : 18;
            int costAX = sd ? 6 : 10;
            int costBX = sd ? 23 : 35;
            int resultX = sd ? 45 : 68;
            int padLockX = sd ? 34 : 52;
            int arrowX = sd ? -26 : -4;
            int arrowY = sd ? -2 : 1;
            int itemSize = sd ? 10 : 16;
            float itemScale = itemSize / 16.0f;
            int offerY = (buttonHeight - itemSize) / 2;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(lx, ly, 0F);
            for (int index = 0; index < 9; index++) {
                if (index + scrollOff >= merchantOffers.size()) break;
                FactoryGuiGraphics.of(guiGraphics).blitSprite(index + scrollOff == shopItem ? LegacySprites.BUTTON_SLOT_SELECTED : ScreenUtil.isMouseOver(i,j,lx,ly + index * buttonHeight,buttonWidth,buttonHeight) ? LegacySprites.BUTTON_SLOT_HIGHLIGHTED : LegacySprites.BUTTON_SLOT, 0, 0, buttonWidth, buttonHeight);
                MerchantOffer merchantOffer = merchantOffers.get(index + scrollOff);
                ItemStack itemStack = merchantOffer.getBaseCostA();
                ItemStack itemStack2 = merchantOffer.getCostA();
                ItemStack itemStack3 = merchantOffer.getCostB();
                ItemStack itemStack4 = merchantOffer.getResult();
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(costAX, offerY, 0);
                guiGraphics.pose().scale(itemScale, itemScale, itemScale);
                this.renderAndDecorateCostA(guiGraphics, itemStack2, itemStack, 0, 0);
                guiGraphics.pose().popPose();
                if (!itemStack3.isEmpty()) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(costBX, offerY, 0);
                    guiGraphics.pose().scale(itemScale, itemScale, itemScale);
                    guiGraphics.renderFakeItem(itemStack3, 0, 0);
                    guiGraphics.renderItemDecorations(this.font, itemStack3, 0, 0);
                    guiGraphics.pose().popPose();
                }
                this.renderButtonArrows(guiGraphics, merchantOffer, arrowX, arrowY);
                if (((LegacyMerchantOffer)merchantOffer).getRequiredLevel() > menu.getTraderLevel()) FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PADLOCK, padLockX, offerY, itemSize, itemSize);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(resultX, offerY, 0);
                guiGraphics.pose().scale(itemScale, itemScale, itemScale);
                guiGraphics.renderFakeItem(itemStack4, 0, 0);
                guiGraphics.renderItemDecorations(this.font, itemStack4, 0, 0);
                guiGraphics.pose().popPose();
                guiGraphics.pose().translate(0, buttonHeight, 0F);
            }
            guiGraphics.pose().popPose();

            for (int index = 0; index < 9; index++) {
                if (index + scrollOff >= merchantOffers.size()) break;
                MerchantOffer merchantOffer = merchantOffers.get(index + scrollOff);
                int diffY = index * buttonHeight;
                if (ScreenUtil.isMouseOver(i,j,lx + costAX, ly + diffY + offerY,itemSize,itemSize)) guiGraphics.renderTooltip(font,merchantOffer.getCostA(),i,j);
                else if (!merchantOffer.getCostB().isEmpty() && ScreenUtil.isMouseOver(i,j,lx + costBX, ly + diffY + offerY,itemSize,itemSize)) guiGraphics.renderTooltip(font,merchantOffer.getCostB(),i,j);
                else if (ScreenUtil.isMouseOver(i,j,lx + resultX, ly + diffY + offerY,itemSize,itemSize)) guiGraphics.renderTooltip(font,merchantOffer.getResult(),i,j);
            }

            MerchantOffer merchantOffer = merchantOffers.get(this.shopItem);
            if (shopItem - scrollOff < 9 && shopItem - scrollOff >= 0 && merchantOffer.isOutOfStock() && ScreenUtil.isMouseOver(i, j, lx, ly + buttonHeight * (shopItem - scrollOff), buttonWidth, buttonHeight) && this.menu.canRestock()) {
                guiGraphics.renderTooltip(this.font, DEPRECATED_TOOLTIP, i, j);
            }
        }


        this.renderTooltip(guiGraphics, i, j);
    }
    @Inject(method = "renderLabels",at = @At("HEAD"), cancellable = true)
    public void renderLabels(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        ci.cancel();
        ScreenUtil.applySDFont(sd -> {
            int k = this.menu.getTraderLevel();
            int titleY = sd ? 6 : 10;
            int rightHalf = sd ? 126 : 189;
            if (k > 0 && k <= 5 && this.menu.showProgressBar()) {
                Component component = LegacyMerchantScreen.getMerchantTile(this.title,k);
                guiGraphics.drawString(this.font, component, inventoryLabelX + (rightHalf - this.font.width(component)) / 2, titleY, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            } else {
                guiGraphics.drawString(this.font, this.title, inventoryLabelX + (rightHalf - this.font.width(title)) / 2, titleY, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            }

            guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            guiGraphics.drawString(this.font, TRADES_LABEL, (sd ? 4 : 7) + ((sd ? 70 : 105) - this.font.width(TRADES_LABEL)) / 2, titleY, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        });
    }
    @Inject(method = "mouseClicked",at = @At("HEAD"), cancellable = true)
    public void mouseClicked(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        this.isDragging = false;
        boolean sd = LegacyOptions.getUIMode().isSD();
        float lx = leftPos + (sd ? 5.5f : 8.5f);
        float ly = topPos + (sd ? 15.5f : 22.5f);
        int buttonWidth = sd ? 67 : 102;
        int buttonHeight = sd ? 12 : 18;
        for (int index = 0; index < 9; index++) {
            boolean hovered = false;
            if (index + scrollOff >= this.menu.getOffers().size() || (hovered = ScreenUtil.isMouseOver(d,e,lx,ly + index * buttonHeight,buttonWidth,buttonHeight))){
                if (hovered){
                    ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1.0f);
                    if (shopItem == index + scrollOff && ((LegacyMerchantOffer)menu.getOffers().get(index + scrollOff)).getRequiredLevel() <= menu.getTraderLevel()) postButtonClick();
                    else shopItem = index + scrollOff;
                    cir.setReturnValue(true);
                    return;
                }
                break;
            }

        }
        int scrollX = leftPos + (sd ? 76 : 115);
        int scrollY = topPos + (sd ? 14 : 21);
        int scrollHeight = sd ? 110 : 165;
        if (this.menu.getOffers().size() > 9 && ScreenUtil.isMouseOver(d,e,scrollX,scrollY,13,scrollHeight)) this.isDragging = true;

        cir.setReturnValue(super.mouseClicked(d, e, i));
    }
    @Inject(method = "mouseDragged",at = @At("HEAD"), cancellable = true)
    public void mouseDragged(double d, double e, int i, double f, double g, CallbackInfoReturnable<Boolean> cir) {
        if (this.isDragging) {
            boolean sd = LegacyOptions.getUIMode().isSD();
            int scrollY = topPos + (sd ? 14 : 21);
            int scrollHeight = sd ? 110 : 165;
            int oldScroll = scrollOff;
            this.scrollOff = (int) Math.round(Math.max(0,Math.min( (e - scrollY) / scrollHeight,menu.getOffers().size() - 9)));
            if (scrollOff != oldScroll) scrollRenderer.updateScroll(oldScroll - scrollOff > 0 ? ScreenDirection.UP : ScreenDirection.DOWN);
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(super.mouseDragged(d, e, i, f, g));
        }
    }
    @Inject(method = "mouseScrolled",at = @At("HEAD"), cancellable = true)
    public void mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g, CallbackInfoReturnable<Boolean> cir) {
        if (menu.getOffers().size() > 9) {
            int j = menu.getOffers().size() - 9;
            int oldScroll = scrollOff;
            this.scrollOff = Mth.clamp((int)((double)this.scrollOff - Math.signum(g)), 0, j);
            if (scrollOff != oldScroll) scrollRenderer.updateScroll(oldScroll - scrollOff > 0 ? ScreenDirection.UP : ScreenDirection.DOWN);
        }

        cir.setReturnValue(true);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (CommonInputs.selected(i) && shopItem + scrollOff < menu.getOffers().size()){
            ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1.0f);
            postButtonClick();
            return true;
        }
        return super.keyPressed(i, j, k);
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

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        int panelRecessX = leftPos + (sd ? 4 : 7);
        int panelRecessY = topPos + (sd ? 14 : 21);
        int panelRecessWidth = sd ? 70 : 105;
        int panelRecessHeight = sd ? 110 : 165;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getElementValue("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,panelRecessX,panelRecessY,panelRecessWidth,panelRecessHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + (sd ? 150 : 219.5f),topPos + (sd ? 28 : 42.4f),0);
        if (!sd) guiGraphics.pose().scale(1.5f,1.5f,1.0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(sd ? LegacySprites.SMALL_ARROW : ARROW,0,0,sd ? 16 : 22,sd ? 14 : 16);
        guiGraphics.pose().popPose();
        int scrollX = leftPos + (sd ? 76 : 115);
        int scrollY = topPos + (sd ? 14 : 21);
        int scrollHeight = sd ? 110 : 165;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(scrollX, scrollY, 0f);
        if (menu.getOffers().size() > 9) {
            if (scrollOff != menu.getOffers().size() - 9)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN, 0, scrollHeight + 4);
            if (scrollOff > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.UP,0,-11);
        }else FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,0.5f);
        FactoryScreenUtil.enableBlend();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,0, 0,13,scrollHeight);
        guiGraphics.pose().translate(-2f, -1f + (menu.getOffers().size() > 9 ?  (scrollHeight - 13.5f) * scrollOff / (menu.getOffers().size() - 9) : 0), 0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL,0,0, 16,16);
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,1.0f);
        FactoryScreenUtil.disableBlend();
        guiGraphics.pose().popPose();
        if (this.menu.showProgressBar()) {
            int k = this.menu.getTraderLevel();
            int l = this.menu.getTraderXp();
            if (k >= 5) {
                return;
            }
            int progressBarWidth = sd ? 107 : 161;
            int progressBarHeight = sd ? 3 : 4;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(leftPos + (sd ? 96 : 144.5f),topPos + (sd ? 14 : 21),0);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.EXPERIENCE_BAR_BACKGROUND, 0, 0, 0, progressBarWidth, progressBarHeight);
            int m = VillagerData.getMinXpPerLevel(k);
            if (l < m || !VillagerData.canLevelUp(k)) {
                guiGraphics.pose().popPose();
                return;
            }
            float v = progressBarWidth / (float)(VillagerData.getMaxXpPerLevel(k) - m);
            int o = Math.min(Mth.floor(v * (float)(l - m)), progressBarWidth);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.EXPERIENCE_BAR_CURRENT, progressBarWidth, progressBarHeight, 0, 0, 0, 0, 0, o, progressBarHeight);
            int p = menu.getFutureTraderXp();
            if (p > 0) {
                int q = Math.min(Mth.floor((float)p * v), progressBarWidth - o);
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.EXPERIENCE_BAR_RESULT, progressBarWidth, progressBarHeight, o, 0, o, 0, 0, q, progressBarHeight);
            }
            guiGraphics.pose().popPose();
        }
    }
}
