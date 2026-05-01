package wily.legacy.skins.client.render;

import net.minecraft.resources.Identifier;
import wily.legacy.skins.client.render.boxloader.BuiltBoxModel;

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

    boolean consoleskins$skipCustomAnimation();

    void consoleskins$setSkipCustomAnimation(boolean skip);

    default Identifier consoleskins$getCachedTexture() {
        return null;
    }

    default void consoleskins$setCachedTexture(Identifier tex) {
    }

    default Identifier consoleskins$getCachedBoxTexture() {
        return null;
    }

    default void consoleskins$setCachedBoxTexture(Identifier tex) {
    }

    default Identifier consoleskins$getCachedModelId() {
        return null;
    }

    default void consoleskins$setCachedModelId(Identifier id) {
    }

    default BuiltBoxModel consoleskins$getCachedBoxModel() {
        return null;
    }

    default void consoleskins$setCachedBoxModel(BuiltBoxModel model) {
    }
}
