package wily.legacy.mixin.base.client.stonecutter;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.2 {
/*import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
*///?} else if >1.20.1 {
import net.minecraft.world.item.crafting.RecipeHolder;
//?}
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.LegacyScrollRenderer;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.List;

import static wily.legacy.util.LegacySprites.*;

@Mixin(StonecutterScreen.class)
public abstract class StonecutterScreenMixin extends AbstractContainerScreen<StonecutterMenu> {

    private LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();

    @Shadow private int startIndex;

    @Shadow protected abstract int getOffscreenRows();

    @Shadow protected abstract boolean isScrollBarActive();

    @Shadow private boolean displayRecipes;

    @Shadow private boolean scrolling;

    public StonecutterScreenMixin(StonecutterMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
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

    @Redirect(method = "isScrollBarActive",at = @At(value = "INVOKE",target = /*? if <1.21.2 {*/"Lnet/minecraft/world/inventory/StonecutterMenu;getNumRecipes()I"/*?} else {*//*"Lnet/minecraft/world/inventory/StonecutterMenu;getNumberOfVisibleRecipes()I"*//*?}*/))
    private int isScrollBarActive(StonecutterMenu instance){
        return getNumRecipes() - 4;
    }
    @Inject(method = "getOffscreenRows",at = @At("HEAD"), cancellable = true)
    private void getOffscreenRows(CallbackInfoReturnable<Integer> cir){
        cir.setReturnValue(Math.max(0,getNumRecipes() / 4 - 4));
    }

    @Unique
    private int getNumRecipes(){
        return menu./*? if <1.21.2 {*/getNumRecipes/*?} else {*//*getNumberOfVisibleRecipes*//*?}*/();
    }

    @Unique
    private List</*? if <1.20.2 {*//*StonecutterRecipe*//*?} else if <1.21.2 {*/RecipeHolder<StonecutterRecipe>/*?} else {*//*SelectableRecipe.SingleInputEntry<StonecutterRecipe>*//*?}*/> getRecipes(){
        return menu./*? if <1.21.2 {*/getRecipes/*?} else {*//*getVisibleRecipes().entries*//*?}*/();
    }

    @Unique
    private ItemStack getResultItem(/*? if <1.20.2 {*//*StonecutterRecipe*//*?} else if <1.21.2 {*/RecipeHolder<StonecutterRecipe>/*?} else {*//*SelectableRecipe.SingleInputEntry<StonecutterRecipe>*//*?}*/ recipe){
        return /*? if <1.21.2 {*/recipe/*? if >1.20.1 {*/.value()/*?}*/.getResultItem(this.minecraft.level.registryAccess())/*?} else {*//*recipe.recipe().optionDisplay().resolveForFirstStack(SlotDisplayContext.fromLevel(this.minecraft.level))*//*?}*/;
    }

    @Override
    public void init() {
        boolean sd = LegacyOptions.getUIMode().isSD();
        imageWidth = sd ? 130 : 215;
        imageHeight = sd ? 135 : 208;
        inventoryLabelX = sd ? 7 : 14;
        inventoryLabelY = sd ? 63 : 95;
        titleLabelX = sd ? 7 : 14;
        titleLabelY = sd ? 5 : 10;
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
                LegacySlotDisplay.override(s, sd ? 12 : 31, sd ? 30 : 45,new LegacySlotDisplay(){
                    public int getWidth() {
                        return sd ? 13 : 23;
                    }
                });
            } else if (i == 1) {
                LegacySlotDisplay.override(s, sd ? 102 : 166, sd ? 27 : 41,new LegacySlotDisplay(){
                    public int getWidth() {
                        return sd ? 21 : 32;
                    }
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 72 : 108) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsSize, sd ? 116 : 178, defaultDisplay);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        ScreenUtil.applySDFont(ignored -> super.renderLabels(guiGraphics, i, j));
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        int panelSize = sd ? 51 : 75;
        int panelX = leftPos + (sd ? 32 : 70);
        int panelY = topPos + (sd ? 12 : 18);
        int buttonSize = sd ? 12 : 18;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getElementValue("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,panelX, panelY, panelSize, panelSize);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(panelX + panelSize + 2.5f, panelY, 0f);
        if (isScrollBarActive() && getOffscreenRows() > 0) {
            if (getOffscreenRows() != startIndex)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN, 0, panelSize + 4);
            if (startIndex > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.UP,0,-11);
        }else FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,0.5f);
        FactoryScreenUtil.enableBlend();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,0, 0,13,panelSize);
        guiGraphics.pose().translate(-2f, -1f + (this.isScrollBarActive() && getOffscreenRows() > 0 ?  (panelSize - 13.5f) * startIndex / getOffscreenRows() : 0), 0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL,0,0, 16,16);
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,1.0f);
        FactoryScreenUtil.disableBlend();
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(panelX + 1.5f,panelY + 1.5f,0);
        if (this.displayRecipes) {
            int size = getRecipes().size();
            block0: for (int p = 0; p < 4; ++p) {
                for (int q = 0; q < 4; ++q) {
                    int r = p + this.startIndex;
                    int s = r * 4 + q;
                    if (s >= size) break block0;
                    int t = q * buttonSize;
                    int u = p * buttonSize;
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(s == menu.getSelectedRecipeIndex() ? BUTTON_SLOT_SELECTED : (ScreenUtil.isMouseOver(i,j,panelX + 1.5f + t,panelY + 1.5f + u,buttonSize,buttonSize)? BUTTON_SLOT_HIGHLIGHTED : BUTTON_SLOT), t, u, buttonSize, buttonSize);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(t, u, 0);
                    guiGraphics.pose().scale(buttonSize / 18.0f, buttonSize / 18.0f, 1.0f);
                    guiGraphics.renderItem(getResultItem(getRecipes().get(s)), 1, 1);
                    guiGraphics.pose().popPose();
                }
            }
        }
        guiGraphics.pose().popPose();
    }
    @Inject(method = "mouseClicked",at = @At("HEAD"), cancellable = true)
    public void mouseClicked(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        this.scrolling = false;
        if (this.displayRecipes) {
            boolean sd = LegacyOptions.getUIMode().isSD();
            int panelSize = sd ? 51 : 75;
            int panelX = leftPos + (sd ? 32 : 70);
            int panelY = topPos + (sd ? 12 : 18);
            int buttonSize = sd ? 12 : 18;
            double j = panelX + 1.5;
            double k = panelY + 1.5;
            for (int m = this.startIndex; m < startIndex + 16; ++m) {
                int n = m - this.startIndex;
                double f = d - (j + n % 4 * buttonSize);
                double g = e - (k + n / 4 * buttonSize);
                if (!(f >= 0.0) || !(g >= 0.0) || !(f < buttonSize) || !(g < buttonSize) || !this.menu.clickMenuButton(this.minecraft.player, m)) continue;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0f));
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, m);
                cir.setReturnValue(true);
                return;
            }
            if (ScreenUtil.isMouseOver(d,e,panelX + panelSize + 2.5f,panelY,13,panelSize)) this.scrolling = true;
        }
        cir.setReturnValue(super.mouseClicked(d, e, i));
    }
    @Inject(method = "renderTooltip",at = @At("HEAD"), cancellable = true)
    public void renderTooltip(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        ci.cancel();
        super.renderTooltip(guiGraphics, i, j);
        if (this.displayRecipes) {
            int size = getRecipes().size();
            boolean sd = LegacyOptions.getUIMode().isSD();
            int panelX = leftPos + (sd ? 32 : 70);
            int panelY = topPos + (sd ? 12 : 18);
            int buttonSize = sd ? 12 : 18;
            block0: for (int p = 0; p < 4; ++p) {
                for (int q = 0; q < 4; ++q) {
                    int r = p + this.startIndex;
                    int s = r * 4 + q;
                    if (s >= size) break block0;
                    if (ScreenUtil.isMouseOver(i,j,panelX + 1.5f + q * buttonSize,panelY + 1.5f + p * buttonSize,buttonSize,buttonSize)) guiGraphics.renderTooltip(this.font, getResultItem(getRecipes().get(s)), i, j);
                }
            }
        }
    }
    @Inject(method = "mouseDragged",at = @At("HEAD"), cancellable = true)
    public void mouseDragged(double d, double e, int i, double f, double g, CallbackInfoReturnable<Boolean> cir) {
        if (this.scrolling && this.displayRecipes && isScrollBarActive()) {
            boolean sd = LegacyOptions.getUIMode().isSD();
            int panelSize = sd ? 51 : 75;
            int panelY = topPos + (sd ? 12 : 18);
            int oldIndex = startIndex;
            this.startIndex = (int) Math.max(Math.round(getOffscreenRows() * Math.min(1,(e - panelY) / panelSize)), 0) * 4;
            if (oldIndex != startIndex){
                scrollRenderer.updateScroll(oldIndex - startIndex > 0 ? ScreenDirection.UP : ScreenDirection.DOWN);
            }
            cir.setReturnValue(true);
            return;
        }
        cir.setReturnValue(super.mouseDragged(d, e, i, f, g));
    }
    @Redirect(method = "mouseScrolled",at = @At(value = "FIELD",target = "Lnet/minecraft/client/gui/screens/inventory/StonecutterScreen;startIndex:I"))
    private void mouseDragged(StonecutterScreen instance, int value){
        if (startIndex!= value){
            scrollRenderer.updateScroll(startIndex - value > 0 ? ScreenDirection.UP : ScreenDirection.DOWN);
            startIndex = value;
        }
    }
}
