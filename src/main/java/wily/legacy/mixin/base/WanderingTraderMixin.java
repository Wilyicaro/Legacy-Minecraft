package wily.legacy.mixin.base;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

import java.util.Set;

@Mixin(WanderingTrader.class)
public abstract class WanderingTraderMixin extends AbstractVillager {
    @Unique
    private static final Set<Item> LEGACY_TRADE_RESULTS = Set.of(Items.LILAC, Items.ROSE_BUSH, Items.SUNFLOWER, Items.PEONY, Items.RED_SANDSTONE);

    @Unique
    private ItemStack legacy$tradePreview = ItemStack.EMPTY;

    protected WanderingTraderMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "updateTrades", at = @At("RETURN"))
    private void legacy$removeLegacyTrades(ServerLevel level, CallbackInfo ci) {
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyLootTables)) {
            return;
        }
        getOffers().removeIf(offer -> LEGACY_TRADE_RESULTS.contains(offer.assemble().getItem()));
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void showTradePreview(CallbackInfo ci) {
        if (!(level() instanceof ServerLevel level) || isTrading() || isBaby() || isUsingItem() || legacy$wantsVisibilityItem()) {
            legacy$clearTradePreview();
            return;
        }

        Player player = level.getNearestPlayer(this, 4.0D);
        ItemStack preview = player == null ? ItemStack.EMPTY : legacy$getTradePreview(player.getMainHandItem());
        if (preview.isEmpty()) {
            legacy$clearTradePreview();
        } else if (!ItemStack.matches(legacy$tradePreview, preview)) {
            legacy$tradePreview = preview;
            setItemSlot(EquipmentSlot.MAINHAND, preview);
            setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    @Unique
    private ItemStack legacy$getTradePreview(ItemStack item) {
        if (item.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (MerchantOffer offer : getOffers()) {
            if (!offer.isOutOfStock() && (ItemStack.isSameItem(item, offer.getCostA()) || ItemStack.isSameItem(item, offer.getCostB()))) {
                return offer.assemble();
            }
        }
        return ItemStack.EMPTY;
    }

    @Unique
    private void legacy$clearTradePreview() {
        if (legacy$tradePreview.isEmpty()) {
            return;
        }

        legacy$tradePreview = ItemStack.EMPTY;
        if (isUsingItem()) {
            return;
        }
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        setDropChance(EquipmentSlot.MAINHAND, 0.085F);
    }

    @Unique
    private boolean legacy$wantsVisibilityItem() {
        return level().isDarkOutside() && !isInvisible() || level().isBrightOutside() && isInvisible();
    }
}
