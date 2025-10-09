package wily.legacy.mixin.base.client.sign;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SignEditScreen.class)
public abstract class SignEditScreenMixin extends AbstractSignEditScreen {


    public SignEditScreenMixin(SignBlockEntity signBlockEntity, boolean bl, boolean bl2) {
        super(signBlockEntity, bl, bl2);
    }

    @Unique
    private int getYOffset() {
        return this.sign.getBlockState().getBlock() instanceof StandingSignBlock ? -42 : 0;
    }

    @ModifyReturnValue(method = "getSignYOffset", at = @At("RETURN"))
    private float offsetSign(float original) {
        return height / 2f + 15.5f + getYOffset();
    }

    @ModifyArg(method = "renderSignBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;submitSignRenderState(Lnet/minecraft/client/model/Model$Simple;FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V"))
    private float renderSignBackground(float original){
        return original * 144/93;
    }

    @ModifyArg(method = "renderSignBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;submitSignRenderState(Lnet/minecraft/client/model/Model$Simple;FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V"), index = 3)
    private int changeSignX0(int original) {
        return original - 30;
    }

    @ModifyArg(method = "renderSignBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;submitSignRenderState(Lnet/minecraft/client/model/Model$Simple;FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V"), index = 5)
    private int changeSignX1(int original) {
        return original + 30;
    }

    @ModifyArg(method = "renderSignBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;submitSignRenderState(Lnet/minecraft/client/model/Model$Simple;FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V"), index = 4)
    private int changeSignY0(int original) {
        return height / 2 - 40 + getYOffset();
    }

    @ModifyArg(method = "renderSignBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;submitSignRenderState(Lnet/minecraft/client/model/Model$Simple;FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V"), index = 6)
    private int changeSignY1(int original) {
        return  height / 2 + 120 + getYOffset();
    }
}
