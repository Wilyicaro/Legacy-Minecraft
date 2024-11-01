package wily.legacy.neoforge.mixin;

import net.neoforged.neoforge.client.ClientNeoForgeMod;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.neoforge.Legacy4JForgeClient;

@Mixin(ClientNeoForgeMod.class)
public class ClientNeoForgeModMixin {

    @ModifyArg(method = "onRegisterClientExtensions", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/extensions/common/RegisterClientExtensionsEvent;registerFluidType(Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;[Lnet/neoforged/neoforge/fluids/FluidType;)V", ordinal = 0), index = 0)
    private static IClientFluidTypeExtensions onRegisterClientExtensions(IClientFluidTypeExtensions extensions){
        return Legacy4JForgeClient.CLIENT_WATER_FLUID_TYPE;
    }
}
