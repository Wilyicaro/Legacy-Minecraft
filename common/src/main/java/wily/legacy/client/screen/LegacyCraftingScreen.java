package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.*;
import org.jetbrains.annotations.Nullable;
import wily.legacy.Legacy4JPlatform;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyCraftingTabListing;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.*;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.inventory.RecipeMenu;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static wily.legacy.util.LegacySprites.SMALL_ARROW;
import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.RecipeIconHolder.getActualItem;

public class LegacyCraftingScreen extends AbstractContainerScreen<LegacyCraftingMenu> implements Controller.Event,ControlTooltip.Event {
    public static final Offset CRAFTING_OFFSET = new Offset(0.5,0.5,0);
    private final Inventory inventory;
    protected final List<ItemStack> compactItemStackList = new ArrayList<>();
    private final boolean is2x2;
    private final int gridDimension;
    private boolean onlyCraftableRecipes = false;
    protected Stocker.Sizeable infoType = new Stocker.Sizeable(0,2);
    protected final List<Ingredient> ingredientsGrid;
    protected ItemStack resultStack = ItemStack.EMPTY;
    public static final Component INGREDIENTS = Component.translatable("legacy.container.ingredients");
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
    protected final TabList groupTabList = new TabList().add(0,0,42, 42, 4,LegacyTabButton.iconOf(Items.CRAFTING_TABLE),Component.empty(),null,b->repositionElements()).add(0,0,42, 42, 4, LegacyTabButton.iconOf(Items.FIREWORK_ROCKET),Component.empty(),null,b->resetElements()).add(0,0,42, 42, 4, LegacyTabButton.iconOf(Items.CYAN_DYE),Component.empty(),null,b->resetElements());
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    private final boolean[] warningSlots;
    protected final ContainerListener listener = new ContainerListener() {
        public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
            if (onlyCraftableRecipes && groupTabList.selectedTab == 0) {
                filteredRecipesByGroup = recipesByTab.get(craftingTabList.selectedTab).stream().map(l -> l.stream().filter(r -> RecipeMenu.canCraft(r.getIngredients(), inventory,abstractContainerMenu.getCarried())).toList()).filter(l -> !l.isEmpty()).toList();
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
                l.forEach(v->v.addRecipes(RecipeType.CRAFTING,manager,group,r-> !r.getIngredients().isEmpty() && (!is2x2 || is2x2Recipe(r))));
                if (!group.isEmpty()) groups.add(group);
            });
            if (groups.isEmpty()) continue;

            recipesByTab.add(groups);

