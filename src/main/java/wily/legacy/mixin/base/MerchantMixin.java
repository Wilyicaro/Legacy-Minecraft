package wily.legacy.mixin.base;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import wily.legacy.inventory.LegacyMerchantMenu;
import wily.legacy.entity.LegacyPlayer;

import java.util.OptionalInt;
@Mixin(Merchant.class)
public interface MerchantMixin {
    /**
     * @author Wilyicaro
     * @reason Add Legacy Merchant menu option
     */
    @Overwrite
    default void openTradingScreen(Player player2, Component component, int i2) {
        MerchantOffers merchantOffers;
        OptionalInt optionalInt = player2.openMenu(new SimpleMenuProvider((i, inventory, player) -> player2 instanceof LegacyPlayer p && !p.hasClassicCrafting() ? new LegacyMerchantMenu(i,inventory,(Merchant) this) : new MerchantMenu(i, inventory, (Merchant) this), component));
        if (optionalInt.isPresent() && !(merchantOffers = ((Merchant) this).getOffers()).isEmpty())
            player2.sendMerchantOffers(optionalInt.getAsInt(), merchantOffers, i2, ((Merchant) this).getVillagerXp(), ((Merchant) this).showProgressBar(), ((Merchant) this).canRestock());

    }
}
