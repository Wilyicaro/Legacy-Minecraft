package wily.legacy.client.screen.compat;

import dev.jfronny.libjf.config.api.v2.EntryInfo;
import dev.jfronny.libjf.config.api.v2.Naming;
import dev.jfronny.libjf.config.api.v2.type.Type;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacySliderButton;
import wily.legacy.client.screen.TickBox;

import java.util.Arrays;
import java.util.function.Consumer;

/// @see wily.legacy.client.screen.LegacyConfigWidgets
public class LegacyTTTConfigWidgets {
    private static final int DEFAULT_HEIGHT = 20;

    private LegacyTTTConfigWidgets() {}

    public static <T> @Nullable AbstractWidget createWidget(EntryInfo<T> info, Naming.Entry naming, int x, int y, int width, T value, Consumer<T> afterSet) {
        if (!info.supportsRepresentation()) return null;

        Type type = info.getValueType();
        try {
            return switch (type) {
                case Type.TInt _, Type.TLong _, Type.TFloat _, Type.TDouble _ ->
                        createNumberWidgetTrust(info, naming, x, y, width, value, afterSet);
                case Type.TString ignored -> createTextWidget(info, naming, x, y, width, (String) value, afterSet);
                case Type.TBool ignored -> createToggleWidget(info, naming, x, y, width, (Boolean) value, afterSet);
                case Type.TEnum<?> enumType ->
                        createEnumWidget(info, naming, x, y, width, value, enumType.options(), afterSet);
                case Type.TUnknown ignored -> null;
            };
        } catch (Throwable t) {
            Legacy4J.LOGGER.error("Failed to create widget for {}", info, t);
            return null;
        }
    }

    private static <T, U extends Number> AbstractWidget createNumberWidgetTrust(EntryInfo<?> info, Naming.Entry naming, int x, int y, int width, Object value, Consumer<T> afterSet
    ) {
        //noinspection unchecked
        return createNumberWidget((EntryInfo<U>)info, naming, x, y, width, (U) value, (Consumer<U>) afterSet);
    }

    private static <T extends Number> AbstractWidget createNumberWidget(EntryInfo<T> info, Naming.Entry naming, int x, int y, int width, T value, Consumer<T> afterSet
    ) {
        double min = info.getMinValue();
        double max = info.getMaxValue();

        if (isProper(min) && isProper(max)) {
            return LegacySliderButton.createFromIntRange(x, y, width, DEFAULT_HEIGHT, slider -> Component.literal(slider.getObjectValue() + ""), _ -> createTooltip(naming.tooltip()), value instanceof Integer ? ((Integer) value) : value.intValue(), (int) Math.floor(min), (int) Math.ceil(max), slider -> {
                        @SuppressWarnings("unchecked")
                        T result = (T) slider.getObjectValue();
                        afterSet.accept(result);
                    }, value::intValue
            );
        }

        return createTextWidget(info, naming, x, y, width, value == null ? "" : value.toString(), _ -> {});
    }

    private static <T> AbstractWidget createTextWidget(
            EntryInfo<T> info,
            Naming.Entry naming,
            int x,
            int y,
            int width,
            String value,
            Consumer<T> afterSet
    ) {
        EditBox editBox = new EditBox(Minecraft.getInstance().font, x, y, width, DEFAULT_HEIGHT, naming.name());

        editBox.setValue(value == null ? "" : value);

        if (naming.tooltip() instanceof Component component) editBox.setTooltip(Tooltip.create(component));

        editBox.setResponder(current -> {
            editBox.setTextColor(0xffe0e0e0);
            if (current.isBlank()) return;
            try {
                @SuppressWarnings("unchecked")
                T parsed = (T) parseValue(info, current);

                if (!withinBounds(info, parsed)) {
                    editBox.setTextColor(0xffff5555);
                    return;
                }
                afterSet.accept(parsed);
            } catch (Throwable _) {
                editBox.setTextColor(0xffff5555);
            }
        });

        return editBox;
    }

    private static <T> AbstractWidget createToggleWidget(
            EntryInfo<T> info,
            Naming.Entry naming,
            int x,
            int y,
            int width,
            Boolean value,
            Consumer<T> afterSet
    ) {
        return new TickBox(x, y, width, value, _ -> naming.name(), _ -> createTooltip(naming.tooltip()), selected -> {
            @SuppressWarnings("unchecked")
            T result = (T) Boolean.valueOf(selected.selected);
            afterSet.accept(result);
        }, () -> value
        );
    }

    private static <T> AbstractWidget createEnumWidget(EntryInfo<T> info, Naming.Entry naming, int x, int y, int width, T value, Object[] values, Consumer<T> afterSet
    ) {
		LegacySliderButton<T> b;
        //noinspection unchecked
		b = new LegacySliderButton<>(x, y, width, DEFAULT_HEIGHT, h -> CommonComponents.optionNameValue(naming.name(), naming.enumValue(info.getValueType(), h.getObjectValue())), _ -> null, value, () -> (java.util.List<T>) Arrays.stream(values).toList(), v -> afterSet.accept(v.getObjectValue()));
		if (naming.tooltip() instanceof Component component) b.setTooltip(Tooltip.create(component));
        return b;
    }

    private static Object parseValue(
            EntryInfo<?> info,
            String value
    ) {
        return switch (info.getValueType()) {
            case Type.TInt _ -> Integer.parseInt(value);
            case Type.TLong _ -> Long.parseLong(value);
            case Type.TFloat _ -> Float.parseFloat(value);
            case Type.TDouble _ -> Double.parseDouble(value);
            case Type.TString _, Type.TBool _, Type.TEnum<?> _, Type.TUnknown _ -> value;
        };
    }

    private static boolean withinBounds(EntryInfo<?> info, Object value) {
        if (!(value instanceof Number number)) return true;

        double numeric = number.doubleValue();

        return numeric >= info.getMinValue() && numeric <= info.getMaxValue();
    }

    private static Tooltip createTooltip(@Nullable Component component) {
        return component == null ? null : Tooltip.create(component);
    }

    private static boolean isProper(double value) {
        return !Double.isNaN(value) && Double.isFinite(value);
    }
}
