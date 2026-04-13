package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.HelpAndOptionsScreen;
import wily.legacy.client.screen.LegacyConfigWidgets;
import wily.legacy.client.screen.TickBox;
import java.util.List;

@Mixin(value = HelpAndOptionsScreen.class, remap = false)
public class SkinsOptionMixin {
    @Inject(method = "createLegacyPlayerSkinWidgets", at = @At("RETURN"), remap = false, require = 0)
    private static void consoleskins$addLegacySkinOptions(CallbackInfoReturnable<List<AbstractWidget>> cir) {
        List<AbstractWidget> list = cir.getReturnValue();
        if (list == null) return;
        list.add(new TickBox(
                0,
                0,
                ConsoleSkinsClientSettings.isTu3ChangeSkinScreen(),
                b -> Component.translatable("legacy.menu.change_skin.tu3_change_skin_screen"),
                b -> null,
                t -> ConsoleSkinsClientSettings.setTu3ChangeSkinScreen(t.selected)
        ));
        list.add(new TickBox(
                0,
                0,
                ConsoleSkinsClientSettings.isSmoothPreviewScroll(),
                b -> Component.translatable("legacy.menu.change_skin.smooth_preview_scroll"),
                b -> null,
                t -> ConsoleSkinsClientSettings.setSmoothPreviewScroll(t.selected)
        ));
        list.add(new TickBox(
                0,
                0,
                ConsoleSkinsClientSettings.isHideArmorOnAllBoxSkins(),
                b -> Component.translatable("legacy.menu.change_skin.hide_armor"),
                b -> null,
                t -> ConsoleSkinsClientSettings.setHideArmorOnAllBoxSkins(t.selected)
        ));
        if (!LegacyOptions.legacySettingsMenus.get()) {
            AbstractWidget widget = LegacyConfigWidgets.createWidget(LegacyOptions.customSkinAnimation);
            if (widget != null) {
                list.add(widget);
            }
        }
        list.add(new TickBox(
                0,
                0,
                ConsoleSkinsClientSettings.isShowCustomPackOptionsTooltip(),
                b -> Component.translatable("legacy.menu.change_skin.custom_pack_options"),
                b -> null,
                t -> ConsoleSkinsClientSettings.setShowCustomPackOptionsTooltip(t.selected)
        ));
    }
}
