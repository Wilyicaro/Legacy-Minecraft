package wily.legacy.mixin.base.client;

import net.minecraft.client.entity.ClientAvatarState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.entity.PlayerYBobbing;

@Mixin(ClientAvatarState.class)
public class ClientAvatarStateMixin implements PlayerYBobbing {

    @Unique
    float oYBob;
    @Unique
    float yBob;

    @Override
    public float oYBob() {
        return oYBob;
    }

    @Override
    public void setOYBob(float bob) {
        oYBob = bob;
    }

    @Override
    public float yBob() {
        return yBob;
    }

    @Override
    public void setYBob(float bob) {
        yBob = bob;
    }

    @Inject(method = "updateBob", at = @At("RETURN"))
    public void tick(CallbackInfo ci) {
        handleYBobbing();
    }
}
