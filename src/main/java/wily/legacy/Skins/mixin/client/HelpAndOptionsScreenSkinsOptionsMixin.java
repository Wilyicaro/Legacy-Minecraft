package wily.legacy.Skins.mixin.client;


/**
 * Mixin: console skins / CPM rendering glue.
 */

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.client.screen.HelpAndOptionsScreen;
import wily.legacy.client.screen.TickBox;

import java.util.List;

@Mixin(value = HelpAndOptionsScreen.class, remap = false)
public class HelpAndOptionsScreenSkinsOptionsMixin {

    @Inject(method = "createPlayerSkinWidgets", at = @At("RETURN"), remap = false, require = 0)
    private static void consoleskins$addSmoothPreviewScroll(CallbackInfoReturnable<List<AbstractWidget>> cir) {
        List<AbstractWidget> list = cir.getReturnValue();
        if (list == null) return;

        list.add(0, new TickBox(
                0,
                0,
                ConsoleSkinsClientSettings.isSmoothPreviewScroll(),
                b -> Component.translatable("legacy.menu.change_skin.smooth_preview_scroll"),
                b -> null,
                t -> ConsoleSkinsClientSettings.setSmoothPreviewScroll(t.selected)
        ));

        list.add(1, new TickBox(
                0,
                0,
                ConsoleSkinsClientSettings.isSkinAnimations(),
                b -> Component.translatable("legacy.menu.change_skin.skin_animations"),
                b -> null,
                t -> ConsoleSkinsClientSettings.setSkinAnimations(t.selected)
        ));
    }
}
