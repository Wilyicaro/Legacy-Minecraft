package wily.legacy.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.studiohartman.jamepad.ControllerButton;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.controller.ControllerComponent;
import wily.legacy.client.controller.ControllerHandler;
import wily.legacy.client.controller.LegacyKeyMapping;

import java.util.function.Function;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements LegacyKeyMapping {
    @Shadow public abstract InputConstants.Key getDefaultKey();

    private ControllerComponent defaultButton;
    private ControllerComponent button;
    @Inject(method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILjava/lang/String;)V",at = @At("RETURN"))
    private void init(String string, InputConstants.Type type, int i, String string2, CallbackInfo ci){
        ControllerComponent b = ControllerHandler.DEFAULT_CONTROLLER_BUTTONS_BY_KEY.get(i);
        setDefaultButton(b);
        setButton(b);
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
    public void setButton(ControllerComponent button) {
        this.button = button;
    }

    @Override
    public void setDefaultButton(ControllerComponent defaultButton) {
        this.defaultButton = defaultButton;
    }
}
