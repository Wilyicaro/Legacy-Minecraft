package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.client.render.boxloader.BuiltBoxModel;

import java.util.UUID;

@Mixin(AvatarRenderState.class)
public abstract class SkinIdStateMixin implements RenderStateSkinIdAccess {
    @Unique
    private String consoleskins$skinId;
    @Unique
    private UUID consoleskins$uuid;
    @Unique
    private boolean consoleskins$moving, consoleskins$sitting, consoleskins$usingItem, consoleskins$blocking;
    @Unique
    private float consoleskins$moveSpeedSq;
    @Unique
    private Identifier consoleskins$cachedTexture, consoleskins$cachedBoxTexture, consoleskins$cachedModelId;
    @Unique
    private BuiltBoxModel consoleskins$cachedBoxModel;

    @Override
    public String consoleskins$getSkinId() {
        return consoleskins$skinId;
    }

    @Override
    public void consoleskins$setSkinId(String id) {
        consoleskins$skinId = id;
    }

    @Override
    public UUID consoleskins$getEntityUuid() {
        return consoleskins$uuid;
    }

    @Override
    public void consoleskins$setEntityUuid(UUID uuid) {
        consoleskins$uuid = uuid;
    }

    @Override
    public boolean consoleskins$isMoving() {
        return consoleskins$moving;
    }

    @Override
    public void consoleskins$setMoving(boolean moving) {
        consoleskins$moving = moving;
    }

    @Override
    public float consoleskins$getMoveSpeedSq() {
        return consoleskins$moveSpeedSq;
    }

    @Override
    public void consoleskins$setMoveSpeedSq(float speedSq) {
        consoleskins$moveSpeedSq = speedSq;
    }

    @Override
    public boolean consoleskins$isSitting() {
        return consoleskins$sitting;
    }

    @Override
    public void consoleskins$setSitting(boolean sitting) {
        consoleskins$sitting = sitting;
    }

    @Override
    public boolean consoleskins$isUsingItem() {
        return consoleskins$usingItem;
    }

    @Override
    public void consoleskins$setUsingItem(boolean usingItem) {
        consoleskins$usingItem = usingItem;
    }

    @Override
    public boolean consoleskins$isBlocking() {
        return consoleskins$blocking;
    }

    @Override
    public void consoleskins$setBlocking(boolean blocking) {
        consoleskins$blocking = blocking;
    }

    @Override
    public Identifier consoleskins$getCachedTexture() {
        return consoleskins$cachedTexture;
    }

    @Override
    public void consoleskins$setCachedTexture(Identifier tex) {
        consoleskins$cachedTexture = tex;
    }

    @Override
    public Identifier consoleskins$getCachedBoxTexture() {
        return consoleskins$cachedBoxTexture;
    }

    @Override
    public void consoleskins$setCachedBoxTexture(Identifier tex) {
        consoleskins$cachedBoxTexture = tex;
    }

    @Override
    public Identifier consoleskins$getCachedModelId() {
        return consoleskins$cachedModelId;
    }

    @Override
    public void consoleskins$setCachedModelId(Identifier id) {
        consoleskins$cachedModelId = id;
    }

    @Override
    public BuiltBoxModel consoleskins$getCachedBoxModel() {
        return consoleskins$cachedBoxModel;
    }

    @Override
    public void consoleskins$setCachedBoxModel(BuiltBoxModel model) {
        consoleskins$cachedBoxModel = model;
    }
}
