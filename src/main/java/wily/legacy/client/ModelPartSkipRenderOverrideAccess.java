package wily.legacy.client;

/**
 * A per-ModelPart flag used by our box-model hiding system.
 *
 * IMPORTANT: this must NOT live under the mixin package (wily.legacy.mixin.base.*),
 * otherwise Mixin will block loading it directly and the game will crash.
 */
public interface ModelPartSkipRenderOverrideAccess {
    boolean consoleskins$getSkipRenderOverride();
    void consoleskins$setSkipRenderOverride(boolean value);
}
