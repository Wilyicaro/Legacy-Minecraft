package wily.legacy.client.control.tooltip;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import wily.legacy.client.control.ControlType;

import java.util.Optional;

public abstract class CharsIcon extends LegacyIcon {
    public static final Codec<char[]> CHARS_CODEC = Codec.STRING.xmap(String::toCharArray, String::new);
    private final Optional<char[]> iconChars;
    private final Optional<char[]> iconOverlayChars;
    private final Optional<String> tipIcon;

    protected CharsIcon(Optional<char[]> iconChars, Optional<char[]> iconOverlayChars, Optional<String> tipIcon) {
        this.iconChars = iconChars;
        this.iconOverlayChars = iconOverlayChars;
        this.tipIcon = tipIcon;
    }

    @Override
    public Component getComponent(boolean allowPressed) {
        return iconChars.isPresent() ? getActualIcon(iconChars.get(), allowPressed, getControlType()) : null;
    }

    @Override
    public Component getOverlayComponent(boolean allowPressed) {
        return iconOverlayChars.isPresent() ? getActualIcon(iconOverlayChars.get(), allowPressed, getControlType()) : null;
    }

    @Override
    public Component getComponent() {
        return tipIcon.isEmpty() ? super.getComponent() == null ? getOverlayComponent(false) : super.getComponent() : ControlTooltip.getControlIcon(tipIcon.get(), ControlType.getActiveControllerType()).getComponent();
    }

    public abstract ControlType getControlType();

    public Optional<char[]> iconChars() {
        return iconChars;
    }

    public Optional<char[]> iconOverlayChars() {
        return iconOverlayChars;
    }

    public Optional<String> tipIcon() {
        return tipIcon;
    }

    public abstract String name();
}
