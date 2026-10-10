package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.ColorResolver;
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
import wily.legacy.client.BedrockSnowyLeaves;
import wily.legacy.client.control.Controller;
import wily.legacy.client.control.ControllerManager;
import wily.legacy.client.control.LegacyControlsOptions;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches;

    @Shadow
    public abstract int calculateBlockTint(BlockPos pos, ColorResolver colorResolver);

    protected ClientLevelMixin(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void legacy$registerSnowyBiomeTint(CallbackInfo ci) {
        tintCaches.put(BedrockSnowyLeaves.SNOWY_BIOME_RESOLVER, new BlockTintCache(pos -> calculateBlockTint(pos, BedrockSnowyLeaves.SNOWY_BIOME_RESOLVER)));
    }

    @ModifyReturnValue(method = "getClientLeafTintColor", at = @At("RETURN"))
    private int legacy$applySnowyLeaves(int color, @Local(argsOnly = true) BlockPos pos) {
        return BedrockSnowyLeaves.applyParticleTint(getBlockState(pos), (ClientLevel) (Object) this, pos, color);
    }

    @Inject(method = "trackExplosionEffects", at = @At("RETURN"))
    private void trackExplosionEffects(Vec3 center, float radius, int blockCount, WeightedList<ExplosionParticleInfo> blockParticles, CallbackInfo ci) {
        float vibration = LegacyControlsOptions.vibrationWhenExploding.get().floatValue();

        if (vibration <= 0 || radius <= 0) return;

        Controller controller = ControllerManager.getInstance().connectedController;

        if (controller != null && ControllerManager.getInstance().isControllerTheLastInput()) {
            LocalPlayer player = minecraft.player;
            Vec2 rumble = ControllerManager.rumbleIntensityFromTarget(player.position(), player.getRotationVector(), center, radius + 2, 0.7f).scale(vibration);

            if (rumble.length() == 0) return;

//            Legacy4J.LOGGER.warn("Left Rumble: {}, Right Rumble: {}", rumble.x, rumble.y);

            controller.rumble((char) (Character.MAX_VALUE * rumble.x), (char) (Character.MAX_VALUE * rumble.y), 800);
        }
    }
}
