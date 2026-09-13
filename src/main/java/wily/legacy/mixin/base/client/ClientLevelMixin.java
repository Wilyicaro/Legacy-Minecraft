package wily.legacy.mixin.base.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.control.Controller;
import wily.legacy.client.control.ControllerManager;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level {
    @Shadow
    @Final
    private Minecraft minecraft;

    protected ClientLevelMixin(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Inject(method = "trackExplosionEffects", at = @At("RETURN"))
    private void trackExplosionEffects(Vec3 center, float radius, int blockCount, WeightedList<ExplosionParticleInfo> blockParticles, CallbackInfo ci) {
        float vibration = LegacyOptions.vibrationWhenExploding.get().floatValue();

        if (vibration <= 0) return;

        Controller controller = Legacy4JClient.controllerManager.connectedController;

        if (controller != null && Legacy4JClient.controllerManager.isControllerTheLastInput()) {
            LocalPlayer player = minecraft.player;
            Vec2 rumble = ControllerManager.rumbleIntensityFromTarget(player.position(), player.getRotationVector(), center, radius, 0.7f).scale(vibration);

            if (rumble.length() == 0) return;

//            Legacy4J.LOGGER.warn("Left Rumble: {}, Right Rumble: {}", rumble.x, rumble.y);

            controller.rumble((char) (Character.MAX_VALUE * rumble.x), (char) (Character.MAX_VALUE * rumble.y), 800);
        }
    }
}
