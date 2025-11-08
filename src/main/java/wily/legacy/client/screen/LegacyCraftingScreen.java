package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.FactoryIngredient;
import wily.factoryapi.base.StackIngredient;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.base.network.CommonNetwork;
import wily.factoryapi.base.network.CommonRecipeManager;
import wily.factoryapi.util.ModInfo;
import wily.factoryapi.util.PagedList;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.inventory.ImpossibleIngredient;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.mixin.base.FireworkRocketRecipeAccessor;
import wily.legacy.mixin.base.FireworkStarRecipeAccessor;
import wily.legacy.network.ServerMenuCraftPayload;
import wily.legacy.util.*;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.inventory.RecipeMenu;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.RecipeIconHolder.getActualItem;

public class LegacyCraftingScreen extends AbstractContainerScreen<LegacyCraftingMenu> implements Controller.Event, ControlTooltip.Event, TabList.Access {
    public static final Item[] VANILLA_CATEGORY_ICONS = new Item[]{Items.BRICKS, Items.REDSTONE, Items.GOLDEN_SWORD, Items.LAVA_BUCKET};
    protected final List<ItemStack> compactItemStackList = new ArrayList<>();
    protected final List<Optional<Ingredient>> ingredientsGrid;
    protected final CraftingContainer container;
    protected final UIAccessor accessor = UIAccessor.of(this);
    protected final List<RecipeIconHolder<CraftingRecipe>> craftingButtons = new PagedList<>(new Stocker.Sizeable(0, 0), () -> accessor.getInteger("maxCraftingButtonsCount", 12));
    protected final List<CustomRecipeIconHolder> dyeItemButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> dyeArmorButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> bannerButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> decorateShieldButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> decoratedPotButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> fireworkStarButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> fireworkStarFadeButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> fireworkButtons = new ArrayList<>();
    protected final List<List<List<RecipeInfo<CraftingRecipe>>>> recipesByTab = new ArrayList<>();
    protected final Stocker.Sizeable page = new Stocker.Sizeable(0);
    protected final Stocker.Sizeable craftingButtonsOffset = new Stocker.Sizeable(0);
    protected final TabList craftingTabList = new TabList(accessor, new PagedList<>(page, this::getMaxTabCount));
    protected final List<TabList> typeTabLists = new ArrayList<>();
    protected final List<TypeCraftingTab> typeTabs = new ArrayList<>();
    protected final TabList typeTabList = new TabList(accessor);
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    protected final ScrollableRenderer scrollableRenderer = new ScrollableRenderer();
    private final Inventory inventory;
    private final boolean is2x2;
    private final int gridDimension;
    private final boolean[] warningSlots;
    protected Stocker.Sizeable infoType = new Stocker.Sizeable(0, 2);
    protected ItemStack resultStack = ItemStack.EMPTY;
    protected List<List<RecipeInfo<CraftingRecipe>>> filteredRecipesByGroup = Collections.emptyList();
    protected int selectedCraftingButton;
    private boolean onlyCraftableRecipes = false;
    protected final ContainerListener listener = new ContainerListener() {
        public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
            if (typeTabList.getIndex() == 0) {
                if (onlyCraftableRecipes)
                    filteredRecipesByGroup = recipesByTab.get(page.get() * getMaxTabCount() + craftingTabList.getIndex()).stream().map(l -> l.stream().filter(r -> RecipeMenu.canCraft(r.getOptionalIngredients(), inventory, abstractContainerMenu.getCarried())).toList()).filter(l -> !l.isEmpty()).toList();
                craftingButtons.get(selectedCraftingButton).updateRecipeDisplay();
            } else {
                if (getCraftingButtons().size() > selectedCraftingButton && getCraftingButtons().get(selectedCraftingButton) instanceof CustomCraftingIconHolder h)
                    h.updateRecipe();
            }
        }

