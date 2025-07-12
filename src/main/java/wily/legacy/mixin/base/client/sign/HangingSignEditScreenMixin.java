package wily.legacy.mixin.base.client.sign;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HangingSignEditScreen.class)
public abstract class HangingSignEditScreenMixin extends Screen {


    protected HangingSignEditScreenMixin(Component component) {
        super(component);
    }

    @ModifyReturnValue(method = "getSignYOffset", at = @At("RETURN"))
    private float offsetSign(float original){
        return height - 26.5f;
    }

    @Redirect(method = "renderSignBackground", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;scale(FF)Lorg/joml/Matrix3x2f;", remap = false))
    private Matrix3x2f renderSignBackground(Matrix3x2fStack instance, float x, float y){
        return instance.scale(x * 144/93,y * 144/93);
    }
}
