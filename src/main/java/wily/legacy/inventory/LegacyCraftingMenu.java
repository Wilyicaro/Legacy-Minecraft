package wily.legacy.inventory;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
//? if >=1.20.5 {
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.FactoryIngredient;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.network.ServerMenuCraftPayload;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public abstract class LegacyCraftingMenu extends AbstractContainerMenu implements RecipeMenu {
    public static final Component CRAFTING_TITLE = Component.translatable("container.crafting");
    public static final Component STONECUTTER_TITLE = Component.translatable("container.stonecutter");
    public static final Component LOOM_TITLE = Component.translatable("container.loom");
    public static final Map<ResourceKey<BannerPattern>,Optional<Ingredient>> LOOM_PATTERN_EXTRA_INGREDIENT_CACHE = new ConcurrentHashMap<>();
    public final Predicate<Player> stillValid;
    public boolean inventoryActive = true;
    public static final Vec3 DEFAULT_INVENTORY_OFFSET = new Vec3(0.5,0.5,0);
    public Vec3 inventoryOffset = DEFAULT_INVENTORY_OFFSET;
    public boolean showedNotEnoughIngredientsHint = false;

    public static LegacyCraftingMenu craftingMenu(Inventory inventory, @Nullable MenuType<?> menuType, int i, BlockPos pos, int gridDimension){
        return new LegacyCraftingMenu(inventory,menuType,i,p->pos == null || isValidBlock(p,pos, Blocks.CRAFTING_TABLE)) {
            /*? if >1.20.1 {*/RecipeHolder<CraftingRecipe>/*?} else {*//*CraftingRecipe*//*?}*/ customRcp;
            final CraftingContainer container = new TransientCraftingContainer(this,gridDimension,gridDimension);
            //? if >=1.20.5
            CraftingInput input;

            @Override
            public void onCraft(Player player, ServerMenuCraftPayload packet, ItemStack result) {
                super.onCraft(player, packet, result);
                if (packet.craftId().isPresent() || customRcp != null) player.getServer().getRecipeManager().byKey(customRcp == null ? getRecipeKey(packet.craftId().get()) : customRcp./*? if >1.20.1 {*/id()/*?} else {*//*getId()*//*?}*/).ifPresent(h-> player.triggerRecipeCrafted(h,container.getItems()));
            }

            @Override
            public ItemStack getResult(Player player, ServerMenuCraftPayload packet) {
                //? if >=1.20.5
                input = container.asCraftInput();
                if (packet.craftId().isEmpty()) return player.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,/*? if <1.20.5 {*//*container*//*?} else {*/input/*?}*/,player.level()).map(h-> (customRcp = h)/*? if >1.20.1 {*/.value()/*?}*/.assemble(/*? if <1.20.5 {*//*container*//*?} else {*/input/*?}*/,player.level().registryAccess())).orElse(ItemStack.EMPTY);
                return player.getServer().getRecipeManager().byKey(getRecipeKey(packet.craftId().get())).map(h-> h/*? if >1.20.1 {*/.value()/*?}*/ instanceof CraftingRecipe rcp ? rcp.assemble(/*? if <1.20.5 {*//*container*//*?} else {*/input/*?}*/,player.level().registryAccess()) : null).orElse(ItemStack.EMPTY);
            }

            @Override
            public List<ItemStack> getRemainingItems(Player player, ServerMenuCraftPayload packet) {
                return player.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,/*? if <1.20.5 {*//*container*//*?} else {*/input/*?}*/,player.level()).map(h->(List<ItemStack>) h/*? if >1.20.1 {*/.value()/*?}*/.getRemainingItems(/*? if <1.20.5 {*//*container*//*?} else {*/input/*?}*/)).orElse(super.getRemainingItems(player,packet));
            }

            @Override
            public List<Optional<Ingredient>> getIngredients(Player player, ServerMenuCraftPayload packet) {
                return packet.craftId().isEmpty() || !packet.customIngredients().isEmpty() ? super.getIngredients(player,packet) : player.getServer().getRecipeManager().byKey(getRecipeKey(packet.craftId().get())).map(r-> getRecipeOptionalIngredients(r/*? if >1.20.1 {*/.value()/*?}*/)).orElse(Collections.emptyList());
            }

            @Override
            public void setupActualItems(Player player, ServerMenuCraftPayload packet, ItemStack setItem, int index) {
               if (setItem == null)
                   container.clearContent();
               else container.setItem(index,setItem);
            }
        };
    }

    public static List<Optional<Ingredient>> getRecipeOptionalIngredients(Recipe<?> recipe){
        //? if <1.21.2 {
        /*return convertIngredientsToOptional(recipe.getIngredients());
        *///?} else {
        return recipe instanceof ShapedRecipe rcp ? rcp.getIngredients() : recipe instanceof ShapelessRecipe sr ? sr.placementInfo().ingredients().stream().map(Optional::of).toList() : recipe instanceof StonecutterRecipe stonecutterRecipe ? Collections.singletonList(Optional.of(stonecutterRecipe.input())) : Collections.emptyList();
        //?}
    }

    //? if <1.21.2 {
    /*static List<Optional<Ingredient>> convertIngredientsToOptional(List<Ingredient> ingredients){
        return ingredients.stream().map(i-> i.isEmpty() ? Optional.<Ingredient>empty() : Optional.of(i)).toList();
    }
    *///?}

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
        return new LegacyCraftingMenu(inventory, LegacyRegistries.LOOM_PANEL_MENU.get(), window, p->isValidBlock(p, blockPos, Blocks.LOOM)) {
            final Container container = new SimpleContainer(3);

            @Override
            public List<Optional<Ingredient>> getIngredients(Player player, ServerMenuCraftPayload packet) {
                List<Optional<Ingredient>> customIngredients = super.getIngredients(player, packet);
                if (packet.craftId().isPresent() && customIngredients.size() == 2 && Arrays.stream(FactoryIngredient.of(customIngredients.get(0).get()).getStacks()).allMatch(i->i.getItem() instanceof BannerItem) && Arrays.stream(FactoryIngredient.of(customIngredients.get(1).get()).getStacks()).allMatch(i->i.getItem() instanceof DyeItem)){
                    return player.level().registryAccess().lookup(Registries.BANNER_PATTERN).flatMap(b->
                            b.get(ResourceKey.create(Registries.BANNER_PATTERN, packet.craftId().get())).map(p-> {
                                        Optional<Ingredient> extraIng = getBannerPatternExtraIngredient(player.level().registryAccess(),p.key());
                                        if (extraIng.isEmpty()) return customIngredients;
                                        else return ImmutableList.<Optional<Ingredient>>builder().addAll(customIngredients).add(extraIng).build();
                                    }
                            )).orElse(Collections.emptyList());
                }
                return Collections.emptyList();
            }

            @Override
            public ItemStack getResult(Player player, ServerMenuCraftPayload packet) {
                return player.level().registryAccess().lookup(Registries.BANNER_PATTERN).flatMap(b->
                        b.get(ResourceKey.create(Registries.BANNER_PATTERN, packet.craftId().get())).map(p-> {
                                    ItemStack banner = container.getItem(0);
                                    //? if <1.20.5 {
                                    /*CompoundTag beTag = banner.getOrCreateTagElement("BlockEntityTag");
                                    ListTag patternsTag = beTag.getList("Patterns", 10);
                                    beTag.put("Patterns", patternsTag);
                                    CompoundTag patternTag = new CompoundTag();
                                    patternsTag.add(patternTag);
                                    patternTag.putString("Pattern", p.value().getHashname());
                                    patternTag.putInt("Color", ((DyeItem) container.getItem(1).getItem()).getDyeColor().getId());
                                    *///?} else {
                                    banner.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers.Builder().addAll(banner.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)).add(p, ((DyeItem) container.getItem(1).getItem()).getDyeColor()).build());
                                     //?}
                                    return banner;
                                })).orElse(ItemStack.EMPTY);
            }

            @Override
            public List<ItemStack> getRemainingItems(Player player, ServerMenuCraftPayload packet) {
                return container.getItem(2).isEmpty() ? super.getRemainingItems(player,packet) : List.of(container.getItem(2));
            }

            @Override
            public void setupActualItems(Player player, ServerMenuCraftPayload packet, ItemStack setItem, int index) {
                if (setItem == null)
                    container.clearContent();
                else container.setItem(index,setItem);
            }
        };
    }
    public static LegacyCraftingMenu loomMenu(int window, Inventory inventory){
        return loomMenu(window,inventory,null);
    }
    public static LegacyCraftingMenu stoneCutterMenu(int window, Inventory inventory, BlockPos blockPos){
        return new LegacyCraftingMenu(inventory, LegacyRegistries.STONECUTTER_PANEL_MENU.get(),window, p->isValidBlock(p,blockPos, Blocks.STONECUTTER)){
            long lastSoundTime;
            @Override
            public void onCraft(Player player, ServerMenuCraftPayload packet, ItemStack result) {
                super.onCraft(player, packet, result);
                long l = player.level().getGameTime();
                if (lastSoundTime != l) {
                    player.level().playSound(null, blockPos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f);
                    lastSoundTime = l;
                }
            }

            @Override
            public List<Optional<Ingredient>> getIngredients(Player player, ServerMenuCraftPayload packet) {
                return player.getServer().getRecipeManager().byKey(getRecipeKey(packet.craftId().get())).map(r-> getRecipeOptionalIngredients(r/*? if >1.20.1 {*/.value()/*?}*/)).orElse(Collections.emptyList());
            }

            @Override
            public ItemStack getResult(Player player, ServerMenuCraftPayload packet) {
                return player.getServer().getRecipeManager().byKey(getRecipeKey(packet.craftId().get())).map(h->h/*? if >1.20.1 {*/.value()/*?}*/ instanceof StonecutterRecipe rcp ? rcp.assemble( null,player.level().registryAccess()) : null).orElse(ItemStack.EMPTY);
            }
        };
    }
    public static LegacyCraftingMenu stoneCutterMenu(int window, Inventory inventory){
        return stoneCutterMenu(window, inventory,null);
    }
    public LegacyCraftingMenu(@Nullable MenuType<?> menuType, int i, Predicate<Player> stillValid) {
        super(menuType, i);
        this.stillValid = stillValid;
    }
    public LegacyCraftingMenu(Inventory inventory, @Nullable MenuType<?> menuType, int i, Predicate<Player> stillValid) {
        this(menuType, i,stillValid);
        addInventorySlotGrid(inventory, 9,186, 133,3);
        addInventorySlotGrid(inventory, 0,186, 186,1);
    }

    public static List<Optional<Ingredient>> updateShapedIngredients(List<Optional<Ingredient>> ingredientsGrid, List<Optional<Ingredient>> recipeIngredients, int gridDimension, int rcpWidth, int rcpHeight){
        int rcpDimension = Math.max(rcpWidth, rcpHeight);
        if (rcpDimension > gridDimension) return Collections.emptyList();
        Ingredient[] ingredients = new Ingredient[rcpDimension * rcpDimension];
        for (int i = 0; i < recipeIngredients.size(); i++)
            ingredients[rcpWidth < rcpHeight ? (i / rcpWidth) * rcpHeight + (i % rcpWidth) : i] = recipeIngredients.get(i).orElse(null);
        for (int i = 0; i < ingredients.length; i++)
            ingredientsGrid.set(i > 1 && gridDimension > rcpDimension ? i + 1 : i, Optional.ofNullable(ingredients[i]));
        return ingredientsGrid;
    }

    public static Optional<Ingredient> getBannerPatternExtraIngredient(RegistryAccess registryAccess, ResourceKey<BannerPattern> pattern){
        return LOOM_PATTERN_EXTRA_INGREDIENT_CACHE.computeIfAbsent(pattern,key->{
            Holder<BannerPattern> holder = registryAccess.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(pattern);
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof BannerPatternItem p && holder.is(p.getBannerPattern())) return Optional.of(Ingredient.of(p));
            }
            return Optional.empty();
        });
    }


    public /*? if >=1.21.2 {*/ResourceKey<Recipe<?>>/*?} else {*//*ResourceLocation*//*?}*/ getRecipeKey(ResourceLocation id){
        return /*? if >=1.21.2 {*/ResourceKey.create(Registries.RECIPE,id)/*?} else {*//*id*//*?}*/;
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
                    public Vec3 getOffset() {
                        return inventoryOffset;
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
                slot.setByPlayer(ItemStack.EMPTY/*? if >1.20.1 {*/, itemStack/*?}*/);
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
        return stillValid.test(player);
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
    public void onCraft(Player player, ServerMenuCraftPayload packet, ItemStack result) {
        result.onCraftedBy(player.level(),player,result.getCount());

    }
}
