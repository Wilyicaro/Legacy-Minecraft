package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import wily.legacy.client.screen.ControlTooltip;

import java.util.function.Function;

public interface LegacyKeyMapping {
    default KeyMapping keyToDefaultButton(Function<InputConstants.Key,ControllerComponent> buttonGetter){
        setDefaultComponent(buttonGetter.apply(self().getDefaultKey()));
        return self();
    }
    default KeyMapping self(){
        return (KeyMapping) this;
    }
    ControllerComponent getDefaultComponent();
    ControllerComponent getComponent();

    default Component getDisplayName() {
        String name = self().getName();
        Minecraft minecraft = Minecraft.getInstance();
        boolean hasPlayer = minecraft.player != null && minecraft.gameMode != null;
        switch (name){
            case "legacy.key.crafting" -> name = hasPlayer && minecraft.gameMode.hasInfiniteItems() ? "selectWorld.gameMode.creative" : name;
            case "legacy.key.inventory" -> name = "key.inventory";
        }
        return ControlTooltip.CONTROL_ACTION_CACHE.getUnchecked(name);
    }
    InputConstants.Key getKey();

    void setComponent(ControllerComponent button);
    void setDefaultComponent(ControllerComponent button);
}