        public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int j) {

        }
    };

    public LegacyCraftingScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component, boolean is2x2) {
        super(abstractContainerMenu, inventory, component);
        this.inventory = inventory;
        this.is2x2 = is2x2;
        gridDimension = is2x2 ? 2 : 3;
        ingredientsGrid = new ArrayList<>(Collections.nCopies(gridDimension * gridDimension, Optional.empty()));
        container = new TransientCraftingContainer(abstractContainerMenu, gridDimension, gridDimension);
        warningSlots = new boolean[gridDimension * gridDimension];
        if (Minecraft.getInstance().level == null) return;
        CraftingInput input = container.asCraftInput();
        List<RecipeInfo<CraftingRecipe>> allRecipes = CommonRecipeManager.byType(RecipeType.CRAFTING).stream().map(h -> RecipeInfo.create(h./*? if >1.20.1 {*/id()/*?} else {*//*getId()*//*?}*/, h/*? if >1.20.1 {*/.value()/*?}*/, h/*? if >1.20.1 {*/.value()/*?}*/ instanceof ShapedRecipe rcp ? LegacyCraftingMenu.updateShapedIngredients(new ArrayList<>(ingredientsGrid), LegacyCraftingMenu.getRecipeOptionalIngredients(rcp), gridDimension, rcp.getWidth(), rcp.getHeight()) : h/*? if >1.20.1 {*/.value()/*?}*/ instanceof ShapelessRecipe r ? LegacyCraftingMenu.getRecipeOptionalIngredients(r) : Collections.emptyList(), h/*? if >1.20.1 {*/.value()/*?}*/.isSpecial() ? ItemStack.EMPTY : h/*? if >1.20.1 {*/.value()/*?}*/.assemble(/*? if <1.20.5 {*//*container*//*?} else {*/input/*?}*/, Minecraft.getInstance().level.registryAccess()))).filter(h -> h.getOptionalIngredients().size() <= ingredientsGrid.size()).toList();
        for (LegacyCraftingTabListing listing : Legacy4JClient.legacyCraftingListingManager.map().values()) {
            List<List<RecipeInfo<CraftingRecipe>>> groups = new ArrayList<>();
            listing.craftings().values().forEach(l -> {
                if (l.isEmpty()) return;
                List<RecipeInfo<CraftingRecipe>> group = new ArrayList<>();
                l.forEach(v -> v.addRecipes(allRecipes, group::add));
                group.removeIf(i -> i.isInvalid() || i.getOptionalIngredients().size() > ingredientsGrid.size());
                if (!group.isEmpty()) groups.add(group);
            });
            if (groups.isEmpty()) continue;

            recipesByTab.add(groups);

            craftingTabList.add(LegacyTabButton.Type.MIDDLE, listing.icon(), listing.nameOrEmpty(), t -> resetElements());

        }

        var recipesByGroupsCollector = Collectors.<RecipeInfo<CraftingRecipe>, String>groupingBy(h -> h.get().group().isEmpty() ? h.getId().toString() : h.get().group());


        if (LegacyOptions.vanillaTabs.get()) {
            allRecipes.stream().collect(Collectors.groupingBy(h -> h.get().category(), () -> new TreeMap<>(Comparator.comparingInt(Enum::ordinal)), recipesByGroupsCollector)).forEach((category, m) -> {
                if (m.isEmpty()) return;
                List<List<RecipeInfo<CraftingRecipe>>> groups = new ArrayList<>();
                m.values().forEach(l -> {
                    l.removeIf(i -> i.isInvalid() || i.getOptionalIngredients().size() > ingredientsGrid.size());
                    if (!l.isEmpty()) groups.add(l);
                });
                if (groups.isEmpty()) return;
                recipesByTab.add(groups);
                craftingTabList.add(LegacyTabButton.Type.MIDDLE, LegacyTabButton.iconOf(VANILLA_CATEGORY_ICONS[category.ordinal()]), getTitle(), t -> resetElements());
            });
        }
        if (LegacyOptions.modCraftingTabs.get()) {
            allRecipes.stream().collect(Collectors.groupingBy(h -> h.getId().getNamespace(), () -> new TreeMap<>(Comparator.<String>naturalOrder()), recipesByGroupsCollector)).forEach((namespace, m) -> {
                ModInfo modInfo = FactoryAPIPlatform.getModInfo(namespace);
                if (modInfo == null || namespace.equals("minecraft") || namespace.equals(Legacy4J.MOD_ID) || m.isEmpty()) return;
                List<List<RecipeInfo<CraftingRecipe>>> groups = new ArrayList<>();
                m.values().forEach(l -> {
                    l.removeIf(i -> i.isInvalid() || i.getOptionalIngredients().size() > ingredientsGrid.size());
                    if (!l.isEmpty()) groups.add(l);
                });
                if (groups.isEmpty()) return;
                recipesByTab.add(groups);
                craftingTabList.add(LegacyTabButton.Type.MIDDLE, LegacyTabButton.iconOf(ModsScreen.modLogosCache.apply(modInfo)), Component.literal(modInfo.getName()), t -> resetElements());
            });
        }

        for (TypeCraftingTab value : Legacy4JClient.typeCraftingTabs.map().values()) {
            if (!value.isValid()) continue;

            if (value.is(TypeCraftingTab.CRAFTING)) {
                typeTabLists.add(craftingTabList);
            } else {
                TabList tabList = new TabList(accessor);
                typeTabLists.add(tabList);
                for (TypeCraftingTab.CustomTab tab : value.tabs()) {
                    if (!tab.allows2x2Grid() && is2x2) continue;
                    tabList.add(LegacyTabButton.Type.MIDDLE, tab.iconHolder().icon(), CommonComponents.EMPTY, t -> resetElements());
                }
            }

            typeTabs.add(value);

            typeTabList.add(42, 42, LegacyTabButton.Type.MIDDLE, value.icon(), value.nameOrEmpty(), b -> resetAll());
        }

        addCraftingButtons();
        resetElements(false);
        accessor.addStatic(UIDefinition.createBeforeInit(a -> {
            accessor.putStaticElement("is2x2", is2x2);
            accessor.putStaticElement("craftingButtons.count", getCraftingButtons().size());
        }));

        Consumer<CustomCraftingIconHolder> fireworkStarUpdateRecipe = h -> {
            clearIngredients(ingredientsGrid);
            if (fireworkStarButtons.isEmpty()) return;
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid, List.of(Optional.empty(), Optional.of(FactoryIngredient.of(fireworkStarButtons.get(0).itemIcon).toIngredient()), Optional.empty(), Optional.of(FireworkStarRecipeAccessor.getGunpowderIngredient())), gridDimension, 2, 2);
            fireworkStarButtons.forEach(CustomRecipeIconHolder::applyAddedIngredients);
            resultStack = fireworkStarButtons.get(0).hasItem() ? h.assembleCraftingResult(minecraft.level, container) : Items.FIREWORK_STAR.getDefaultInstance();
            canCraft(ingredientsGrid, true);
        };
        Consumer<CustomCraftingIconHolder> fireworkStarFadeUpdateRecipe = h -> {
            clearIngredients(ingredientsGrid);
            if (fireworkStarFadeButtons.isEmpty()) return;
            ItemStack item = fireworkStarFadeButtons.get(0).itemIcon.isEmpty() ? Items.FIREWORK_STAR.getDefaultInstance() : fireworkStarFadeButtons.get(0).itemIcon.copyWithCount(1);
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid, List.of(Optional.empty(), Optional.of(FactoryIngredient.of(fireworkStarFadeButtons.get(1).itemIcon).toIngredient()), Optional.empty(), Optional.of(StackIngredient.of(true, item))), gridDimension, 2, 2);
            fireworkStarFadeButtons.get(1).applyAddedIngredients();
            resultStack = fireworkStarFadeButtons.get(0).itemIcon.isEmpty() ? item : h.assembleCraftingResult(Minecraft.getInstance().level, container);
            canCraft(ingredientsGrid, true);
        };
        Consumer<CustomCraftingIconHolder> fireworkRocketUpdateRecipe = h -> {
            clearIngredients(ingredientsGrid);
            if (fireworkButtons.isEmpty()) return;
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid, List.of(Optional.empty(), Optional.of(FactoryIngredient.of(fireworkButtons.get(0).itemIcon).toIngredient()), Optional.empty(), Optional.of(FireworkRocketRecipeAccessor.getPaperIngredient())), gridDimension, 2, 2);
            fireworkButtons.forEach(CustomRecipeIconHolder::applyAddedIngredients);
            resultStack = fireworkButtons.get(0).hasItem() ? h.assembleCraftingResult(Minecraft.getInstance().level, container) : new ItemStack(Items.FIREWORK_ROCKET, 3);
            canCraft(ingredientsGrid, true);
        };
        Consumer<CustomCraftingIconHolder> dyeArmorUpdateRecipe = h -> {
            clearIngredients(ingredientsGrid);
            if (dyeArmorButtons.isEmpty()) return;
            ItemStack armor = dyeArmorButtons.get(0).itemIcon.isEmpty() ? Items.LEATHER_HELMET.getDefaultInstance() : dyeArmorButtons.get(0).itemIcon.copyWithCount(1);
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid, List.of(Optional.empty(), Optional.of(Ingredient.of(dyeArmorButtons.get(1).itemIcon.getItem())), Optional.empty(), Optional.of(StackIngredient.of(true, armor))), gridDimension, 2, 2);
            dyeArmorButtons.forEach(CustomRecipeIconHolder::applyAddedIngredients);
            resultStack = dyeArmorButtons.get(0).itemIcon.isEmpty() || !dyeArmorButtons.get(1).hasItem() ? armor : h.assembleCraftingResult(Minecraft.getInstance().level, container);
            canCraft(ingredientsGrid, true);
        };
        Consumer<CustomCraftingIconHolder> dyeItemUpdateRecipe = h -> {
            clearIngredients(ingredientsGrid);
            if (dyeItemButtons.isEmpty()) return;
            ItemStack item = dyeItemButtons.get(0).itemIcon.isEmpty() ? Items.WHITE_BED.getDefaultInstance() : dyeItemButtons.get(0).itemIcon.copyWithCount(1);
            Optional<Ingredient> dyeIngredient = Optional.of(Ingredient.of(dyeItemButtons.get(1).itemIcon.getItem()));
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid, List.of(Optional.empty(), dyeIngredient, Optional.empty(), Optional.of(StackIngredient.of(true, item))), gridDimension, 2, 2);
            resultStack = dyeItemButtons.get(0).itemIcon.isEmpty() ? item : h.assembleCraftingResult(Minecraft.getInstance().level, container);
            if (resultStack.isEmpty()) {
                resultStack = item;
                ingredientsGrid.set(ingredientsGrid.indexOf(dyeIngredient), Optional.of(new ImpossibleIngredient(dyeItemButtons.get(1).itemIcon)));
            }
            canCraft(ingredientsGrid, true);

        };
        List<ItemStack> dyes = Arrays.stream(DyeColor.values()).map(c -> DyeItem.byColor(c).getDefaultInstance()).toList();
        fireworkStarButtons.add(craftingButtonByList(LegacyComponents.COLOR_TAB, dyes, fireworkStarUpdateRecipe).enableAddIngredients());
        fireworkStarButtons.add(craftingButtonByList(LegacyComponents.SHAPE_TAB, FireworkStarRecipeAccessor.getShapeByItem().keySet().stream().map(ItemStack::new).toList(), fireworkStarUpdateRecipe).enableAddIngredients(h -> ingredientsGrid.stream().noneMatch(i -> i.isPresent() && Arrays.stream(FactoryIngredient.of(i.get()).getStacks()).anyMatch(item -> FireworkStarRecipeAccessor.getShapeByItem().containsKey(item.getItem())))));
        fireworkStarButtons.add(craftingButtonByList(LegacyComponents.EFFECT_TAB, Stream.concat(Arrays.stream(FactoryIngredient.of(FireworkStarRecipeAccessor.getTwinkleIngredient()).getStacks()), Arrays.stream(FactoryIngredient.of(FireworkStarRecipeAccessor.getTrailIngredient()).getStacks())).toList(), fireworkStarUpdateRecipe).enableAddIngredients(h -> ingredientsGrid.stream().noneMatch(i -> i.isPresent() && i.get().test(h.itemIcon))));

        fireworkStarFadeButtons.add(craftingButtonByPredicate(LegacyComponents.SELECT_STAR_TAB, i -> i.is(Items.FIREWORK_STAR), fireworkStarFadeUpdateRecipe));
        fireworkStarFadeButtons.add(craftingButtonByList(LegacyComponents.ADD_FADE_TAB, dyes, fireworkStarFadeUpdateRecipe).enableAddIngredients());

        fireworkButtons.add(craftingButtonByList(LegacyComponents.ADD_POWER_TAB, Arrays.stream(FactoryIngredient.of(FireworkRocketRecipeAccessor.getGunpowderIngredient()).getStacks()).toList(), fireworkRocketUpdateRecipe).enableAddIngredients(h -> ingredientsGrid.stream().filter(i -> i.isPresent() && i.get().equals(FireworkRocketRecipeAccessor.getGunpowderIngredient())).count() < 3));
        fireworkButtons.add(craftingButtonByPredicate(LegacyComponents.SELECT_STAR_TAB, i -> i.is(Items.FIREWORK_STAR) && /*? if <1.20.5 {*//*i.hasTag() && i.getTag().contains("Explosion")*//*?} else {*/i.get(DataComponents.FIREWORK_EXPLOSION) != null/*?}*/, fireworkRocketUpdateRecipe).enableAddIngredients());

        dyeArmorButtons.add(craftingButtonByPredicate(Component.translatable("legacy.container.tab.armour"), i -> /*? if <1.20.5 {*//*i.getItem() instanceof DyeableLeatherItem*//*?} else {*/i.is(ItemTags.DYEABLE)/*?}*/, dyeArmorUpdateRecipe));
        dyeArmorButtons.add(craftingButtonByList(LegacyComponents.COLOR_TAB, dyes, dyeArmorUpdateRecipe).enableAddIngredients());
        dyeItemButtons.add(craftingButtonByPredicate(Component.translatable("entity.minecraft.item"), i -> i.getItem() instanceof BedItem || (i.getItem() instanceof BlockItem b && b.getBlock() instanceof ShulkerBoxBlock/*? if >=1.21.4 {*/ || i.getItem() instanceof BundleItem/*?}*/), dyeItemUpdateRecipe));
        dyeItemButtons.add(craftingButtonByList(LegacyComponents.COLOR_TAB, dyes, dyeItemUpdateRecipe));
        if (!is2x2)
            bannerButtons.add(craftingButtonByRecipes(LegacyComponents.CREATE_BANNER_TAB, Arrays.stream(DyeColor.values()).flatMap(c -> allRecipes.stream().filter(new RecipeInfo.Filter.ItemId(BuiltInRegistries.ITEM.getKey(BannerBlock.byColor(c).asItem())))).toList()));
        bannerButtons.add(craftingButtonByPredicate(LegacyComponents.COPY_BANNER, i -> i.getItem() instanceof BannerItem && LegacyItemUtil.hasValidPatterns(i), h -> {
            clearIngredients(ingredientsGrid);
            if (bannerButtons.isEmpty() || h.itemIcon.isEmpty()) return;
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid, List.of(Optional.empty(), Optional.empty(), Optional.of(StackIngredient.of(true, h.itemIcon.getItem().getDefaultInstance())), Optional.of(StackIngredient.of(true, h.itemIcon.copyWithCount(1)))), gridDimension, 2, 2);
            resultStack = h.itemIcon.copyWithCount(1);
            canCraft(ingredientsGrid, true);
        }));
        Consumer<CustomCraftingIconHolder> decorateShieldUpdateRecipe = h -> {
            clearIngredients(ingredientsGrid);
            if (decorateShieldButtons.isEmpty()) return;

            ItemStack inputStack = decorateShieldButtons.get(0).itemIcon.isEmpty() ? Items.SHIELD.getDefaultInstance() : decorateShieldButtons.get(0).itemIcon.copyWithCount(1);

            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid, List.of(Optional.empty(), Optional.empty(), Optional.of(StackIngredient.of(true, inputStack.copy())), Optional.of(StackIngredient.of(true, decorateShieldButtons.get(1).itemIcon.isEmpty() ? Items.WHITE_BANNER.getDefaultInstance() : decorateShieldButtons.get(1).itemIcon.copyWithCount(1)))), gridDimension, 2, 2);

            resultStack = decorateShieldButtons.get(0).itemIcon.isEmpty() ? inputStack : h.assembleCraftingResult(minecraft.level, container);
            canCraft(ingredientsGrid, true);
        };
        decorateShieldButtons.add(craftingButtonByPredicate(LegacyComponents.SELECT_SHIELD, i -> i.getItem() instanceof ShieldItem && LegacyItemUtil.getPatternsCount(i) == 0, decorateShieldUpdateRecipe));
        decorateShieldButtons.add(craftingButtonByPredicate(LegacyComponents.SELECT_BANNER_TAB, i -> i.getItem() instanceof BannerItem, decorateShieldUpdateRecipe));

        decoratedPotButtons.add(craftingButtonByList(LegacyComponents.ADD_SHERD, DecoratedPotPatterns.ITEM_TO_POT_TEXTURE.keySet().stream().map(Item::getDefaultInstance).toList(), h -> {
            clearIngredients(ingredientsGrid);
            if (is2x2) return;
            Function<Integer, Item> sherdByIndex = i -> h.addedIngredientsItems.size() > i ? h.addedIngredientsItems.get(i).getItem() : Items.BRICK;
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid, List.of(Optional.empty(), Optional.of(Ingredient.of(sherdByIndex.apply(0))), Optional.empty(), Optional.of(Ingredient.of(sherdByIndex.apply(1))), Optional.empty(), Optional.of(Ingredient.of(sherdByIndex.apply(2))), Optional.empty(), Optional.of(Ingredient.of(sherdByIndex.apply(3)))), gridDimension, 3, 3);
            resultStack = h.assembleCraftingResult(minecraft.level, container);
            canCraft(ingredientsGrid, true);
        }).enableAddIngredients(h -> h.addedIngredientsItems.size() < 4));
    }

    public static LegacyCraftingScreen craftingScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
        return new LegacyCraftingScreen(abstractContainerMenu, inventory, component, false);
    }

    public static LegacyCraftingScreen playerCraftingScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
        return new LegacyCraftingScreen(abstractContainerMenu, inventory, component, true);
    }

    public static boolean canCraft(List<ItemStack> compactItemStackList, List<Optional<Ingredient>> ings, boolean[] warningSlots) {
        boolean canCraft = true;
        boolean isAllEmpty = true;
        main:
        for (int i1 = 0; i1 < ings.size(); i1++) {
            Optional<Ingredient> ing = ings.get(i1);
            if (ing.isEmpty()) continue;
            isAllEmpty = false;
            ingLoop:
            for (int c = 0; c < FactoryIngredient.of(ing.get()).getCount(); c++) {
                for (ItemStack itemStack : compactItemStackList) {
                    if (!itemStack.isEmpty() && ing.get().test(itemStack.copyWithCount(1))) {
                        itemStack.shrink(1);
                        if (warningSlots != null) warningSlots[i1] = false;
                        continue ingLoop;
                    }
                }
                canCraft = false;
                if (warningSlots == null) break main;
                else warningSlots[i1] = true;
            }
        }
        return canCraft && !isAllEmpty;
    }

    public static void clearIngredients(List<Optional<Ingredient>> ingredientsGrid) {
        for (int i = 0; i < ingredientsGrid.size(); i++) {
            if (ingredientsGrid.get(i).isPresent()) ingredientsGrid.set(i, Optional.empty());
        }
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        ControlTooltip.setupDefaultButtons(renderer, this);
        Event.super.addControlTooltips(renderer);
        renderer.
                add(EXTRA::get, () -> typeTabList.getIndex() == 0 ? LegacyComponents.INFO : getFocused() instanceof CustomCraftingIconHolder h && h.addedIngredientsItems != null && !h.addedIngredientsItems.isEmpty() ? LegacyComponents.REMOVE : null).
                add(OPTION::get, () -> typeTabList.getIndex() == 0 ? onlyCraftableRecipes ? LegacyComponents.ALL_RECIPES : LegacyComponents.SHOW_CRAFTABLE_RECIPES : ControlTooltip.getKeyMessage(InputConstants.KEY_O, this)).
                add(CONTROL_TYPE::get, () -> hasTypeTabList() ? LegacyComponents.TYPE : null).
                add(CONTROL_TAB::get, () -> LegacyComponents.GROUP).
                add(CONTROL_PAGE::get, () -> page.max > 0 && typeTabList.getIndex() == 0 ? LegacyComponents.PAGE : null);
    }

    public void resetElements() {
        resetElements(true);
    }

    public void resetElements(boolean reposition) {
        if (!reposition) listener.slotChanged(menu, -1, ItemStack.EMPTY);
        selectedCraftingButton = 0;
        infoType.set(0);
        craftingButtonsOffset.set(0);
        if (typeTabList.getIndex() == 0) craftingButtons.get(selectedCraftingButton).invalidateFocused();
        if (reposition) repositionElements();
    }

    public void resetAll() {
        resetElements();
        getTabList().resetSelectedTab();
    }

    protected CustomCraftingIconHolder craftingButtonByRecipes(Component displayName, List<RecipeInfo<CraftingRecipe>> recipes) {
        List<ItemStack> results = recipes.stream().map(RecipeInfo::getResultItem).toList();
        return new CustomCraftingIconHolder(results.get(0)) {
            public Component getDisplayName() {
                return displayName;
            }

            public ItemStack nextItem() {
                return nextItem(results);
            }

            public ItemStack previousItem() {
                return previousItem(results);
            }

            public int findInventoryMatchSlot() {
                return -1;
            }

            public void updateRecipe() {
                scrollableRenderer.resetScrolled();
                ingredientsGrid.clear();
                ingredientsGrid.addAll(recipes.get(results.indexOf(itemIcon)).getOptionalIngredients());
                resultStack = itemIcon.copyWithCount(1);
                LegacyCraftingScreen.this.canCraft(ingredientsGrid, true);
            }

            @Override
            protected boolean hasItem(ItemStack stack) {
                return LegacyCraftingScreen.this.canCraft(recipes.get(results.indexOf(stack)).getOptionalIngredients(), false);
            }

            @Override
            public void craft(InputWithModifiers input) {
                LegacySoundUtil.playSimpleUISound(SoundEvents.ITEM_PICKUP, 1.0f);
                CommonNetwork.sendToServer(new ServerMenuCraftPayload(recipes.get(results.indexOf(itemIcon)), input.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.state().pressed));
            }
        };
    }

    protected CustomCraftingIconHolder craftingButtonByList(Component displayName, List<ItemStack> itemStacks, Consumer<CustomCraftingIconHolder> updateRecipe) {
        return new CustomCraftingIconHolder(itemStacks.get(0)) {
            public Component getDisplayName() {
                return displayName;
            }

            public ItemStack nextItem() {
                return nextItem(itemStacks);
            }

            public ItemStack previousItem() {
                return previousItem(itemStacks);
            }

            public int findInventoryMatchSlot() {
                for (int i = 0; i < menu.slots.size(); i++)
                    if (menu.slots.get(i).getItem().is(itemIcon.getItem())) return i;
                return 0;
            }

            public void updateRecipe() {
                scrollableRenderer.resetScrolled();
                updateRecipe.accept(this);
            }
        };
    }

    protected CustomCraftingIconHolder craftingButtonByPredicate(Component displayName, Predicate<ItemStack> isValid, Consumer<CustomCraftingIconHolder> updateRecipe) {
        return new CustomCraftingIconHolder() {
            public Component getDisplayName() {
                return displayName;
            }

            public ItemStack nextItem() {
                return nextItem(inventory, isValid);
            }

            public ItemStack previousItem() {
                return previousItem(inventory, isValid);
            }

            public boolean applyNextItemIfAbsent() {
                return true;
            }

            public int findInventoryMatchSlot() {
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

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        Component title = getTabList() == craftingTabList ? getTabList().tabButtons.get(getTabList().getIndex()).getMessage() : getFocused() instanceof CustomCraftingIconHolder h ? h.getDisplayName() : CommonComponents.EMPTY;
        LegacyFontUtil.applySDFont(b -> guiGraphics.drawString(this.font, title, ((typeTabList.getIndex() == 0 ? imageWidth : imageWidth / 2) - font.width(title)) / 2, accessor.getInteger("title.y", 17), CommonColor.INVENTORY_GRAY_TEXT.get(), false));
        int inventoryPanelX = accessor.getInteger("inventoryPanel.x", 176);
        int bottomPanelY = accessor.getInteger("bottomPanel.y", 103);
        int inventoryPanelWidth = accessor.getInteger("inventoryPanel.width", 163);
        if (infoType.get() <= 0)
            LegacyFontUtil.applySDFont(b -> guiGraphics.drawString(this.font, this.playerInventoryTitle, inventoryPanelX + (inventoryPanelWidth - font.width(playerInventoryTitle)) / 2, bottomPanelY + accessor.getInteger("inventoryTitle.y", 11), CommonColor.INVENTORY_GRAY_TEXT.get(), false));
        else {
            if (selectedCraftingButton < getCraftingButtons().size() && getCraftingButtons().get(selectedCraftingButton) instanceof RecipeIconHolder<?> h) {
                if (infoType.get() == 1 && LegacyTipManager.hasTip(h.getFocusedResult())) {
                    LegacyFontUtil.applySDFont(b -> {
                        List<FormattedCharSequence> l = font.split(LegacyTipManager.getTipComponent(h.getFocusedResult()), inventoryPanelWidth - 11);
                        scrollableRenderer.scrolled.max = Math.max(0, l.size() - 7);
                        scrollableRenderer.render(guiGraphics, inventoryPanelX + 5, bottomPanelY + 2, inventoryPanelWidth - 11, 84, () -> {
                            for (int i1 = 0; i1 < l.size(); i1++)
                                guiGraphics.drawString(font, l.get(i1), inventoryPanelX + 5, bottomPanelY + 5 + i1 * (b ? 8 : 12), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                        });
                    });
                } else if (infoType.get() == 2) {
                    LegacyFontUtil.applySDFont(b -> guiGraphics.drawString(this.font, LegacyComponents.INGREDIENTS, inventoryPanelX + (inventoryPanelWidth - font.width(LegacyComponents.INGREDIENTS)) / 2, bottomPanelY + 5, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
                    if (h.getFocusedRecipe() != null) {
                        compactItemStackList.clear();
                        RecipeMenu.handleCompactItemStackList(compactItemStackList, () -> h.getFocusedRecipe().getOptionalIngredients().stream().map(RecipeIconHolder::getActualItem).iterator());
                        scrollableRenderer.scrolled.max = Math.max(0, compactItemStackList.size() - 4);
                        scrollableRenderer.render(guiGraphics, inventoryPanelX + 2, bottomPanelY + 19, 152, 60, () -> {
                            for (int i1 = 0; i1 < compactItemStackList.size(); i1++) {
                                ItemStack ing = compactItemStackList.get(i1);
                                LegacyRenderUtil.iconHolderRenderer.itemHolder(inventoryPanelX + 4, bottomPanelY + 21 + 15 * i1, 14, 14, ing, false, Vec2.ZERO).render(guiGraphics, i, j, 0);
                                guiGraphics.pose().pushMatrix();
                                guiGraphics.pose().translate(inventoryPanelX + 22, bottomPanelY + 25 + 15 * i1);
                                LegacyFontUtil.applySmallerFont(LegacyFontUtil.MOJANGLES_11_FONT, b -> {
                                    if (!b) guiGraphics.pose().scale(2 / 3f, 2 / 3f);
                                    guiGraphics.drawString(font, ing.getHoverName(), 0, 0, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                                });
                                guiGraphics.pose().popMatrix();
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderContents(guiGraphics, i, j, f);

        getCraftingButtons().forEach(b -> b.render(guiGraphics, i, j, 0));
        if (selectedCraftingButton < getCraftingButtons().size())
            getCraftingButtons().get(selectedCraftingButton).renderSelection(guiGraphics, i, j, 0);
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state.pressed && state.canClick()) {
            if ((state.is(ControllerBinding.LEFT_TRIGGER) || state.is(ControllerBinding.RIGHT_TRIGGER)) && hasTypeTabList())
                typeTabList.controlTab(state.is(ControllerBinding.LEFT_TRIGGER), state.is(ControllerBinding.RIGHT_TRIGGER));
            if (state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis s && typeTabList.getIndex() == 0)
                craftingTabList.controlPage(page, s.x < 0 && -s.x > Math.abs(s.y), s.x > 0 && s.x > Math.abs(s.y));
        }
    }

    @Override
    public int getTabYOffset() {
        return accessor.getInteger("tabYOffset", 18);
    }

    @Override
    public int getTabXOffset() {
        return hasTypeTabList() ? 21 : 0;
    }

    @Override
    protected void init() {
        resultStack = ItemStack.EMPTY;
        imageWidth = 348;
        imageHeight = 215;
        super.init();
        LegacySlotDisplay display = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return 16;
            }

            @Override
            public Vec2 getOffset() {
                return menu.inventoryOffset;
            }

            @Override
            public boolean isVisible() {
                return menu.inventoryActive;
            }
        };
        for (int i = 0; i < 36; i++) {
            Slot s = menu.slots.get(i);
            if (i < 27) {
                LegacySlotDisplay.override(s, 186 + (s.getContainerSlot() - 9) % 9 * 16, 133 + (s.getContainerSlot() - 9) / 9 * 16, display);
            } else {
                LegacySlotDisplay.override(s, 186 + s.getContainerSlot() * 16, 186, display);
            }
        }
        leftPos += getTabXOffset();
        topPos += getTabYOffset();
        menu.addSlotListener(listener);
        menu.inventoryActive = accessor.putBearer("isInventoryActive", Bearer.of(infoType.get() <= 0));
        menu.inventoryOffset = accessor.getElementValue("inventoryOffset", LegacyCraftingMenu.DEFAULT_INVENTORY_OFFSET, Vec2.class);
        if (hasTypeTabList()) addWidget(typeTabList);
        int count = getCraftingButtons().size();
        if (selectedCraftingButton < count) setFocused(getCraftingButtons().get(selectedCraftingButton));
        int craftingButtonsX = accessor.getInteger("craftingButtons.x", 13);
        int craftingButtonsY = accessor.getInteger("craftingButtons.y", 38);
        int craftingButtonsSize = accessor.getInteger("craftingButtons.size", 27);
        if (typeTabList.getIndex() == 0 || !hasTypeTabList()) {
            craftingButtonsOffset.max = Math.max(0, recipesByTab.get(page.get() * getMaxTabCount() + craftingTabList.getIndex()).size() - 12);
            craftingButtons.forEach(b -> {
                b.width = b.height = craftingButtonsSize;
                b.setPos(leftPos + craftingButtonsX + craftingButtons.indexOf(b) * b.width, topPos + craftingButtonsY);
                addWidget(b);
            });
        } else {
            int slotX = accessor.getInteger("customCraftingButtons.x", (count == 1 ? 77 : count == 2 ? 52 : count == 3 ? 21 : 8));
            int xOffset = accessor.getInteger("customCraftingButtons.offset.x", (count == 2 ? 35 : count == 3 ? 28 : 18));
            getCraftingButtons().forEach(b -> {
                b.width = b.height = craftingButtonsSize;
                int index = getCraftingButtons().indexOf(b);
                b.setPos(leftPos + slotX + index * (b.width + xOffset), topPos + craftingButtonsY);
                if (count == 3)
                    b.offset = new Vec2((LegacyRenderUtil.hasHorizontalArtifacts() ? 0.0125f : 0.0f) + 0.5f + index * 0.5f, 0);
                b.init();
                addWidget(b);
            });
        }
        addWidget(getTabList());
        getTabList().init(leftPos, topPos - 37, imageWidth, 43, (t, i) -> {
            int index = getTabList().tabButtons.indexOf(t);
            t.type = LegacyTabButton.Type.bySize(index, getMaxTabCount());
            t.setWidth(accessor.getInteger("tabList.buttonWidth", 51));
            t.offset = (t1) -> new Vec2((LegacyRenderUtil.hasHorizontalArtifacts() && index % 2 != 0 ? 0.0125f : 0.0f) + accessor.getFloat("tabList.buttonOffset.x", -1.5f) * index, t1.selected ? 0 : accessor.getFloat("tabList.selectedOffset.y", 4.4f));
        });
        if (hasTypeTabList()) typeTabList.init((b, i) -> {
            b.setWidth(accessor.getInteger("typeTabList.buttonWidth", 42));
            b.setHeight(accessor.getInteger("typeTabList.height", 42));
            b.spriteRender = accessor.getElementValue("typeTabList.sprites", LegacyTabButton.ToggleableTabSprites.VERTICAL, LegacyTabButton.Render.class);
            b.setX(leftPos - b.getWidth() + 6);
            b.setY(topPos + i + 4);
            b.offset = (t1) -> new Vec2(t1.selected ? 0 : 3.4f, 0.4f);
        }, true);
        listener.slotChanged(menu, -1, ItemStack.EMPTY);
    }

    public TabList getTabList() {
        return hasTypeTabList() ? typeTabLists.get(Math.min(typeTabList.getIndex(), typeTabLists.size() - 1)) : craftingTabList;
    }

    public boolean hasTypeTabList() {
        return accessor.getBoolean("hasTypeTabList", true) && !typeTabLists.isEmpty();
    }

    protected boolean canCraft(List<Optional<Ingredient>> ingredients, boolean isFocused) {
        compactItemStackList.clear();
        RecipeMenu.handleCompactInventoryList(compactItemStackList, inventory, menu.getCarried());
        return canCraft(compactItemStackList, isFocused ? ingredientsGrid : ingredients, isFocused ? warningSlots : null);
    }

    protected int getMaxTabCount() {
        return accessor.getInteger("maxTabCount", 7);
    }

    protected void addCraftingButtons() {
        for (int i = 0; i < 12; i++) {
            int index = i;

            RecipeIconHolder<CraftingRecipe> h;
            craftingButtons.add(h = new RecipeIconHolder<>(leftPos + 13 + i * 27, topPos + 38) {
                @Override
                public void render(GuiGraphics graphics, int i, int j, float f) {
                    if (isFocused()) selectedCraftingButton = index;
                    super.render(graphics, i, j, f);
                }

                protected boolean canCraft(RecipeInfo<CraftingRecipe> rcp) {
                    if (rcp == null || onlyCraftableRecipes) return true;
                    return LegacyCraftingScreen.this.canCraft(rcp.getOptionalIngredients(), isFocused() && getFocusedRecipe() == rcp);
                }

                protected List<RecipeInfo<CraftingRecipe>> getRecipes() {
                    List<List<RecipeInfo<CraftingRecipe>>> list = onlyCraftableRecipes ? filteredRecipesByGroup : recipesByTab.get(page.get() * getMaxTabCount() + craftingTabList.getIndex());
                    return list.size() <= craftingButtonsOffset.get() + index ? Collections.emptyList() : list.get(craftingButtonsOffset.get() + index);
                }

                @Override
                public LegacyScrollRenderer getScrollRenderer() {
                    return scrollRenderer;
                }

                @Override
                protected void toggleCraftableRecipes(InputWithModifiers input) {
                    onlyCraftableRecipes = !onlyCraftableRecipes;
                    listener.slotChanged(menu, 0, ItemStack.EMPTY);
                }

                @Override
                public boolean keyPressed(KeyEvent keyEvent) {
                    if (controlCyclicNavigation(keyEvent.key(), index, craftingButtons, craftingButtonsOffset, scrollRenderer, LegacyCraftingScreen.this))
                        return true;
                    if (keyEvent.key() == InputConstants.KEY_X && (typeTabList.getIndex() == 0 || hasTypeTabList())) {
                        infoType.add(1, true);
                        menu.inventoryActive = infoType.get() <= 0;
                        LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
                        return true;
                    }
                    return super.keyPressed(keyEvent);
                }

                protected void updateRecipeDisplay(RecipeInfo<CraftingRecipe> rcp) {
                    scrollableRenderer.resetScrolled();
                    resultStack = getFocusedResult();
                    clearIngredients(ingredientsGrid);
                    if (rcp == null) return;
                    for (int i = 0; i < rcp.getOptionalIngredients().size(); i++)
                        ingredientsGrid.set(i, rcp.getOptionalIngredients().get(i));
                }

                @Override
                public void craft(InputWithModifiers input) {
                    LegacySoundUtil.playSimpleUISound(SoundEvents.ITEM_PICKUP, 1.0f);
                    super.craft(input);
                }
            });
            h.offset = LegacyCraftingMenu.DEFAULT_INVENTORY_OFFSET;
        }
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (this.getChildAt(d, e).filter((guiEventListener) -> guiEventListener.mouseScrolled(d, e, f, g)).isPresent())
            return true;
        if (super.mouseScrolled(d, e, f, g)) return true;
        if (scrollableRenderer.mouseScrolled(g)) return true;
        int scroll = (int) Math.signum(g);
        if (((craftingButtonsOffset.get() > 0 && scroll < 0) || (scroll > 0 && craftingButtonsOffset.max > 0)) && craftingButtonsOffset.add(scroll, false) != 0) {
            repositionElements();
            return true;
        }
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        if (hasTypeTabList()) typeTabList.render(guiGraphics, i, j, f);
        getTabList().render(guiGraphics, i, j, f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(accessor.getResourceLocation("imageSprite", LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        getTabList().renderSelected(guiGraphics, i, j, f);
        if (hasTypeTabList()) typeTabList.renderSelected(guiGraphics, i, j, f);
        int bottomPanelHeight = accessor.getInteger("bottomPanel.height", 105);
        int panelWidth = accessor.getInteger("craftingGridPanel.width", 163);
        int bottomPanelY = accessor.getInteger("bottomPanel.y", 103);
        int craftingGridPanelX = accessor.getInteger("craftingGridPanel.x", 9);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + craftingGridPanelX, topPos + bottomPanelY, panelWidth, bottomPanelHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + accessor.getInteger("inventoryPanel.x", 176), topPos + bottomPanelY, accessor.getInteger("inventoryPanel.width", 163), bottomPanelHeight);
        int descriptionPanelWidth = accessor.getInteger("descriptionPanel.width", 163);
        int descriptionPanelHeight = accessor.getInteger("descriptionPanel.height", 93);
        int descriptionPanelX = accessor.getInteger("descriptionPanel.x", 176);
        int descriptionPanelY = accessor.getInteger("descriptionPanel.y", 8);
        if (typeTabList.getIndex() != 0 && hasTypeTabList())
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + descriptionPanelX, topPos + descriptionPanelY, descriptionPanelWidth, descriptionPanelHeight);
        int slotSize = accessor.getInteger("craftingGridSlot.size", 23);
        int xDiff = leftPos + craftingGridPanelX;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMALL_ARROW, xDiff + accessor.getInteger("craftingArrow.x", 77) + (is2x2 ? 0 : slotSize / 2), topPos + bottomPanelY + accessor.getInteger("craftingArrow.y", 57), 16, 14);
        if (typeTabList.getIndex() == 0 || !hasTypeTabList()) {
            if (craftingButtonsOffset.get() > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, accessor.getInteger("horizontalScroll.x", leftPos + 5), topPos + 45);
            if (craftingButtonsOffset.max > 0 && craftingButtonsOffset.get() < craftingButtonsOffset.max)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + imageWidth - 11, topPos + 45);
        }

        int resultSlotSize = accessor.getInteger("craftingResultSlot.size", 36);
        int yDiff = topPos + bottomPanelY;
        int craftingGridSlotX = accessor.getInteger("craftingGridSlot.x", is2x2 ? 24 : 12);
        int craftingGridSlotY = accessor.getInteger("craftingGridSlot.y", 30);
        boolean anyWarning = false;
        for (int index = 0; index < ingredientsGrid.size(); index++) {
            LegacyIconHolder holder = LegacyRenderUtil.iconHolderRenderer.itemHolder(xDiff + craftingGridSlotX + index % gridDimension * slotSize, yDiff + craftingGridSlotY + (is2x2 ? slotSize / 2 : 0) + index / gridDimension * slotSize, slotSize, slotSize, getActualItem(ingredientsGrid.get(index)), ((!onlyCraftableRecipes || typeTabList.getIndex() != 0) && !getActualItem(ingredientsGrid.get(index)).isEmpty() && warningSlots[index]), new Vec2(is2x2 ? 0 : LegacyRenderUtil.hasHorizontalArtifacts() ? 0.4f : 0.5f, is2x2 ? 0 : 0.4f));
            if (holder.isWarning()) anyWarning = true;
            holder.render(guiGraphics, i, j, f);
            holder.renderTooltip(minecraft, guiGraphics, i, j);
        }

        LegacyIconHolder holder = LegacyRenderUtil.iconHolderRenderer.itemHolder(xDiff + accessor.getInteger("resultSlot.x", 104) + (is2x2 ? 0 : slotSize / 2), yDiff + accessor.getInteger("resultSlot.y", 48), resultSlotSize, resultSlotSize, resultStack, anyWarning, new Vec2(LegacyRenderUtil.hasHorizontalArtifacts() ? 0.4f : 0.5f, 0));
        holder.render(guiGraphics, i, j, f);
        holder.renderTooltip(minecraft, guiGraphics, i, j);

        if (!resultStack.isEmpty()) {
            Component resultName = getCraftingButtons().get(selectedCraftingButton) instanceof RecipeIconHolder<?> h && h.isValidIndex() ? h.getFocusedRecipe().getName() : resultStack.getHoverName();
            Component description = getCraftingButtons().get(selectedCraftingButton) instanceof RecipeIconHolder<?> h && h.isValidIndex() ? h.getFocusedRecipe().getDescription() : null;
            int titleY = bottomPanelY + accessor.getInteger("craftingTitle.y", 11) - (description == null ? 0 : 6);
            LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderScrollingString(guiGraphics, font, resultName, xDiff + 2 + Math.max(panelWidth - font.width(resultName), 0) / 2, topPos + titleY, xDiff + panelWidth - 2, topPos + titleY + 11, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
            if (description != null)
                LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderScrollingString(guiGraphics, font, description.copy().setStyle(Style.EMPTY), xDiff + 2 + Math.max(panelWidth - font.width(description), 0) / 2, topPos + titleY + 12, xDiff + panelWidth - 2, topPos + titleY + 23, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
            LegacyFontUtil.applySDFont(b -> {
                if (typeTabList.getIndex() != 0) {
                    int descriptionTextXOffset = accessor.getInteger("descriptionText.x", 4);
                    int descriptionTextYOffset = accessor.getInteger("descriptionText.y", 7);
                    int descriptionTextWidth = descriptionPanelWidth - descriptionTextXOffset * 2;
                    int lineSpacing = (b ? 8 : 12);
                    scrollableRenderer.lineHeight = lineSpacing;
                    int lineAmount = (descriptionPanelHeight - descriptionTextYOffset * 2 - 7) / lineSpacing;
                    List<FormattedCharSequence> list = LegacyRenderUtil.getTooltip(resultStack, descriptionTextWidth);
                    scrollableRenderer.scrolled.max = Math.max(0, list.size() - lineAmount);
                    scrollableRenderer.render(guiGraphics, leftPos + descriptionPanelX + descriptionTextXOffset, topPos + descriptionPanelY + descriptionTextYOffset, descriptionTextWidth, lineAmount * lineSpacing, () -> {
                        for (int i1 = 0; i1 < list.size(); i1++) {
                            FormattedCharSequence sequence = list.get(i1);
                            guiGraphics.drawString(font, sink -> sequence.accept((i2, style, j1) -> sink.accept(i2, Style.EMPTY, j1)), leftPos + descriptionPanelX + descriptionTextXOffset, topPos + descriptionPanelY + descriptionTextYOffset + i1 * lineSpacing, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                        }
                    });
                }
            });
        }
    }

    @Override
    public boolean disableCursorOnInit() {
        return true;
    }

    @Override
    public boolean onceClickBindings(BindingState state) {
        return !state.is(ControllerBinding.DOWN_BUTTON) && Controller.Event.super.onceClickBindings(state);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (hasTypeTabList() && keyEvent.hasShiftDown() && typeTabList.controlTab(keyEvent.key())) {
            return true;
        }
        getTabList().controlTab(keyEvent.key());
        if (keyEvent.hasShiftDown() && typeTabList.getIndex() == 0 && craftingTabList.controlPage(page, keyEvent.isLeft(), keyEvent.isRight()))
            return true;
        return super.keyPressed(keyEvent);
    }

    public List<? extends LegacyIconHolder> getCraftingButtons() {
        TabList tabList = getTabList();
        if (!typeTabs.isEmpty()) {
            TypeCraftingTab tab = typeTabs.get(typeTabList.getIndex());
            if (tab.is(TypeCraftingTab.BANNER))
                return tabList.getIndex() == 0 ? bannerButtons : decorateShieldButtons;
            else if (tab.is(TypeCraftingTab.FIREWORK))
                return tabList.getIndex() == 0 ? fireworkStarButtons : tabList.getIndex() == 1 ? fireworkStarFadeButtons : fireworkButtons;
            else if (tab.is(TypeCraftingTab.DYING))
                return tabList.getIndex() == 0 ? dyeArmorButtons : tabList.getIndex() == 1 ? dyeItemButtons : decoratedPotButtons;
        }
        return craftingButtons;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        getCraftingButtons().forEach(h -> h.renderTooltip(minecraft, guiGraphics, i, j));

        renderTooltip(guiGraphics, i, j);
    }

    protected abstract class CustomCraftingIconHolder extends CustomRecipeIconHolder {
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
            return LegacyCraftingScreen.this.canCraft(getIngredientsGrid(), false);
        }

        public List<Optional<Ingredient>> getIngredientsGrid() {
            return ingredientsGrid;
        }

        @Override
        public void render(GuiGraphics graphics, int i, int j, float f) {
            if (isFocused()) selectedCraftingButton = getCraftingButtons().indexOf(this);
            super.render(graphics, i, j, f);
        }

        @Override
        public void craft(InputWithModifiers input) {
            LegacySoundUtil.playSimpleUISound(SoundEvents.ITEM_PICKUP, 1.0f);
            super.craft(input);
        }
    }
}
