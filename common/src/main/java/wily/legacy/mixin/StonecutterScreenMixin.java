package wily.legacy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.screen.LegacyScrollRenderer;
import wily.legacy.util.ScreenUtil;

import java.util.List;

import static wily.legacy.util.LegacySprites.*;

@Mixin(StonecutterScreen.class)
public abstract class StonecutterScreenMixin extends AbstractContainerScreen<StonecutterMenu> {

    private LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();

    @Shadow protected abstract void renderButtons(GuiGraphics guiGraphics, int i, int j, int k, int l, int m);

    @Shadow protected abstract void renderRecipes(GuiGraphics guiGraphics, int i, int j, int k);

    @Shadow private int startIndex;

    @Shadow protected abstract int getOffscreenRows();

    @Shadow protected abstract boolean isScrollBarActive();

    @Shadow private boolean displayRecipes;

    @Shadow private boolean scrolling;

    public StonecutterScreenMixin(StonecutterMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics,f,i,j);
    }
    @Redirect(method = "isScrollBarActive",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/StonecutterMenu;getNumRecipes()I"))
    private int isScrollBarActive(StonecutterMenu instance){
        return instance.getNumRecipes() - 4;
    }
    @Inject(method = "getOffscreenRows",at = @At("HEAD"), cancellable = true)
    private void getOffscreenRows(CallbackInfoReturnable<Integer> cir){
        cir.setReturnValue(Math.max(0,menu.getNumRecipes() / 4 - 4));
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
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        ScreenUtil.renderPanel(guiGraphics,leftPos,topPos,imageWidth,imageHeight,2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics,leftPos + 70,  topPos+ 18, 75, 75,2f);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 148.5, topPos + 18, 0f);
        if (isScrollBarActive() && getOffscreenRows() > 0) {
            if (getOffscreenRows() != startIndex)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN, 0, 79);
            if (startIndex > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.UP,0,-11);
        }else guiGraphics.setColor(1.0f,1.0f,1.0f,0.5f);
        RenderSystem.enableBlend();
        ScreenUtil.renderSquareRecessedPanel(guiGraphics,0, 0,13,75,2f);
        guiGraphics.pose().translate(-2f, -1f + (this.isScrollBarActive() ?  61.5f * startIndex / getOffscreenRows() : 0), 0f);
        ScreenUtil.renderPanel(guiGraphics,0,0, 16,16,3f);
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 71.5f,topPos + 19.5f,0);
        if (this.displayRecipes) {
            List<RecipeHolder<StonecutterRecipe>> list = this.menu.getRecipes();
            block0: for (int p = 0; p < 4; ++p) {
                for (int q = 0; q < 4; ++q) {
                    int r = p + this.startIndex;
                    int s = r * 4 + q;
                    if (s >= list.size()) break block0;
                    int t = q * 18;
                    int u = p * 18;
                    guiGraphics.blitSprite(s == menu.getSelectedRecipeIndex() ? BUTTON_SLOT_SELECTED : (ScreenUtil.isMouseOver(i,j,leftPos + 73.5f + t,topPos + 19.5f + u,18,18)? BUTTON_SLOT_HIGHLIGHTED : BUTTON_SLOT), t, u, 18, 18);
                    guiGraphics.renderItem(list.get(s).value().getResultItem(this.minecraft.level.registryAccess()), 1 + t, 1 + u);
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
            List<RecipeHolder<StonecutterRecipe>> list = this.menu.getRecipes();
            block0: for (int p = 0; p < 4; ++p) {
                for (int q = 0; q < 4; ++q) {
                    int r = p + this.startIndex;
                    int s = r * 4 + q;
                    if (s >= list.size()) break block0;
                    if (ScreenUtil.isMouseOver(i,j,leftPos + 73.5f + q * 18,topPos + 19.5f + p * 18,18,18)) guiGraphics.renderTooltip(this.font, list.get(s).value().getResultItem(this.minecraft.level.registryAccess()), i, j);
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
