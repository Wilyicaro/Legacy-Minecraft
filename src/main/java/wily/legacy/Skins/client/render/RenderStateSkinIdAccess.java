package wily.legacy.Skins.client.render;

import java.util.UUID;

public interface RenderStateSkinIdAccess {
    String consoleskins$getSkinId();

    void consoleskins$setSkinId(String id);

    UUID consoleskins$getEntityUuid();

    void consoleskins$setEntityUuid(UUID uuid);

    boolean consoleskins$isMoving();

    void consoleskins$setMoving(boolean moving);
}
