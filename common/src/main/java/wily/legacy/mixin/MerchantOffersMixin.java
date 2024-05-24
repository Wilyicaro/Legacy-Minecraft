package wily.legacy.mixin;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.inventory.LegacyMerchantOffer;

@Mixin(MerchantOffers.class)
public class MerchantOffersMixin {
    @Inject(method = "writeToStream", at = @At("HEAD"), cancellable = true)
    private void write(FriendlyByteBuf buf, CallbackInfo info){
        info.cancel();
        buf.writeCollection((MerchantOffers)(Object)this, (friendlyByteBuf, merchantOffer) -> {
            ItemStack result = merchantOffer.getResult().copy();
            result.getOrCreateTag().putInt("requiredLevel",((LegacyMerchantOffer)merchantOffer).getRequiredLevel());
            friendlyByteBuf.writeItem(merchantOffer.getBaseCostA());
            friendlyByteBuf.writeItem(result);
            friendlyByteBuf.writeItem(merchantOffer.getCostB());
            friendlyByteBuf.writeBoolean(merchantOffer.isOutOfStock());
            friendlyByteBuf.writeInt(merchantOffer.getUses());
            friendlyByteBuf.writeInt(merchantOffer.getMaxUses());
            friendlyByteBuf.writeInt(merchantOffer.getXp());
            friendlyByteBuf.writeInt(merchantOffer.getSpecialPriceDiff());
            friendlyByteBuf.writeFloat(merchantOffer.getPriceMultiplier());
            friendlyByteBuf.writeInt(merchantOffer.getDemand());
        });
    }
    @Inject(method = "createFromStream", at = @At("HEAD"), cancellable = true)
    private static void createFromStream(FriendlyByteBuf buf, CallbackInfoReturnable<MerchantOffers> info) {
        info.setReturnValue(buf.readCollection(MerchantOffers::new, friendlyByteBuf -> {
            ItemStack itemStack = friendlyByteBuf.readItem();
            ItemStack itemStack2 = friendlyByteBuf.readItem();
            ItemStack itemStack3 = friendlyByteBuf.readItem();
            boolean bl = friendlyByteBuf.readBoolean();
            int i = friendlyByteBuf.readInt();
            int j = friendlyByteBuf.readInt();
            int k = friendlyByteBuf.readInt();
            int l = friendlyByteBuf.readInt();
            float f = friendlyByteBuf.readFloat();
            int m = friendlyByteBuf.readInt();
            MerchantOffer merchantOffer = new MerchantOffer(itemStack, itemStack3, itemStack2, i, j, k, f, m);
            if (bl) {
                merchantOffer.setToOutOfStock();
            }
            merchantOffer.setSpecialPriceDiff(l);
            ((LegacyMerchantOffer)merchantOffer).setRequiredLevel(itemStack3.getOrCreateTag().getInt("requiredLevel"));
            itemStack3.removeTagKey("requiredLevel");
            return merchantOffer;
        }));
    }
}
