package wily.legacy.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import wily.factoryapi.util.CompoundTagUtil;
import wily.legacy.init.LegacyRegistries;

public class WaterCauldronBlockEntity extends BlockEntity {
    public Holder<Potion> potion = getDefaultPotion();
    public Holder<Item> lastPotionItemUsed = Items.POTION.builtInRegistryHolder();
    public Integer waterColor;

    public WaterCauldronBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get(), blockPos, blockState);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        saveAdditional(output);
        return output.buildResult();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level == null)
            return;
        if (!level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
            if (level instanceof ServerLevel l) {
                l.getChunkSource().blockChanged(getBlockPos());
            }
        }
    }

    public void convertToColored() {
        convertTo(LegacyRegistries.COLORED_WATER_CAULDRON.get());
    }

    public void convertTo(LayeredCauldronBlock block) {
        if (getBlockState().is(block)) return;
        BlockState coloredBlockState = block.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, getBlockState().getValue(LayeredCauldronBlock.LEVEL));
        level.setBlock(getBlockPos(), coloredBlockState, 3);
        setBlockState(coloredBlockState);
        level.setBlockEntity(this);
    }

    public void setWaterColor(Integer waterColor) {
        if (waterColor == null) {
            convertTo((LayeredCauldronBlock) Blocks.WATER_CAULDRON);
        } else convertToColored();
        this.waterColor = waterColor;
        setChanged();
    }

    public Holder<Potion> getDefaultPotion() {
        return Potions.WATER;
    }

    public boolean hasWater() {
        return potion.value().equals(Potions.WATER.value());
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        waterColor = null;
        input.getInt("dyeColor").ifPresent(i -> waterColor = i);
        input.getString("potion").flatMap(id -> BuiltInRegistries.POTION.get(ResourceKey.create(Registries.POTION, ResourceLocation.tryParse(id)))).ifPresent(p -> potion = p);
        input.getString("lastPotionItemUsed").flatMap(id -> BuiltInRegistries.ITEM.get(ResourceKey.create(Registries.ITEM, ResourceLocation.tryParse(id)))).ifPresent(p -> lastPotionItemUsed = p);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (waterColor != null) {
            output.putInt("dyeColor", waterColor);
        }
        potion.unwrapKey().ifPresent(r -> output.putString("potion", r.location().toString()));
        lastPotionItemUsed.unwrapKey().ifPresent(r -> output.putString("lastPotionItemUsed", r.location().toString()));
    }
}
