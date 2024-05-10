package wily.legacy.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import wily.legacy.init.LegacyMenuTypes;

import java.util.List;

public class LegacyMerchantMenu extends LegacyCraftingMenu{
    public final Merchant merchant;
    public int merchantLevel;
    public boolean showProgressBar;
    public LegacyMerchantMenu(int i, Inventory inventory) {
        this(i, inventory, new ClientSideMerchant(inventory.player));
    }
    public LegacyMerchantMenu(int i, Inventory inventory, Merchant merchant) {
        super(LegacyMenuTypes.MERCHANT_MENU.get(), i, BlockPos.ZERO);
        addInventorySlotGrid(inventory, 9,130, 98,3);
        addInventorySlotGrid(inventory, 0,130, 154,1);
        this.merchant = merchant;
    }

    @Override
    public void onCraft(Player player, int buttonInfo, List<Ingredient> ingredients, ItemStack result) {
        super.onCraft(player, buttonInfo, ingredients, result);
        if (player instanceof ServerPlayer && buttonInfo >= 0 && buttonInfo < merchant.getOffers().size() && !merchant.getOffers().isEmpty()){
            MerchantOffer offer = merchant.getOffers().get(buttonInfo);
            offer.getResult().onCraftedBy(player.level(),player,offer.getResult().getCount());
            if (merchant instanceof LivingEntity e) e.playSound(merchant.getNotifyTradeSound(), 1.0f, e.getVoicePitch());
            merchant.notifyTrade(offer);
            player.awardStat(Stats.TRADED_WITH_VILLAGER);
            merchant.overrideXp(merchant.getVillagerXp() + offer.getXp());
            player.sendMerchantOffers(containerId, merchant.getOffers(), merchant instanceof Villager v ? v.getVillagerData().getLevel() : 0, merchant.getVillagerXp(), showProgressBar, merchant.canRestock());
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.merchant.setTradingPlayer(null);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.merchant.getTradingPlayer() == player;
    }
}
