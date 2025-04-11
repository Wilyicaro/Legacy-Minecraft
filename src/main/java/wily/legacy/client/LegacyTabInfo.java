package wily.legacy.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.client.screen.LegacyTabButton;

public interface LegacyTabInfo {
    ResourceLocation id();

    Component name();

    LegacyTabButton.IconHolder<?> iconHolder();

    default LegacyTabButton.Render icon(){
        return iconHolder() == null ? null : iconHolder().icon();
    }

    default boolean isValid(){
        return name() != null && iconHolder() != null;
    }
}
