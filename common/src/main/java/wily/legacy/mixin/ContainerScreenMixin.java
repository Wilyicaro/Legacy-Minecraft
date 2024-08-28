package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

@Mixin({ContainerScreen.class, ShulkerBoxScreen.class, HopperScreen.class, DispenserScreen.class})
public abstract class ContainerScreenMixin extends AbstractContainerScreen {

    public ContainerScreenMixin(AbstractContainerMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }
    @Override
    public void renderBackground(PoseStack poseStack) {
    }
    public void init() {
        int k = ((menu instanceof ChestMenu m ? m.getRowCount() : menu  instanceof HopperMenu ? 1 : 3) - 3) * 21;
        boolean centeredTitle = menu instanceof HopperMenu || menu instanceof DispenserMenu;
        imageWidth = 215;
        imageHeight = 207 + k;
        titleLabelX = centeredTitle ? (imageWidth - font.width(title)) / 2: 14;
        titleLabelY = 11;
        inventoryLabelX = 14;
        inventoryLabelY = 94 + k;
        super.init();
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(PoseStack poseStack, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.SMALL_PANEL,leftPos,topPos,imageWidth,imageHeight);
    }
}
