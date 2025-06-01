package wily.legacy.block;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import wily.legacy.block.entity.WaterCauldronBlockEntity;

public class ColoredWaterCauldronBlock extends LayeredCauldronBlock {
    public static final BooleanProperty MID = BooleanProperty.create("mid");

    public ColoredWaterCauldronBlock(Properties properties) {
        super(/*? if <1.20.2 {*//*properties, p-> p == Biome.Precipitation.NONE, CauldronInteraction.WATER*//*?} else {*/Biome.Precipitation.NONE, CauldronInteraction.WATER, properties/*?}*/);
        Item.BY_BLOCK.put(this, Items.CAULDRON);
        this.registerDefaultState(defaultBlockState().setValue(MID, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MID);
    }

    public static void lowerFillLevel(WaterCauldronBlockEntity be) {
        be.convertToColored();
        if (be.getBlockState().getValue(MID)) {
            LayeredCauldronBlock.lowerFillLevel(be.getBlockState().setValue(MID, false), be.getLevel(), be.getBlockPos());
        } else be.getLevel().setBlock(be.getBlockPos(), be.getBlockState().setValue(MID, true), 3);
    }

    @Override
    public boolean isFull(BlockState blockState) {
        return super.isFull(blockState) && !blockState.getValue(MID);
    }
}
