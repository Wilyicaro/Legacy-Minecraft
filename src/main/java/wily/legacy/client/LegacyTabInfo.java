package wily.legacy.client;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.client.screen.LegacyTabButton;

import java.util.function.Function;

public interface LegacyTabInfo {
    ResourceLocation id();
    Component name();
    LegacyTabButton.IconHolder<?> iconHolder();
    default Function<LegacyTabButton, Renderable> icon(){
        return iconHolder() == null ? null : iconHolder().icon();
    }
    default boolean isValid(){
        return name() != null && iconHolder() != null;
    }
}
