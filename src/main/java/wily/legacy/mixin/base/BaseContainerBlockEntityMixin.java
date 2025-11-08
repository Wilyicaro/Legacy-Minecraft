package wily.legacy.mixin.base;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.block.entity.BaseContainerBlockEntityAccessor;

import static net.minecraft.world.level.block.entity.BlockEntity.parseCustomNameSafe;

@Mixin(BaseContainerBlockEntity.class)
public class BaseContainerBlockEntityMixin implements BaseContainerBlockEntityAccessor {
    @Unique
    Component tempName;

    @Override
    public Component getTempName() {
        return tempName;
    }

    @Override
    public void setTempName(Component component) {
        this.tempName = component;
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void loadAdditional(ValueInput input, CallbackInfo ci) {
        this.tempName = parseCustomNameSafe(input, "TempName");
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void saveAdditional(ValueOutput valueOutput, CallbackInfo ci) {
        valueOutput.storeNullable("TempName", ComponentSerialization.CODEC, this.tempName);
    }

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void getName(CallbackInfoReturnable<Component> cir) {
        if (tempName != null) cir.setReturnValue(tempName);
    }
}
