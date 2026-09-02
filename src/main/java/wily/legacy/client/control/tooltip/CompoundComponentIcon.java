package wily.legacy.client.control.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Arrays;
import java.util.Objects;

public class CompoundComponentIcon implements ComponentIcon, CompoundIcon {

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
        return ControlTooltip.COMPOUND_COMPONENT_ICON_FUNCTION.apply(componentIcons);
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
