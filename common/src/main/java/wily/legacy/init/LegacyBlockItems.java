package wily.legacy.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import wily.legacy.LegacyMinecraft;

public class LegacyBlockItems {

    private static final DeferredRegister<Block> BLOCK_ITEMS_REGISTER = DeferredRegister.create(LegacyMinecraft.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEM_REGISTER = DeferredRegister.create(LegacyMinecraft.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Block> SHRUB = BLOCK_ITEMS_REGISTER.register("shrub",()-> new TallGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).replaceable().noCollission().instabreak().sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XYZ).ignitedByLava().pushReaction(PushReaction.DESTROY)));

    public static void register(){
        if (LegacyMinecraft.serverProperties != null && !LegacyMinecraft.serverProperties.legacyRegistries) return;
        BLOCK_ITEMS_REGISTER.register();
        BLOCK_ITEMS_REGISTER.forEach(b-> ITEM_REGISTER.register(b.getId(),()-> new BlockItem(b.get(), new Item.Properties())));
        ITEM_REGISTER.register();
    }
}
