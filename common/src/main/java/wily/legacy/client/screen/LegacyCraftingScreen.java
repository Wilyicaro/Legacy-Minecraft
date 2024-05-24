package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import org.jetbrains.annotations.Nullable;
import wily.legacy.Legacy4JPlatform;
import wily.legacy.client.LegacyCraftingTabListing;
import wily.legacy.client.Offset;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.network.ServerInventoryCraftPacket;
import wily.legacy.util.PagedList;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static wily.legacy.util.LegacySprites.SMALL_ARROW;
import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.RecipeIconHolder.getActualItem;

public class LegacyCraftingScreen extends AbstractContainerScreen<LegacyCraftingMenu> implements Controller.Event {
    public static final Offset CRAFTING_OFFSET = new Offset(0.5,0.5,0);
    private final Inventory inventory;
    private final boolean is2x2;
    private final int gridDimension;
    private boolean onlyCraftableRecipes = false;
    protected final List<Ingredient> ingredientsGrid;
    protected ItemStack resultStack = ItemStack.EMPTY;
    public static final Component COLOR_TAB = Component.translatable("legacy.container.tab.color");
    public static final Component SHAPE_TAB = Component.translatable("legacy.container.tab.shape");
    public static final Component EFFECT_TAB = Component.translatable("legacy.container.tab.effect");
    public static final Component SELECT_STAR_TAB = Component.translatable("legacy.container.tab.select_star");
    public static final Component ADD_FADE_TAB = Component.translatable("legacy.container.tab.add_fade");
    public static final Component ADD_POWER_TAB = Component.translatable("legacy.container.tab.add_power");
    public static final Component SELECT_SHIELD_BANNER = Component.translatable("legacy.container.tab.select_shield_banner");
    public static final Component COPY_BANNER = Component.translatable("legacy.container.tab.copy_banner");
    public static final Component ADD_SHERD = Component.translatable("legacy.container.tab.add_pottery_sherd");
    protected final List<RecipeIconHolder<CraftingRecipe>> craftingButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> dyeItemButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> dyeArmorButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> dyeBannerButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> decoratedPotButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> fireworkStarButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> fireworkStarFadeButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> fireworkButtons = new ArrayList<>();
    protected final List<List<List<CraftingRecipe>>> recipesByTab = new ArrayList<>();
    protected List<List<CraftingRecipe>> filteredRecipesByGroup = Collections.emptyList();
    protected final Stocker.Sizeable page =  new Stocker.Sizeable(0);
    protected final Stocker.Sizeable craftingButtonsOffset =  new Stocker.Sizeable(0);
    protected final TabList craftingTabList = new TabList(new PagedList<>(page,7));
    protected final TabList fireworkTabList = new TabList();
    protected final TabList dyeTabList = new TabList();
    protected final TabList groupTabList = new TabList().add(0,0,42, 42, 4, new ResourceLocation("crafting_table"),null,Component.empty(),null,b->repositionElements()).add(0,0,42, 42, 4, new ResourceLocation("firework_rocket"),null,Component.empty(),null,b->resetElements()).add(0,0,42, 42, 4, new ResourceLocation("cyan_dye"),null,Component.empty(),null,b->resetElements());
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    private final boolean[] warningSlots;
    protected final ContainerListener listener = new ContainerListener() {
        public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
            if (onlyCraftableRecipes && groupTabList.selectedTab == 0) {
                filteredRecipesByGroup = recipesByTab.get(craftingTabList.selectedTab).stream().map(l -> l.stream().filter(r -> ServerInventoryCraftPacket.canCraft(r.getIngredients(), inventory,abstractContainerMenu.getCarried())).toList()).filter(l -> !l.isEmpty()).toList();
                craftingButtons.get(selectedCraftingButton).updateRecipeDisplay();
            }else {
                if (getCraftingButtons().size() > selectedCraftingButton && getCraftingButtons().get(selectedCraftingButton) instanceof CustomCraftingIconHolder h) h.updateRecipe();
            }
        }

