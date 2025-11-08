package wily.legacy.client;

import net.minecraft.client.OptionInstance;

public interface OptionInstanceAccessor<T> {
    static <T> OptionInstanceAccessor<T> of(OptionInstance<T> optionInstance) {
        return (OptionInstanceAccessor<T>) (Object) optionInstance;
    }

    T defaultValue();

    OptionInstance.TooltipSupplier<T> tooltip();

    String getKey();
}
