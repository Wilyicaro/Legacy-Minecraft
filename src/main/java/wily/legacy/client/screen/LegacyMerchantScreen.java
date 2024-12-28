package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.Controller;
import wily.legacy.util.LegacyComponents;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.inventory.RecipeMenu;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.inventory.LegacyMerchantMenu;
import wily.legacy.inventory.LegacyMerchantOffer;
import wily.legacy.network.ServerMenuCraftPayload;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.util.LegacySprites.DISCOUNT_STRIKETHRUOGH_SPRITE;

public class LegacyMerchantScreen extends AbstractContainerScreen<LegacyMerchantMenu> implements Controller.Event,ControlTooltip.Event {
    protected final boolean[] displaySlotsWarning = new boolean[3];;

    protected final List<LegacyIconHolder> merchantTradeButtons = new ArrayList<>();;

    protected final ContainerListener listener;
    protected final Stocker.Sizeable tradingButtonsOffset = new Stocker.Sizeable(0);
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();

    public LegacyMerchantScreen(LegacyMerchantMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
        listener = new ContainerListener() {
            public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
                updateSlotsDisplay();
            }
            public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int j) {

            }
        };
        for (int index = 0; index < 10; index++)
            addTradeButton(index);
    }
    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        super.setFocused(guiEventListener);
        if (guiEventListener instanceof LegacyIconHolder) updateSlotsDisplay();
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        ControlTooltip.setupDefaultButtons(renderer,this);
        Event.super.addControlTooltips(renderer);
    }

    @Override
    public boolean disableCursorOnInit() {
        return true;
    }

    @Override
    public boolean onceClickBindings() {
        return false;
    }

    private void updateSlotsDisplay(){
        tradingButtonsOffset.max = Math.max(0,menu.merchant.getOffers().size() - 10);
        List<ItemStack> compactList = new ArrayList<>();
        RecipeMenu.handleCompactInventoryList(compactList,Minecraft.getInstance().player.getInventory(),menu.getCarried());
        merchantTradeButtons.forEach(b->{
            b.allowFocusedItemTooltip = true;
            int i = tradingButtonsOffset.get() + merchantTradeButtons.indexOf(b);
            boolean warning = false;
            if (i < menu.merchant.getOffers().size()) {
                MerchantOffer offer = menu.merchant.getOffers().get(i);
                boolean matchesCostA = compactList.stream().anyMatch(item -> offer.satisfiedBy(item, offer.getCostB()) && item.getCount() >= offer.getCostA().getCount());
                boolean matchesCostB = offer.getCostB().isEmpty() || compactList.stream().anyMatch(item -> offer.satisfiedBy(offer.getCostA(),item) && item.getCount() >= offer.getCostB().getCount());
                warning = !matchesCostA || !matchesCostB;
                if (offer == getSelectedMerchantOffer()){
                    displaySlotsWarning[0] = !matchesCostA;
                    displaySlotsWarning[1] = !matchesCostB;
                    displaySlotsWarning[2] = warning;
                }
            }
            b.setWarning(warning);
        });
    }


    protected void addTradeButton(int index){
        LegacyIconHolder h = new LegacyIconHolder(27,27){

            @Override
            public @Nullable Component getAction(Context context) {
                return context.actionOfContext(KeyContext.class, c-> c.key() == InputConstants.KEY_RETURN && !displaySlotsWarning[2] && isValidIndex() && isFocused() ? LegacyComponents.TRADE : null);
            }

            @Override
            public void render(GuiGraphics graphics, int i, int j, float f) {
                itemIcon = isValidIndex() ? menu.merchant.getOffers().get(getIndex()).getResult() : ItemStack.EMPTY;
                super.render(graphics, i, j, f);
                graphics.pose().pushPose();
                graphics.pose().translate(0,0,232);
                if (isValidIndex() && ((LegacyMerchantOffer)menu.merchant.getOffers().get(getIndex())).getRequiredLevel() > menu.merchantLevel) {
                    renderIcon(LegacySprites.PADLOCK, graphics, false, 16, 16);
                } else if (isValidIndex() && menu.merchant.getOffers().get(getIndex()).isOutOfStock()) {
                    renderIcon(LegacySprites.ERROR_CROSS, graphics, false, 15, 15);
                }
                graphics.pose().popPose();
            }

            @Override
            public void renderItem(GuiGraphics graphics, int i, int j, float f) {
                if(itemIcon.isEmpty()) return;
                ScreenUtil.secureTranslucentRender(graphics,isValidIndex() && menu.merchant.getOffers().get(getIndex()).isOutOfStock(),0.5f, (u)-> super.renderItem(graphics, i, j, f));
            }

            @Override
            public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
                for (int index = 0; index < 3; index++) {
                    MerchantOffer offer = getSelectedMerchantOffer();
                    if (index == 1 && (offer == null || offer.getCostB().isEmpty())) continue;
                    LegacyIconHolder iconHolder = ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (index == 2 ? 86 : 17), topPos +  (index == 0 ? 114 + (offer == null || offer.getCostB().isEmpty() ? 16 : 0) : index == 1 ? 144 : 130), 27,27,offer == null || index == 0 ? ItemStack.EMPTY : index == 1 ? offer.getCostB() : offer.getResult(),offer != null && displaySlotsWarning[index], Vec3.ZERO.ZERO);
                    iconHolder.render(graphics, i, j, f);
                    if (offer == null || index != 0) continue;
                    iconHolder.renderItem(graphics,()->{
                        ItemStack costA = offer.getCostA();
                        ItemStack baseCostA = offer.getBaseCostA();
                        graphics.renderFakeItem(costA, 0, 0);
                        if (baseCostA.getCount() == costA.getCount()) {
                            graphics.renderItemDecorations(font, costA, 0, 0);
                        } else {
                            graphics.renderItemDecorations(font, baseCostA, -12, 0, baseCostA.getCount() == 1 ? "1" : null);
                            graphics.renderItemDecorations(font, costA, 0, 0, costA.getCount() == 1 ? "1" : null);
                            graphics.pose().pushPose();
                            graphics.pose().translate(0.0f, 0.0f, 300.0f);
                            FactoryGuiGraphics.of(graphics).blitSprite(DISCOUNT_STRIKETHRUOGH_SPRITE, -5, +12, 0, 9, 2);
                            graphics.pose().popPose();
                        }
                    }, iconHolder.getX(),iconHolder.getY(),iconHolder.isWarning());
                }

                super.renderSelection(graphics, i, j, f);
            }

            private int getIndex(){
                return tradingButtonsOffset.get() + index;
            }
            @Override
            public void renderTooltip(Minecraft minecraft, GuiGraphics graphics, int i, int j) {
                super.renderTooltip(minecraft, graphics, i, j);
                if (!isFocused()) return;
                MerchantOffer offer = getSelectedMerchantOffer();
                if (offer != null)
                    for (int index = 0; index < 3; index++) {
                        ItemStack s = index == 0 ? offer.getCostA() : index == 1 ? offer.getCostB() : offer.getResult();
                        if (!s.isEmpty() && ScreenUtil.isMouseOver(i,j,leftPos + (index == 2 ? 86 : 17), topPos +  (index == 0 ? 114 + (offer.getCostB().isEmpty() ? 16 : 0) : index == 1 ? 144 : 130), 27,27)) renderTooltip(minecraft,graphics,s, i, j);
                    }
            }

            @Override
            public boolean keyPressed(int i, int j, int k) {
                if ((i == 263 && index == 0) || (i == 262 && index == merchantTradeButtons.size() - 1)){
                    int oldOffset = tradingButtonsOffset.get();
                    tradingButtonsOffset.add(i == 263 ? -1 : 1,true);
                    if ((oldOffset == tradingButtonsOffset.max && i == 262) || (oldOffset == 0 && i == 263)) LegacyMerchantScreen.this.setFocused(merchantTradeButtons.get(i == 263 ? merchantTradeButtons.size() - 1 : 0));
                    else {
                        scrollRenderer.updateScroll(i == 263 ? ScreenDirection.LEFT : ScreenDirection.RIGHT);
                        updateSlotsDisplay();
                    }
                    ScreenUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(),true);
                    return true;
                }
                return super.keyPressed(i, j, k);
            }


            @Override
            public ResourceLocation getIconHolderSprite() {
                return isValidIndex() && ((LegacyMerchantOffer)menu.merchant.getOffers().get(getIndex())).getRequiredLevel() > menu.merchantLevel ? LegacyIconHolder.GRAY_ICON_HOLDER : super.getIconHolderSprite();
            }
            @Override
            public boolean isWarning() {
                return super.isWarning() && isValidIndex() && ((LegacyMerchantOffer)menu.merchant.getOffers().get(getIndex())).getRequiredLevel() <= menu.merchantLevel && !menu.merchant.getOffers().get(getIndex()).isOutOfStock();
            }

            private boolean isValidIndex(){
                return getIndex() < menu.merchant.getOffers().size();
            }

            @Override
            public void onPress() {
                if (isValidIndex() && isFocused()) {
                    MerchantOffer offer = menu.merchant.getOffers().get(getIndex());
                    if (((LegacyMerchantOffer)offer).getRequiredLevel() <= menu.merchantLevel && !offer.isOutOfStock() && !displaySlotsWarning[2]) {
                        CommonNetwork.sendToServer(new ServerMenuCraftPayload(Collections.emptyList(),getIndex(),hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.bindingState.pressed));
                    }else ScreenUtil.playSimpleUISound(LegacyRegistries.CRAFT_FAIL.get(),1.0f);
                }
            }
        };
        h.offset = new Vec3(0.5f,0,0);
        merchantTradeButtons.add(h);
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

    public void init() {
        imageWidth = 294;
        imageHeight = 181;
        titleLabelX = (imageWidth - font.width(title)) / 2;
        titleLabelY = 12;
        inventoryLabelX = 128 + (153 - font.width(playerInventoryTitle))/2;
        inventoryLabelY = 87;
        super.init();
        updateSlotsDisplay();
        if (!(getFocused() instanceof LegacyIconHolder)) setFocused(merchantTradeButtons.get(0));
        merchantTradeButtons.forEach(holder->{
            int i = merchantTradeButtons.indexOf(holder);
            holder.setX(leftPos + 13 + 27*i);
            holder.setY(topPos + 44);
            addRenderableWidget(holder);
        });
        menu.addSlotListener(listener);
    }

    boolean initOffers = false;

    @Override
    public void removed() {
        super.removed();
        menu.removeSlotListener(listener);
    }

    public MerchantOffer getSelectedMerchantOffer(){
        return getSelectedOfferIndex() < menu.merchant.getOffers().size() ? menu.merchant.getOffers().get(getSelectedOfferIndex()) : null;
    }
    public int getSelectedOfferIndex(){
        return (getFocused() instanceof LegacyIconHolder h ? merchantTradeButtons.indexOf(h) : 0) + tradingButtonsOffset.get();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        if (!menu.merchant.getOffers().isEmpty() && !initOffers){
            initOffers = true;
            updateSlotsDisplay();
        }
        super.render(guiGraphics, i, j, f);
        if (getFocused() instanceof LegacyIconHolder h) h.renderSelection(guiGraphics,i,j,f);
        merchantTradeButtons.forEach(b-> b.renderTooltip(minecraft,guiGraphics,i,j));
        renderTooltip(guiGraphics,i,j);
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if (super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g)) return true;
        int scroll = (int)Math.signum(g);
        if (((tradingButtonsOffset.get() > 0 && scroll < 0) || (scroll > 0 && tradingButtonsOffset.max > 0)) && tradingButtonsOffset.add(scroll,false) != 0){
            updateSlotsDisplay();
            return true;
        }
        return false;
    }


    private void renderProgressBar(GuiGraphics guiGraphics) {
        int k = this.menu.merchantLevel;
        int l = this.menu.merchant.getVillagerXp();
        if (k >= 5) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + (imageWidth - 1.5f* 161) / 2,topPos + 28,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.EXPERIENCE_BAR_BACKGROUND, 0, 0, 0, 161, 4);
        int m = VillagerData.getMinXpPerLevel(k);
        if (l < m || !VillagerData.canLevelUp(k)) {
            return;
        }
        float f = 161.0f / (float)(VillagerData.getMaxXpPerLevel(k) - m);
        int o = Math.min(Mth.floor(f * (float)(l - m)), 161);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.EXPERIENCE_BAR_CURRENT, 161, 4, 0, 0, 0, 0, 0, o, 4);
        int p = getSelectedMerchantOffer() != null ? getSelectedMerchantOffer().getXp() : 0;
        if (p > 0) {
            int q = Math.min(Mth.floor((float)p * f), 161 - o);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.EXPERIENCE_BAR_RESULT, 161, 4, o, 0, o, 0, 0, q, 4);
        }
        guiGraphics.pose().popPose();
    }

    public static MutableComponent getMerchantTile(Component title, int i){
        return /*? if >1.20.1 {*/Component.translatable("merchant.title", title, Component.translatable("merchant.level." + i))/*?} else {*//*title.copy().append(" - ").append(Component.translatable("merchant.level." + i))*//*?}*/;
    }

    @Override
    public void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        int k = this.menu.merchantLevel;
        if (k > 0 && k <= 5 && this.menu.showProgressBar) {
            MutableComponent component = getMerchantTile(title,k);
            guiGraphics.drawString(this.font, component, (imageWidth - this.font.width(component)) / 2, titleLabelY, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        } else {
            guiGraphics.drawString(this.font, this.title, titleLabelX, titleLabelY, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        }
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        if (getSelectedMerchantOffer() != null && menu.showProgressBar) {
            int level = ((LegacyMerchantOffer)getSelectedMerchantOffer()).getRequiredLevel();
            if (level > 0) {
                Component c = Component.translatable("merchant.level." + level);
                guiGraphics.drawString(this.font, c, 15 + (105 - font.width(c)) / 2, 100, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            }
        }
    }

    @Override
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 12,topPos + 79,110,93);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 126,topPos + 79,157,93);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 47,topPos + 131,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ARROW,0,0,22,15);
        if (getSelectedMerchantOffer() != null && getSelectedMerchantOffer().isOutOfStock())
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ERROR_CROSS, 4, 0, 15, 15);
        guiGraphics.pose().popPose();
        if (getSelectedMerchantOffer() instanceof LegacyMerchantOffer o && o.getRequiredLevel() > menu.merchantLevel)
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PADLOCK, leftPos + 56,  topPos + 134, 16, 16);
        if (menu.showProgressBar)
            renderProgressBar(guiGraphics);

        if (tradingButtonsOffset.get() > 0)
            scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, leftPos + 5, topPos + 52);
        if (tradingButtonsOffset.max > 0 && tradingButtonsOffset.get() < tradingButtonsOffset.max)
            scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + 283, topPos + 52);

    }
}
