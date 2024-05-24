package wily.legacy.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import wily.legacy.init.LegacyRegistries;

public class WaterCauldronBlockEntity extends BlockEntity {
    public Holder<Potion> potion = Potions.WATER;
    public Integer waterColor;
    public WaterCauldronBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get(),blockPos, blockState);
    }
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag,provider);
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

    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag,provider);
        if (compoundTag.contains("dyeColor")) waterColor = compoundTag.getInt("dyeColor");
        potion = BuiltInRegistries.POTION.getHolder(ResourceLocation.tryParse(compoundTag.getString("potion"))).map(r->(Holder<Potion>)r).orElse(Potions.WATER);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag,provider);
        if (waterColor != null) compoundTag.putInt("dyeColor",waterColor);
        compoundTag.putString("potion", BuiltInRegistries.POTION.getKey(potion.value()).toString());
    }
}
