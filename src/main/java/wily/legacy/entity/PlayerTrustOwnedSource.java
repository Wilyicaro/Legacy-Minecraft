package wily.legacy.entity;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlayerTrustOwnedSource {
    @Nullable UUID getResponsiblePlayer();

    void setResponsiblePlayer(@Nullable UUID player);
}
