package wily.legacy.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.inventory.LegacyMerchantOffer;

@Mixin(MerchantOffer.class)
public class MerchantOfferMixin implements LegacyMerchantOffer {

    private int requiredLevel;
    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void init(CompoundTag compoundTag, CallbackInfo ci){
        if (compoundTag.contains("requiredLevel", 3))
            this.requiredLevel = compoundTag.getInt("requiredLevel");
    }
    @Inject(method = "createTag", at = @At("RETURN"))
    private void createTag(CallbackInfoReturnable<CompoundTag> cir){
        cir.getReturnValue().putInt("requiredLevel", requiredLevel);
    }
    @Inject(method = "copy", at = @At("RETURN"))
    private void copy(CallbackInfoReturnable<MerchantOffer> cir){
       ((LegacyMerchantOffer) cir.getReturnValue()).setRequiredLevel(requiredLevel);
    }
    @Override
    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    @Override
    public int getRequiredLevel() {
        return requiredLevel;
    }
}
