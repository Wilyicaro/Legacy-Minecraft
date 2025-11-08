package wily.legacy.client;

import wily.legacy.client.screen.LegacyTabButton;

import java.util.Optional;

public interface LegacyTabInfo<T extends LegacyTabInfo<T>> extends IdValueInfo<T> {
    Optional<LegacyTabButton.IconHolder<?>> iconHolder();

    default LegacyTabButton.Render icon() {
        return iconHolder().isEmpty() ? null : iconHolder().get().icon();
    }

    default boolean isValid() {
        return name().isPresent() && iconHolder().isPresent();
    }
}
