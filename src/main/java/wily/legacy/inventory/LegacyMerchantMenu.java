package wily.legacy.inventory;

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
import wily.factoryapi.base.StackIngredient;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.network.ServerMenuCraftPayload;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class LegacyMerchantMenu extends LegacyCraftingMenu {
    public final Merchant merchant;
    public int merchantLevel;
    public boolean showProgressBar;
    public LegacyMerchantMenu(int i, Inventory inventory) {
        this(i, inventory, new ClientSideMerchant(inventory.player));
    }
    public LegacyMerchantMenu(int i, Inventory inventory, Merchant merchant) {
        super(LegacyRegistries.MERCHANT_MENU.get(), i, p->merchant.getTradingPlayer() == p);
        addInventorySlotGrid(inventory, 9,133, 98,3);
        addInventorySlotGrid(inventory, 0,133, 154,1);
        this.merchant = merchant;
    }

    public static List<Optional<Ingredient>> ingredientsFromStacks(ItemStack... s){
        return Arrays.stream(s).map(i-> i.isEmpty() ? Optional.<Ingredient>empty() : Optional.of((Ingredient) StackIngredient.of(true,i))).toList();
    }

    @Override
    public void onCraft(Player player, ServerMenuCraftPayload packet, ItemStack result) {
        super.onCraft(player, packet, result);
        if (player instanceof ServerPlayer && packet.button() >= 0 && packet.button() < merchant.getOffers().size() && !merchant.getOffers().isEmpty()){
            MerchantOffer offer = merchant.getOffers().get(packet.button());
            offer.getResult().onCraftedBy(player.level(),player,offer.getResult().getCount());
            if (merchant instanceof LivingEntity e) e.playSound(merchant.getNotifyTradeSound(), 1.0f, e.getVoicePitch());
            merchant.notifyTrade(offer);
            player.awardStat(Stats.TRADED_WITH_VILLAGER);
            merchant.overrideXp(merchant.getVillagerXp() + offer.getXp());
            player.sendMerchantOffers(containerId, merchant.getOffers(), merchant instanceof Villager v ? v.getVillagerData().getLevel() : 0, merchant.getVillagerXp(), merchant.showProgressBar(), merchant.canRestock());
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.merchant.setTradingPlayer(null);
    }

    @Override
    public ItemStack getResult(Player player, ServerMenuCraftPayload packet) {
        if (player instanceof ServerPlayer && packet.button() >= 0 && packet.button() < merchant.getOffers().size() && !merchant.getOffers().isEmpty())
            return merchant.getOffers().get(packet.button()).getResult();
        return ItemStack.EMPTY;
    }

    @Override
    public List<Optional<Ingredient>> getIngredients(Player player, ServerMenuCraftPayload packet) {
        if (player instanceof ServerPlayer && packet.button() >= 0 && packet.button() < merchant.getOffers().size() && !merchant.getOffers().isEmpty())
            return ingredientsFromStacks(merchant.getOffers().get(packet.button()).getCostA(),merchant.getOffers().get(packet.button()).getCostB());
        return super.getIngredients(player, packet);
    }
}
