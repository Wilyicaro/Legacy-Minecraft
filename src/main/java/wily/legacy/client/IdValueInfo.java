package wily.legacy.client;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public interface IdValueInfo<T extends IdValueInfo<T>> {
    Optional<Component> name();

    default Component nameOrEmpty() {
        return name().orElse(CommonComponents.EMPTY);
    }

    ResourceLocation id();

    T copyFrom(T other);

    boolean isValid();

    default boolean is(ResourceLocation id) {
        return id().equals(id);
    }
}
