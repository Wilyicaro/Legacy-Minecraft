//? if >=1.21.2 {
package wily.legacy.mixin.base.client;

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
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(GhostSlots.class)
public class GhostSlotsMixin {
    @Shadow
    @Final
    private Reference2ObjectMap<Slot, GhostSlots.GhostSlot> ingredients;

    @Shadow
    @Final
    private SlotSelectTime slotSelectTime;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, boolean bl, CallbackInfo ci) {
        ci.cancel();
        ingredients.forEach((slot, ghostSlot) -> {
            LegacyIconHolder holder = LegacyRenderUtil.iconHolderRenderer.slotBounds(slot);
            guiGraphics.pose().pushMatrix();
            holder.applyOffset(guiGraphics);
            guiGraphics.pose().translate(slot.x, slot.y);
            guiGraphics.pose().scale(holder.getScaleX(), holder.getScaleY());
            guiGraphics.fill(0, 0, 16, 16, 0x30FF0000);
            ItemStack itemStack = ghostSlot.getItem(this.slotSelectTime.currentIndex());
            guiGraphics.renderFakeItem(itemStack, 0, 0);
            guiGraphics.fill(0, 0, 16, 16, 0x30FFFFFF);
            if (ghostSlot.isResultSlot())
                guiGraphics.renderItemDecorations(minecraft.font, itemStack, 0, 0);
            guiGraphics.pose().popMatrix();
        });
    }
}
//?}
