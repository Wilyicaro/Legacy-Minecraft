package wily.legacy.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmithingMenu;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin extends ItemCombinerScreen<SmithingMenu> {


    @Shadow @Final private static Quaternionf ARMOR_STAND_ANGLE;

    @Shadow private ArmorStand armorStandPreview;

    @Shadow @Final private CyclingSlotBackground templateIcon;

    @Shadow @Final private CyclingSlotBackground baseIcon;

    @Shadow @Final private CyclingSlotBackground additionalIcon;

    @Shadow protected abstract void renderOnboardingTooltips(GuiGraphics arg, int i, int j);

    @Shadow protected abstract boolean hasRecipeError();

    public SmithingScreenMixin(SmithingMenu itemCombinerMenu, Inventory inventory, Component component, ResourceLocation resourceLocation) {
        super(itemCombinerMenu, inventory, component, resourceLocation);
    }

    @Override
    public void init() {
        imageWidth = 207;
        imageHeight = 215;
        inventoryLabelX = 10;
        inventoryLabelY = 105;
        titleLabelX = 56;
        titleLabelY = 25;
        super.init();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
    }

    @Override
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        LegacyGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMALL_PANEL, leftPos, topPos, imageWidth, imageHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 13.5, topPos + 9.5,0f);
        guiGraphics.pose().scale(2.5f,2.5f,2.5f);
        LegacyGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMITHING_HAMMER,0,0,15,15);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 82, topPos + 59,0f);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        LegacyGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ARROW,0,0,22,15);
        if (hasRecipeError())
            LegacyGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ERROR_CROSS, 4, 0, 15, 15);
        guiGraphics.pose().popPose();
        InventoryScreen.renderEntityInInventory(guiGraphics, this.leftPos + 182, this.topPos + 95, 35, ARMOR_STAND_ANGLE, null, this.armorStandPreview);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        this.templateIcon.render(this.menu, guiGraphics, f, this.leftPos, this.topPos);
        this.baseIcon.render(this.menu, guiGraphics, f, this.leftPos, this.topPos);
        this.additionalIcon.render(this.menu, guiGraphics, f, this.leftPos, this.topPos);
        renderOnboardingTooltips(guiGraphics,i,j);
    }
}
