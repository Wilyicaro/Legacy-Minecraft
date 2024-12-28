package wily.legacy.mixin.base;

import net.minecraft.client.gui.screens.DisconnectedScreen;
//? if >=1.20.5 {
import net.minecraft.network.DisconnectionDetails;
//?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.client.DisconnectedScreenAccessor;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin implements DisconnectedScreenAccessor {
    //? if <1.20.5 {
    /*@Shadow @Final private Component reason;
    *///?} else {
    @Shadow @Final private DisconnectionDetails details;
    //?}

    @Shadow @Final public Screen parent;

    @Override
    public Component getReason() {
        return /*? if <1.20.5 {*//*reason*//*?} else {*/details.reason()/*?}*/;
    }

    @Override
    public Screen getParent() {
        return parent;
    }
}
