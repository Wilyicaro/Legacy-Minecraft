package wily.legacy.client;

public interface ModelPartSkipRenderOverrideAccess {
    boolean consoleskins$getSkipRenderOverride();
    void consoleskins$setSkipRenderOverride(boolean value);

    /** When true, this part renders even if skipDraw=true. Set on armor model parts only. */
    boolean consoleskins$getForceRender();
    void consoleskins$setForceRender(boolean value);
}
