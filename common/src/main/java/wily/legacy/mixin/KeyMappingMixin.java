package wily.legacy.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.controller.ControllerComponent;
import wily.legacy.client.controller.ControllerHandler;
import wily.legacy.client.controller.LegacyKeyMapping;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements LegacyKeyMapping {

    @Shadow private InputConstants.Key key;
    private ControllerComponent defaultButton;
    private ControllerComponent button;
    @Inject(method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILjava/lang/String;)V",at = @At("RETURN"))
    private void init(String string, InputConstants.Type type, int i, String string2, CallbackInfo ci){
        ControllerComponent b = ControllerHandler.getDefaultKeyMappingComponent(i);
        setDefaultComponent(b);
        setComponent(b);
    }

    @Override
    public ControllerComponent getDefaultComponent() {
        return defaultButton;
    }

    @Override
    public ControllerComponent getComponent() {
        return button;
    }

    @Override
    public void setComponent(ControllerComponent button) {
        this.button = button;
    }

    @Override
    public void setDefaultComponent(ControllerComponent defaultButton) {
        this.defaultButton = defaultButton;
    }

    @Override
    public InputConstants.Key getKey() {
        return key;
    }
}
