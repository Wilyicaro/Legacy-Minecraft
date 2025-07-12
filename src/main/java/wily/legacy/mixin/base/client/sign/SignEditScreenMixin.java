package wily.legacy.mixin.base.client.sign;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SignEditScreen.class)
public abstract class SignEditScreenMixin extends Screen {


    protected SignEditScreenMixin(Component component) {
        super(component);
    }

    @ModifyReturnValue(method = "getSignYOffset", at = @At("RETURN"))
    private float offsetSign(float original){
        return height + 15.5f;
    }

    @ModifyArg(method = "renderSignBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;submitSignRenderState(Lnet/minecraft/client/model/Model;FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V"))
    private float renderSignBackground(float original){
        return original * 144/93;
    }
}
