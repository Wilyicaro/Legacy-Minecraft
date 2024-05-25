package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.CreativeModeScreen;
import wily.legacy.client.screen.ReplaceableScreen;
import wily.legacy.util.ScreenUtil;

import static wily.legacy.util.LegacySprites.SMALL_ARROW;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> implements ReplaceableScreen {
    @Shadow @Final private RecipeBookComponent recipeBookComponent;

    @Shadow private boolean widthTooNarrow;

    private ImageButton recipeButton;
    private boolean canReplace = true;

    public InventoryScreenMixin(InventoryMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }
    private boolean hasClassicCrafting(){
        return ((LegacyOptions) Minecraft.getInstance().options).classicCrafting().get();
    }
    @Inject(method = "containerTick",at = @At("HEAD"), cancellable = true)
    public void containerTick(CallbackInfo ci) {
        ci.cancel();
        if (canReplace()) {
            this.minecraft.setScreen(getReplacement());
        } else {
            this.recipeBookComponent.tick();
        }
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        if (canReplace()) {
            this.minecraft.setScreen(getReplacement());
        }else {
            imageWidth = 215;
            imageHeight = 217;
            inventoryLabelX = 14;
            inventoryLabelY = 103;
            super.init();
            this.widthTooNarrow = this.width < 379;
            this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
            if (((LegacyOptions) minecraft.options).showVanillaRecipeBook().get() &&
                    hasClassicCrafting()) {
                this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
                recipeButton = this.addRenderableWidget(new ImageButton(this.leftPos + 180, topPos + 71, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, (button) -> {
                    this.recipeBookComponent.toggleVisibility();
                    this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
                    button.setPosition(this.leftPos + 180, topPos + 71);
                }));
                if (recipeBookComponent.isVisible()) recipeButton.setFocused(true);
            } else if (recipeBookComponent.isVisible()) recipeBookComponent.toggleVisibility();
        }
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics graphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        ScreenUtil.renderPanel(graphics,leftPos,topPos,imageWidth,imageHeight,2f);
        ScreenUtil.renderEntityPanel(graphics,leftPos + 40 + (hasClassicCrafting() ? 0 : 50),topPos + 13,63,84,2);
        Pose pose = minecraft.player.getPose();
        minecraft.player.setPose(Pose.STANDING);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,leftPos + 40 + (hasClassicCrafting() ? 0 : 50),topPos + 13,leftPos + 103 + (hasClassicCrafting() ? 0 : 50),topPos + 97,35,0.0625f,i,j, minecraft.player);
        minecraft.player.setPose(pose);
        if (hasClassicCrafting()) {
            graphics.drawString(this.font, this.title, leftPos + 111, topPos + 16, 0x383838, false);
            graphics.blitSprite(SMALL_ARROW,leftPos + 158,topPos + 43,16,13);
        }
        if (!recipeBookComponent.isVisible() && recipeButton != null && !recipeButton.isHovered()) recipeButton.setFocused(false);
    }
    @Inject(method = "renderLabels",at = @At("HEAD"), cancellable = true)
    public void renderLabels(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        ci.cancel();
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x383838, false);
    }

    public boolean canReplace() {
        return this.minecraft.gameMode.hasInfiniteItems() && canReplace;
    }

    public void setCanReplace(boolean canReplace) {
        this.canReplace = canReplace;
    }

    public Screen getReplacement() {
        return CreativeModeScreen.getActualCreativeScreenInstance(minecraft);
    }
}
