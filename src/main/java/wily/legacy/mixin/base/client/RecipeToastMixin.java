package wily.legacy.mixin.base.client;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if <1.21.2 {
/*import net.minecraft.client.gui.components.toasts.ToastComponent;
//? if >1.20.1 {
import net.minecraft.world.item.crafting.RecipeHolder;
//?}
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?} else {
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.ArrayList;
import java.util.List;

@Mixin(RecipeToast.class)
public abstract class RecipeToastMixin implements Toast {
    @Shadow
    @Final
    private static Component TITLE_TEXT;

    @Shadow
    @Final
    private static Component DESCRIPTION_TEXT;
    private final List<Pair<ItemStack, ItemStack>> displayItems = new ArrayList<>();
    @Shadow
    private int displayedRecipeIndex;

    @Inject(method = "addOrUpdate", at = @At("HEAD"), cancellable = true)
    private static void addOrUpdate(CallbackInfo ci) {
        if (!LegacyOptions.hasClassicCrafting()) ci.cancel();
    }

    @Inject(method = "addItem", at = @At("RETURN"))
    private void addItem(ItemStack itemStack, ItemStack itemStack2, CallbackInfo ci) {
        displayItems.add(Pair.of(itemStack, itemStack2));
    }

    @Override
    public int width() {
        return 70 + 3 * Minecraft.getInstance().font.width(TITLE_TEXT) / 2;
    }

    @Override
    public int height() {
        return 40;
    }

    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, Font font, long l, CallbackInfo ci) {
        ci.cancel();

        LegacyRenderUtil.renderPointerPanel(guiGraphics, 0, 0, width(), height());
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((width() - 1.5f * Minecraft.getInstance().font.width(TITLE_TEXT)) / 2, 10);
        guiGraphics.pose().scale(1.5f, 1.5f);
        guiGraphics.drawString(Minecraft.getInstance().font, TITLE_TEXT, 0, 0, 0xFFFFFFFF);
        guiGraphics.pose().popMatrix();
        guiGraphics.drawString(Minecraft.getInstance().font, DESCRIPTION_TEXT, (width() - Minecraft.getInstance().font.width(DESCRIPTION_TEXT)) / 2, 27, 0xFFFFFFFF);
        ItemStack toastSymbol = displayItems.get(displayedRecipeIndex).key();
        ItemStack resultItem = displayItems.get(displayedRecipeIndex).value();

        LegacyRenderUtil.iconHolderRenderer.itemHolder(8, (height() - 27) / 2, 27, 27, toastSymbol, false, Vec2.ZERO).renderItem(guiGraphics, 0, 0, 0);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMALL_PANEL, width() - 36, (height() - 28) / 2, 28, 28);
        guiGraphics.renderItem(resultItem, width() - 30, (height() - 16) / 2);
    }
}
