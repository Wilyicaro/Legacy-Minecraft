package wily.legacy.mixin.base.skins.client;

import net.minecraft.resources.ResourceLocation;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.Skins.skin.SkinEntry;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(AvatarRenderState.class)
public abstract class SkinIdStateMixin implements RenderStateSkinIdAccess {
    @Unique private String consoleskins$skinId;
    @Unique private UUID consoleskins$uuid;
    @Unique private boolean consoleskins$moving;
    @Unique private float consoleskins$moveSpeedSq;
    @Unique private boolean consoleskins$sitting;
    @Unique private boolean consoleskins$usingItem;
    @Unique private boolean consoleskins$blocking;
    @Unique private SkinEntry consoleskins$cachedEntry;
    @Unique private ResourceLocation consoleskins$cachedTexture;
    @Unique private ResourceLocation consoleskins$cachedModelId;
    @Unique private BuiltBoxModel consoleskins$cachedBoxModel;

    @Override
    public String consoleskins$getSkinId() {
        return consoleskins$skinId;
    }

    @Override
    public void consoleskins$setSkinId(String id) {
        this.consoleskins$skinId = id;
    }

    @Override
    public UUID consoleskins$getEntityUuid() {
        return consoleskins$uuid;
    }

    @Override
    public void consoleskins$setEntityUuid(UUID uuid) {
        this.consoleskins$uuid = uuid;
    }

    @Override
    public boolean consoleskins$isMoving() {
        return consoleskins$moving;
    }

    @Override
    public void consoleskins$setMoving(boolean moving) {
        this.consoleskins$moving = moving;
    }

    @Override
    public float consoleskins$getMoveSpeedSq() {
        return consoleskins$moveSpeedSq;
    }

    @Override
    public void consoleskins$setMoveSpeedSq(float speedSq) {
        this.consoleskins$moveSpeedSq = speedSq;
    }

    @Override
    public boolean consoleskins$isSitting() {
        return consoleskins$sitting;
    }

    @Override
    public void consoleskins$setSitting(boolean sitting) {
        this.consoleskins$sitting = sitting;
    }

    @Override
    public boolean consoleskins$isUsingItem() {
        return consoleskins$usingItem;
    }

    @Override
    public void consoleskins$setUsingItem(boolean usingItem) {
        this.consoleskins$usingItem = usingItem;
    }

    @Override
    public boolean consoleskins$isBlocking() {
        return consoleskins$blocking;
    }

    @Override
    public void consoleskins$setBlocking(boolean blocking) {
        this.consoleskins$blocking = blocking;
    }

    @Override public SkinEntry consoleskins$getCachedEntry() { return consoleskins$cachedEntry; }
    @Override public void consoleskins$setCachedEntry(SkinEntry entry) { this.consoleskins$cachedEntry = entry; }
    @Override public ResourceLocation consoleskins$getCachedTexture() { return consoleskins$cachedTexture; }
    @Override public void consoleskins$setCachedTexture(ResourceLocation tex) { this.consoleskins$cachedTexture = tex; }
    @Override public ResourceLocation consoleskins$getCachedModelId() { return consoleskins$cachedModelId; }
    @Override public void consoleskins$setCachedModelId(ResourceLocation id) { this.consoleskins$cachedModelId = id; }
    @Override public BuiltBoxModel consoleskins$getCachedBoxModel() { return consoleskins$cachedBoxModel; }
    @Override public void consoleskins$setCachedBoxModel(BuiltBoxModel model) { this.consoleskins$cachedBoxModel = model; }
}