            craftingTabList.addTabButton(43,0,listing.icon,listing.displayName, t->resetElements());

        }
        if (ScreenUtil.getLegacyOptions().vanillaTabs().get()) manager.getAllRecipesFor(RecipeType.CRAFTING).stream().collect(Collectors.groupingBy(h->h.category(),()->new TreeMap<>(Comparator.comparingInt(Enum::ordinal)),Collectors.groupingBy(h->h.getGroup().isEmpty() ? h.getId().toString() : h.getGroup()))).forEach((category, m)->{
            if (m.isEmpty()) return;
            List<List<CraftingRecipe>> groups = new ArrayList<>();
            m.values().forEach(l->{
                List<CraftingRecipe> group = l.stream().filter(h->!(h instanceof CustomRecipe) && (!is2x2 || is2x2Recipe(h))).collect(Collectors.toList());
                if (!group.isEmpty()) groups.add(group);
            });
            if (groups.isEmpty()) return;
            recipesByTab.add(groups);
            craftingTabList.addTabButton(43,0,LegacyTabButton.iconOf(VANILLA_CATEGORY_ICONS[category.ordinal()]), getTitle(), t->resetElements());
        });
        craftingTabList.resetSelectedTab();
        inited = true;
        addCraftingButtons();
        ItemStack redStar = Items.FIREWORK_STAR.getDefaultInstance();
        CompoundTag redStarTag = redStar.getOrCreateTag();
        CompoundTag explosionTag = new CompoundTag();
        redStarTag.put("Explosion",explosionTag);
        explosionTag.putIntArray("Colors", List.of(DyeColor.RED.getFireworkColor()));
        fireworkTabList.add(0,0,0,43,0,LegacyTabButton.iconOf(Items.FIREWORK_STAR), Component.empty(),null, b-> resetElements());
        fireworkTabList.add(0,0,0,43,0,LegacyTabButton.iconOf(redStar), Component.empty(),null, b-> resetElements());
        fireworkTabList.add(0,0,0,43,0,LegacyTabButton.iconOf(Items.FIREWORK_ROCKET), Component.empty(),null, b-> resetElements());
        dyeTabList.add(0,0,0,43,0,LegacyTabButton.iconOf(DyeableLeatherItem.dyeArmor(Items.LEATHER_CHESTPLATE.getDefaultInstance(),List.of((DyeItem) Items.GREEN_DYE))), Component.empty(),null, b-> resetElements());
        dyeTabList.add(0,0,0,43,0,groupTabList.tabButtons.get(2).icon, Component.empty(),null, b-> resetElements());
        dyeTabList.add(0,0,0,43,0,LegacyTabButton.iconOf(Items.WHITE_BANNER), Component.empty(),null, b-> resetElements());
        if (!is2x2) dyeTabList.add(0,0,0,43,0,LegacyTabButton.iconOf(Items.DECORATED_POT), Component.empty(),null, b-> resetElements());

        Consumer<CustomCraftingIconHolder> fireworkStarUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (fireworkStarButtons.isEmpty()) return;
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(fireworkStarButtons.get(0).itemIcon),Ingredient.EMPTY, FireworkStarRecipe.GUNPOWDER_INGREDIENT),gridDimension,2,2,2);
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
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(fireworkStarFadeButtons.get(1).itemIcon),Ingredient.EMPTY, Legacy4JPlatform.getStrictNBTIngredient(item)),gridDimension,2,2,2);
            fireworkStarFadeButtons.get(1).applyAddedIngredients();
            resultStack = item.copy();
            if (fireworkStarFadeButtons.get(1).hasItem()) resultStack.getOrCreateTagElement("Explosion").putIntArray("FadeColors", Stream.concat(Stream.of(fireworkStarFadeButtons.get(1).itemIcon),fireworkStarFadeButtons.get(1).addedIngredientsItems.stream()).map(i->((DyeItem)i.getItem()).getDyeColor().getFireworkColor()).toList());
            canCraft(ingredientsGrid,true);
        };
        Consumer<CustomCraftingIconHolder> fireworkRocketUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (fireworkButtons.isEmpty()) return;
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(fireworkButtons.get(0).itemIcon),Ingredient.EMPTY,FireworkRocketRecipe.PAPER_INGREDIENT),gridDimension,2,2,2);
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
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(dyeArmorButtons.get(1).itemIcon),Ingredient.EMPTY, Legacy4JPlatform.getStrictNBTIngredient(armor)),gridDimension,2,2,2);
            DyeItem dye = dyeArmorButtons.get(1).itemIcon.getItem() instanceof DyeItem i ? i : (DyeItem) Items.WHITE_DYE;
            dyeArmorButtons.forEach(CustomRecipeIconHolder::applyAddedIngredients);
            resultStack = dyeArmorButtons.get(0).itemIcon.isEmpty() || !dyeArmorButtons.get(1).hasItem() ? armor : DyeableLeatherItem.dyeArmor(armor,Stream.concat(Stream.of(dye), dyeArmorButtons.get(1).addedIngredientsItems.stream().map(stack->(DyeItem)stack.getItem())).toList());
            canCraft(ingredientsGrid,true);
        };
        Consumer<CustomCraftingIconHolder> dyeItemUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (dyeItemButtons.isEmpty()) return;
            ItemStack item = dyeItemButtons.get(0).itemIcon.isEmpty() ? Items.WHITE_BED.getDefaultInstance() : dyeItemButtons.get(0).itemIcon;
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(dyeItemButtons.get(1).itemIcon),Ingredient.EMPTY, Legacy4JPlatform.getStrictNBTIngredient(item)),gridDimension,2,2,2);
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
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.EMPTY,Legacy4JPlatform.getStrictNBTIngredient(h.itemIcon.getItem().getDefaultInstance()),Legacy4JPlatform.getStrictNBTIngredient(h.itemIcon)),gridDimension,2,2,2);
            resultStack = h.itemIcon.copyWithCount(1);
            canCraft(ingredientsGrid,true);
        }));

        dyeBannerButtons.add(craftingButtonByPredicate(SELECT_SHIELD_BANNER, i-> i.getItem() instanceof BannerItem, h->{
            clearIngredients(ingredientsGrid);
            if (dyeBannerButtons.isEmpty()) return;
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.EMPTY,Legacy4JPlatform.getStrictNBTIngredient(Items.SHIELD.getDefaultInstance()),Legacy4JPlatform.getStrictNBTIngredient(h.itemIcon)),gridDimension,2,2,2);
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
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(sherdByIndex.apply(0)),Ingredient.EMPTY,Ingredient.of(sherdByIndex.apply(1)),Ingredient.EMPTY,Ingredient.of(sherdByIndex.apply(2)),Ingredient.EMPTY,Ingredient.of(sherdByIndex.apply(3))),gridDimension,3,3,3);
            resultStack = createDecoratedPotItem(new DecoratedPotBlockEntity.Decorations(sherdByIndex.apply(0),sherdByIndex.apply(1),sherdByIndex.apply(2),sherdByIndex.apply(3)));
            canCraft(ingredientsGrid,true);
        }).enableAddIngredients(h->h.addedIngredientsItems.size() < 4));
    }
    @Override
    public void addControlTooltips(Renderer renderer) {
        Event.super.addControlTooltips(renderer);
        renderer.
                set(0,create(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(),()->getFocused() instanceof RecipeIconHolder<?> h && h.canCraft() && h.isValidIndex() || getFocused() instanceof CustomCraftingIconHolder c && c.canCraft() ? getAction("legacy.action.create") : null)).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), ()-> groupTabList.selectedTab == 0 ? getAction("legacy.action.info") : getFocused() instanceof CustomCraftingIconHolder h && h.addedIngredientsItems != null && !h.addedIngredientsItems.isEmpty() ? getAction("legacy.action.remove") : null).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), ()-> groupTabList.selectedTab == 0 ? getAction(onlyCraftableRecipes ? "legacy.action.all_recipes" : "legacy.action.show_craftable_recipes") : getFocused() instanceof CustomCraftingIconHolder h && h.canAddIngredient() ? getAction("legacy.action.add") : null).
                add(()-> ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET)}) : COMPOUND_ICON_FUNCTION.apply(new Icon[]{ControllerBinding.LEFT_TRIGGER.bindingState.getIcon(),ControlTooltip.SPACE_ICON, ControllerBinding.RIGHT_TRIGGER.bindingState.getIcon()}),()->getAction("legacy.action.type")).
                addCompound(()-> new Icon[]{ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.bindingState.getIcon(),ControlTooltip.SPACE_ICON, ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.bindingState.getIcon()},()->getAction("legacy.action.group")).
                add(()-> page.max > 0 && groupTabList.selectedTab == 0 ? ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.RIGHT_STICK.bindingState.getIcon() : null,()->getAction("legacy.action.page"));
    }


    public void resetElements(){
        listener.slotChanged(menu,-1,ItemStack.EMPTY);
        selectedCraftingButton = 0;
        infoType.set(0);
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

    public static boolean is2x2Recipe(CraftingRecipe recipe){
        return (!(recipe instanceof ShapedRecipe rcp) || Math.max(rcp.getHeight(), rcp.getWidth()) < 3) && ((recipe instanceof ShapedRecipe s) || recipe.getIngredients().size() <= 4);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int i, int j) {
        Component title = getTabList() == craftingTabList ? getTabList().tabButtons.get(getTabList().selectedTab).getMessage() : getFocused() instanceof CustomCraftingIconHolder h ? h.getDisplayName() : Component.empty();
        poseStack.drawString(this.font, title,((groupTabList.selectedTab == 0 ? imageWidth : imageWidth / 2) - font.width(title)) / 2,17, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        if (infoType.get() <= 0) poseStack.drawString(this.font, this.playerInventoryTitle, (355 + 160 - font.width(playerInventoryTitle))/ 2, 114, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        else {
            if (selectedCraftingButton < getCraftingButtons().size() && getCraftingButtons().get(selectedCraftingButton) instanceof RecipeIconHolder<?> h) {
                if (infoType.get() == 1 && ScreenUtil.hasTip(h.getFocusedResult())) {
                    List<FormattedCharSequence> l = font.split(ScreenUtil.getTip(h.getFocusedResult()), 152);
                    for (int i1 = 0; i1 < l.size(); i1++) {
                        if (i1 > 7) break;
                        poseStack.drawString(font, l.get(i1), 181, 108 + i1 * 12, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                    }

                } else if (infoType.get() == 2) {
                    poseStack.drawString(this.font, INGREDIENTS, (355 + 160 - font.width(INGREDIENTS))/ 2, 108, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                    if (h.getFocusedRecipe() != null) {
                        compactItemStackList.clear();
                        RecipeMenu.handleCompactItemStackList(compactItemStackList, () -> h.getFocusedRecipe().getIngredients().stream().map(RecipeIconHolder::getActualItem).iterator());
                        for (int i1 = 0; i1 < compactItemStackList.size(); i1++) {
                            if (i1 > 4) break;
                            ItemStack ing = compactItemStackList.get(i1);
                            ScreenUtil.iconHolderRenderer.itemHolder(180, 124 + 15 * i1, 14, 14, ing, false, Offset.ZERO).render(poseStack, i, j, 0);
                            poseStack.pose().pushPose();
                            poseStack.pose().translate(198, 128 + 15 * i1, 0);
                            poseStack.pose().scale(2 / 3f, 2 / 3f, 2 / 3f);
                            poseStack.drawString(font, ing.getHoverName(), 0, 0, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                            poseStack.pose().popPose();
                        }
                    }
                }
            }
        }
        poseStack.pose().translate(-leftPos,-topPos,0);
        getCraftingButtons().forEach(b-> b.render(poseStack,i,j,0));
        if (selectedCraftingButton < getCraftingButtons().size()) getCraftingButtons().get(selectedCraftingButton).renderSelection(poseStack, i, j, 0);
        poseStack.pose().translate(leftPos,topPos,0);
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
        menu.inventoryActive = infoType.get() <= 0;
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
        compactItemStackList.clear();
        RecipeMenu.handleCompactInventoryList(compactItemStackList,Minecraft.getInstance().player.getInventory(),menu.getCarried());
        return canCraft(compactItemStackList, isFocused ? ingredientsGrid : ingredients, isFocused ? warningSlots : null);
    }
    public static boolean canCraft(List<ItemStack> compactItemStackList, List<Ingredient> ings, boolean[] warningSlots) {
        boolean canCraft = true;
        for (int i1 = 0; i1 < ings.size(); i1++) {
            Ingredient ing = ings.get(i1);
            if (ing.isEmpty()) continue;
            Optional<ItemStack> match = compactItemStackList.stream().filter(i-> !i.isEmpty() && ing.test(i.copyWithCount(1))).findFirst();
            if (match.isPresent()) {
                match.get().shrink(1);
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
                public void render(PoseStack graphics, int i, int j, float f) {
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
                    if (i == InputConstants.KEY_X && groupTabList.selectedTab == 0){
                        infoType.add(1,true);
                        menu.inventoryActive = infoType.get() <= 0;
                        ScreenUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(),1.0f);
                        return true;
                    }
                    return super.keyPressed(i, j, k);
                }

                protected void updateRecipeDisplay(CraftingRecipe rcp) {
                    resultStack = getFocusedResult();
                    clearIngredients(ingredientsGrid);
                    if (rcp == null) return;
                    if (!(rcp instanceof ShapedRecipe r)) {
                        for (int i = 0; i < rcp.getIngredients().size(); i++)
                            ingredientsGrid.set(i, rcp.getIngredients().get(i));
                        return;
                    }
                    LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,r.getIngredients(),gridDimension,Math.max(r.getHeight(), r.getWidth()),r.getWidth(),r.getHeight());
                }

                @Override
                public void craft() {
                    ScreenUtil.playSimpleUISound(SoundEvents.ITEM_PICKUP,1.0f);
                    super.craft();
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
        public void render(PoseStack graphics, int i, int j, float f) {
            if (isFocused()) selectedCraftingButton = getCraftingButtons().indexOf(this);
            super.render(graphics, i, j, f);
        }
    }

    @Override
    public boolean mouseScrolled(double d, double e, double g) {
        if (super.mouseScrolled(d, e, g)) return true;
        int scroll = (int)Math.signum(g);
        if (((craftingButtonsOffset.get() > 0 && scroll < 0) || (scroll > 0 && craftingButtonsOffset.max > 0)) && craftingButtonsOffset.add(scroll,false) != 0){
            repositionElements();
            return true;
        }
        return false;
    }

    @Override
    public void renderBackground(PoseStack poseStack) {
    }
    @Override
    protected void renderBg(PoseStack poseStack, float f, int i, int j) {
        groupTabList.render(poseStack, i, j, f);
        getTabList().render(poseStack, i, j, f);
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.SMALL_PANEL, leftPos, topPos, imageWidth, imageHeight);
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + 9, topPos + 103, 163, 105);
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 176, topPos + 103, 163, 105);
        if (groupTabList.selectedTab != 0) LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + 176, topPos + 8, 163, 93);
        LegacyGuiGraphics.of(poseStack).blitSprite(SMALL_ARROW, leftPos + (is2x2 ? 87 : 97), topPos + 161, 16, 13);
        if (groupTabList.selectedTab == 0) {
            if (craftingButtonsOffset.get() > 0)
                scrollRenderer.renderScroll(poseStack, ScreenDirection.LEFT, leftPos + 5, topPos + 45);
            if (craftingButtonsOffset.max > 0 && craftingButtonsOffset.get() < craftingButtonsOffset.max)
                scrollRenderer.renderScroll(poseStack, ScreenDirection.RIGHT, leftPos + 337, topPos + 45);
        }
    }

    @Override
    public boolean disableCursorOnInit() {
        return true;
    }

    @Override
    public boolean onceClickBindings() {
        return false;
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
    public void render(PoseStack poseStack
            , int i, int j, float f) {
        super.render(poseStack, i, j, f);
        for (int index = 0; index < ingredientsGrid.size(); index++)
            ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (is2x2 ? 33 : 21) + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23, 23, 23, getActualItem(ingredientsGrid.get(index)), (!onlyCraftableRecipes || groupTabList.selectedTab != 0) && !getActualItem(ingredientsGrid.get(index)).isEmpty() &&  warningSlots[index], new Offset(0.5, is2x2 ? 0 : 0.5, 0)).render(poseStack, i, j, f);;
        ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (is2x2 ? 113 : 124), topPos + 151, 36, 36, resultStack, (!onlyCraftableRecipes || groupTabList.selectedTab != 0) && ingredientsGrid.stream().anyMatch(ing-> !ing.isEmpty()) && !canCraft(ingredientsGrid,false), new Offset(0.5, 0, 0)).render(poseStack, i, j, f);
        if (!resultStack.isEmpty()) {
            Component resultName = resultStack.getHoverName();
            ScreenUtil.renderScrollingString(poseStack, font, resultName, leftPos + 11 + Math.max(163 - font.width(resultName), 0) / 2, topPos + 114, leftPos + 170, topPos + 125, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            if (groupTabList.selectedTab != 0){
                List<Component> list = resultStack.getTooltipLines(minecraft.player, TooltipFlag.NORMAL);
                for (int i1 = 0; i1 < list.size(); i1++) {
                    if (26 + i1 * 13 >= 93) break;
                    Component c = list.get(i1);
                    ScreenUtil.renderScrollingString(poseStack, font, c.copy().setStyle(Style.EMPTY), leftPos + 180, topPos + 15 + i1 * 13, leftPos + 335, topPos + 26 + i1 * 13, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                }
            }
            if (ScreenUtil.isMouseOver(i,j,leftPos + (is2x2 ? 113 : 124), topPos + 151,36,36)) poseStack.renderTooltip(font, resultStack,i,j);
        }
        for (int index = 0; index < ingredientsGrid.size(); index++) if (!getActualItem(ingredientsGrid.get(index)).isEmpty() && ScreenUtil.isMouseOver(i,j,leftPos + (is2x2 ? 33 : 21) + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23,23,23)) poseStack.renderTooltip(font,getActualItem(ingredientsGrid.get(index)),i,j);
        getCraftingButtons().forEach(h -> h.renderTooltip(minecraft, poseStack, i, j));

        renderTooltip(poseStack, i, j);
    }
}
