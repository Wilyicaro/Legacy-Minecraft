package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.ReplaceableScreen;
import wily.legacy.util.ScreenUtil;

import static wily.legacy.LegacyMinecraftClient.SMALL_ARROW_SPRITE;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> implements ReplaceableScreen {
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
    public void containerTick() {
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
    @Override
    public void init() {
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

    @Override
    public void renderBg(GuiGraphics graphics, float f, int i, int j) {
        ScreenUtil.renderPanel(graphics,leftPos,topPos,imageWidth,imageHeight,2f);
        ScreenUtil.renderEntityPanel(graphics,leftPos + 40 + (hasClassicCrafting() ? 0 : 50),topPos + 13,63,84,2);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,leftPos + 40 + (hasClassicCrafting() ? 0 : 50),topPos + 13,leftPos + 103 + (hasClassicCrafting() ? 0 : 50),topPos + 97,35,0.0625f,i,j, minecraft.player);
        if (hasClassicCrafting()) {
            graphics.drawString(this.font, this.title, leftPos + 111, topPos + 16, 0x404040, false);
            graphics.blitSprite(SMALL_ARROW_SPRITE,leftPos + 158,topPos + 43,16,13);
        }
        if (!recipeBookComponent.isVisible() && recipeButton != null && !recipeButton.isHovered()) recipeButton.setFocused(false);
    }
    @Override
    public void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    public boolean canReplace() {
        return this.minecraft.gameMode.hasInfiniteItems() && canReplace;
    }

    public void setCanReplace(boolean canReplace) {
        this.canReplace = canReplace;
    }

    public Screen getReplacement() {
        return new CreativeModeInventoryScreen(this.minecraft.player, this.minecraft.player.connection.enabledFeatures(), (Boolean)this.minecraft.options.operatorItemsTab().get());
    }
}
