package wily.legacy.config;

import net.minecraft.network.chat.Component;

import java.util.function.Function;

public record LegacyConfigDisplay<T>(Component name, Function<T,Component> tooltip) {
    public LegacyConfigDisplay(String key, Function<T,Component> tooltip){
        this(Component.translatable("legacy.options."+key),tooltip);
    }
    public LegacyConfigDisplay(String key){
        this(key,b->null);
    }
    public LegacyConfigDisplay(Component name){
        this(name,b->null);
    }
}
