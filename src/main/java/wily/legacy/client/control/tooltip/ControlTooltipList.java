package wily.legacy.client.control.tooltip;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import wily.legacy.client.control.LegacyKeyMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public class ControlTooltipList {
    public final List<ControlTooltip> tooltips = new ArrayList<>();
    
    public ControlTooltipList clear() {
        tooltips.clear();
        return this;
    }

    public ControlTooltipList set(int ordinal, Supplier<Icon> control, Supplier<Component> action) {
        return set(ordinal, ControlTooltip.create(control, action));
    }

    public ControlTooltipList set(int ordinal, ControlTooltip tooltip) {
        tooltips.set(ordinal, tooltip);
        return this;
    }

    public ControlTooltipList replace(int ordinal, Function<Icon, Icon> control, Function<Component, Component> action) {
        ControlTooltip old = tooltips.get(ordinal);
        return set(ordinal, ControlTooltip.create(() -> control.apply(old.getIcon()), () -> action.apply(old.getAction())));
    }

    public ControlTooltipList add(KeyMapping mapping) {
        return add(LegacyKeyMapping.of(mapping));
    }

    public ControlTooltipList add(KeyMapping mapping, Supplier<Component> action) {
        return add(ControlTooltip.create(LegacyKeyMapping.of(mapping), action));
    }

    public ControlTooltipList add(KeyMapping mapping, BooleanSupplier extraCondition) {
        return add(ControlTooltip.create(LegacyKeyMapping.of(mapping), () -> extraCondition.getAsBoolean() ? LegacyKeyMapping.of(mapping).getDisplayName() : null));
    }

    public ControlTooltipList add(LegacyKeyMapping mapping) {
        return add(mapping, mapping::getDisplayName);
    }

    public ControlTooltipList add(LegacyKeyMapping mapping, Supplier<Component> action) {
        return add(ControlTooltip.create(mapping, action));
    }

    public ControlTooltipList add(LegacyKeyMapping mapping, BooleanSupplier extraCondition) {
        return add(ControlTooltip.create(mapping, () -> extraCondition.getAsBoolean() ? mapping.getDisplayName() : null));
    }

    public ControlTooltipList addCompound(Supplier<Icon[]> control, Supplier<Component> action) {
        return add(ControlTooltip.create(() -> ControlTooltip.COMPOUND_ICON_FUNCTION.apply(control.get()), action));
    }

    public ControlTooltipList add(Supplier<Icon> control, Supplier<Component> action) {
        return add(ControlTooltip.create(control, action));
    }

    public ControlTooltipList add(ControlTooltip tooltip) {
        tooltips.add(tooltip);
        return this;
    }
}
