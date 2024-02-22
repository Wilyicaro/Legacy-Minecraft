package wily.legacy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.LegacySprites;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.inventory.LegacyMerchantOffer;
import wily.legacy.network.ServerInventoryCraftPacket;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
    @Shadow private int shopItem;
    @Shadow protected abstract void postButtonClick();

    private int lastFocused = -1;
    protected boolean[] displaySlotsWarning;

    protected List<LegacyIconHolder> villagerTradeButtons;

    private ContainerListener listener;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci){
        listener = new ContainerListener() {
            public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
                updateSlotsDisplay();
            }
            public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int j) {

            }
        };
        displaySlotsWarning = new boolean[3];
        villagerTradeButtons = new ArrayList<>();
        for (int index = 0; index < 10; index++)
            addTradeButton(index);
    }
    private void updateSlotsDisplay(){
        List<ItemStack> compactList = new ArrayList<>();
        for (int i1 = 0; i1 < 39; i1++) {
            if (i1 == 2)  continue;
            Slot s = menu.slots.get(i1);
            if (s.getItem().isEmpty()) continue;
            ItemStack item = s.getItem();
            compactList.stream().filter(i->ItemStack.isSameItemSameTags(i,item)).findFirst().ifPresentOrElse(i-> i.grow(item.getCount()), ()-> compactList.add(item.copy()));
        }
        villagerTradeButtons.forEach(b->{
            int i =villagerTradeButtons.indexOf(b);
            boolean warning = false;
            if (i < menu.getOffers().size()) {
                MerchantOffer offer = menu.getOffers().get(i);
                boolean matchesCostA = compactList.stream().anyMatch(item -> offer.isRequiredItem(item, offer.getCostA()) && item.getCount() >= offer.getCostA().getCount());
                boolean matchesCostB = offer.getCostB().isEmpty() || compactList.stream().anyMatch(item -> offer.isRequiredItem(item, offer.getCostB()) && item.getCount() >= offer.getCostB().getCount());
                warning = !matchesCostA || !matchesCostB;
                if (i == shopItem){
                    displaySlotsWarning[0] = !matchesCostA;
                    displaySlotsWarning[1] = !matchesCostB;
                    displaySlotsWarning[2] = warning;
                }
            }
            b.setWarning(warning);
        });
    }

    public MerchantScreenMixin(MerchantMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }
    protected void addTradeButton(int index){
        villagerTradeButtons.add(new LegacyIconHolder(27,27){
            @Override
            public void render(GuiGraphics graphics, int i, int j, float f) {
                if (isValidIndex())
                    itemIcon = menu.getOffers().get(index).getResult();
                super.render(graphics, i, j, f);
                if (isValidIndex() && ((LegacyMerchantOffer)menu.getOffers().get(index)).getRequiredLevel() > menu.getTraderLevel()) {
                    RenderSystem.disableDepthTest();
                    renderIcon(LegacySprites.PADLOCK_SPRITE, graphics, false, 16, 16);
                    RenderSystem.enableDepthTest();
                }
            }

            @Override
            public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
                MerchantOffer offer = getSelectedMerchantOffer();
                if (offer != null) {
                    if (hasAutoCrafting()) {
                        for (int index = 0; index < 3; index++) {
                            Slot s = menu.slots.get(index);
                            if (s.hasItem()) break;
                            LegacyIconHolder iconHolder = ScreenUtil.iconHolderRenderer.slotBounds(leftPos, topPos, s);
                            iconHolder.itemIcon = index == 0 ? offer.getCostA() : index == 1 ? offer.getCostB() : offer.getResult();
                            iconHolder.setWarning(displaySlotsWarning[index]);
                            iconHolder.render(graphics, i, j, f);
                        }
                    }
                }
                super.renderSelection(graphics, i, j, f);
            }

            @Override
            public void renderTooltip(Minecraft minecraft, GuiGraphics graphics, int i, int j) {
                super.renderTooltip(minecraft, graphics, i, j);
                if (!isFocused()) return;
                MerchantOffer offer = getSelectedMerchantOffer();
                if (offer != null && hasAutoCrafting())
                    for (int index = 0; index < 3; index++) {
                        Slot s = menu.slots.get(index);
                        if (ScreenUtil.isHovering(s,leftPos,topPos,i,j)) renderTooltip(minecraft,graphics,index == 0 ? offer.getCostA() : index == 1 ? offer.getCostB() : offer.getResult(), i, j);
                    }
            }

            @Override
            public boolean keyPressed(int i, int j, int k) {
                if ((i == 263 && index == 0) || (i == 262 && index == villagerTradeButtons.size() - 1)){
                    MerchantScreenMixin.this.setFocused(villagerTradeButtons.get(i == 263 ? villagerTradeButtons.size() - 1 : 0));
                    ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(), 1.0f);
                    return true;
                }
                return super.keyPressed(i, j, k);
            }

            @Override
            public void setFocused(boolean bl) {
                if (bl){
                    shopItem = index;
                    updateSlotsDisplay();
                }
                super.setFocused(bl);
            }

            @Override
            public ResourceLocation getIconHolderSprite() {
                return isValidIndex() && ((LegacyMerchantOffer)menu.getOffers().get(index)).getRequiredLevel() > menu.getTraderLevel() ? LegacyIconHolder.GRAY_ICON_HOLDER : super.getIconHolderSprite();
            }
            @Override
            protected boolean isWarning() {
                return super.isWarning() && ((LegacyMerchantOffer)menu.getOffers().get(index)).getRequiredLevel() <= menu.getTraderLevel();
            }

            private boolean isValidIndex(){
                return index < menu.getOffers().size();
            }

            @Override
            public void onPress() {
                if (isValidIndex() && isFocused() && index == shopItem) {
                    MerchantOffer offer = menu.getOffers().get(index);
                    if (((LegacyMerchantOffer)offer).getRequiredLevel() <= menu.getTraderLevel() && !offer.isOutOfStock() && !displaySlotsWarning[2]) {
                        if (hasAutoCrafting()) {
                            LegacyMinecraft.NETWORK.sendToServer(new ServerInventoryCraftPacket(ingredientsFromStacks(offer.getCostA(),offer.getCostB()),offer.getResult(),index, 3, 39));
                        } else {
                            postButtonClick();
                        }
                    }else ScreenUtil.playSimpleUISound(LegacySoundEvents.CRAFT_FAIL.get(),1.0f);
                }
            }
        });
    }
    private boolean hasAutoCrafting(){
        return !menu.getSlot(0).hasItem() && !menu.getSlot(1).hasItem();
    }

    private List<Ingredient> ingredientsFromStacks(ItemStack... s){
        if (s.length == 0) return Collections.emptyList();
        List<Ingredient> ings = new ArrayList<>();
        for (ItemStack stack : s) {
            for (int i = 0; i < stack.getCount(); i++)
                ings.add(Ingredient.of(stack));
        }
        return ings;
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
    public void repositionElements() {
        lastFocused = getFocused() instanceof LegacyIconHolder h ? villagerTradeButtons.indexOf(h) : -1;
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
        if (lastFocused >= 0 && lastFocused < villagerTradeButtons.size()) setInitialFocus(villagerTradeButtons.get(lastFocused));
        else setInitialFocus(villagerTradeButtons.get(0));
        villagerTradeButtons.forEach(holder->{
            int i = villagerTradeButtons.indexOf(holder);
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
        if (!menu.getOffers().isEmpty() && !initOffers){
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
        return shopItem < menu.getOffers().size()  ? menu.getOffers().get(shopItem) : null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        villagerTradeButtons.get(shopItem).renderSelection(guiGraphics,i,j,f);
        villagerTradeButtons.forEach(b-> b.renderTooltip(minecraft,guiGraphics,i,j));
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
        int k = this.menu.getTraderLevel();
        int l = this.menu.getTraderXp();
        if (k >= 5) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 25,topPos + 28,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        guiGraphics.blitSprite(LegacySprites.EXPERIENCE_BAR_BACKGROUND_SPRITE, 0, 0, 0, 161, 4);
        int m = VillagerData.getMinXpPerLevel(k);
        if (l < m || !VillagerData.canLevelUp(k)) {
            return;
        }
        float f = 161.0f / (float)(VillagerData.getMaxXpPerLevel(k) - m);
        int o = Math.min(Mth.floor(f * (float)(l - m)), 161);
        guiGraphics.blitSprite(LegacySprites.EXPERIENCE_BAR_CURRENT_SPRITE, 161, 4, 0, 0, 0, 0, 0, o, 4);
        int p = this.menu.getFutureTraderXp() > 0 ? menu.getFutureTraderXp() :  getSelectedMerchantOffer() != null ? getSelectedMerchantOffer().getXp() : 0;
        if (p > 0) {
            int q = Math.min(Mth.floor((float)p * f), 161 - o);
            guiGraphics.blitSprite(LegacySprites.EXPERIENCE_BAR_RESULT_SPRITE, 161, 4, o, 0, o, 0, 0, q, 4);
        }
        guiGraphics.pose().popPose();
    }
    @Override
    public void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        int k = this.menu.getTraderLevel();
        if (k > 0 && k <= 5 && this.menu.showProgressBar()) {
            MutableComponent component = Component.translatable("merchant.title", this.title, Component.translatable("merchant.level." + k));
            guiGraphics.drawString(this.font, component, (imageWidth - this.font.width(component)) / 2, titleLabelY, 0x404040, false);
        } else {
            guiGraphics.drawString(this.font, this.title, titleLabelX, titleLabelY, 0x404040, false);
        }
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        if (getSelectedMerchantOffer() != null && menu.showProgressBar()) {
            int level = ((LegacyMerchantOffer)getSelectedMerchantOffer()).getRequiredLevel();
            if (level > 0) {
                Component c = Component.translatable("merchant.level." + level);
                guiGraphics.drawString(this.font, c, 13 + (105 - font.width(c)) / 2, 100, 0x404040, false);
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
        guiGraphics.blitSprite(LegacySprites.ARROW_SPRITE,0,0,22,15);
        if (getSelectedMerchantOffer() != null && getSelectedMerchantOffer().isOutOfStock())
            guiGraphics.blitSprite(LegacySprites.ERROR_CROSS_SPRITE, 4, 0, 15, 15);
        guiGraphics.pose().popPose();
        if (getSelectedMerchantOffer() instanceof LegacyMerchantOffer o && o.getRequiredLevel() > menu.getTraderLevel())
            guiGraphics.blitSprite(LegacySprites.PADLOCK_SPRITE, leftPos + 56,  topPos + 134, 16, 16);
        if (menu.showProgressBar())
            renderProgressBar(guiGraphics,leftPos,topPos);
    }
}
