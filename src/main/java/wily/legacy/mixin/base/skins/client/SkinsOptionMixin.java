package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.HelpAndOptionsScreen;
import wily.legacy.client.screen.LegacyConfigWidgets;
import java.util.List;

@Mixin(value = HelpAndOptionsScreen.class, remap = false)
public class SkinsOptionMixin {
    @Inject(method = "createLegacyPlayerSkinWidgets", at = @At("RETURN"), remap = false, require = 0)
    private static void consoleskins$addLegacySkinOptions(CallbackInfoReturnable<List<AbstractWidget>> cir) {
        List<AbstractWidget> list = cir.getReturnValue();
        if (list == null) return;
        addWidget(list, LegacyOptions.tu3ChangeSkinScreen);
        addWidget(list, LegacyOptions.smoothPreviewScroll);
        addWidget(list, LegacyOptions.hideArmorOnAllBoxSkins);
        if (!LegacyOptions.legacySettingsMenus.get()) {
            addWidget(list, LegacyOptions.customSkinAnimation);
        }
        addWidget(list, LegacyOptions.showCustomPackOptionsTooltip);
    }

    private static void addWidget(List<AbstractWidget> list, wily.factoryapi.base.config.FactoryConfig<?> config) {
        AbstractWidget widget = LegacyConfigWidgets.createWidget(config);
        if (widget != null) list.add(widget);
    }
}
