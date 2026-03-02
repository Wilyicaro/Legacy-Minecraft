package wily.legacy.Skins.client.render;

import java.util.UUID;

public interface RenderStateSkinIdAccess {
    String consoleskins$getSkinId();

    void consoleskins$setSkinId(String id);

    UUID consoleskins$getEntityUuid();

    void consoleskins$setEntityUuid(UUID uuid);

    boolean consoleskins$isMoving();

    void consoleskins$setMoving(boolean moving);

    float consoleskins$getMoveSpeedSq();

    void consoleskins$setMoveSpeedSq(float speedSq);

    boolean consoleskins$isSitting();

    void consoleskins$setSitting(boolean sitting);

    boolean consoleskins$isUsingItem();

    void consoleskins$setUsingItem(boolean usingItem);

    boolean consoleskins$isBlocking();

    void consoleskins$setBlocking(boolean blocking);
}
