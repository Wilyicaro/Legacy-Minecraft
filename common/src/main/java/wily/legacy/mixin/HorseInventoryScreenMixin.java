package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

@Mixin(HorseInventoryScreen.class)
public abstract class HorseInventoryScreenMixin extends AbstractContainerScreen<HorseInventoryMenu> {
    @Shadow @Final private AbstractHorse horse;

    public HorseInventoryScreenMixin(HorseInventoryMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    public void init() {
        imageWidth = 215;
        imageHeight = 203;
        inventoryLabelX = 14;
        inventoryLabelY = 91;
        titleLabelX = 14;
        titleLabelY = 8;
        super.init();
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(PoseStack graphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        LegacyGuiGraphics.of(graphics).blitSprite(LegacySprites.SMALL_PANEL,leftPos,topPos,imageWidth,imageHeight);
        LegacyGuiGraphics.of(graphics).blitSprite(LegacySprites.SQUARE_ENTITY_PANEL,leftPos + 34,topPos + 20,63,63);
        LegacyGuiGraphics.of(graphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 97,topPos + 20,105,63);
        ScreenUtil.renderEntityInInventoryFollowsMouse(graphics,leftPos + 35,topPos + 21,leftPos + 95,topPos + 81,25,0.0625f,i,j, horse);

    }
    @Override
    public void renderBackground(PoseStack poseStack) {
    }
}
