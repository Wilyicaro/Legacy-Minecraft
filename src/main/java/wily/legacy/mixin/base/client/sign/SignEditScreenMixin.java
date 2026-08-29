package wily.legacy.mixin.base.client.sign;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.PlainSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SignEditScreen.class)
public abstract class SignEditScreenMixin extends AbstractSignEditScreen {


    public SignEditScreenMixin(SignBlockEntity signBlockEntity, boolean bl, boolean bl2) {
        super(signBlockEntity, bl, bl2);
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/StandingSignRenderer;createSignModel(Lnet/minecraft/client/model/geom/EntityModelSet;Lnet/minecraft/world/level/block/state/properties/WoodType;Lnet/minecraft/world/level/block/PlainSignBlock$Attachment;)Lnet/minecraft/client/model/Model$Simple;"), index = 2)
    private PlainSignBlock.Attachment useStandingSign(PlainSignBlock.Attachment original) {
        return PlainSignBlock.Attachment.GROUND;
    }

    @ModifyReturnValue(method = "getSignYOffset", at = @At("RETURN"))
    private float offsetSign(float original) {
        return height / 2f - 26.5f;
    }

    @ModifyArg(method = "extractSignBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;sign(Lnet/minecraft/client/model/Model$Simple;FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V"))
    private float renderSignBackground(float original) {
        return original * 144 / 93;
    }

    @ModifyArg(method = "extractSignBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;sign(Lnet/minecraft/client/model/Model$Simple;FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V"), index = 3)
    private int changeSignX0(int original) {
        return original - 30;
    }

    @ModifyArg(method = "extractSignBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;sign(Lnet/minecraft/client/model/Model$Simple;FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V"), index = 5)
    private int changeSignX1(int original) {
        return original + 30;
    }

    @ModifyArg(method = "extractSignBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;sign(Lnet/minecraft/client/model/Model$Simple;FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V"), index = 4)
    private int changeSignY0(int original) {
        return height / 2 - 82;
    }

    @ModifyArg(method = "extractSignBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;sign(Lnet/minecraft/client/model/Model$Simple;FLnet/minecraft/world/level/block/state/properties/WoodType;IIII)V"), index = 6)
    private int changeSignY1(int original) {
        return height / 2 + 78;
    }
}
