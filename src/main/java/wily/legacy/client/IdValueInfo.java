package wily.legacy.client;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;

import java.util.Optional;

public interface IdValueInfo<T extends IdValueInfo<T>> {
    Optional<Component> name();

    default Component nameOrEmpty() {
        return name().orElse(CommonComponents.EMPTY);
    }

    /*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ id();

    T copyFrom(T other);

    boolean isValid();

    default boolean is(/*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ id) {
        return id().equals(id);
    }
}
