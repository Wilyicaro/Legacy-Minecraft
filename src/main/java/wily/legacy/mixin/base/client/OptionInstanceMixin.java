package wily.legacy.mixin.base.client;

import com.mojang.serialization.Codec;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.OptionInstanceAccessor;

import java.util.function.Consumer;

@Mixin(OptionInstance.class)
public abstract class OptionInstanceMixin implements OptionInstanceAccessor {
    @Shadow public abstract void set(Object object);

    @Shadow @Final private OptionInstance.TooltipSupplier tooltip;
    @Unique
    Object defaultValue;
    @Unique
    String key;
    @Inject(method = "<init>(Ljava/lang/String;Lnet/minecraft/client/OptionInstance$TooltipSupplier;Lnet/minecraft/client/OptionInstance$CaptionBasedToString;Lnet/minecraft/client/OptionInstance$ValueSet;Lcom/mojang/serialization/Codec;Ljava/lang/Object;Ljava/util/function/Consumer;)V", at = @At("RETURN"))
    public void init(String string, OptionInstance.TooltipSupplier tooltipSupplier, OptionInstance.CaptionBasedToString captionBasedToString, OptionInstance.ValueSet valueSet, Codec codec, Object object, Consumer consumer, CallbackInfo ci) {
        Legacy4JClient.whenResetOptions.add(()->set(object));
        defaultValue = object;
        key = string;
    }

    @Override
    public Object defaultValue() {
        return defaultValue;
    }

    @Override
    public OptionInstance.TooltipSupplier tooltip() {
        return tooltip;
    }

    @Override
    public String getKey() {
        return key;
    }
}
