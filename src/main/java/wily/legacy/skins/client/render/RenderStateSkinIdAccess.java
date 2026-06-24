package wily.legacy.skins.client.render;

import net.minecraft.resources.ResourceLocation;
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

    default ResourceLocation consoleskins$getCachedTexture() {
        return null;
    }

    default void consoleskins$setCachedTexture(ResourceLocation tex) {
    }

    default ResourceLocation consoleskins$getCachedBoxTexture() {
        return null;
    }

    default void consoleskins$setCachedBoxTexture(ResourceLocation tex) {
    }

    default ResourceLocation consoleskins$getCachedModelId() {
        return null;
    }

    default void consoleskins$setCachedModelId(ResourceLocation id) {
    }

    default BuiltBoxModel consoleskins$getCachedBoxModel() {
        return null;
    }

    default void consoleskins$setCachedBoxModel(BuiltBoxModel model) {
    }
}
