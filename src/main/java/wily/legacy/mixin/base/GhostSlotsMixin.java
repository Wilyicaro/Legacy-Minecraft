//? if >=1.21.2 {
package wily.legacy.mixin.base;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.SlotSelectTime;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.util.ScreenUtil;

@Mixin(GhostSlots.class)
public class GhostSlotsMixin {
    @Shadow @Final private Reference2ObjectMap<Slot, GhostSlots.GhostSlot> ingredients;

    @Shadow @Final private SlotSelectTime slotSelectTime;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, boolean bl, CallbackInfo ci) {
        ci.cancel();
        ingredients.forEach((slot, ghostSlot) -> {
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(slot);
            guiGraphics.pose().pushPose();
            holder.applyOffset(guiGraphics);
            guiGraphics.pose().translate(slot.x, slot.y, 0);
            guiGraphics.pose().scale(holder.getSelectableWidth() / 16f,holder.getSelectableHeight() / 16f,holder.getSelectableHeight() / 16f);

            guiGraphics.fill(0, 0, 16, 16, 0x30FF0000);
            ItemStack itemStack = ghostSlot.getItem(this.slotSelectTime.currentIndex());
            guiGraphics.renderFakeItem(itemStack, 0, 0);
            guiGraphics.fill(RenderType.guiGhostRecipeOverlay(), 0, 0, 16,16, 0x30FFFFFF);
            if (ghostSlot.isResultSlot())
                guiGraphics.renderItemDecorations(minecraft.font, itemStack, 0, 0);
            guiGraphics.pose().popPose();
        });
    }
}
//?}