        public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int j) {

        }
    };
    protected int selectedCraftingButton;
    public static final Item[] VANILLA_CATEGORY_ICONS = new Item[]{Items.BRICKS,Items.REDSTONE,Items.GOLDEN_SWORD,Items.LAVA_BUCKET};
    protected RecipeManager manager;
    public static LegacyCraftingScreen craftingScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component){
        return new LegacyCraftingScreen(abstractContainerMenu,inventory,component,false);
    }
    public static LegacyCraftingScreen playerCraftingScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component){
        return new LegacyCraftingScreen(abstractContainerMenu,inventory,component,true);
    }
    protected boolean inited = false;
    public LegacyCraftingScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component, boolean is2x2) {
        super(abstractContainerMenu, inventory, component);
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().tooltips.set(0,create(()->getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_RETURN,true) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(true),()->getFocused() instanceof RecipeIconHolder<?> h && h.canCraft() || getFocused() instanceof CustomCraftingIconHolder c && c.canCraft() ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.create") : null));
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().add(()-> getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_O,true) : ControllerBinding.UP_BUTTON.bindingState.getIcon(true), ()-> groupTabList.selectedTab == 0 ? CONTROL_ACTION_CACHE.getUnchecked(onlyCraftableRecipes ? "legacy.action.all_recipes" : "legacy.action.show_craftable_recipes") : getFocused() instanceof CustomCraftingIconHolder h && h.canAddIngredient() ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.add") : null);
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().add(()-> getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_X,true) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(true), ()-> groupTabList.selectedTab != 0 && getFocused() instanceof CustomCraftingIconHolder h && h.addedIngredientsItems != null && !h.addedIngredientsItems.isEmpty() ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.remove") : null);
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().addCompound(()-> new Component[]{ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET,true) : ControllerBinding.LEFT_BUMPER.bindingState.getIcon(true),ControlTooltip.SPACE,ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET,true) : ControllerBinding.RIGHT_BUMPER.bindingState.getIcon(true)},()->CONTROL_ACTION_CACHE.getUnchecked("legacy.action.group"));
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().add(()-> ControlTooltip.getActiveType().isKeyboard() ? COMPOUND_COMPONENT_FUNCTION.apply(new Component[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT,true),ControlTooltip.PLUS,ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET,true),ControlTooltip.SPACE,ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET,true)}) : COMPOUND_COMPONENT_FUNCTION.apply(new Component[]{ControllerBinding.LEFT_TRIGGER.bindingState.getIcon(true),ControlTooltip.SPACE, ControllerBinding.RIGHT_TRIGGER.bindingState.getIcon(true)}),()->CONTROL_ACTION_CACHE.getUnchecked("legacy.action.type"));
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().add(()-> page.max > 0 && groupTabList.selectedTab == 0 ? ControlTooltip.getActiveType().isKeyboard() ? COMPOUND_COMPONENT_FUNCTION.apply(new Component[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT,true),ControlTooltip.PLUS,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT,true),ControlTooltip.SPACE,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT,true)}) : ControllerBinding.RIGHT_STICK.bindingState.getIcon(true) : null,()->CONTROL_ACTION_CACHE.getUnchecked("legacy.action.page"));
        this.inventory = inventory;
        this.is2x2 = is2x2;
        gridDimension = is2x2 ? 2 : 3;
        ingredientsGrid = new ArrayList<>(Collections.nCopies(gridDimension * gridDimension,Ingredient.EMPTY));
        warningSlots = new boolean[gridDimension * gridDimension];
        if (Minecraft.getInstance().level == null) return;
        manager = Minecraft.getInstance().level.getRecipeManager();
        for (LegacyCraftingTabListing listing : LegacyCraftingTabListing.list) {
            if (!listing.isValid()) continue;
            List<List<CraftingRecipe>> groups = new ArrayList<>();
            listing.craftings.values().forEach(l->{
                if (l.isEmpty()) return;
                List<CraftingRecipe> group = new ArrayList<>();
                l.forEach(v->v.addRecipes(RecipeType.CRAFTING,manager,group,r-> !is2x2 || is2x2Recipe(r)));
                if (!group.isEmpty()) groups.add(group);
            });
            if (groups.isEmpty()) continue;

            recipesByTab.add(groups);

            craftingTabList.addTabButton(43,0,listing.icon,listing.itemIconTag,listing.displayName, t->resetElements());

        }
        if (ScreenUtil.getLegacyOptions().vanillaTabs().get()) manager.getAllRecipesFor(RecipeType.CRAFTING).stream().collect(Collectors.groupingBy(h->h.value().category(),()->new TreeMap<>(Comparator.comparingInt(Enum::ordinal)),Collectors.groupingBy(h->h.value().getGroup().isEmpty() ? h.id().toString() : h.value().getGroup()))).forEach((category, m)->{
            if (m.isEmpty()) return;
            List<List<CraftingRecipe>> groups = new ArrayList<>();
            m.values().forEach(l->{
                List<CraftingRecipe> group = l.stream().filter(h->!(h.value() instanceof CustomRecipe) && (!is2x2 || is2x2Recipe(h.value()))).map(RecipeHolder::value).collect(Collectors.toList());
                if (!group.isEmpty()) groups.add(group);
            });
            if (groups.isEmpty()) return;
            recipesByTab.add(groups);
            craftingTabList.addTabButton(43,0,VANILLA_CATEGORY_ICONS[category.ordinal()].arch$registryName(), getTitle(), t->resetElements());
        });
        craftingTabList.resetSelectedTab();
        inited = true;
        addCraftingButtons();
        CompoundTag redStarTag = new CompoundTag();
        CompoundTag explosionTag = new CompoundTag();
        redStarTag.put("Explosion",explosionTag);
        explosionTag.putIntArray("Colors", List.of(DyeColor.RED.getFireworkColor()));
        fireworkTabList.add(0,0,0,43,0,new ResourceLocation("firework_star"),null, Component.empty(),null, b-> resetElements());
        fireworkTabList.add(0,0,0,43,0,new ResourceLocation("firework_star"),redStarTag, Component.empty(),null, b-> resetElements());
        fireworkTabList.add(0,0,0,43,0,new ResourceLocation("firework_rocket"),null, Component.empty(),null, b-> resetElements());
        dyeTabList.add(0,0,0,43,0,new ResourceLocation("leather_chestplate"),DyeableLeatherItem.dyeArmor(Items.LEATHER_CHESTPLATE.getDefaultInstance(),List.of((DyeItem) Items.GREEN_DYE)).getTag(), Component.empty(),null, b-> resetElements());
        dyeTabList.add(0,0,0,43,0,groupTabList.tabButtons.get(2).icon,null, Component.empty(),null, b-> resetElements());
        dyeTabList.add(0,0,0,43,0,new ResourceLocation("white_banner"),null, Component.empty(),null, b-> resetElements());
        if (!is2x2) dyeTabList.add(0,0,0,43,0,new ResourceLocation("decorated_pot"),null, Component.empty(),null, b-> resetElements());
        Consumer<CustomCraftingIconHolder> fireworkStarUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (fireworkStarButtons.isEmpty()) return;
            updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(fireworkStarButtons.get(0).itemIcon),Ingredient.EMPTY, FireworkStarRecipe.GUNPOWDER_INGREDIENT),gridDimension,2,2,2);
            fireworkStarButtons.forEach(CustomRecipeIconHolder::applyAddedIngredients);
            resultStack = Items.FIREWORK_STAR.getDefaultInstance();
            if (fireworkStarButtons.get(0).hasItem()) {
                CompoundTag fireworkExplosionTag = resultStack.getOrCreateTagElement("Explosion");
                fireworkExplosionTag.putIntArray("Colors", Stream.concat(Stream.of(fireworkStarButtons.get(0).itemIcon),fireworkStarButtons.get(0).addedIngredientsItems.stream()).map(i->((DyeItem)i.getItem()).getDyeColor().getFireworkColor()).toList());
                FireworkRocketItem.Shape s;
                if (!fireworkStarButtons.get(1).addedIngredientsItems.isEmpty() && (s = FireworkStarRecipe.SHAPE_BY_ITEM.get(fireworkStarButtons.get(1).addedIngredientsItems.get(0).getItem())) != null) fireworkExplosionTag.putByte("Type", (byte) s.getId());
                if (!fireworkStarButtons.get(2).addedIngredientsItems.isEmpty()){
                    if (fireworkStarButtons.get(2).addedIngredientsItems.stream().anyMatch(FireworkStarRecipe.FLICKER_INGREDIENT)) fireworkExplosionTag.putBoolean("Flicker", true);
                    if (fireworkStarButtons.get(2).addedIngredientsItems.stream().anyMatch(FireworkStarRecipe.TRAIL_INGREDIENT)) fireworkExplosionTag.putBoolean("Trail", true);
                }
            }
            canCraft(ingredientsGrid,true);
        };
        Consumer<CustomCraftingIconHolder> fireworkStarFadeUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (fireworkStarFadeButtons.isEmpty()) return;
            ItemStack item = fireworkStarFadeButtons.get(0).itemIcon.isEmpty() ? Items.FIREWORK_STAR.getDefaultInstance() : fireworkStarFadeButtons.get(0).itemIcon;
            updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(fireworkStarFadeButtons.get(1).itemIcon),Ingredient.EMPTY, Legacy4JPlatform.getStrictNBTIngredient(item)),gridDimension,2,2,2);
            fireworkStarFadeButtons.get(1).applyAddedIngredients();
            resultStack = item.copy();
            if (fireworkStarFadeButtons.get(1).hasItem()) resultStack.getOrCreateTagElement("Explosion").putIntArray("FadeColors", Stream.concat(Stream.of(fireworkStarFadeButtons.get(1).itemIcon),fireworkStarFadeButtons.get(1).addedIngredientsItems.stream()).map(i->((DyeItem)i.getItem()).getDyeColor().getFireworkColor()).toList());
            canCraft(ingredientsGrid,true);
        };
        Consumer<CustomCraftingIconHolder> fireworkRocketUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (fireworkButtons.isEmpty()) return;
            updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(fireworkButtons.get(0).itemIcon),Ingredient.EMPTY,FireworkRocketRecipe.PAPER_INGREDIENT),gridDimension,2,2,2);
            fireworkButtons.forEach(CustomRecipeIconHolder::applyAddedIngredients);
            resultStack = new ItemStack(Items.FIREWORK_ROCKET,3);
            if (fireworkButtons.get(0).hasItem()) {
                CompoundTag fireworksTag = resultStack.getOrCreateTagElement("Fireworks");
                ListTag explosionsTag = new ListTag();
                fireworksTag.putByte("Flight",(byte)(fireworkButtons.get(0).addedIngredientsItems.size() + 1));
                fireworksTag.put("Explosions",explosionsTag);
                fireworkButtons.get(1).addedIngredientsItems.forEach(i-> {
                    CompoundTag tag;
                    if ((tag = i.getTagElement("Explosion")) != null) explosionsTag.add(tag);});

            }
            canCraft(ingredientsGrid,true);
        };
        Consumer<CustomCraftingIconHolder> dyeArmorUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (dyeArmorButtons.isEmpty()) return;
            ItemStack armor = dyeArmorButtons.get(0).itemIcon.isEmpty() ? Items.LEATHER_HELMET.getDefaultInstance() : dyeArmorButtons.get(0).itemIcon;
            updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(dyeArmorButtons.get(1).itemIcon),Ingredient.EMPTY, Legacy4JPlatform.getStrictNBTIngredient(armor)),gridDimension,2,2,2);
            DyeItem dye = dyeArmorButtons.get(1).itemIcon.getItem() instanceof DyeItem i ? i : (DyeItem) Items.WHITE_DYE;
            dyeArmorButtons.forEach(CustomRecipeIconHolder::applyAddedIngredients);
            resultStack = dyeArmorButtons.get(0).itemIcon.isEmpty() || !dyeArmorButtons.get(1).hasItem() ? armor : DyeableLeatherItem.dyeArmor(armor,Stream.concat(Stream.of(dye), dyeArmorButtons.get(1).addedIngredientsItems.stream().map(stack->(DyeItem)stack.getItem())).toList());
            canCraft(ingredientsGrid,true);
        };
        Consumer<CustomCraftingIconHolder> dyeItemUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (dyeItemButtons.isEmpty()) return;
            ItemStack item = dyeItemButtons.get(0).itemIcon.isEmpty() ? Items.WHITE_BED.getDefaultInstance() : dyeItemButtons.get(0).itemIcon;
            updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(dyeItemButtons.get(1).itemIcon),Ingredient.EMPTY, Legacy4JPlatform.getStrictNBTIngredient(item)),gridDimension,2,2,2);
            DyeItem dye = dyeItemButtons.get(1).itemIcon.getItem() instanceof DyeItem i ? i : (DyeItem) Items.WHITE_DYE;
            ResourceLocation location = BuiltInRegistries.ITEM.getKey(item.getItem());
            String path = location.getPath();
            Item result = BuiltInRegistries.ITEM.get(location.withPath(path.contains(dye.getDyeColor().getName()) ? path : dye.getDyeColor().getName() + "_" + (Arrays.stream(DyeColor.values()).anyMatch(s-> path.contains(s.getName())) ? path.substring(path.indexOf("_") + 1) : path)));
            resultStack = dyeItemButtons.get(0).itemIcon.isEmpty() || result == Items.AIR ? item : result.getDefaultInstance();
            resultStack.setTag(item.getTag());
            canCraft(ingredientsGrid,true);
        };
        List<ItemStack> dyes = Arrays.stream(DyeColor.values()).map(c-> DyeItem.byColor(c).getDefaultInstance()).toList();
        fireworkStarButtons.add(craftingButtonByList(COLOR_TAB, dyes,fireworkStarUpdateRecipe).enableAddIngredients());
        fireworkStarButtons.add(craftingButtonByList(SHAPE_TAB, Arrays.stream(FireworkStarRecipe.SHAPE_INGREDIENT.getItems()).toList(),fireworkStarUpdateRecipe).enableAddIngredients(h-> ingredientsGrid.stream().noneMatch(i -> i.equals(FireworkStarRecipe.SHAPE_INGREDIENT))));
        fireworkStarButtons.add(craftingButtonByList(EFFECT_TAB, Stream.concat(Arrays.stream(FireworkStarRecipe.FLICKER_INGREDIENT.getItems()),Arrays.stream(FireworkStarRecipe.TRAIL_INGREDIENT.getItems())).toList(),fireworkStarUpdateRecipe).enableAddIngredients(h-> ingredientsGrid.stream().noneMatch(i -> i.test(h.itemIcon))));

        fireworkStarFadeButtons.add(craftingButtonByPredicate(SELECT_STAR_TAB,i-> i.getItem() instanceof FireworkStarItem,fireworkStarFadeUpdateRecipe));
        fireworkStarFadeButtons.add(craftingButtonByList(ADD_FADE_TAB, dyes,fireworkStarFadeUpdateRecipe).enableAddIngredients());

        fireworkButtons.add(craftingButtonByList(ADD_POWER_TAB,Arrays.stream(FireworkRocketRecipe.GUNPOWDER_INGREDIENT.getItems()).toList(),fireworkRocketUpdateRecipe).enableAddIngredients(h-> ingredientsGrid.stream().filter(i-> i.equals(FireworkRocketRecipe.GUNPOWDER_INGREDIENT)).count() < 3));
        fireworkButtons.add(craftingButtonByPredicate(SELECT_STAR_TAB,i-> i.getItem() instanceof FireworkStarItem,fireworkRocketUpdateRecipe).enableAddIngredients());

        dyeArmorButtons.add(craftingButtonByPredicate(Component.translatable("legacy.container.tab.armour"), i-> i.getItem() instanceof DyeableLeatherItem,dyeArmorUpdateRecipe));
        dyeArmorButtons.add(craftingButtonByList(COLOR_TAB, dyes,dyeArmorUpdateRecipe).enableAddIngredients());
        dyeItemButtons.add(craftingButtonByPredicate(Component.translatable("entity.minecraft.item"),i-> i.getItem() instanceof BedItem || (i.getItem() instanceof BlockItem b &&  b.getBlock() instanceof ShulkerBoxBlock),dyeItemUpdateRecipe));
        dyeItemButtons.add(craftingButtonByList(COLOR_TAB, dyes,dyeItemUpdateRecipe));
        dyeBannerButtons.add(craftingButtonByPredicate(COPY_BANNER,i-> i.getItem() instanceof BannerItem && itemHasPatterns(i), h->{
            clearIngredients(ingredientsGrid);
            if (dyeBannerButtons.isEmpty()) return;
            updateShapedIngredients(ingredientsGrid,List.of(Legacy4JPlatform.getStrictNBTIngredient(h.itemIcon.getItem().getDefaultInstance())),gridDimension,2,2,2);
            resultStack = h.itemIcon.copyWithCount(1);
            canCraft(ingredientsGrid,true);
        }));
        dyeBannerButtons.add(craftingButtonByPredicate(SELECT_SHIELD_BANNER, i-> i.getItem() instanceof BannerItem, h->{
            clearIngredients(ingredientsGrid);
            if (dyeBannerButtons.isEmpty()) return;
            updateShapedIngredients(ingredientsGrid,List.of(Legacy4JPlatform.getStrictNBTIngredient(Items.SHIELD.getDefaultInstance()),Legacy4JPlatform.getStrictNBTIngredient(h.itemIcon)),gridDimension,2,2,2);
            resultStack = Items.SHIELD.getDefaultInstance();
            if (h.itemIcon.getItem() instanceof BannerItem b){
                CompoundTag compoundTag = BlockItem.getBlockEntityData(h.itemIcon);
                CompoundTag compoundTag2 = compoundTag == null ? new CompoundTag() : compoundTag.copy();
                compoundTag2.putInt("Base", b.getColor().getId());
                BlockItem.setBlockEntityData(resultStack, BlockEntityType.BANNER, compoundTag2);
            }
            canCraft(ingredientsGrid,true);
        }));
        decoratedPotButtons.add(craftingButtonByList(ADD_SHERD, DecoratedPotPatterns.ITEM_TO_POT_TEXTURE.keySet().stream().map(Item::getDefaultInstance).toList(),h->{
            clearIngredients(ingredientsGrid);
            if (is2x2) return;
            Function<Integer,Item> sherdByIndex = i-> h.addedIngredientsItems.size() >= i + 1 ? h.addedIngredientsItems.get(i).getItem() : Items.BRICK;
            updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(sherdByIndex.apply(0)),Ingredient.EMPTY,Ingredient.of(sherdByIndex.apply(1)),Ingredient.EMPTY,Ingredient.of(sherdByIndex.apply(2)),Ingredient.EMPTY,Ingredient.of(sherdByIndex.apply(3))),gridDimension,3,3,3);
            resultStack = DecoratedPotBlockEntity.createDecoratedPotItem(new DecoratedPotBlockEntity.Decorations(sherdByIndex.apply(0),sherdByIndex.apply(1),sherdByIndex.apply(2),sherdByIndex.apply(3)));
            canCraft(ingredientsGrid,true);
        }).enableAddIngredients(h->h.addedIngredientsItems.size() < 4));
    }
    public void resetElements(){
        listener.slotChanged(menu,-1,ItemStack.EMPTY);
        selectedCraftingButton = 0;
        craftingButtonsOffset.set(0);
        if (inited) repositionElements();
    }
    public static boolean itemHasPatterns(ItemStack stack){
        CompoundTag beTag = stack.getTagElement("BlockEntityTag");
        return stack.getItem() instanceof BannerItem && (beTag != null && beTag.contains("Patterns") && !beTag.getList("Patterns",10).isEmpty());
    }
    protected CustomCraftingIconHolder craftingButtonByList(Component displayName, List<ItemStack> itemStacks, Consumer<CustomCraftingIconHolder> updateRecipe){
        return new CustomCraftingIconHolder(itemStacks.get(0)){
            public Component getDisplayName() {
                return displayName;
            }

            ItemStack nextItem() {
                return nextItem(itemStacks);
            }
            ItemStack previousItem() {
                return previousItem(itemStacks);
            }

            int findInventoryMatchSlot() {
                for (int i = 0; i < menu.slots.size(); i++)
                    if (menu.slots.get(i).getItem().is(itemIcon.getItem())) return i;
                return 0;
            }
            void updateRecipe() {
                updateRecipe.accept(this);
            }
        };
    }
    protected CustomCraftingIconHolder craftingButtonByPredicate(Component displayName, Predicate<ItemStack> isValid, Consumer<CustomCraftingIconHolder> updateRecipe){
        return new CustomCraftingIconHolder(){
            public Component getDisplayName() {
                return displayName;
            }

            ItemStack nextItem() {
                return nextItem(inventory,isValid);
            }
            ItemStack previousItem() {
                return previousItem(inventory,isValid);
            }
            public boolean applyNextItemIfAbsent() {
                return true;
            }

            int findInventoryMatchSlot() {
                for (int i = 0; i < menu.slots.size(); i++)
                    if (menu.slots.get(i).getItem() == itemIcon) return i;
                itemIcon = nextItem;
                return itemIcon.isEmpty() ? 0 : findInventoryMatchSlot();
            }
            void updateRecipe() {
                updateRecipe.accept(this);
            }
        };
    }
    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        if (guiEventListener instanceof TabList) return;
        super.setFocused(guiEventListener);
    }

    public boolean is2x2Recipe(CraftingRecipe recipe){
        return (!(recipe instanceof ShapedRecipe rcp) || Math.max(rcp.getHeight(), rcp.getWidth()) < 3) && (!(recipe instanceof ShapelessRecipe s) || s.getIngredients().size() <= 4);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        Component title = getTabList() == craftingTabList ? getTabList().tabButtons.get(getTabList().selectedTab).getMessage() : getFocused() instanceof CustomCraftingIconHolder h ? h.getDisplayName() : Component.empty();
        guiGraphics.drawString(this.font, title,((groupTabList.selectedTab == 0 ? imageWidth : imageWidth / 2) - font.width(title)) / 2,17, 0x383838, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, (355 + 160 - font.width(playerInventoryTitle))/ 2, 114, 0x383838, false);
        guiGraphics.pose().translate(-leftPos,-topPos,0);
        getCraftingButtons().forEach(b-> b.render(guiGraphics,i,j,0));
        if (selectedCraftingButton < getCraftingButtons().size()) getCraftingButtons().get(selectedCraftingButton).renderSelection(guiGraphics, i, j, 0);
        guiGraphics.pose().translate(leftPos,topPos,0);
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state.pressed && state.canClick()){
            if (state.is(ControllerBinding.LEFT_TRIGGER) || state.is(ControllerBinding.RIGHT_TRIGGER)) groupTabList.controlTab(state.is(ControllerBinding.LEFT_TRIGGER),state.is(ControllerBinding.RIGHT_TRIGGER));
            if (state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis s) controlPage(s.x < 0 && -s.x > Math.abs(s.y),s.x > 0 && s.x > Math.abs(s.y));
        }
    }
    @Override
    protected void init() {
        resultStack = ItemStack.EMPTY;
        imageWidth = 348;
        imageHeight = 215;
        super.init();
        leftPos+=21;
        topPos+=18;
        menu.addSlotListener(listener);
        addWidget(groupTabList);
       if (selectedCraftingButton < getCraftingButtons().size()) setFocused(getCraftingButtons().get(selectedCraftingButton));
       if (groupTabList.selectedTab == 0) {
            craftingButtonsOffset.max = Math.max(0,recipesByTab.get(page.get() * 7 + craftingTabList.selectedTab).size() - 12);
            craftingButtons.forEach(b->{
                b.setPos(leftPos + 13 + craftingButtons.indexOf(b) * 27,topPos + 38);
                addWidget(b);
            });
        }else {
            int size = getCraftingButtons().size();
            getCraftingButtons().forEach(b->{
                b.setPos(leftPos + (size == 1 ? 77 : size ==2 ? 52 : size ==3 ? 21 : 8) + getCraftingButtons().indexOf(b) * (size ==2 ? 62 : size ==3 ? 55 : 45),topPos + 39);
                if (size == 3) b.offset = new Offset(0.5 + getCraftingButtons().indexOf(b) * 0.5,0,0);
                b.init();
                addWidget(b);
            });
        }

        addWidget(getTabList());
        getTabList().init(leftPos, topPos - 37, imageWidth, (t, i) -> {
            int index = getTabList().tabButtons.indexOf(t);
            t.type = index == 0 ? 0 : index >= 6 ? 2 : 1;
            t.setWidth(51);
            t.offset = (t1) -> new Offset(-1.5 * getTabList().tabButtons.indexOf(t), t1.selected ? 0 : 4.5, 0);
        });
        groupTabList.init((b,i)->{
            b.setX(leftPos - b.getWidth() + 6);
            b.setY(topPos + i + 4);
            b.offset = (t1) -> new Offset(t1.selected ? 0 : 3.5, 0.5, 0);
        },true);
    }
    protected TabList getTabList(){
        return switch (groupTabList.selectedTab){
            case 1 -> fireworkTabList;
            case 2 -> dyeTabList;
            default -> craftingTabList;
        };
    }
    protected boolean canCraft(List<Ingredient> ingredients, boolean isFocused) {
        return canCraft(isFocused ? ingredientsGrid : ingredients, isFocused ? warningSlots : null);
    }
    public static boolean canCraft(List<Ingredient> ings, boolean[] warningSlots) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ings.stream().allMatch(Ingredient::isEmpty)) return false;
        boolean canCraft = true;
        for (int i1 = 0; i1 < ings.size(); i1++) {
            Ingredient ing = ings.get(i1);
            if (ing.isEmpty()) continue;
            int itemCount = minecraft.player.getInventory().items.stream().filter(i-> !i.isEmpty() && ing.test(i.copyWithCount(1))).mapToInt(ItemStack::getCount).sum() + (minecraft.player.containerMenu.getCarried().isEmpty() || !ing.test(minecraft.player.containerMenu.getCarried()) ? 0 : minecraft.player.containerMenu.getCarried().getCount());
            long ingCount = ings.stream().filter(i -> !i.isEmpty() && i.equals(ing)).count();
            if (itemCount >= ingCount || PagedList.occurrenceOf(ings, ing, i1) < itemCount) {
                if (warningSlots != null) warningSlots[i1] = false;
            } else {
                canCraft = false;
                if (warningSlots == null) break;
                else warningSlots[i1] = true;
            }
        }
        return canCraft;
    }
    protected void addCraftingButtons(){
        for (int i = 0; i < 12; i++) {
            int index = i;

            RecipeIconHolder<CraftingRecipe> h;
            craftingButtons.add(h = new RecipeIconHolder<>(leftPos + 13 + i * 27, topPos + 38) {
                @Override
                public void render(GuiGraphics graphics, int i, int j, float f) {
                    if (isFocused()) selectedCraftingButton = index;
                    super.render(graphics, i, j, f);
                }

                protected boolean canCraft(CraftingRecipe rcp) {
                    if (rcp == null || onlyCraftableRecipes) return true;
                    return LegacyCraftingScreen.this.canCraft(rcp.getIngredients(),isFocused() && getFocusedRecipe() == rcp);
                }

                protected List<CraftingRecipe> getRecipes() {
                    List<List<CraftingRecipe>> list = onlyCraftableRecipes ? filteredRecipesByGroup : recipesByTab.get(page.get() * 7 + craftingTabList.selectedTab);
                    return list.size() <= craftingButtonsOffset.get() + index ? Collections.emptyList() : list.get(craftingButtonsOffset.get() + index);
                }

                @Override
                protected void toggleCraftableRecipes() {
                    onlyCraftableRecipes = !onlyCraftableRecipes;
                    listener.slotChanged(menu, 0, ItemStack.EMPTY);
                }

                @Override
                public boolean keyPressed(int i, int j, int k) {
                    if (controlCyclicNavigation(i, index, craftingButtons, craftingButtonsOffset, scrollRenderer, LegacyCraftingScreen.this))
                        return true;
                    return super.keyPressed(i, j, k);
                }

                protected void updateRecipeDisplay(CraftingRecipe rcp) {
                    resultStack = getFocusedResult();
                    clearIngredients(ingredientsGrid);
                    if (!(rcp instanceof ShapedRecipe r)) {
                        if (rcp != null) for (int i = 0; i < rcp.getIngredients().size(); i++)
                            ingredientsGrid.set(i, rcp.getIngredients().get(i));
                        return;
                    }
                    updateShapedIngredients(ingredientsGrid,r.getIngredients(),gridDimension,Math.max(r.getHeight(), r.getWidth()),r.getWidth(),r.getHeight());
                }

            });
            h.offset = CRAFTING_OFFSET;
        }
    }

    public static void clearIngredients(List<Ingredient> ingredientsGrid){
        for (int i = 0; i < ingredientsGrid.size(); i++) {
            if (!ingredientsGrid.get(i).isEmpty()) ingredientsGrid.set(i, Ingredient.EMPTY);
        }
    }
    public static void updateShapedIngredients(List<Ingredient> ingredientsGrid,List<Ingredient> recipeIngredients, int gridDimension, int rcpDimension, int rcpWidth, int rcpHeight){
        Ingredient[] ingredients = new Ingredient[rcpDimension * rcpDimension];
        for (int i = 0; i < recipeIngredients.size(); i++)
            ingredients[rcpWidth < rcpHeight ? (i / rcpWidth) * rcpHeight + (i % rcpWidth) : i] = recipeIngredients.get(i);

        for (int i = 0; i < ingredients.length; i++)
            ingredientsGrid.set(i > 1 && gridDimension > rcpDimension ? i + 1 : i, ingredients[i] == null ? Ingredient.EMPTY : ingredients[i]);
    }
    protected abstract class CustomCraftingIconHolder extends CustomRecipeIconHolder{
        public CustomCraftingIconHolder(ItemStack itemStack) {
            super(itemStack);
        }

        public CustomCraftingIconHolder() {
            super();
        }

        LegacyScrollRenderer getScrollRenderer() {
            return scrollRenderer;
        }
        public boolean canCraft() {
            return LegacyCraftingScreen.this.canCraft(getIngredientsGrid(),false);
        }
        public List<Ingredient> getIngredientsGrid() {
            return ingredientsGrid;
        }
        public ItemStack getResultStack() {
            return resultStack;
        }
        @Override
        public void render(GuiGraphics graphics, int i, int j, float f) {
            if (isFocused()) selectedCraftingButton = getCraftingButtons().indexOf(this);
            super.render(graphics, i, j, f);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        groupTabList.render(guiGraphics, i, j, f);
        getTabList().render(guiGraphics, i, j, f);
        ScreenUtil.renderPanel(guiGraphics, leftPos, topPos, imageWidth, imageHeight, 2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics, leftPos + 9, topPos + 103, 163, 105, 2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics, leftPos + 176, topPos + 103, 163, 105, 2f);
        if (groupTabList.selectedTab != 0) ScreenUtil.renderSquareRecessedPanel(guiGraphics, leftPos + 176, topPos + 8, 163, 93, 2f);
        guiGraphics.blitSprite(SMALL_ARROW, leftPos + (is2x2 ? 87 : 97), topPos + 161, 16, 13);
        if (groupTabList.selectedTab == 0) {
            if (craftingButtonsOffset.get() > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, leftPos + 5, topPos + 45);
            if (craftingButtonsOffset.max > 0 && craftingButtonsOffset.get() < craftingButtonsOffset.max)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + 337, topPos + 45);
        }
    }


    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (hasShiftDown() && groupTabList.controlTab(i)) return true;
        getTabList().controlTab(i);
        if (hasShiftDown() && controlPage(i == 263, i == 262)) return true;
        return super.keyPressed(i, j, k);
    }
    protected boolean controlPage(boolean left, boolean right){
        if ((left || right) && page.max > 0 && groupTabList.selectedTab == 0){
            int lastPage = page.get();
            page.add(left ? -1 : 1);
            if (lastPage != page.get()) {
                craftingTabList.resetSelectedTab();
                rebuildWidgets();
                return true;
            }
        }return false;
    }
    public List<? extends LegacyIconHolder> getCraftingButtons(){
        return switch (groupTabList.selectedTab){
            case 1 -> fireworkTabList.selectedTab == 0 ? fireworkStarButtons : fireworkTabList.selectedTab == 1 ? fireworkStarFadeButtons : fireworkButtons;
            case 2 -> dyeTabList.selectedTab == 0 ? dyeArmorButtons : dyeTabList.selectedTab == 1 ? dyeItemButtons : dyeTabList.selectedTab == 2 ? dyeBannerButtons : decoratedPotButtons;
            default -> craftingButtons;
        };
    }
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBackground(guiGraphics, i, j, f);
        super.render(guiGraphics, i, j, f);
        for (int index = 0; index < ingredientsGrid.size(); index++)
            ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (is2x2 ? 33 : 21) + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23, 23, 23, getActualItem(ingredientsGrid.get(index)), (!onlyCraftableRecipes || groupTabList.selectedTab != 0) && !getActualItem(ingredientsGrid.get(index)).isEmpty() &&  warningSlots[index], new Offset(0.5, is2x2 ? 0 : 0.5, 0)).render(guiGraphics, i, j, f);;
        ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (is2x2 ? 113 : 124), topPos + 151, 36, 36, resultStack, (!onlyCraftableRecipes || groupTabList.selectedTab != 0) && ingredientsGrid.stream().anyMatch(ing-> !ing.isEmpty()) && !canCraft(ingredientsGrid,false), new Offset(0.5, 0, 0)).render(guiGraphics, i, j, f);
        if (!resultStack.isEmpty()) {
            Component resultName = resultStack.getHoverName();
            ScreenUtil.renderScrollingString(guiGraphics, font, resultName, leftPos + 11 + Math.max(163 - font.width(resultName), 0) / 2, topPos + 114, leftPos + 170, topPos + 125, 0x383838, false);
            if (groupTabList.selectedTab != 0){
                List<Component> list = resultStack.getTooltipLines(minecraft.player, TooltipFlag.NORMAL);
                for (int i1 = 0; i1 < list.size(); i1++) {
                    if (26 + i1 * 13 >= 93) break;
                    Component c = list.get(i1);
                    ScreenUtil.renderScrollingString(guiGraphics, font, c.copy().withColor(0x383838), leftPos + 180, topPos + 15 + i1 * 13, leftPos + 335, topPos + 26 + i1 * 13, 0x383838, false);
                }
            }
            if (ScreenUtil.isMouseOver(i,j,leftPos + (is2x2 ? 113 : 124), topPos + 151,36,36)) guiGraphics.renderTooltip(font, resultStack,i,j);
        }
        for (int index = 0; index < ingredientsGrid.size(); index++) if (!getActualItem(ingredientsGrid.get(index)).isEmpty() && ScreenUtil.isMouseOver(i,j,leftPos + (is2x2 ? 33 : 21) + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23,23,23)) guiGraphics.renderTooltip(font,getActualItem(ingredientsGrid.get(index)),i,j);
        getCraftingButtons().forEach(h -> h.renderTooltip(minecraft, guiGraphics, i, j));

        renderTooltip(guiGraphics, i, j);
    }
}
