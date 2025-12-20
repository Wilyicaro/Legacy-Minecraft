package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyGuiItemRenderer;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacyComponents;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.inventory.RecipeMenu;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.inventory.LegacyMerchantMenu;
import wily.legacy.inventory.LegacyMerchantOffer;
import wily.legacy.network.ServerMenuCraftPayload;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static wily.legacy.util.LegacySprites.DISCOUNT_STRIKETHRUOGH_SPRITE;

public class LegacyMerchantScreen extends RecipesScreen<LegacyMerchantMenu, LegacyIconHolder> {

    protected final boolean[] displaySlotsWarning = new boolean[3];
    boolean initOffers = false;

    public LegacyMerchantScreen(LegacyMerchantMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    protected void updateRecipes() {
        recipeButtonsOffset.max = Math.max(0, menu.merchant.getOffers().size() - 10);
        List<ItemStack> compactList = new ArrayList<>();
        RecipeMenu.handleCompactInventoryList(compactList, Minecraft.getInstance().player.getInventory(), menu.getCarried());
        recipeButtons.forEach(b -> {
            b.allowFocusedItemTooltip = true;
            int i = recipeButtonsOffset.get() + recipeButtons.indexOf(b);
            boolean warning = false;
            if (i < menu.merchant.getOffers().size()) {
                MerchantOffer offer = menu.merchant.getOffers().get(i);
                boolean matchesCostA = compactList.stream().anyMatch(item -> offer.satisfiedBy(item, offer.getCostB()) && item.getCount() >= offer.getCostA().getCount());
                boolean matchesCostB = offer.getCostB().isEmpty() || compactList.stream().anyMatch(item -> offer.satisfiedBy(offer.getCostA(), item) && item.getCount() >= offer.getCostB().getCount());
                warning = !matchesCostA || !matchesCostB;
                if (offer == getSelectedMerchantOffer()) {
                    displaySlotsWarning[0] = !matchesCostA;
                    displaySlotsWarning[1] = !matchesCostB;
                    displaySlotsWarning[2] = warning;
                }
            }
            b.setWarning(warning);
        });
    }

    @Override
    protected LegacyIconHolder createRecipeButton(int index) {
        LegacyIconHolder h = new LegacyIconHolder(27, 27) {

            @Override
            public @Nullable Component getAction(Context context) {
                return context.actionOfContext(KeyContext.class, c -> c.key() == InputConstants.KEY_RETURN && !displaySlotsWarning[2] && isValidIndex() && isFocused() && ((LegacyMerchantOffer) menu.merchant.getOffers().get(getIndex())).getRequiredLevel() <= menu.merchantLevel && !menu.merchant.getOffers().get(getIndex()).isOutOfStock() ? LegacyComponents.TRADE : null);
            }

            @Override
            public void render(GuiGraphics graphics, int i, int j, float f) {
                itemIcon = isValidIndex() ? menu.merchant.getOffers().get(getIndex()).getResult() : ItemStack.EMPTY;
                super.render(graphics, i, j, f);
                graphics.pose().pushMatrix();
                graphics.nextStratum();
                if (isValidIndex() && ((LegacyMerchantOffer) menu.merchant.getOffers().get(getIndex())).getRequiredLevel() > menu.merchantLevel) {
                    renderIcon(LegacySprites.PADLOCK, graphics, false, 16, 16);
                } else if (isValidIndex() && menu.merchant.getOffers().get(getIndex()).isOutOfStock()) {
                    renderIcon(LegacySprites.ERROR_CROSS, graphics, false, 15, 15);
                }
                graphics.pose().popMatrix();
            }

            @Override
            public void renderItem(GuiGraphics graphics, int i, int j, float f) {
                if (itemIcon.isEmpty()) return;
                LegacyGuiItemRenderer.secureTranslucentRender(isValidIndex() && menu.merchant.getOffers().get(getIndex()).isOutOfStock(), 0.5f, (u) -> super.renderItem(graphics, i, j, f));
            }

            @Override
            public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
                int xDiff = leftPos + accessor.getInteger("tradingGridPanel.x", 9);
                int yDiff = topPos + accessor.getInteger("bottomPanel.y", 79);
                int tradingSlotX = accessor.getInteger("tradingSlot.x", 8);
                int tradingSlotY = accessor.getInteger("tradingSlot.y", 51);
                int tradingSlotSize = accessor.getInteger("tradingSlot.size", 27);
                int firstTradingSlotY = accessor.getInteger("firstTradingSlot.y", 35);
                int secondTradingSlotY = accessor.getInteger("secondTradingSlot.y", 65);
                int resultSlotX = accessor.getInteger("resultSlot.x", 77);
                int resultSlotY = accessor.getInteger("tradingSlot.y", 51);

                for (int index = 0; index < 3; index++) {
                    MerchantOffer offer = getSelectedMerchantOffer();
                    if (index == 1 && (offer == null || offer.getCostB().isEmpty())) continue;
                    LegacyIconHolder iconHolder = LegacyRenderUtil.iconHolderRenderer.itemHolder(xDiff + (index == 2 ? resultSlotX : tradingSlotX), yDiff + (index == 0 ? (offer == null || offer.getCostB().isEmpty() ? tradingSlotY : firstTradingSlotY) : index == 1 ? secondTradingSlotY : resultSlotY), tradingSlotSize, tradingSlotSize, offer == null  ? ItemStack.EMPTY : index == 0 ? offer.getCostA() : index == 1 ? offer.getCostB() : offer.getResult(), offer != null && displaySlotsWarning[index], Vec2.ZERO);
                    if (index == 0) iconHolder.allowItemDecorations = false;
                    iconHolder.render(graphics, i, j, f);
                    iconHolder.renderTooltip(minecraft, graphics, i, j);
                    if (offer == null || index != 0) continue;

                    iconHolder.renderScaled(graphics, iconHolder.getX(), iconHolder.getY(), () -> {
                        ItemStack costA = offer.getCostA();
                        ItemStack baseCostA = offer.getBaseCostA();
                        if (baseCostA.getCount() == costA.getCount()) {
                            graphics.renderItemDecorations(font, costA, 0, 0);
                        } else {
                            graphics.renderItemDecorations(font, baseCostA, -12, 0, baseCostA.getCount() == 1 ? "1" : null);
                            graphics.renderItemDecorations(font, costA, 0, 0, costA.getCount() == 1 ? "1" : null);
                            FactoryGuiGraphics.of(graphics).blitSprite(DISCOUNT_STRIKETHRUOGH_SPRITE, -5, 12, 0, 9, 2);
                        }
                    });
                }

                super.renderSelection(graphics, i, j, f);
            }

            private int getIndex() {
                return recipeButtonsOffset.get() + index;
            }

            @Override
            public void setFocused(boolean bl) {
                super.setFocused(bl);
                if (bl) {
                    selectedRecipeButton = index;
                    updateRecipesAndResetTimer();
                }
            }

            @Override
            public boolean keyPressed(KeyEvent keyEvent) {
                if ((keyEvent.isLeft() && index == 0) || (keyEvent.isRight() && index == recipeButtons.size() - 1)) {
                    int oldOffset = recipeButtonsOffset.get();
                    recipeButtonsOffset.add(keyEvent.isLeft() ? -1 : 1, true);
                    if ((oldOffset == recipeButtonsOffset.max && keyEvent.isRight()) || (oldOffset == 0 && keyEvent.isLeft()))
                        LegacyMerchantScreen.this.setFocused(recipeButtons.get(keyEvent.isLeft() ? recipeButtons.size() - 1 : 0));
                    else {
                        scrollRenderer.updateScroll(keyEvent.isLeft() ? ScreenDirection.LEFT : ScreenDirection.RIGHT);
                        updateRecipesAndResetTimer();
                    }
                    LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
                    return true;
                }
                return super.keyPressed(keyEvent);
            }


            @Override
            public ResourceLocation getIconHolderSprite() {
                return isValidIndex() && ((LegacyMerchantOffer) menu.merchant.getOffers().get(getIndex())).getRequiredLevel() > menu.merchantLevel ? LegacySprites.GRAY_ICON_HOLDER : super.getIconHolderSprite();
            }

            @Override
            public boolean isWarning() {
                return super.isWarning() && isValidIndex() && ((LegacyMerchantOffer) menu.merchant.getOffers().get(getIndex())).getRequiredLevel() <= menu.merchantLevel && !menu.merchant.getOffers().get(getIndex()).isOutOfStock();
            }

            private boolean isValidIndex() {
                return getIndex() < menu.merchant.getOffers().size();
            }

            @Override
            public void onPress(InputWithModifiers input) {
                if (isValidIndex() && isFocused()) {
                    MerchantOffer offer = menu.merchant.getOffers().get(getIndex());
                    if (((LegacyMerchantOffer) offer).getRequiredLevel() <= menu.merchantLevel && !offer.isOutOfStock() && !displaySlotsWarning[2]) {
                        CommonNetwork.sendToServer(new ServerMenuCraftPayload(Collections.emptyList(), getIndex(), input.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.state().pressed));
                    } else LegacySoundUtil.playSimpleUISound(LegacyRegistries.CRAFT_FAIL.get(), 1.0f);
                }
            }
        };
        h.offset = new Vec2(0.5f, 0);
        return h;
    }

