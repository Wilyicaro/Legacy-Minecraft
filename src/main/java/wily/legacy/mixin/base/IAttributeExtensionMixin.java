//? if >=1.20.5 && neoforge {
/*package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import net.neoforged.neoforge.common.extensions.IAttributeExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

import java.text.DecimalFormat;

@Mixin(value = IAttributeExtension.class, remap = false)
public interface IAttributeExtensionMixin {

    @ModifyReceiver(method = "toValueComponent", at = @At(value = "INVOKE", target = "Ljava/text/DecimalFormat;format(D)Ljava/lang/String;"))
    private DecimalFormat toValueComponent(DecimalFormat instance, double v) {
        instance.setMinimumFractionDigits(FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat) ? 1 : 0);
        return instance;
    }
}
*///?}