package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.particle.ItemPickupParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import wily.legacy.client.LegacyItemPickupParticle;
import wily.legacy.client.LegacyOptions;

@Mixin(targets = {"net.minecraft.client.particle.ItemPickupParticleGroup$ParticleInstance"})
public class ItemPickupParticleGroupMixin {

    @ModifyVariable(method = "fromParticle", at = @At("STORE"), index = 3)
    private static float render(float original, ItemPickupParticle itemPickupParticle, Camera camera, float partialTick) {
        return LegacyOptions.legacyItemPickup.get() ? ((float) ((LegacyItemPickupParticle)itemPickupParticle).getPickupLife() + partialTick) / ((LegacyItemPickupParticle)itemPickupParticle).getPickupLifetime() : original;
    }
}
