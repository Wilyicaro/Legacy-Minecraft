package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.trading.MerchantOffer;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JPlatform;
import wily.legacy.client.Offset;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.inventory.LegacyMerchantMenu;
import wily.legacy.inventory.LegacyMerchantOffer;
import wily.legacy.network.ServerInventoryCraftPacket;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.ControlTooltip.CONTROL_ACTION_CACHE;
import static wily.legacy.util.LegacySprites.DISCOUNT_STRIKETHRUOGH_SPRITE;

public class LegacyMerchantScreen extends AbstractContainerScreen<LegacyMerchantMenu> {
    private int selectedTrade = 0;
    private int lastFocused = -1;
    protected final boolean[] displaySlotsWarning = new boolean[3];;

    protected final List<LegacyIconHolder> merchantTradeButtons = new ArrayList<>();;

    private final ContainerListener listener;

    public LegacyMerchantScreen(LegacyMerchantMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().tooltips.set(0,create(()->getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_RETURN,true) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(true),()->getFocused() instanceof LegacyIconHolder && !displaySlotsWarning[2] ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.trade") : null));
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

    private void updateSlotsDisplay(){
        List<ItemStack> compactList = new ArrayList<>();
        for (int i1 = 0; i1 < 36; i1++) {
            if (i1 == 2)  continue;
            Slot s = menu.slots.get(i1);
            if (s.getItem().isEmpty()) continue;
            ItemStack item = s.getItem();
            compactList.stream().filter(i->ItemStack.isSameItemSameTags(i,item)).findFirst().ifPresentOrElse(i-> i.grow(item.getCount()), ()-> compactList.add(item.copy()));
        }
        merchantTradeButtons.forEach(b->{
            b.allowFocusedItemTooltip = true;
            int i = merchantTradeButtons.indexOf(b);
            boolean warning = false;
            if (i < menu.merchant.getOffers().size()) {
                MerchantOffer offer = menu.merchant.getOffers().get(i);
                boolean matchesCostA = compactList.stream().anyMatch(item -> offer.isRequiredItem(item, offer.getCostA()) && item.getCount() >= offer.getCostA().getCount());
                boolean matchesCostB = offer.getCostB().isEmpty() || compactList.stream().anyMatch(item -> offer.isRequiredItem(item, offer.getCostB()) && item.getCount() >= offer.getCostB().getCount());
                warning = !matchesCostA || !matchesCostB;
                if (i == selectedTrade){
                    displaySlotsWarning[0] = !matchesCostA;
                    displaySlotsWarning[1] = !matchesCostB;
                    displaySlotsWarning[2] = warning;
                }
            }
            b.setWarning(warning);
        });
    }


    protected void addTradeButton(int index){
        merchantTradeButtons.add(new LegacyIconHolder(27,27){
            @Override
            public void render(GuiGraphics graphics, int i, int j, float f) {
                if (isValidIndex())
                    itemIcon = menu.merchant.getOffers().get(index).getResult();
                super.render(graphics, i, j, f);
                if (isValidIndex() && ((LegacyMerchantOffer)menu.merchant.getOffers().get(index)).getRequiredLevel() > menu.merchantLevel) {
                    RenderSystem.disableDepthTest();
                    renderIcon(LegacySprites.PADLOCK, graphics, false, 16, 16);
                    RenderSystem.enableDepthTest();
                } else if (isValidIndex() && menu.merchant.getOffers().get(index).isOutOfStock()) {
                    RenderSystem.disableDepthTest();
                    renderIcon(LegacySprites.ERROR_CROSS, graphics, false, 15, 15);
                    RenderSystem.enableDepthTest();
                }
            }

            @Override
            public void renderItem(GuiGraphics graphics, int i, int j, float f) {
                if(itemIcon.isEmpty()) return;
                ScreenUtil.secureTranslucentRender(graphics,isValidIndex() && menu.merchant.getOffers().get(index).isOutOfStock(),0.5f, (u)-> super.renderItem(graphics, i, j, f));
            }

            @Override
            public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
                for (int index = 0; index < 3; index++) {
                    MerchantOffer offer = getSelectedMerchantOffer();
                    if (index == 1 && (offer == null || offer.getCostB().isEmpty())) continue;
                    LegacyIconHolder iconHolder = ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (index == 2 ? 86 : 17), topPos +  (index == 0 ? 114 + (offer == null || offer.getCostB().isEmpty() ? 16 : 0) : index == 1 ? 144 : 130), 27,27,offer == null || index == 0 ? ItemStack.EMPTY : index == 1 ? offer.getCostB() : offer.getResult(),offer != null && displaySlotsWarning[index], Offset.ZERO);
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
                            graphics.blitSprite(DISCOUNT_STRIKETHRUOGH_SPRITE, -5, +12, 0, 9, 2);
                            graphics.pose().popPose();
                        }
                    }, iconHolder.getX(),iconHolder.getY(),iconHolder.isWarning());
                }

                super.renderSelection(graphics, i, j, f);
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
                    LegacyMerchantScreen.this.setFocused(merchantTradeButtons.get(i == 263 ? merchantTradeButtons.size() - 1 : 0));
                    ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(), 1.0f);
                    return true;
                }
                return super.keyPressed(i, j, k);
            }

            @Override
            public void setFocused(boolean bl) {
                if (bl){
                    selectedTrade = index;
                    updateSlotsDisplay();
                }
                super.setFocused(bl);
            }

            @Override
            public ResourceLocation getIconHolderSprite() {
                return isValidIndex() && ((LegacyMerchantOffer)menu.merchant.getOffers().get(index)).getRequiredLevel() > menu.merchantLevel ? LegacyIconHolder.GRAY_ICON_HOLDER : super.getIconHolderSprite();
            }
            @Override
            public boolean isWarning() {
                return super.isWarning() && isValidIndex() && ((LegacyMerchantOffer)menu.merchant.getOffers().get(index)).getRequiredLevel() <= menu.merchantLevel && !menu.merchant.getOffers().get(index).isOutOfStock();
            }

            private boolean isValidIndex(){
                return index < menu.merchant.getOffers().size();
            }

            @Override
            public void onPress() {
                if (isValidIndex() && isFocused() && index == selectedTrade) {
                    MerchantOffer offer = menu.merchant.getOffers().get(index);
                    if (((LegacyMerchantOffer)offer).getRequiredLevel() <= menu.merchantLevel && !offer.isOutOfStock() && !displaySlotsWarning[2]) {
                        Legacy4J.NETWORK.sendToServer(new ServerInventoryCraftPacket(ingredientsFromStacks(offer.getCostA(),offer.getCostB()),offer.getResult(),index,hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.bindingState.pressed));
                    }else ScreenUtil.playSimpleUISound(LegacySoundEvents.CRAFT_FAIL.get(),1.0f);
                }
            }
        });
    }

    public static List<Ingredient> ingredientsFromStacks(ItemStack... s){
        if (s.length == 0) return Collections.emptyList();
        List<Ingredient> ings = new ArrayList<>();
        for (ItemStack stack : s) {
            for (int i = 0; i < stack.getCount(); i++)
                ings.add(Legacy4JPlatform.getStrictNBTIngredient(stack));
        }
        return ings;
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
    public void repositionElements() {
        lastFocused = getFocused() instanceof LegacyIconHolder h ? merchantTradeButtons.indexOf(h) : -1;
        super.repositionElements();
    }
    public void init() {
        imageWidth = 291;
        imageHeight = 181;
        titleLabelX = (imageWidth - font.width(title)) / 2;
        titleLabelY = 12;
        inventoryLabelX = 125 + (153 - font.width(playerInventoryTitle))/2;
        inventoryLabelY = 87;
        super.init();
        updateSlotsDisplay();
        if (lastFocused >= 0 && lastFocused < merchantTradeButtons.size()) setInitialFocus(merchantTradeButtons.get(lastFocused));
        else setInitialFocus(merchantTradeButtons.get(0));
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
    protected void containerTick() {
        super.containerTick();
        if (!menu.merchant.getOffers().isEmpty() && !initOffers){
            initOffers = true;
            updateSlotsDisplay();
        }
    }

    @Override
    public void removed() {
        super.removed();
        menu.removeSlotListener(listener);
    }

    public MerchantOffer getSelectedMerchantOffer(){
        return selectedTrade < menu.merchant.getOffers().size()  ? menu.merchant.getOffers().get(selectedTrade) : null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        merchantTradeButtons.get(selectedTrade).renderSelection(guiGraphics,i,j,f);
        merchantTradeButtons.forEach(b-> b.renderTooltip(minecraft,guiGraphics,i,j));
        renderTooltip(guiGraphics,i,j);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        return super.mouseScrolled(d, e, f, g);
    }

    @Override
    public boolean mouseDragged(double d, double e, int i, double f, double g) {
        return super.mouseDragged(d, e, i, f, g);
    }
    private void renderProgressBar(GuiGraphics guiGraphics, int leftPos, int topPos) {
        int k = this.menu.merchantLevel;
        int l = this.menu.merchant.getVillagerXp();
        if (k >= 5) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 25,topPos + 28,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        guiGraphics.blitSprite(LegacySprites.EXPERIENCE_BAR_BACKGROUND, 0, 0, 0, 161, 4);
        int m = VillagerData.getMinXpPerLevel(k);
        if (l < m || !VillagerData.canLevelUp(k)) {
            return;
        }
        float f = 161.0f / (float)(VillagerData.getMaxXpPerLevel(k) - m);
        int o = Math.min(Mth.floor(f * (float)(l - m)), 161);
        guiGraphics.blitSprite(LegacySprites.EXPERIENCE_BAR_CURRENT, 161, 4, 0, 0, 0, 0, 0, o, 4);
        int p = getSelectedMerchantOffer() != null ? getSelectedMerchantOffer().getXp() : 0;
        if (p > 0) {
            int q = Math.min(Mth.floor((float)p * f), 161 - o);
            guiGraphics.blitSprite(LegacySprites.EXPERIENCE_BAR_RESULT, 161, 4, o, 0, o, 0, 0, q, 4);
        }
        guiGraphics.pose().popPose();
    }
    @Override
    public void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        int k = this.menu.merchantLevel;
        if (k > 0 && k <= 5 && this.menu.showProgressBar) {
            MutableComponent component = Component.translatable("merchant.title", this.title, Component.translatable("merchant.level." + k));
            guiGraphics.drawString(this.font, component, (imageWidth - this.font.width(component)) / 2, titleLabelY, 0x383838, false);
        } else {
            guiGraphics.drawString(this.font, this.title, titleLabelX, titleLabelY, 0x383838, false);
        }
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x383838, false);
        if (getSelectedMerchantOffer() != null && menu.showProgressBar) {
            int level = ((LegacyMerchantOffer)getSelectedMerchantOffer()).getRequiredLevel();
            if (level > 0) {
                Component c = Component.translatable("merchant.level." + level);
                guiGraphics.drawString(this.font, c, 13 + (105 - font.width(c)) / 2, 100, 0x383838, false);
            }
        }
    }

    @Override
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        ScreenUtil.renderPanel(guiGraphics,leftPos,topPos,imageWidth,imageHeight,2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics,leftPos + 11,topPos + 79,108,93,2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics,leftPos + 123,topPos + 79,156,93,2f);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 47,topPos + 131,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        guiGraphics.blitSprite(LegacySprites.ARROW,0,0,22,15);
        if (getSelectedMerchantOffer() != null && getSelectedMerchantOffer().isOutOfStock())
            guiGraphics.blitSprite(LegacySprites.ERROR_CROSS, 4, 0, 15, 15);
        guiGraphics.pose().popPose();
        if (getSelectedMerchantOffer() instanceof LegacyMerchantOffer o && o.getRequiredLevel() > menu.merchantLevel)
            guiGraphics.blitSprite(LegacySprites.PADLOCK, leftPos + 56,  topPos + 134, 16, 16);
        if (menu.showProgressBar)
            renderProgressBar(guiGraphics,leftPos,topPos);
    }
}
