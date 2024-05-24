package wily.legacy.mixin;

import net.minecraft.client.gui.GuiGraphics;
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
    public void renderBg(GuiGraphics graphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        ScreenUtil.renderPanel(graphics,leftPos,topPos,imageWidth,imageHeight,2f);
        ScreenUtil.renderSquareEntityPanel(graphics,leftPos + 34,topPos + 20,63,63,2);
        ScreenUtil.renderSquareRecessedPanel(graphics,leftPos + 97,topPos + 20,105,63,2);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,leftPos + 35,topPos + 21,leftPos + 95,topPos + 81,25,0.0625f,i,j, horse);

    }
}
