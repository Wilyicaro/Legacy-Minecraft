package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.util.function.Function;

public interface LegacyKeyMapping {
    default KeyMapping keyToDefaultButton(Function<InputConstants.Key,ControllerComponent> buttonGetter){
        setDefaultButton(buttonGetter.apply(self().getDefaultKey()));
        return self();
    }
    default KeyMapping self(){
        return (KeyMapping) this;
    }
    ControllerComponent getDefaultComponent();
    ControllerComponent getComponent();
    void setButton(ControllerComponent button);
    void setDefaultButton(ControllerComponent button);
}
