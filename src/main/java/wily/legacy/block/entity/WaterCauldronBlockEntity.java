package wily.legacy.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import wily.legacy.init.LegacyRegistries;

public class WaterCauldronBlockEntity extends BlockEntity {
    public Holder<Potion> potion = /*? if <1.20.5 {*//*BuiltInRegistries.POTION.wrapAsHolder(Potions.WATER)*//*?} else {*/Potions.WATER/*?}*/;
    public Holder<Item> lastPotionItemUsed = Items.POTION.builtInRegistryHolder();
    public Integer waterColor;
    public WaterCauldronBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get(),blockPos, blockState);
    }
    @Override
    public CompoundTag getUpdateTag(/*? if >=1.20.5 {*/HolderLookup.Provider provider/*?}*/) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag/*? if >=1.20.5 {*/, provider/*?}*/);
        return tag;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level.isClientSide){
            level.sendBlockUpdated(getBlockPos(),getBlockState(),getBlockState(),2);
        }else if (level instanceof ServerLevel l){
            l.getChunkSource().blockChanged(getBlockPos());
        }
    }

    public boolean hasWater(){
        return potion./*? if <1.20.5 {*//*value().*//*?}*/equals(Potions.WATER);
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void /*? if <1.20.5 {*//*load*//*?} else {*/loadAdditional/*?}*/(CompoundTag compoundTag/*? if >=1.20.5 {*/, HolderLookup.Provider provider/*?}*/) {
        super./*? if <1.20.5 {*//*load*//*?} else {*/loadAdditional/*?}*/(compoundTag/*? if >=1.20.5 {*/, provider/*?}*/);
        if (compoundTag.contains("dyeColor")) waterColor = compoundTag.getInt("dyeColor");
       BuiltInRegistries.POTION./*? if <1.21.2 {*//*getHolder*//*?} else {*/get/*?}*/(ResourceKey.create(Registries.POTION, ResourceLocation.tryParse(compoundTag.getString("potion")))).ifPresent(p-> potion = p);
       BuiltInRegistries.ITEM./*? if <1.21.2 {*//*getHolder*//*?} else {*/get/*?}*/(ResourceKey.create(Registries.ITEM, ResourceLocation.tryParse(compoundTag.getString("lastItemUsed")))).ifPresent(p-> lastPotionItemUsed = p);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag/*? if >=1.20.5 {*/, HolderLookup.Provider provider/*?}*/) {
        super.saveAdditional(compoundTag/*? if >=1.20.5 {*/, provider/*?}*/);
        if (waterColor != null) compoundTag.putInt("dyeColor",waterColor);
        potion.unwrapKey().ifPresent(r-> compoundTag.putString("potion",r.location().toString()));
        lastPotionItemUsed.unwrapKey().ifPresent(r-> compoundTag.putString("lastPotionUsed",r.location().toString()));
    }
}
