package wily.legacy.mixin.base;

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
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
//?} else if >1.20.1 {
/*import net.minecraft.world.item.crafting.RecipeHolder;
*///?}
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
import wily.factoryapi.base.client.UIDefinition;
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

    @Redirect(method = "isScrollBarActive",at = @At(value = "INVOKE",target = /*? if <1.21.2 {*//*"Lnet/minecraft/world/inventory/StonecutterMenu;getNumRecipes()I"*//*?} else {*/"Lnet/minecraft/world/inventory/StonecutterMenu;getNumberOfVisibleRecipes()I"/*?}*/))
    private int isScrollBarActive(StonecutterMenu instance){
        return getNumRecipes() - 4;
    }
    @Inject(method = "getOffscreenRows",at = @At("HEAD"), cancellable = true)
    private void getOffscreenRows(CallbackInfoReturnable<Integer> cir){
        cir.setReturnValue(Math.max(0,getNumRecipes() / 4 - 4));
    }

    @Unique
    private int getNumRecipes(){
        return menu./*? if <1.21.2 {*//*getNumRecipes*//*?} else {*/getNumberOfVisibleRecipes/*?}*/();
    }

    @Unique
    private List</*? if <1.20.2 {*//*StonecutterRecipe*//*?} else if <1.21.2 {*//*RecipeHolder<StonecutterRecipe>*//*?} else {*/SelectableRecipe.SingleInputEntry<StonecutterRecipe>/*?}*/> getRecipes(){
        return menu./*? if <1.21.2 {*//*getRecipes*//*?} else {*/getVisibleRecipes().entries/*?}*/();
    }

    @Unique
    private ItemStack getResultItem(/*? if <1.20.2 {*//*StonecutterRecipe*//*?} else if <1.21.2 {*//*RecipeHolder<StonecutterRecipe>*//*?} else {*/SelectableRecipe.SingleInputEntry<StonecutterRecipe>/*?}*/ recipe){
        return /*? if <1.21.2 {*//*recipe/^? if >1.20.1 {^/.value()/^?}^/.getResultItem(this.minecraft.level.registryAccess())*//*?} else {*/recipe.recipe().optionDisplay().resolveForFirstStack(SlotDisplayContext.fromLevel(this.minecraft.level))/*?}*/;
    }

    @Override
    public void init() {
        imageWidth = 215;
        imageHeight = 208;
        inventoryLabelX = 14;
        inventoryLabelY = 95;
        titleLabelX = 14;
        titleLabelY = 10;
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, 31, 45,new LegacySlotDisplay(){
                    public int getWidth() {
                        return 23;
                    }
                });
            } else if (i == 1) {
                LegacySlotDisplay.override(s, 166, 41,new LegacySlotDisplay(){
                    public int getWidth() {
                        return 32;
                    }
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, 14 + (s.getContainerSlot() - 9) % 9 * 21,108 + (s.getContainerSlot() - 9) / 9 * 21);
            } else {
                LegacySlotDisplay.override(s, 14 + s.getContainerSlot() * 21,178);
            }
        }
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 70,  topPos+ 18, 75, 75);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 148.5, topPos + 18, 0f);
        if (isScrollBarActive() && getOffscreenRows() > 0) {
            if (getOffscreenRows() != startIndex)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN, 0, 79);
            if (startIndex > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.UP,0,-11);
        }else FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,0.5f);
        RenderSystem.enableBlend();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,0, 0,13,75);
        guiGraphics.pose().translate(-2f, -1f + (this.isScrollBarActive() ?  61.5f * startIndex / getOffscreenRows() : 0), 0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL,0,0, 16,16);
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,1.0f);
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 71.5f,topPos + 19.5f,0);
        if (this.displayRecipes) {
            int size = getRecipes().size();
            block0: for (int p = 0; p < 4; ++p) {
                for (int q = 0; q < 4; ++q) {
                    int r = p + this.startIndex;
                    int s = r * 4 + q;
                    if (s >= size) break block0;
                    int t = q * 18;
                    int u = p * 18;
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(s == menu.getSelectedRecipeIndex() ? BUTTON_SLOT_SELECTED : (ScreenUtil.isMouseOver(i,j,leftPos + 73.5f + t,topPos + 19.5f + u,18,18)? BUTTON_SLOT_HIGHLIGHTED : BUTTON_SLOT), t, u, 18, 18);
                    guiGraphics.renderItem(getResultItem(getRecipes().get(s)), 1 + t, 1 + u);
                }
            }
        }
        guiGraphics.pose().popPose();
    }
    @Inject(method = "mouseClicked",at = @At("HEAD"), cancellable = true)
    public void mouseClicked(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        this.scrolling = false;
        if (this.displayRecipes) {
            double j = this.leftPos + 71.5;
            double k = this.topPos + 19.5;
            for (int m = this.startIndex; m < startIndex + 16; ++m) {
                int n = m - this.startIndex;
                double f = d - (j + n % 4 * 18);
                double g = e - (k + n / 4 * 18);
                if (!(f >= 0.0) || !(g >= 0.0) || !(f < 18.0) || !(g < 18.0) || !this.menu.clickMenuButton(this.minecraft.player, m)) continue;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0f));
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, m);
                cir.setReturnValue(true);
                return;
            }
            if (ScreenUtil.isMouseOver(d,e,leftPos + 148.5,topPos + 18,13,75)) this.scrolling = true;
        }
        cir.setReturnValue(super.mouseClicked(d, e, i));
    }
    @Inject(method = "renderTooltip",at = @At("HEAD"), cancellable = true)
    public void renderTooltip(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        ci.cancel();
        super.renderTooltip(guiGraphics, i, j);
        if (this.displayRecipes) {
            int size = getRecipes().size();
            block0: for (int p = 0; p < 4; ++p) {
                for (int q = 0; q < 4; ++q) {
                    int r = p + this.startIndex;
                    int s = r * 4 + q;
                    if (s >= size) break block0;
                    if (ScreenUtil.isMouseOver(i,j,leftPos + 73.5f + q * 18,topPos + 19.5f + p * 18,18,18)) guiGraphics.renderTooltip(this.font, getResultItem(getRecipes().get(s)), i, j);
                }
            }
        }
    }
    @Inject(method = "mouseDragged",at = @At("HEAD"), cancellable = true)
    public void mouseDragged(double d, double e, int i, double f, double g, CallbackInfoReturnable<Boolean> cir) {
        if (this.scrolling && this.displayRecipes && isScrollBarActive()) {
            int oldIndex = startIndex;
            this.startIndex = (int) Math.max(Math.round(getOffscreenRows() * Math.min(1,(e - (topPos + 18)) / 75)), 0) * 4;
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
