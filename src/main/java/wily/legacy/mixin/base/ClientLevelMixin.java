package wily.legacy.mixin.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyCloudAtmosphere;
import wily.legacy.client.LegacyDragonEggTeleportParticles;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Shadow
    public abstract DimensionSpecialEffects effects();

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void getCloudColor(float partialTick, CallbackInfoReturnable</*? if <1.21.2 {*/Vec3/*?} else {*//*Integer*//*?}*/> cir) {
        if (Minecraft.getInstance().gameRenderer.getMainCamera().getEntity() instanceof LivingEntity entity
                && entity.hasEffect(MobEffects.NIGHT_VISION)
                && !entity.hasEffect(MobEffects.DARKNESS)) return;
        if (!LegacyCloudAtmosphere.shouldUseConsoleAtmosphere(effects())) return;
        //? if <1.21.2 {
        cir.setReturnValue(LegacyCloudAtmosphere.getWarmCloudColor(cir.getReturnValue(), LegacyCloudAtmosphere.getTimeOfDay((ClientLevel) (Object) this, partialTick)));
        //?} else {
        /*cir.setReturnValue(LegacyCloudAtmosphere.getWarmCloudColor(cir.getReturnValue(), LegacyCloudAtmosphere.getTimeOfDay((ClientLevel) (Object) this, partialTick)));
        *///?}
    }

    @Inject(method = "setServerVerifiedBlockState", at = @At("HEAD"))
    private void handleDragonEggTeleportBlockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        ClientLevel level = (ClientLevel) (Object) this;
        LegacyDragonEggTeleportParticles.handleBlockUpdate(level, pos, level.getBlockState(pos), state);
    }
}
