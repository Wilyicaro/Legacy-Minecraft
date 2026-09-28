package wily.legacy.client.control.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 * Implements the {@link ComponentIcon} into the {@link CompoundIcon} in a lightweight way
 */
public class CompoundComponentIcon implements ComponentIcon, CompoundIcon {

    public static final Function<ComponentIcon[], ComponentIcon> COMPOUND_COMPONENT_ICON_CACHE = Util.memoize(CompoundComponentIcon::new);
    private final ComponentIcon[] componentIcons;
    private final MutableComponent component = Component.empty();
    private boolean isAdditive = false;

    public CompoundComponentIcon(ComponentIcon[] componentIcons) {
        this.componentIcons = Arrays.stream(componentIcons).filter(Objects::nonNull).toArray(ComponentIcon[]::new);
        for (ComponentIcon componentIcon : this.componentIcons) {
            component.append(componentIcon.getComponent());
            if (componentIcon == ControlTooltip.PLUS_ICON) isAdditive = true;
        }
    }

    public static ComponentIcon of(ComponentIcon... componentIcons) {
        return COMPOUND_COMPONENT_ICON_CACHE.apply(componentIcons);
    }

    @Override
    public Component getComponent() {
        return component;
    }

    @Override
    public Icon[] getIcons() {
        return componentIcons;
    }

    @Override
    public boolean isAdditive() {
        return isAdditive;
    }
}