    public static MutableComponent getMerchantTile(Component title, int i) {
        return Component.translatable("merchant.title", title, Component.translatable("merchant.level." + i));
    }

    @Override
    public int getMaxRecipeButtons() {
        return accessor.getInteger("maxTradingButtonsCount", 10);
    }

    @Override
    protected void init() {
        imageWidth = 294;
        imageHeight = 181;
        LegacyFontUtil.applySDFont(b -> {
            titleLabelX = (imageWidth - font.width(title)) / 2;
            inventoryLabelX = accessor.getInteger("inventoryPanel.x", 126) + (accessor.getInteger("inventoryPanel.width", 157) - font.width(playerInventoryTitle)) / 2;
        });
        super.init();
        LegacySlotDisplay display = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return 16;
            }

            @Override
            public Vec2 getOffset() {
                return menu.inventoryOffset;
            }

            @Override
            public boolean isVisible() {
                return menu.inventoryActive;
            }
        };
        for (int i = 0; i < 36; i++) {
            Slot s = menu.slots.get(i);
            if (i < 27) {
                LegacySlotDisplay.override(s, 133 + (s.getContainerSlot() - 9) % 9 * 16, 98 + (s.getContainerSlot() - 9) / 9 * 16, display);
            } else {
                LegacySlotDisplay.override(s, 133 + s.getContainerSlot() * 16, 154, display);
            }
        }
        updateRecipesAndResetTimer();
        if (selectedRecipeButton < recipeButtons.size()) setFocused(recipeButtons.get(selectedRecipeButton));
        int tradingButtonsX = accessor.getInteger("tradingButtons.x", 13);
        int tradingButtonsY = accessor.getInteger("tradingButtons.y", 44);
        int tradingButtonsSize = accessor.getInteger("tradingButtons.size", 27);
        recipeButtons.forEach(holder -> {
            holder.width = holder.height = tradingButtonsSize;
            int i = recipeButtons.indexOf(holder);
            holder.setX(leftPos + tradingButtonsX + tradingButtonsSize * i);
            holder.setY(topPos + tradingButtonsY);
            addRenderableWidget(holder);
        });
    }

    public MerchantOffer getSelectedMerchantOffer() {
        return selectedRecipeButton < menu.merchant.getOffers().size() ? menu.merchant.getOffers().get(selectedRecipeButton) : null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        if (!menu.merchant.getOffers().isEmpty() && !initOffers) {
            initOffers = true;
            updateRecipesAndResetTimer();
        }
        super.render(guiGraphics, i, j, f);
    }

    protected void renderProgressBar(GuiGraphics guiGraphics) {
        int k = this.menu.merchantLevel;
        int l = this.menu.merchant.getVillagerXp();
        if (k >= 5) {
            return;
        }

        int width = accessor.getInteger("progressBar.width", 243);
        int height = accessor.getInteger("progressBar.height", 6);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(leftPos + accessor.getInteger("progressBar.x", (imageWidth - width) / 2), topPos + accessor.getInteger("progressBar.y", 28));
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.EXPERIENCE_BAR_BACKGROUND, 0, 0, 0, width, height);
        int m = VillagerData.getMinXpPerLevel(k);
        if (l < m || !VillagerData.canLevelUp(k)) {
            guiGraphics.pose().popMatrix();
            return;
        }
        float f = width / (float) (VillagerData.getMaxXpPerLevel(k) - m);
        int o = Math.min(Mth.floor(f * (float) (l - m)), width);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.EXPERIENCE_BAR_CURRENT, width, height, 0, 0, 0, 0, 0, o, height);
        int p = getSelectedMerchantOffer() != null ? getSelectedMerchantOffer().getXp() : 0;
        if (p > 0) {
            int q = Math.min(Mth.floor((float) p * f), width - o);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.EXPERIENCE_BAR_RESULT, width, height, o, 0, o, 0, 0, q, height);
        }
        guiGraphics.pose().popMatrix();
    }

    @Override
    public void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        LegacyFontUtil.applySDFont(b -> {
            int titleY = accessor.getInteger("title.y", 12);
            int k = this.menu.merchantLevel;
            if (k > 0 && k <= 5 && this.menu.showProgressBar) {
                MutableComponent component = getMerchantTile(title, k);
                guiGraphics.drawString(this.font, component, (imageWidth - this.font.width(component)) / 2, titleY, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            } else {
                guiGraphics.drawString(this.font, this.title, titleLabelX, titleY, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            }
            int bottomPanelY = accessor.getInteger("bottomPanel.y", 79);
            guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, bottomPanelY  + accessor.getInteger("inventoryTitle.y", 8), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            if (getSelectedMerchantOffer() != null && menu.showProgressBar) {
                int level = ((LegacyMerchantOffer) getSelectedMerchantOffer()).getRequiredLevel();
                if (level > 0) {
                    Component c = Component.translatable("merchant.level." + level);
                    int tradingGridPanelX = accessor.getInteger("tradingGridPanel.x", 12);
                    int tradingPanelWidth = accessor.getInteger("tradingGridPanel.width", 110);
                    guiGraphics.drawString(this.font, c, tradingGridPanelX + (tradingPanelWidth - font.width(c)) / 2, bottomPanelY + accessor.getInteger("levelTitle.y", 21), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                }
            }
        });
    }

    @Override
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        boolean sd = LegacyOptions.getUIMode().isSD();

        FactoryGuiGraphics.of(guiGraphics).blitSprite(accessor.getResourceLocation("imageSprite", LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        int bottomPanelHeight = accessor.getInteger("bottomPanel.height", 93);
        int tradingPanelWidth = accessor.getInteger("tradingGridPanel.width", 110);
        int bottomPanelY = accessor.getInteger("bottomPanel.y", 79);
        int tradingGridPanelX = accessor.getInteger("tradingGridPanel.x", 12);
        int inventoryPanelX = accessor.getInteger("inventoryPanel.x", 126);
        int inventoryPanelWidth = accessor.getInteger("inventoryPanel.width", 157);
        int tradingArrowX = accessor.getInteger("tradingArrow.x", 35);
        int tradingArrowY = accessor.getInteger("tradingArrow.y", 52);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + tradingGridPanelX, topPos + bottomPanelY, tradingPanelWidth, bottomPanelHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + inventoryPanelX, topPos + bottomPanelY, inventoryPanelWidth, bottomPanelHeight);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(leftPos + tradingGridPanelX + tradingArrowX, topPos + bottomPanelY + tradingArrowY);
        guiGraphics.pose().scale(1.5f, 1.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(sd ? LegacySprites.SMALL_ARROW : LegacySprites.ARROW, 0, 0, sd ? 16 : 22, sd ? 14 : 15);
        if (getSelectedMerchantOffer() != null && getSelectedMerchantOffer().isOutOfStock())
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ERROR_CROSS, 4, 0, 15, 15);
        guiGraphics.pose().popMatrix();
        if (getSelectedMerchantOffer() instanceof LegacyMerchantOffer o && o.getRequiredLevel() > menu.merchantLevel)
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PADLOCK, leftPos + tradingGridPanelX + tradingArrowX + 9, topPos + bottomPanelY + tradingArrowY + 3, 16, 16);
        if (accessor.getBoolean("showProgressBar", menu.showProgressBar))
            renderProgressBar(guiGraphics);

        renderRecipesScroll(guiGraphics, 5, 52);
    }
}
