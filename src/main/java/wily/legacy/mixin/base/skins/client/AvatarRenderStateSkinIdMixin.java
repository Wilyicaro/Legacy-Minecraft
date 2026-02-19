package wily.legacy.mixin.base.skins.client;



import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateSkinIdMixin implements RenderStateSkinIdAccess {
    @Unique
    private String consoleskins$skinId;
    @Unique
    private UUID consoleskins$uuid;
    @Unique
    private boolean consoleskins$moving;

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
}
