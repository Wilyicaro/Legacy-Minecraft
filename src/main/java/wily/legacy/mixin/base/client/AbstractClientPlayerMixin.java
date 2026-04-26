package wily.legacy.mixin.base.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.entity.PlayerYBobbing;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin extends Player {
    public AbstractClientPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Shadow public abstract ClientAvatarState avatarState();

    @Inject(method = "updateBob", at = @At("RETURN"))
    private void updateBob(CallbackInfo ci) {
        PlayerYBobbing.of(avatarState()).handleYBobbing(this);
    }
}
