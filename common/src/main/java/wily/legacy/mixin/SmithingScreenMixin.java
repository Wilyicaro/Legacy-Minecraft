package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
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

    @Shadow protected abstract void renderOnboardingTooltips(PoseStack arg, int i, int j);

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
    public void renderBackground(PoseStack poseStack) {
    }

    @Override
    public void renderBg(PoseStack poseStack, float f, int i, int j) {
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.SMALL_PANEL, leftPos, topPos, imageWidth, imageHeight);
        poseStack.pose().pushPose();
        poseStack.pose().translate(leftPos + 13.5, topPos + 9.5,0f);
        poseStack.pose().scale(2.5f,2.5f,2.5f);
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.SMITHING_HAMMER,0,0,15,15);
        poseStack.pose().popPose();
        poseStack.pose().pushPose();
        poseStack.pose().translate(leftPos + 82, topPos + 59,0f);
        poseStack.pose().scale(1.5f,1.5f,1.5f);
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.ARROW,0,0,22,15);
        if (hasRecipeError())
            LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.ERROR_CROSS, 4, 0, 15, 15);
        poseStack.pose().popPose();
        InventoryScreen.renderEntityInInventory(poseStack, this.leftPos + 182, this.topPos + 95, 35, ARMOR_STAND_ANGLE, null, this.armorStandPreview);
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        super.render(poseStack, i, j, f);
        this.templateIcon.render(this.menu, poseStack, f, this.leftPos, this.topPos);
        this.baseIcon.render(this.menu, poseStack, f, this.leftPos, this.topPos);
        this.additionalIcon.render(this.menu, poseStack, f, this.leftPos, this.topPos);
        renderOnboardingTooltips(poseStack,i,j);
    }
}
