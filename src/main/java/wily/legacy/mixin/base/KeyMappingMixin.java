package wily.legacy.mixin.base;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.LegacyKeyMapping;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements LegacyKeyMapping {

    @Shadow private InputConstants.Key key;
    private ControllerBinding defaultButton;
    private ControllerBinding button;
    @Inject(method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILjava/lang/String;)V",at = @At("RETURN"))
    private void init(String string, InputConstants.Type type, int i, String string2, CallbackInfo ci){
        ControllerBinding b = ControllerBinding.getDefaultKeyMappingBinding(i);
        setDefaultBinding(b);
        setBinding(b);
    }

    @Override
    public ControllerBinding getDefaultBinding() {
        return defaultButton;
    }

    @Override
    public ControllerBinding getBinding() {
        return button;
    }

    @Override
    public void setBinding(ControllerBinding binding) {
        this.button = binding;
    }

    @Override
    public void setDefaultBinding(ControllerBinding binding) {
        this.defaultButton = binding;
    }

    @Override
    public InputConstants.Key getKey() {
        return key;
    }
}
