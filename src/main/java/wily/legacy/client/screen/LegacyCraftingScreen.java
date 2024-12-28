package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.*;
//? if >=1.20.5 {
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.FactoryIngredient;
import wily.factoryapi.base.StackIngredient;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.base.network.CommonNetwork;
import wily.factoryapi.util.PagedList;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.inventory.ImpossibleIngredient;
import wily.legacy.mixin.base.FireworkRocketRecipeAccessor;
import wily.legacy.mixin.base.FireworkStarRecipeAccessor;
import wily.legacy.network.CommonRecipeManager;
import wily.legacy.network.ServerMenuCraftPayload;
import wily.legacy.util.*;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
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

public class LegacyCraftingScreen extends AbstractContainerScreen<LegacyCraftingMenu> implements Controller.Event,ControlTooltip.Event,TabList.Access {
    private final Inventory inventory;
    protected final List<ItemStack> compactItemStackList = new ArrayList<>();
    private final boolean is2x2;
    private final int gridDimension;
    private boolean onlyCraftableRecipes = false;
    protected Stocker.Sizeable infoType = new Stocker.Sizeable(0,2);
    protected final List<Optional<Ingredient>> ingredientsGrid;
    protected final CraftingContainer container;
    protected ItemStack resultStack = ItemStack.EMPTY;
    protected final UIDefinition.Accessor accessor = UIDefinition.Accessor.of(this);
    protected final List<RecipeIconHolder<CraftingRecipe>> craftingButtons = new PagedList<>(new Stocker.Sizeable(0,0),()->accessor.getInteger("maxCraftingButtonsCount",12));
    protected final List<CustomRecipeIconHolder> dyeItemButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> dyeArmorButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> bannerButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> decorateShieldButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> decoratedPotButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> fireworkStarButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> fireworkStarFadeButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> fireworkButtons = new ArrayList<>();
    protected final List<List<List<RecipeInfo<CraftingRecipe>>>> recipesByTab = new ArrayList<>();
    protected List<List<RecipeInfo<CraftingRecipe>>> filteredRecipesByGroup = Collections.emptyList();
    protected final Stocker.Sizeable page =  new Stocker.Sizeable(0);
    protected final Stocker.Sizeable craftingButtonsOffset =  new Stocker.Sizeable(0);
    protected final TabList craftingTabList = new TabList(new PagedList<>(page, this::getMaxTabCount));
    protected final TabList bannerTabList = new TabList();
    protected final TabList fireworkTabList = new TabList();
    protected final TabList dyeTabList = new TabList();
    protected final TabList typeTabList = new TabList().add(0,0,42, 42, 4,LegacyTabButton.iconOf(Items.CRAFTING_TABLE),Component.empty(),null, b->resetElements()).add(0,0,42, 42, 4,LegacyTabButton.iconOf(Items.WHITE_BANNER),Component.empty(),null, b->resetElements()).add(0,0,42, 42, 4, LegacyTabButton.iconOf(Items.FIREWORK_ROCKET),Component.empty(),null, b->resetElements()).add(0,0,42, 42, 4, LegacyTabButton.iconOf(Items.CYAN_DYE),Component.empty(),null, b->resetElements());
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    protected final ScrollableRenderer scrollableRenderer = new ScrollableRenderer();
    private final boolean[] warningSlots;
    protected final ContainerListener listener = new ContainerListener() {
        public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
            if (onlyCraftableRecipes && typeTabList.selectedTab == 0) {
                filteredRecipesByGroup = recipesByTab.get(craftingTabList.selectedTab).stream().map(l -> l.stream().filter(r -> RecipeMenu.canCraft(r.getOptionalIngredients(), inventory,abstractContainerMenu.getCarried())).toList()).filter(l -> !l.isEmpty()).toList();
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
        ingredientsGrid = new ArrayList<>(Collections.nCopies(gridDimension * gridDimension,Optional.empty()));
        container = new TransientCraftingContainer(abstractContainerMenu,gridDimension,gridDimension);
        warningSlots = new boolean[gridDimension * gridDimension];
        if (Minecraft.getInstance().level == null) return;
        //? if >=1.20.5
        CraftingInput input = container.asCraftInput();
        List<RecipeInfo<CraftingRecipe>> allRecipes = CommonRecipeManager.byType(RecipeType.CRAFTING).stream().map(h-> RecipeInfo.create(h./*? if >1.20.1 {*/id()/*?} else {*//*getId()*//*?}*/, h/*? if >1.20.1 {*/.value()/*?}*/,h/*? if >1.20.1 {*/.value()/*?}*/ instanceof ShapedRecipe rcp ? LegacyCraftingMenu.updateShapedIngredients(new ArrayList<>(ingredientsGrid), LegacyCraftingMenu.getRecipeOptionalIngredients(rcp), gridDimension, rcp.getWidth(), rcp.getHeight()) : h/*? if >1.20.1 {*/.value()/*?}*/ instanceof ShapelessRecipe r ? LegacyCraftingMenu.getRecipeOptionalIngredients(r) : Collections.emptyList(),h/*? if >1.20.1 {*/.value()/*?}*/.isSpecial() ? ItemStack.EMPTY : h/*? if >1.20.1 {*/.value()/*?}*/.assemble(/*? if <1.20.5 {*//*container*//*?} else {*/input/*?}*/,Minecraft.getInstance().level.registryAccess()))).filter(h->h.getOptionalIngredients().size() <= ingredientsGrid.size()).toList();
        for (LegacyCraftingTabListing listing : LegacyCraftingTabListing.map.values()) {
            List<List<RecipeInfo<CraftingRecipe>>> groups = new ArrayList<>();
            listing.craftings().values().forEach(l->{
                if (l.isEmpty()) return;
                List<RecipeInfo<CraftingRecipe>> group = new ArrayList<>();
                l.forEach(v->v.addRecipes(allRecipes,group::add));
                group.removeIf(i->i.isInvalid() || i.getOptionalIngredients().size() > ingredientsGrid.size());
                if (!group.isEmpty()) groups.add(group);
            });
            if (groups.isEmpty()) continue;

            recipesByTab.add(groups);

            craftingTabList.addTabButton(43,0,listing.icon(),listing.name(), t->resetElements());

        }
        if (LegacyOption.vanillaTabs.get()) allRecipes.stream().collect(Collectors.groupingBy(h->h.get().category(),()->new TreeMap<>(Comparator.comparingInt(Enum::ordinal)),Collectors.groupingBy(h->h.get()./*? <1.21.2 {*//*getGroup*//*?} else {*/group/*?}*/().isEmpty() ? h.getId().toString() : h.get()./*? <1.21.2 {*//*getGroup*//*?} else {*/group/*?}*/()))).forEach((category, m)->{
            if (m.isEmpty()) return;
            List<List<RecipeInfo<CraftingRecipe>>> groups = new ArrayList<>();
            m.values().forEach(l->{
                l.removeIf(i->i.isInvalid() || i.getOptionalIngredients().size() > ingredientsGrid.size());
                if (!l.isEmpty()) groups.add(l);
            });
            if (groups.isEmpty()) return;
            recipesByTab.add(groups);
            craftingTabList.addTabButton(43,0,LegacyTabButton.iconOf(VANILLA_CATEGORY_ICONS[category.ordinal()]), getTitle(), t->resetElements());
        });
        craftingTabList.resetSelectedTab();
        inited = true;
        addCraftingButtons();
        accessor.getStaticDefinitions().add(UIDefinition.createBeforeInit(a->accessor.putStaticElement("is2x2",is2x2)));
        ItemStack redStar = Items.FIREWORK_STAR.getDefaultInstance();
        //? if <1.20.5 {
        /*CompoundTag redStarTag = redStar.getOrCreateTag();
        CompoundTag explosionTag = new CompoundTag();
        redStarTag.put("Explosion",explosionTag);
        explosionTag.putIntArray("Colors", List.of(DyeColor.RED.getFireworkColor()));
        *///?} else
        redStar.set(DataComponents.FIREWORK_EXPLOSION,new FireworkExplosion(FireworkExplosion.Shape.SMALL_BALL, IntList.of(DyeColor.RED.getFireworkColor()),IntList.of(),false,false));

        bannerTabList.add(0,0,0,43,0,LegacyTabButton.iconOf(Items.WHITE_BANNER), Component.empty(),null, b-> resetElements());
        bannerTabList.add(0,0,0,43,0,LegacyTabButton.iconOf(Items.SHIELD), Component.empty(),null, b-> resetElements());
        fireworkTabList.add(0,0,0,43,0,LegacyTabButton.iconOf(Items.FIREWORK_STAR), Component.empty(),null, b-> resetElements());
        fireworkTabList.add(0,0,0,43,0,LegacyTabButton.iconOf(redStar), Component.empty(),null, b-> resetElements());
        fireworkTabList.add(0,0,0,43,0,LegacyTabButton.iconOf(Items.FIREWORK_ROCKET), Component.empty(),null, b-> resetElements());
        dyeTabList.add(0,0,0,43,0,LegacyTabButton.iconOf(Legacy4J.dyeItem(Items.LEATHER_CHESTPLATE.getDefaultInstance(),Legacy4J.getDyeColor(DyeColor.GREEN))), Component.empty(),null, b-> resetElements());
        dyeTabList.add(0,0,0,43,0, typeTabList.tabButtons.get(3).icon, Component.empty(),null, b-> resetElements());
        if (!is2x2) dyeTabList.add(0,0,0,43,0,LegacyTabButton.iconOf(Items.DECORATED_POT), Component.empty(),null, b-> resetElements());

        Consumer<CustomCraftingIconHolder> fireworkStarUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (fireworkStarButtons.isEmpty()) return;
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Optional.empty(),Optional.of(FactoryIngredient.of(fireworkStarButtons.get(0).itemIcon).toIngredient()),Optional.empty(), Optional.of(FireworkStarRecipeAccessor.getGunpowderIngredient())),gridDimension, 2,2);
            fireworkStarButtons.forEach(CustomRecipeIconHolder::applyAddedIngredients);
            resultStack = fireworkStarButtons.get(0).hasItem() ? h.assembleCraftingResult(minecraft.level,container) : Items.FIREWORK_STAR.getDefaultInstance();
            canCraft(ingredientsGrid,true);
        };
        Consumer<CustomCraftingIconHolder> fireworkStarFadeUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (fireworkStarFadeButtons.isEmpty()) return;
            ItemStack item = fireworkStarFadeButtons.get(0).itemIcon.isEmpty() ? Items.FIREWORK_STAR.getDefaultInstance() : fireworkStarFadeButtons.get(0).itemIcon.copyWithCount(1);
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Optional.empty(),Optional.of(FactoryIngredient.of(fireworkStarFadeButtons.get(1).itemIcon).toIngredient()),Optional.empty(), Optional.of(StackIngredient.of(true,item))),gridDimension, 2,2);
            fireworkStarFadeButtons.get(1).applyAddedIngredients();
            resultStack = fireworkStarFadeButtons.get(0).itemIcon.isEmpty() ? item : h.assembleCraftingResult(Minecraft.getInstance().level,container);
            canCraft(ingredientsGrid,true);
        };
        Consumer<CustomCraftingIconHolder> fireworkRocketUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (fireworkButtons.isEmpty()) return;
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Optional.empty(),Optional.of(FactoryIngredient.of(fireworkButtons.get(0).itemIcon).toIngredient()),Optional.empty(),Optional.of(FireworkRocketRecipeAccessor.getPaperIngredient())),gridDimension, 2,2);
            fireworkButtons.forEach(CustomRecipeIconHolder::applyAddedIngredients);
            resultStack = fireworkButtons.get(0).hasItem() ? h.assembleCraftingResult(Minecraft.getInstance().level,container) : new ItemStack(Items.FIREWORK_ROCKET,3);
            canCraft(ingredientsGrid,true);
        };
        Consumer<CustomCraftingIconHolder> dyeArmorUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (dyeArmorButtons.isEmpty()) return;
            ItemStack armor = dyeArmorButtons.get(0).itemIcon.isEmpty() ? Items.LEATHER_HELMET.getDefaultInstance() : dyeArmorButtons.get(0).itemIcon.copyWithCount(1);
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Optional.empty(),Optional.of(Ingredient.of(dyeArmorButtons.get(1).itemIcon.getItem())), Optional.empty(), Optional.of(StackIngredient.of(true,armor))),gridDimension, 2,2);
            dyeArmorButtons.forEach(CustomRecipeIconHolder::applyAddedIngredients);
            resultStack = dyeArmorButtons.get(0).itemIcon.isEmpty() || !dyeArmorButtons.get(1).hasItem() ? armor : h.assembleCraftingResult(Minecraft.getInstance().level,container);
            canCraft(ingredientsGrid,true);
        };
        Consumer<CustomCraftingIconHolder> dyeItemUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (dyeItemButtons.isEmpty()) return;
            ItemStack item = dyeItemButtons.get(0).itemIcon.isEmpty() ? Items.WHITE_BED.getDefaultInstance() : dyeItemButtons.get(0).itemIcon.copyWithCount(1);
            Optional<Ingredient> dyeIngredient = Optional.of(Ingredient.of(dyeItemButtons.get(1).itemIcon.getItem()));
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Optional.empty(),dyeIngredient, Optional.empty(), Optional.of(StackIngredient.of(true,item))),gridDimension, 2,2);
            resultStack = dyeItemButtons.get(0).itemIcon.isEmpty() ? item : h.assembleCraftingResult(Minecraft.getInstance().level,container);
            if (resultStack.isEmpty()) {
                resultStack = item;
                ingredientsGrid.set(ingredientsGrid.indexOf(dyeIngredient), Optional.of(new ImpossibleIngredient(dyeItemButtons.get(1).itemIcon)));
            }
            canCraft(ingredientsGrid,true);

        };
        List<ItemStack> dyes = Arrays.stream(DyeColor.values()).map(c-> DyeItem.byColor(c).getDefaultInstance()).toList();
        fireworkStarButtons.add(craftingButtonByList(LegacyComponents.COLOR_TAB, dyes,fireworkStarUpdateRecipe).enableAddIngredients());
        fireworkStarButtons.add(craftingButtonByList(LegacyComponents.SHAPE_TAB, FireworkStarRecipeAccessor.getShapeByItem().keySet().stream().map(ItemStack::new).toList(),fireworkStarUpdateRecipe).enableAddIngredients(h-> ingredientsGrid.stream().noneMatch(i -> i.isPresent() && Arrays.stream(FactoryIngredient.of(i.get()).getStacks()).anyMatch(item->FireworkStarRecipeAccessor.getShapeByItem().containsKey(item.getItem())))));
        fireworkStarButtons.add(craftingButtonByList(LegacyComponents.EFFECT_TAB, Stream.concat(Arrays.stream(FactoryIngredient.of(FireworkStarRecipeAccessor.getTwinkleIngredient()).getStacks()),Arrays.stream(FactoryIngredient.of(FireworkStarRecipeAccessor.getTrailIngredient()).getStacks())).toList(),fireworkStarUpdateRecipe).enableAddIngredients(h-> ingredientsGrid.stream().noneMatch(i-> i.isPresent() && i.get().test(h.itemIcon))));

        fireworkStarFadeButtons.add(craftingButtonByPredicate(LegacyComponents.SELECT_STAR_TAB, i-> i.getItem() instanceof FireworkStarItem,fireworkStarFadeUpdateRecipe));
        fireworkStarFadeButtons.add(craftingButtonByList(LegacyComponents.ADD_FADE_TAB, dyes,fireworkStarFadeUpdateRecipe).enableAddIngredients());

        fireworkButtons.add(craftingButtonByList(LegacyComponents.ADD_POWER_TAB,Arrays.stream(FactoryIngredient.of(FireworkRocketRecipeAccessor.getGunpowderIngredient()).getStacks()).toList(),fireworkRocketUpdateRecipe).enableAddIngredients(h-> ingredientsGrid.stream().filter(i->i.isPresent() && i.get().equals(FireworkRocketRecipeAccessor.getGunpowderIngredient())).count() < 3));
        fireworkButtons.add(craftingButtonByPredicate(LegacyComponents.SELECT_STAR_TAB, i-> i.getItem() instanceof FireworkStarItem && /*? if <1.20.5 {*//*i.hasTag() && i.getTag().contains("Explosion")*//*?} else {*/i.get(DataComponents.FIREWORK_EXPLOSION) != null/*?}*/,fireworkRocketUpdateRecipe).enableAddIngredients());

        dyeArmorButtons.add(craftingButtonByPredicate(Component.translatable("legacy.container.tab.armour"), i-> /*? if <1.20.5 {*//*i.getItem() instanceof DyeableLeatherItem*//*?} else {*/i.is(ItemTags.DYEABLE)/*?}*/,dyeArmorUpdateRecipe));
        dyeArmorButtons.add(craftingButtonByList(LegacyComponents.COLOR_TAB, dyes,dyeArmorUpdateRecipe).enableAddIngredients());
        dyeItemButtons.add(craftingButtonByPredicate(Component.translatable("entity.minecraft.item"),i-> i.getItem() instanceof BedItem || (i.getItem() instanceof BlockItem b &&  b.getBlock() instanceof ShulkerBoxBlock/*? if >=1.21.4 {*/ || i.getItem() instanceof BundleItem/*?}*/),dyeItemUpdateRecipe));
        dyeItemButtons.add(craftingButtonByList(LegacyComponents.COLOR_TAB, dyes,dyeItemUpdateRecipe));
        if (!is2x2) bannerButtons.add(craftingButtonByRecipes(LegacyComponents.CREATE_BANNER_TAB, Arrays.stream(DyeColor.values()).flatMap(c-> allRecipes.stream().filter(new RecipeInfo.Filter.ItemId(BuiltInRegistries.ITEM.getKey(BannerBlock.byColor(c).asItem())))).toList()));
        bannerButtons.add(craftingButtonByPredicate(LegacyComponents.COPY_BANNER, i-> i.getItem() instanceof BannerItem && Legacy4J.itemHasValidPatterns(i), h->{
            clearIngredients(ingredientsGrid);
            if (bannerButtons.isEmpty() || h.itemIcon.isEmpty()) return;
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Optional.empty(),Optional.empty(),Optional.of(StackIngredient.of(true,h.itemIcon.getItem().getDefaultInstance())), Optional.of(StackIngredient.of(true,h.itemIcon.copyWithCount(1)))),gridDimension, 2,2);
            resultStack = h.itemIcon.copyWithCount(1);
            canCraft(ingredientsGrid,true);
        }));
        Consumer<CustomCraftingIconHolder> decorateShieldUpdateRecipe = h-> {
            clearIngredients(ingredientsGrid);
            if (decorateShieldButtons.isEmpty()) return;

            ItemStack inputStack = decorateShieldButtons.get(0).itemIcon.isEmpty() ? Items.SHIELD.getDefaultInstance() : decorateShieldButtons.get(0).itemIcon.copyWithCount(1);

            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Optional.empty(),Optional.empty(),Optional.of(StackIngredient.of(true,inputStack.copy())),Optional.of(StackIngredient.of(true,decorateShieldButtons.get(1).itemIcon.isEmpty() ? Items.WHITE_BANNER.getDefaultInstance() : decorateShieldButtons.get(1).itemIcon.copyWithCount(1)))),gridDimension, 2,2);

            resultStack = decorateShieldButtons.get(0).itemIcon.isEmpty() ? inputStack : h.assembleCraftingResult(minecraft.level,container);
            canCraft(ingredientsGrid,true);
        };
        decorateShieldButtons.add(craftingButtonByPredicate(LegacyComponents.SELECT_SHIELD, i-> i.getItem() instanceof ShieldItem && Legacy4J.getItemPatternsCount(i) == 0, decorateShieldUpdateRecipe));
        decorateShieldButtons.add(craftingButtonByPredicate(LegacyComponents.SELECT_BANNER_TAB, i-> i.getItem() instanceof BannerItem, decorateShieldUpdateRecipe));

        decoratedPotButtons.add(craftingButtonByList(LegacyComponents.ADD_SHERD, DecoratedPotPatterns.ITEM_TO_POT_TEXTURE.keySet().stream().map(Item::getDefaultInstance).toList(), h->{
            clearIngredients(ingredientsGrid);
            if (is2x2) return;
            Function<Integer,Item> sherdByIndex = i-> h.addedIngredientsItems.size() > i ? h.addedIngredientsItems.get(i).getItem() : Items.BRICK;
            LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,List.of(Optional.empty(),Optional.of(Ingredient.of(sherdByIndex.apply(0))),Optional.empty(),Optional.of(Ingredient.of(sherdByIndex.apply(1))),Optional.empty(),Optional.of(Ingredient.of(sherdByIndex.apply(2))),Optional.empty(),Optional.of(Ingredient.of(sherdByIndex.apply(3)))),gridDimension, 3,3);
            resultStack = h.assembleCraftingResult(minecraft.level,container);
            canCraft(ingredientsGrid,true);
        }).enableAddIngredients(h->h.addedIngredientsItems.size() < 4));
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        ControlTooltip.setupDefaultButtons(renderer,this);
        Event.super.addControlTooltips(renderer);
        renderer.
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), ()-> typeTabList.selectedTab == 0 ? LegacyComponents.INFO : getFocused() instanceof CustomCraftingIconHolder h && h.addedIngredientsItems != null && !h.addedIngredientsItems.isEmpty() ? LegacyComponents.REMOVE : null).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), ()-> typeTabList.selectedTab == 0 ? onlyCraftableRecipes ? LegacyComponents.ALL_RECIPES : LegacyComponents.SHOW_CRAFTABLE_RECIPES : ControlTooltip.getKeyMessage(InputConstants.KEY_O,this)).
                add(()-> ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET)}) : COMPOUND_ICON_FUNCTION.apply(new Icon[]{ControllerBinding.LEFT_TRIGGER.bindingState.getIcon(),ControlTooltip.SPACE_ICON, ControllerBinding.RIGHT_TRIGGER.bindingState.getIcon()}),()->hasTypeTabList() ? LegacyComponents.TYPE : null).
                addCompound(()-> new Icon[]{ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.bindingState.getIcon(),ControlTooltip.SPACE_ICON, ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.bindingState.getIcon()},()->LegacyComponents.GROUP).
                add(()-> page.max > 0 && typeTabList.selectedTab == 0 ? ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.RIGHT_STICK.bindingState.getIcon() : null,()->LegacyComponents.PAGE);
    }


    public void resetElements(){
        listener.slotChanged(menu,-1,ItemStack.EMPTY);
        selectedCraftingButton = 0;
        infoType.set(0);
        craftingButtonsOffset.set(0);
        if (inited) repositionElements();
    }
    protected CustomCraftingIconHolder craftingButtonByRecipes(Component displayName, List<RecipeInfo<CraftingRecipe>> recipes){
        List<ItemStack> results = recipes.stream().map(RecipeInfo::getResultItem).toList();
        return new CustomCraftingIconHolder(results.get(0)){
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
                scrollableRenderer.scrolled.set(0);
                ingredientsGrid.clear();
                ingredientsGrid.addAll(recipes.get(results.indexOf(itemIcon)).getOptionalIngredients());
                resultStack = itemIcon.copyWithCount(1);
                LegacyCraftingScreen.this.canCraft(ingredientsGrid,true);
            }
            @Override
            protected boolean hasItem(ItemStack stack) {
                return LegacyCraftingScreen.this.canCraft(recipes.get(results.indexOf(stack)).getOptionalIngredients(),false);
            }
            @Override
            public void craft() {
                ScreenUtil.playSimpleUISound(SoundEvents.ITEM_PICKUP,1.0f);
                CommonNetwork.sendToServer(new ServerMenuCraftPayload(recipes.get(results.indexOf(itemIcon)), Screen.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.bindingState.pressed));
            }
        };
    }
    protected CustomCraftingIconHolder craftingButtonByList(Component displayName, List<ItemStack> itemStacks, Consumer<CustomCraftingIconHolder> updateRecipe){
        return new CustomCraftingIconHolder(itemStacks.get(0)){
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
                scrollableRenderer.scrolled.set(0);
                updateRecipe.accept(this);
            }
        };
    }
    protected CustomCraftingIconHolder craftingButtonByPredicate(Component displayName, Predicate<ItemStack> isValid, Consumer<CustomCraftingIconHolder> updateRecipe){
        return new CustomCraftingIconHolder(){
            public Component getDisplayName() {
                return displayName;
            }

            public ItemStack nextItem() {
                return nextItem(inventory,isValid);
            }
            public ItemStack previousItem() {
                return previousItem(inventory,isValid);
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
        Component title = getTabList() == craftingTabList ? getTabList().tabButtons.get(getTabList().selectedTab).getMessage() : getFocused() instanceof CustomCraftingIconHolder h ? h.getDisplayName() : Component.empty();
        guiGraphics.drawString(this.font, title,((typeTabList.selectedTab == 0 ? imageWidth : imageWidth / 2) - font.width(title)) / 2,17, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        int inventoryPanelX = accessor.getInteger("inventoryPanelX",176);
        int inventoryPanelWidth = accessor.getInteger("inventoryPanelWidth",163);
        if (infoType.get() <= 0) guiGraphics.drawString(this.font, this.playerInventoryTitle, inventoryPanelX + (inventoryPanelWidth - font.width(playerInventoryTitle)) / 2, accessor.getInteger("bottomPanelTitleY",114), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        else {
            if (selectedCraftingButton < getCraftingButtons().size() && getCraftingButtons().get(selectedCraftingButton) instanceof RecipeIconHolder<?> h) {
                if (infoType.get() == 1 && LegacyTipManager.hasTip(h.getFocusedResult())) {
                    List<FormattedCharSequence> l = font.split(LegacyTipManager.getTipComponent(h.getFocusedResult()), inventoryPanelWidth - 11);
                    scrollableRenderer.scrolled.max = Math.max(0,l.size()-getMaxTabCount());
                    guiGraphics.pose().translate(-leftPos,-topPos,0);
                    scrollableRenderer.render(guiGraphics,leftPos + inventoryPanelX + 5, topPos + 105,inventoryPanelWidth - 11,84,()->{
                        for (int i1 = 0; i1 < l.size(); i1++)
                            guiGraphics.drawString(font, l.get(i1), leftPos + inventoryPanelX + 5, topPos + 108 + i1 * 12, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                    });
                    guiGraphics.pose().translate(leftPos,topPos,0);

                } else if (infoType.get() == 2) {
                    guiGraphics.drawString(this.font, LegacyComponents.INGREDIENTS, inventoryPanelX + (inventoryPanelWidth - font.width(LegacyComponents.INGREDIENTS)) / 2, 108, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                    if (h.getFocusedRecipe() != null) {
                        compactItemStackList.clear();
                        RecipeMenu.handleCompactItemStackList(compactItemStackList, ()-> h.getFocusedRecipe().getOptionalIngredients().stream().map(RecipeIconHolder::getActualItem).iterator());
                        guiGraphics.pose().translate(-leftPos,-topPos,0);
                        scrollableRenderer.scrolled.max = Math.max(0,compactItemStackList.size()-4);
                        scrollableRenderer.render(guiGraphics,leftPos + inventoryPanelX + 2, topPos + 122,152,60,()->{
                            for (int i1 = 0; i1 < compactItemStackList.size(); i1++) {
                                ItemStack ing = compactItemStackList.get(i1);
                                ScreenUtil.iconHolderRenderer.itemHolder(leftPos + inventoryPanelX + 4, topPos + 124 + 15 * i1, 14, 14, ing, false, Vec3.ZERO).render(guiGraphics, i, j, 0);
                                guiGraphics.pose().pushPose();
                                guiGraphics.pose().translate(leftPos + inventoryPanelX + 22, topPos + 128 + 15 * i1, 0);
                                Legacy4JClient.applyFontOverrideIf(minecraft.getWindow().getHeight() <= 720, LegacyIconHolder.MOJANGLES_11_FONT, b-> {
                                    if (!b) guiGraphics.pose().scale(2 / 3f, 2 / 3f, 2 / 3f);
                                    guiGraphics.drawString(font, ing.getHoverName(), 0, 0, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                                });
                                guiGraphics.pose().popPose();
                            }});
                        guiGraphics.pose().translate(leftPos,topPos,0);
                    }
                }
            }
        }
        guiGraphics.pose().translate(-leftPos,-topPos,0);
        getCraftingButtons().forEach(b-> b.render(guiGraphics, i, j, 0));
        if (selectedCraftingButton < getCraftingButtons().size()) getCraftingButtons().get(selectedCraftingButton).renderSelection(guiGraphics, i, j, 0);
        guiGraphics.pose().translate(leftPos,topPos,0);
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state.pressed && state.canClick()){
            if ((state.is(ControllerBinding.LEFT_TRIGGER) || state.is(ControllerBinding.RIGHT_TRIGGER)) && hasTypeTabList()) typeTabList.controlTab(state.is(ControllerBinding.LEFT_TRIGGER),state.is(ControllerBinding.RIGHT_TRIGGER));
            if (state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis s) controlPage(s.x < 0 && -s.x > Math.abs(s.y),s.x > 0 && s.x > Math.abs(s.y));
        }
    }

    @Override
    protected void init() {
        resultStack = ItemStack.EMPTY;
        imageWidth = 348;
        imageHeight = 215;
        super.init();
        if (hasTypeTabList())
            leftPos+=21;
        topPos+=18;
        menu.addSlotListener(listener);
        menu.inventoryActive = infoType.get() <= 0;
        menu.inventoryOffset = accessor.getElementValue("inventoryOffset",LegacyCraftingMenu.DEFAULT_INVENTORY_OFFSET, Vec3.class);
        if (hasTypeTabList()) addWidget(typeTabList);
        if (selectedCraftingButton < getCraftingButtons().size()) setFocused(getCraftingButtons().get(selectedCraftingButton));
        if (typeTabList.selectedTab == 0 || !hasTypeTabList()) {
            craftingButtonsOffset.max = Math.max(0,recipesByTab.get(page.get() * getMaxTabCount() + craftingTabList.selectedTab).size() - 12);
            craftingButtons.forEach(b->{
                b.setPos(leftPos + accessor.getInteger("craftingButtonsX",13) + craftingButtons.indexOf(b) * 27,topPos + 38);
                addWidget(b);
            });
        }else {
            int size = getCraftingButtons().size();
            getCraftingButtons().forEach(b->{
                b.setPos(leftPos + (size == 1 ? 77 : size == 2 ? 52 : size == 3 ? 21 : 8) + getCraftingButtons().indexOf(b) * (size == 2 ? 62 : size == 3 ? 55 : 45),topPos + 39);
                if (size == 3) b.offset = new Vec3(0.5 + getCraftingButtons().indexOf(b) * 0.5,0,0);
                b.init();
                addWidget(b);
            });
        }
        addWidget(getTabList());
        getTabList().init(leftPos, topPos - 37, imageWidth, (t, i) -> {
            int index = getTabList().tabButtons.indexOf(t);
            t.type = index == 0 ? 0 : index >= (getMaxTabCount() - 1) ? 2 : 1;
            t.setWidth(51);
            t.offset = (t1) -> new Vec3(accessor.getDouble("tabListXOffset",-1.5) * getTabList().tabButtons.indexOf(t), t1.selected ? 0 : accessor.getDouble("tabListSelectedYOffset",4.5), 0);
        });
        if (hasTypeTabList()) typeTabList.init((b, i)->{
            b.setX(leftPos - b.getWidth() + 6);
            b.setY(topPos + i + 4);
            b.offset = (t1) -> new Vec3(t1.selected ? 0 : 3.5, 0.5, 0);
        },true);
    }

    public TabList getTabList(){
        return switch (typeTabList.selectedTab){
            case 1 -> bannerTabList;
            case 2 -> fireworkTabList;
            case 3 -> dyeTabList;
            default -> craftingTabList;
        };
    }

    public boolean hasTypeTabList(){
        return accessor.getBoolean("hasTypeTabList",true);
    }

    protected boolean canCraft(List<Optional<Ingredient>> ingredients, boolean isFocused) {
        compactItemStackList.clear();
        RecipeMenu.handleCompactInventoryList(compactItemStackList,inventory,menu.getCarried());
        return canCraft(compactItemStackList, isFocused ? ingredientsGrid : ingredients, isFocused ? warningSlots : null);
    }
    public static boolean canCraft(List<ItemStack> compactItemStackList, List<Optional<Ingredient>> ings, boolean[] warningSlots) {
        boolean canCraft = true;
        boolean isAllEmpty = true;
        main : for (int i1 = 0; i1 < ings.size(); i1++) {
            Optional<Ingredient> ing = ings.get(i1);
            if (ing.isEmpty()) continue;
            isAllEmpty = false;
            for (int c = 0; c < FactoryIngredient.of(ing.get()).getCount(); c++) {
                Optional<ItemStack> match = compactItemStackList.stream().filter(i-> !i.isEmpty() && ing.get().test(i.copyWithCount(1))).findFirst();
                if (match.isPresent()) {
                    match.get().shrink(1);
                    if (warningSlots != null) warningSlots[i1] = false;
                } else {
                    canCraft = false;
                    if (warningSlots == null) break main;
                    else warningSlots[i1] = true;
                }
            }
        }
        return canCraft && !isAllEmpty;
    }
    protected int getMaxTabCount(){
        return accessor.getInteger("maxTabCount",7);
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

                protected boolean canCraft(RecipeInfo<CraftingRecipe> rcp) {
                    if (rcp == null || onlyCraftableRecipes) return true;
                    return LegacyCraftingScreen.this.canCraft(rcp.getOptionalIngredients(),isFocused() && getFocusedRecipe() == rcp);
                }

                protected List<RecipeInfo<CraftingRecipe>> getRecipes() {
                    List<List<RecipeInfo<CraftingRecipe>>> list = onlyCraftableRecipes ? filteredRecipesByGroup : recipesByTab.get(page.get() * getMaxTabCount() + craftingTabList.selectedTab);
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
                    if (i == InputConstants.KEY_X && (typeTabList.selectedTab == 0 || hasTypeTabList())){
                        infoType.add(1,true);
                        menu.inventoryActive = infoType.get() <= 0;
                        ScreenUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(),true);
                        return true;
                    }
                    return super.keyPressed(i, j, k);
                }

                protected void updateRecipeDisplay(RecipeInfo<CraftingRecipe> rcp) {
                    scrollableRenderer.scrolled.set(0);
                    resultStack = getFocusedResult();
                    clearIngredients(ingredientsGrid);
                    if (rcp == null) return;
                    for (int i = 0; i < rcp.getOptionalIngredients().size(); i++)
                        ingredientsGrid.set(i, rcp.getOptionalIngredients().get(i));
                }

                @Override
                public void craft() {
                    ScreenUtil.playSimpleUISound(SoundEvents.ITEM_PICKUP,1.0f);
                    super.craft();
                }
            });
            h.offset = LegacyCraftingMenu.DEFAULT_INVENTORY_OFFSET;
        }
    }

    public static void clearIngredients(List<Optional<Ingredient>> ingredientsGrid){
        for (int i = 0; i < ingredientsGrid.size(); i++) {
            if (ingredientsGrid.get(i).isPresent()) ingredientsGrid.set(i, Optional.empty());
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
        public List<Optional<Ingredient>> getIngredientsGrid() {
            return ingredientsGrid;
        }
        @Override
        public void render(GuiGraphics graphics, int i, int j, float f) {
            if (isFocused()) selectedCraftingButton = getCraftingButtons().indexOf(this);
            super.render(graphics, i, j, f);
        }

        @Override
        public void craft() {
            ScreenUtil.playSimpleUISound(SoundEvents.ITEM_PICKUP,1.0f);
            super.craft();
        }
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        //? if >=1.21.2 {
        if (this.getChildAt(d, e).filter((guiEventListener) -> guiEventListener.mouseScrolled(d, e, f, g)).isPresent()) return true;
        //?}
        if (super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g)) return true;
        if (scrollableRenderer.mouseScrolled(g)) return true;
        int scroll = (int)Math.signum(g);
        if (((craftingButtonsOffset.get() > 0 && scroll < 0) || (scroll > 0 && craftingButtonsOffset.max > 0)) && craftingButtonsOffset.add(scroll,false) != 0){
            repositionElements();
            return true;
        }
        return false;
    }

    //? if >1.20.1 {
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
    //?} else {
    /*@Override
    public void renderBackground(GuiGraphics guiGraphics) {
    }
    *///?}

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        if (hasTypeTabList()) typeTabList.render(guiGraphics, i, j, f);
        getTabList().render(guiGraphics, i, j, f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(accessor.getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class), leftPos, topPos, imageWidth, imageHeight);
        int panelWidth = accessor.getInteger("craftingGridPanelWidth",163);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + 9, topPos + 103, panelWidth, 105);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + accessor.getInteger("inventoryPanelX",176), topPos + accessor.getInteger("inventoryPanelY",103), accessor.getInteger("inventoryPanelWidth",163), 105);
        if (typeTabList.selectedTab != 0 && hasTypeTabList()) FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + 176, topPos + 8, 163, 93);
        int contentsWidth = (is2x2 ? 2 : 3) * 23 + 69;
        int xDiff = leftPos + 9 + (panelWidth - contentsWidth) / 2;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(SMALL_ARROW, xDiff + (is2x2 ? 2 : 3) * 23 + 8, topPos + 161, 16, 13);
        if (typeTabList.selectedTab == 0 || !hasTypeTabList()) {
            if (craftingButtonsOffset.get() > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, leftPos + 5, topPos + 45);
            if (craftingButtonsOffset.max > 0 && craftingButtonsOffset.get() < craftingButtonsOffset.max)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + imageWidth - 11, topPos + 45);
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
        if (hasTypeTabList() && hasShiftDown() && typeTabList.controlTab(i)) return true;
        getTabList().controlTab(i);
        if (hasShiftDown() && controlPage(i == 263, i == 262)) return true;
        return super.keyPressed(i, j, k);
    }
    protected boolean controlPage(boolean left, boolean right){
        if ((left || right) && page.max > 0 && typeTabList.selectedTab == 0){
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
        return switch (typeTabList.selectedTab){
            case 1 -> bannerTabList.selectedTab == 0 ? bannerButtons : decorateShieldButtons;
            case 2 -> fireworkTabList.selectedTab == 0 ? fireworkStarButtons : fireworkTabList.selectedTab == 1 ? fireworkStarFadeButtons : fireworkButtons;
            case 3 -> dyeTabList.selectedTab == 0 ? dyeArmorButtons : dyeTabList.selectedTab == 1 ? dyeItemButtons : decoratedPotButtons;
            default -> craftingButtons;
        };
    }
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        //? if >1.20.1 {
        renderBackground(guiGraphics, i, j, f);
        //?}
        super.render(guiGraphics, i, j, f);
        int panelWidth = accessor.getInteger("craftingGridPanelWidth",163);
        int contentsWidth = (is2x2 ? 2 : 3) * 23 + 69;
        int xDiff = leftPos + 9 + (panelWidth - contentsWidth) / 2;
        boolean anyWarning = false;
        for (int index = 0; index < ingredientsGrid.size(); index++) {
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.itemHolder(xDiff + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23, 23, 23, getActualItem(ingredientsGrid.get(index)), ((!onlyCraftableRecipes || typeTabList.selectedTab != 0) && !getActualItem(ingredientsGrid.get(index)).isEmpty() && warningSlots[index]), new Vec3(0.5, is2x2 ? 0 : 0.5, 0));
            if (holder.isWarning()) anyWarning = true;
            holder.render(guiGraphics, i, j, f);
        }
        ScreenUtil.iconHolderRenderer.itemHolder(xDiff + contentsWidth - 36, topPos + 151, 36, 36, resultStack, anyWarning, new Vec3(0.5, 0, 0)).render(guiGraphics, i, j, f);
        if (!resultStack.isEmpty()) {
            Component resultName = getCraftingButtons().get(selectedCraftingButton) instanceof RecipeIconHolder<?> h ? h.getFocusedRecipe().getName() : resultStack.getHoverName();
            Component description = getCraftingButtons().get(selectedCraftingButton) instanceof RecipeIconHolder<?> h ? h.getFocusedRecipe().getDescription() : null;
            int titleY = accessor.getInteger("bottomPanelTitleY",114) - (description == null ? 0 : 6);
            ScreenUtil.renderScrollingString(guiGraphics, font, resultName, leftPos + 11 + Math.max(panelWidth - font.width(resultName), 0) / 2, topPos + titleY, leftPos + 7 + panelWidth, topPos + titleY + 11, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            if (description != null) ScreenUtil.renderScrollingString(guiGraphics, font, description.copy().setStyle(Style.EMPTY), leftPos + 11 + Math.max(panelWidth - font.width(description), 0) / 2, topPos + titleY + 12, leftPos + 7 + panelWidth, topPos + titleY + 23, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            if (typeTabList.selectedTab != 0){
                List<Component> list = ScreenUtil.getTooltip(resultStack);
                scrollableRenderer.scrolled.max = Math.max(0,list.size()-6);
                scrollableRenderer.render(guiGraphics,leftPos + 180, topPos + 15, 152, 72,()->{
                    for (int i1 = 0; i1 < list.size(); i1++) {
                        guiGraphics.drawString(font, list.get(i1).copy().setStyle(Style.EMPTY), leftPos + 180, topPos + 15 + i1 * 12, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                    }
                });
            }
            if (ScreenUtil.isMouseOver(i,j,xDiff + contentsWidth - 36, topPos + 151,36,36)) guiGraphics.renderTooltip(font, resultStack,i,j);
        }
        for (int index = 0; index < ingredientsGrid.size(); index++) if (!getActualItem(ingredientsGrid.get(index)).isEmpty() && ScreenUtil.isMouseOver(i,j,xDiff + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23,23,23)) guiGraphics.renderTooltip(font,getActualItem(ingredientsGrid.get(index)),i,j);
        getCraftingButtons().forEach(h -> h.renderTooltip(minecraft, guiGraphics, i, j));

        renderTooltip(guiGraphics, i, j);
    }
}
