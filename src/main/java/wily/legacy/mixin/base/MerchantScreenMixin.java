package wily.legacy.mixin.base;

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
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.client.CommonColor;
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

    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        imageWidth = 330;
        imageHeight = 202;
        inventoryLabelX = 131;
        inventoryLabelY = 85;
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, 165, 44, SLOTS_DISPLAY);
            } else if (i == 1) {
                LegacySlotDisplay.override(s, 195, 44, SLOTS_DISPLAY);
            } else if (i == 2) {
                LegacySlotDisplay.override(s, 258, 39,new LegacySlotDisplay(){
                    public int getWidth() {return 32;}
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, 132 + (s.getContainerSlot() - 9) % 9 * 21,98 + (s.getContainerSlot() - 9) / 9 * 21);
            } else {
                LegacySlotDisplay.override(s, 132 + s.getContainerSlot() * 21,166);
            }
        }
    }

    @Inject(method = "render",at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        super.render(guiGraphics, i, j, f);
        MerchantOffers merchantOffers = this.menu.getOffers();
        if (!merchantOffers.isEmpty()) {

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(leftPos + 8.5F, topPos + 22.5F, 0F);
            for (int index = 0; index < 9; index++) {
                if (index + scrollOff >= merchantOffers.size()) break;
                FactoryGuiGraphics.of(guiGraphics).blitSprite(index + scrollOff == shopItem ? LegacySprites.BUTTON_SLOT_SELECTED : ScreenUtil.isMouseOver(i,j,leftPos + 8.5f,topPos + 22.5f + index * 18,102,18) ? LegacySprites.BUTTON_SLOT_HIGHLIGHTED : LegacySprites.BUTTON_SLOT, 0, 0, 102, 18);
                MerchantOffer merchantOffer = merchantOffers.get(index + scrollOff);
                ItemStack itemStack = merchantOffer.getBaseCostA();
                ItemStack itemStack2 = merchantOffer.getCostA();
                ItemStack itemStack3 = merchantOffer.getCostB();
                ItemStack itemStack4 = merchantOffer.getResult();
                this.renderAndDecorateCostA(guiGraphics, itemStack2, itemStack, 10, 1);
                if (!itemStack3.isEmpty()) {
                    guiGraphics.renderFakeItem(itemStack3, 35, 1);
                    guiGraphics.renderItemDecorations(this.font, itemStack3, 35, 1);
                }
                this.renderButtonArrows(guiGraphics, merchantOffer, -4, 1);
                if (((LegacyMerchantOffer)merchantOffer).getRequiredLevel() > menu.getTraderLevel()) FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PADLOCK, 52, 1, 16, 16);
                guiGraphics.renderFakeItem(itemStack4, 68, 1);
                guiGraphics.renderItemDecorations(this.font, itemStack4, 68, 1);
                guiGraphics.pose().translate(0, 18, 0F);
            }
            guiGraphics.pose().popPose();

            for (int index = 0; index < 9; index++) {
                if (index + scrollOff >= merchantOffers.size()) break;
                MerchantOffer merchantOffer = merchantOffers.get(index + scrollOff);
                int diffY = index * 18;
                if (ScreenUtil.isMouseOver(i,j,leftPos + 18.5F, topPos + diffY + 23.5F,16,16)) guiGraphics.renderTooltip(font,merchantOffer.getCostA(),i,j);
                else if (!merchantOffer.getCostB().isEmpty() && ScreenUtil.isMouseOver(i,j,leftPos + 43.5F, topPos + diffY + 23.5F,16,16)) guiGraphics.renderTooltip(font,merchantOffer.getCostB(),i,j);
                else if (ScreenUtil.isMouseOver(i,j,leftPos + 76.5F, topPos + diffY + 23.5F,16,16)) guiGraphics.renderTooltip(font,merchantOffer.getResult(),i,j);
            }

            MerchantOffer merchantOffer = merchantOffers.get(this.shopItem);
            if (shopItem - scrollOff < 9 && shopItem - scrollOff >= 0 && merchantOffer.isOutOfStock() && this.isHovering( 7,21 + 18 * (shopItem - scrollOff),105,18, i, j) && this.menu.canRestock()) {
                guiGraphics.renderTooltip(this.font, DEPRECATED_TOOLTIP, i, j);
            }
        }


        this.renderTooltip(guiGraphics, i, j);
    }
    @Inject(method = "renderLabels",at = @At("HEAD"), cancellable = true)
    public void renderLabels(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        ci.cancel();
        int k = this.menu.getTraderLevel();
        if (k > 0 && k <= 5 && this.menu.showProgressBar()) {
            Component component = LegacyMerchantScreen.getMerchantTile(this.title,k);
            guiGraphics.drawString(this.font, component, 131 + (189 - this.font.width(component)) / 2, 10, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        } else {
            guiGraphics.drawString(this.font, this.title, 131 + (189 - this.font.width(title)) / 2, 10, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        }

        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        guiGraphics.drawString(this.font, TRADES_LABEL, 7 + (105 - this.font.width(TRADES_LABEL)) / 2, 10, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
    }
    @Inject(method = "mouseClicked",at = @At("HEAD"), cancellable = true)
    public void mouseClicked(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        this.isDragging = false;
        for (int index = 0; index < 9; index++) {
            boolean hovered = false;
            if (index + scrollOff >= this.menu.getOffers().size() || (hovered = ScreenUtil.isMouseOver(d,e,leftPos + 8.5f,topPos + 22.5f + index * 18,102,18))){
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
        if (this.menu.getOffers().size() > 9 && ScreenUtil.isMouseOver(d,e,leftPos + 115,topPos + 21,13,165)) this.isDragging = true;

        cir.setReturnValue(super.mouseClicked(d, e, i));
    }
    @Inject(method = "mouseDragged",at = @At("HEAD"), cancellable = true)
    public void mouseDragged(double d, double e, int i, double f, double g, CallbackInfoReturnable<Boolean> cir) {
        if (this.isDragging) {
            int oldScroll = scrollOff;
            this.scrollOff = (int) Math.round(Math.max(0,Math.min( (e - (topPos + 18)) / 165,menu.getOffers().size() - 9)));
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
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 7,topPos + 21,105,165);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 219.5,topPos + 42.5,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(ARROW,0,0,22,15);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 115, topPos + 21, 0f);
        if (menu.getOffers().size() > 9) {
            if (scrollOff != menu.getOffers().size() - 9)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN, 0, 169);
            if (scrollOff > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.UP,0,-11);
        }else FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,0.5f);
        RenderSystem.enableBlend();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,0, 0,13,165);
        guiGraphics.pose().translate(-2f, -1f + (menu.getOffers().size() > 9 ?  151.5f * scrollOff / (menu.getOffers().size() - 9) : 0), 0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL,0,0, 16,16);
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,1.0f);
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
        if (this.menu.showProgressBar()) {
            int k = this.menu.getTraderLevel();
            int l = this.menu.getTraderXp();
            if (k >= 5) {
                return;
            }
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(leftPos + 144.5,topPos + 21,0);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.EXPERIENCE_BAR_BACKGROUND, 0, 0, 0, 161, 4);
            int m = VillagerData.getMinXpPerLevel(k);
            if (l < m || !VillagerData.canLevelUp(k)) {
                guiGraphics.pose().popPose();
                return;
            }
            float v = 161.0f / (float)(VillagerData.getMaxXpPerLevel(k) - m);
            int o = Math.min(Mth.floor(v * (float)(l - m)), 161);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.EXPERIENCE_BAR_CURRENT, 161, 4, 0, 0, 0, 0, 0, o, 4);
            int p = menu.getFutureTraderXp();
            if (p > 0) {
                int q = Math.min(Mth.floor((float)p * v), 161 - o);
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.EXPERIENCE_BAR_RESULT, 161, 4, o, 0, o, 0, 0, q, 4);
            }
            guiGraphics.pose().popPose();
        }
    }
}
