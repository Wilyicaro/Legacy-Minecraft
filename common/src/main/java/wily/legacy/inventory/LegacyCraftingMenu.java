package wily.legacy.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.jetbrains.annotations.Nullable;
import wily.legacy.util.Offset;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.network.ServerMenuCraftPacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class LegacyCraftingMenu extends AbstractContainerMenu implements RecipeMenu {
    public static final Component CRAFTING_TITLE = Component.translatable("container.crafting");
    public static final Component STONECUTTER_TITLE = Component.translatable("container.stonecutter");
    public static final Component LOOM_TITLE = Component.translatable("container.loom");
    public static final Map<ResourceKey<BannerPattern>,Ingredient> LOOM_PATTERN_EXTRA_INGREDIENT_CACHE = new ConcurrentHashMap<>();
    final BlockPos blockPos;
    public boolean inventoryActive = true;
    public boolean showedNotEnoughIngredientsHint = false;

    public static LegacyCraftingMenu craftingMenu(Inventory inventory, @Nullable MenuType<?> menuType, int i, BlockPos pos, int gridDimension){
        return new LegacyCraftingMenu(inventory,menuType,i,pos) {
            RecipeHolder<CraftingRecipe> customRcp;
            final CraftingContainer container = new TransientCraftingContainer(this,gridDimension,gridDimension);
            final List<Ingredient> ingredientsGrid = new ArrayList<>(Collections.nCopies(gridDimension * gridDimension,Ingredient.EMPTY));
            CraftingInput input;

            @Override
            public void onCraft(Player player, ServerMenuCraftPacket packet, ItemStack result) {
                super.onCraft(player, packet, result);
                player.level().getRecipeManager().byKey(customRcp == null ? packet.craftId() : customRcp.id()).ifPresent(h-> player.triggerRecipeCrafted(h,container.getItems()));
            }

            @Override
            public ItemStack getResult(Player player, ServerMenuCraftPacket packet) {
                input = container.asCraftInput();
                if (packet.craftId().equals(ServerMenuCraftPacket.EMPTY)) return player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,player.level()).map(h-> (customRcp = h).value().assemble(input,player.registryAccess())).orElse(ItemStack.EMPTY);
                return player.level().getRecipeManager().byKey(packet.craftId()).map(h-> h.value() instanceof CraftingRecipe rcp ? rcp.assemble(input,player.registryAccess()) : null).orElse(ItemStack.EMPTY);
            }

            @Override
            public List<ItemStack> getRemainingItems(Player player, ServerMenuCraftPacket packet) {
                return player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,player.level()).map(h->(List<ItemStack>) h.value().getRemainingItems(input)).orElse(super.getRemainingItems(player,packet));
            }

            @Override
            public List<Ingredient> getIngredients(Player player, ServerMenuCraftPacket packet) {
                List<Ingredient> ings = packet.craftId().equals(ServerMenuCraftPacket.EMPTY) || !packet.customIngredients().isEmpty() ? super.getIngredients(player,packet) : player.level().getRecipeManager().byKey(packet.craftId()).map(h->(List<Ingredient>) h.value().getIngredients()).orElse(Collections.emptyList());
                updateShapedIngredients(ingredientsGrid,ings,gridDimension,gridDimension,gridDimension,gridDimension);
                return ingredientsGrid;
            }

            @Override
            public void setupActualItems(Player player, ServerMenuCraftPacket packet, ItemStack setItem, int index) {
               if (setItem == null)
                   container.clearContent();
               else container.setItem(index,setItem);
            }
        };
    }
    public static LegacyCraftingMenu playerCraftingMenu(int window, Inventory inventory){
        return craftingMenu(inventory, LegacyRegistries.PLAYER_CRAFTING_PANEL_MENU.get(),window,null,2);
    }
    public static LegacyCraftingMenu craftingMenu(int window, Inventory inventory, BlockPos pos){
        return craftingMenu(inventory, LegacyRegistries.CRAFTING_PANEL_MENU.get(),window,pos,3);
    }
    public static LegacyCraftingMenu craftingMenu(int window, Inventory inventory){
        return craftingMenu(window, inventory,null);
    }
    public static LegacyCraftingMenu loomMenu(int window, Inventory inventory, BlockPos blockPos){
        return new LegacyCraftingMenu(inventory, LegacyRegistries.LOOM_PANEL_MENU.get(), window, blockPos) {
            @Override
            public ItemStack getResult(Player player, ServerMenuCraftPacket packet) {
                return player.registryAccess().lookup(Registries.BANNER_PATTERN).flatMap(b->
                        b.get(ResourceKey.create(Registries.BANNER_PATTERN, packet.craftId())).map(p-> {
                                    if (packet.customIngredients().size() > 1 && packet.customIngredients().size() <= 3){
                                        Ingredient extraIng;
                                        if ((extraIng = getBannerPatternExtraIngredient(player.registryAccess(),p.key())).isEmpty() || packet.customIngredients().get(1).equals(extraIng)) {
                                            ItemStack banner = packet.customIngredients().get(0).getItems()[0].copy();
                                            banner.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers.Builder().addAll(banner.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)).add(p, ((DyeItem) packet.customIngredients().get(extraIng.isEmpty() ? 1 : 2).getItems()[0].getItem()).getDyeColor()).build());
                                            return banner;
                                        }
                                    }
                                    return ItemStack.EMPTY;
                                }
                        )).orElse(ItemStack.EMPTY);
            }

            @Override
            public List<ItemStack> getRemainingItems(Player player, ServerMenuCraftPacket packet) {
                return packet.customIngredients().size() < 3 ? super.getRemainingItems(player,packet) : List.of(packet.customIngredients().get(1).getItems()[0]);
            }
        };
    }
    public static LegacyCraftingMenu loomMenu(int window, Inventory inventory){
        return loomMenu(window,inventory,null);
    }
    public static LegacyCraftingMenu stoneCutterMenu(int window, Inventory inventory, BlockPos blockPos){
        return new LegacyCraftingMenu(inventory, LegacyRegistries.STONECUTTER_PANEL_MENU.get(),window,blockPos){
            long lastSoundTime;
            @Override
            public void onCraft(Player player, ServerMenuCraftPacket packet, ItemStack result) {
                super.onCraft(player, packet, result);
                long l = player.level().getGameTime();
                if (lastSoundTime != l) {
                    player.level().playSound(null, this.blockPos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f);
                    lastSoundTime = l;
                }
            }

            @Override
            public List<Ingredient> getIngredients(Player player, ServerMenuCraftPacket packet) {
                return player.level().getRecipeManager().byKey(packet.craftId()).map(h->(List<Ingredient>)h.value().getIngredients()).orElse(Collections.emptyList());
            }

            @Override
            public ItemStack getResult(Player player, ServerMenuCraftPacket packet) {
                return player.level().getRecipeManager().byKey(packet.craftId()).map(h->h.value() instanceof StonecutterRecipe rcp ? rcp.getResultItem(player.registryAccess()) : null).orElse(ItemStack.EMPTY);
            }
        };
    }
    public static LegacyCraftingMenu stoneCutterMenu(int window, Inventory inventory){
        return stoneCutterMenu(window, inventory,null);
    }
    public LegacyCraftingMenu(@Nullable MenuType<?> menuType, int i, BlockPos pos) {
        super(menuType, i);
        this.blockPos =pos;
    }
    public LegacyCraftingMenu(Inventory inventory, @Nullable MenuType<?> menuType, int i, BlockPos pos) {
        this(menuType, i,pos);
        addInventorySlotGrid(inventory, 9,186, 133,3);
        addInventorySlotGrid(inventory, 0,186, 186,1);
    }

    public static void updateShapedIngredients(List<Ingredient> ingredientsGrid, List<Ingredient> recipeIngredients, int gridDimension, int rcpDimension, int rcpWidth, int rcpHeight){
        Ingredient[] ingredients = new Ingredient[rcpDimension * rcpDimension];
        for (int i = 0; i < recipeIngredients.size(); i++)
            ingredients[rcpWidth < rcpHeight ? (i / rcpWidth) * rcpHeight + (i % rcpWidth) : i] = recipeIngredients.get(i);

        for (int i = 0; i < ingredients.length; i++)
            ingredientsGrid.set(i > 1 && gridDimension > rcpDimension ? i + 1 : i, ingredients[i] == null ? Ingredient.EMPTY : ingredients[i]);
    }

    public static Ingredient getBannerPatternExtraIngredient(RegistryAccess registryAccess, ResourceKey<BannerPattern> pattern){
        if (LOOM_PATTERN_EXTRA_INGREDIENT_CACHE.containsKey(pattern)) return LOOM_PATTERN_EXTRA_INGREDIENT_CACHE.get(pattern);
        Holder<BannerPattern> holder = registryAccess.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(pattern);
        Ingredient patternIng = BuiltInRegistries.ITEM.stream().filter(i-> i instanceof BannerPatternItem p && holder.is(p.getBannerPattern())).map(Ingredient::of).findFirst().orElse(Ingredient.EMPTY);
        LOOM_PATTERN_EXTRA_INGREDIENT_CACHE.put(pattern,patternIng);
        return patternIng;
    }


    public void addInventorySlotGrid(Container container, int startIndex, int x, int y, int rows){
        for (int j = 0; j < rows; j++) {
            for (int k = 0; k < 9; k++) {
                addSlot(LegacySlotDisplay.override(new Slot(container,startIndex +  j * 9 + k,x + k * 16,y + j * 16){
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        slotsChanged(container);
                    }
                    @Override
                    public boolean isActive() {
                        return inventoryActive;
                    }
                }, new LegacySlotDisplay(){
                    public Offset getOffset() {
                        return new Offset(0.5,0.5,0);
                    }
                    public int getWidth() {
                        return 16;
                    }
                    public int getHeight() {
                        return 16;
                    }
                }));
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (i >= 0 && i < 27) {
                if (!this.moveItemStackTo(itemStack2, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 0, 27, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY, itemStack);
            } else {
                slot.setChanged();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemStack2);
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockPos == null || !player.level().getBlockState(blockPos).isAir() && player.distanceToSqr((double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5) <= 64.0;
    }

    public static boolean isValidBlock(Player player, BlockPos pos, Block wantedBlock){
        return player.level().getBlockState(pos).is(wantedBlock) && player.distanceToSqr((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5) <= 64.0;
    }

    public static MenuProvider getMenuProvider(BlockPos pos, boolean is2x2) {
        return new SimpleMenuProvider((i, inventory, player) ->  is2x2 ? playerCraftingMenu(i,inventory) : craftingMenu(i, inventory, pos), CRAFTING_TITLE);
    }
    public static MenuProvider getMenuProvider(MenuConstructor constructor, Component component) {
        return new SimpleMenuProvider(constructor, component);
    }
    public static MenuProvider getLoomMenuProvider(BlockPos pos) {
        return getMenuProvider((i,inv,p)-> loomMenu(i,inv,pos), LOOM_TITLE);
    }
    public static MenuProvider getStonecutterMenuProvider(BlockPos pos) {
        return getMenuProvider((i,inv,p)-> stoneCutterMenu(i,inv,pos), STONECUTTER_TITLE);
    }
    public void onCraft(Player player, ServerMenuCraftPacket packet, ItemStack result) {
        result.onCraftedBy(player.level(),player,result.getCount());

    }
}
