package wily.legacy.mixin.base;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if <1.21.2 {
/*import net.minecraft.client.gui.components.toasts.ToastComponent;
//? if >1.20.1 {
import net.minecraft.world.item.crafting.RecipeHolder;
//?}
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?} else {
import net.minecraft.client.gui.components.toasts.ToastManager;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.List;

@Mixin(RecipeToast.class)
public abstract class RecipeToastMixin implements Toast {
    @Shadow @Final private static Component TITLE_TEXT;

    @Shadow @Final private static Component DESCRIPTION_TEXT;

    //? if <1.21.2 {
    /*@Shadow private boolean changed;
    @Shadow @Final private List</^? if >1.20.1 {^/RecipeHolder<?>/^?} else {^//^Recipe<?>^//^?}^/> recipes;
    @Shadow private long lastChanged;
    *///?} else {
    @Shadow private int displayedRecipeIndex;
    private final List<Pair<ItemStack,ItemStack>> displayItems = new ArrayList<>();

    @Inject(method = "addItem", at = @At("RETURN"))
    private void addItem(ItemStack itemStack, ItemStack itemStack2, CallbackInfo ci){
        displayItems.add(Pair.of(itemStack,itemStack2));
    }
    //?}

    @Override
    public int width() {
        return Toast.super.width() + 80;
    }
    @Inject(method = "addOrUpdate", at = @At("HEAD"), cancellable = true)
    private static void addOrUpdate(CallbackInfo ci) {
        if (!ScreenUtil.hasClassicCrafting()) ci.cancel();
    }
    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, /*? if <1.21.2 {*/ /*ToastComponent toastComponent*//*?} else {*/Font font /*?}*/, long l, /*? if <1.21.2 {*/ /*CallbackInfoReturnable<Visibility> cir*//*?} else {*/CallbackInfo ci/*?}*/) {
        //? if <1.21.2 {
        /*if (this.changed) {
            this.lastChanged = l;
            this.changed = false;
        }
        if (this.recipes.isEmpty() || !ScreenUtil.hasClassicCrafting()){
            cir.setReturnValue(Toast.Visibility.HIDE);
            return;
        }
        /^? if >1.20.1 {^/RecipeHolder<?>/^?} else {^//^Recipe<?>^//^?}^/ recipeHolder = this.recipes.get((int)((double)l / Math.max(1.0, 5000.0 * toastComponent.getNotificationDisplayTimeMultiplier() / (double)this.recipes.size()) % (double)this.recipes.size()));
        *///?} else {
        ci.cancel();
        //?}

        ScreenUtil.renderPointerPanel(guiGraphics,0,0,width(),height());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((width() - 1.5 * Minecraft.getInstance().font.width(TITLE_TEXT)) / 2, 5,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        guiGraphics.drawString(Minecraft.getInstance().font, TITLE_TEXT, 0,0, 0xFFFFFF);
        guiGraphics.pose().popPose();
        guiGraphics.drawString(Minecraft.getInstance().font, DESCRIPTION_TEXT, (width() - Minecraft.getInstance().font.width(DESCRIPTION_TEXT)) / 2 , 18, 0xFFFFFF);
        ItemStack toastSymbol = /*? if <1.21.2 {*//*recipeHolder/^? if >1.20.1 {^/.value()/^?}^/.getToastSymbol()*//*?} else {*/ displayItems.get(displayedRecipeIndex).key()/*?}*/;
        ItemStack resultItem = /*? if <1.21.2 {*//*recipeHolder/^? if >1.20.1 {^/.value()/^?}^/.getResultItem(Minecraft.getInstance().level.registryAccess())*//*?} else {*/ displayItems.get(displayedRecipeIndex).value()/*?}*/;

        ScreenUtil.iconHolderRenderer.itemHolder(8,(height() - 27) / 2,27,27,toastSymbol,false, Vec3.ZERO).renderItem(guiGraphics,0,0,0);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMALL_PANEL,width() - 36,(height() - 28) / 2,28,28);
        guiGraphics.renderItem(resultItem ,width() - 30, (height() - 16) / 2);
        //? if <1.21.2 {
        /*cir.setReturnValue((double)(l - this.lastChanged) >= 5000.0 * toastComponent.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW);
        *///?}
    }
}
