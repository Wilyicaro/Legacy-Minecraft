package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jetbrains.annotations.Nullable;
import wily.legacy.LegacyMinecraft;
import wily.legacy.LegacyMinecraftPlatform;
import wily.legacy.client.LegacyCraftingTabListing;
import wily.legacy.client.Offset;
import wily.legacy.client.controller.ComponentState;
import wily.legacy.client.controller.ControllerComponent;
import wily.legacy.client.controller.ControllerEvent;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.network.ServerInventoryCraftPacket;
import wily.legacy.util.PagedList;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static wily.legacy.client.LegacySprites.SMALL_ARROW_SPRITE;
import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.RecipeIconHolder.getActualItem;

public class LegacyCraftingScreen extends AbstractContainerScreen<LegacyCraftingMenu> implements ControllerEvent {
    private final Inventory inventory;
    private final boolean is2x2;
    private final int gridDimension;
    private int lastFocused;
    private boolean onlyCraftableRecipes = false;
    protected final List<Ingredient> ingredientsGrid;
    protected ItemStack resultStack = ItemStack.EMPTY;
    public static final Component COLOR_TAB = Component.translatable("legacy.container.tab.color");
    protected final List<RecipeIconHolder<CraftingRecipe>> craftingButtons = new ArrayList<>();
    protected final List<CustomCraftingIconHolder> dyeItemButtons = new ArrayList<>();
    protected final List<CustomCraftingIconHolder> dyeArmorButtons = new ArrayList<>();
    protected final List<List<List<CraftingRecipe>>> recipesByTab = new ArrayList<>();
    protected List<List<CraftingRecipe>> filteredRecipesByGroup = Collections.emptyList();
    protected final Stocker.Sizeable page =  new Stocker.Sizeable(0);
    protected final Stocker.Sizeable craftingButtonsOffset =  new Stocker.Sizeable(0);
    protected final TabList craftingTabList = new TabList(new PagedList<>(page,7));
    protected final TabList dyeTabList = new TabList();
    protected final TabList groupTabList = new TabList().add(0,0,42, 42, 4, new ResourceLocation("crafting_table"),null,Component.empty(),null,b->repositionElements()).add(0,0,42, 42, 4, new ResourceLocation("cyan_dye"),null,Component.empty(),null,b->repositionElements());
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    private final boolean[] warningSlots;
    protected final ContainerListener listener = new ContainerListener() {
        public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
            if (onlyCraftableRecipes && groupTabList.selectedTab == 0) {
                filteredRecipesByGroup = recipesByTab.get(craftingTabList.selectedTab).stream().map(l -> l.stream().filter(r -> ServerInventoryCraftPacket.canCraft(r.getIngredients(), inventory)).toList()).filter(l -> !l.isEmpty()).toList();
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
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().tooltips.set(0,create(()->getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_RETURN,true) : ControllerComponent.DOWN_BUTTON.componentState.getIcon(true),()->getFocused() instanceof RecipeIconHolder<?> h && h.canCraft() || getFocused() instanceof CustomCraftingIconHolder c && c.canCraft() ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.create") : null));
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().add(()-> getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_O,true) : ControllerComponent.UP_BUTTON.componentState.getIcon(true), ()-> groupTabList.selectedTab == 0 ? CONTROL_ACTION_CACHE.getUnchecked(onlyCraftableRecipes ? "legacy.action.all_recipes" : "legacy.action.show_craftable_recipes") : null);
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().addCompound(()-> new Component[]{ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET,true) : ControllerComponent.LEFT_BUMPER.componentState.getIcon(true),ControlTooltip.SPACE,ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET,true) : ControllerComponent.RIGHT_BUMPER.componentState.getIcon(true)},()->CONTROL_ACTION_CACHE.getUnchecked("legacy.action.group"));
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().add(()-> ControlTooltip.getActiveType().isKeyboard() ? COMPOUND_COMPONENT_FUNCTION.apply(new Component[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT,true),ControlTooltip.PLUS,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT,true),ControlTooltip.SPACE,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT,true)}) : COMPOUND_COMPONENT_FUNCTION.apply(new Component[]{ControllerComponent.LEFT_TRIGGER.componentState.getIcon(true),ControlTooltip.SPACE,ControllerComponent.RIGHT_TRIGGER.componentState.getIcon(true)}),()->CONTROL_ACTION_CACHE.getUnchecked("legacy.action.type"));
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().add(()-> page.max > 0 && groupTabList.selectedTab == 0 ? ControlTooltip.getActiveType().isKeyboard() ? COMPOUND_COMPONENT_FUNCTION.apply(new Component[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT,true),ControlTooltip.PLUS,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT,true),ControlTooltip.SPACE,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT,true)}) : ControllerComponent.RIGHT_STICK.componentState.getIcon(true) : null,()->CONTROL_ACTION_CACHE.getUnchecked("legacy.action.page"));
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

            craftingTabList.addTabButton(43,0,listing.icon,listing.itemIconTag,listing.displayName, t->{
                listener.slotChanged(menu,-1,ItemStack.EMPTY);
                setFocused(null);
                craftingButtonsOffset.set(0);
                if (inited) repositionElements();
            });

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
            craftingTabList.addTabButton(43,0,VANILLA_CATEGORY_ICONS[category.ordinal()].arch$registryName(), getTitle(), t->{
                listener.slotChanged(menu,-1,ItemStack.EMPTY);
                setFocused(null);
                craftingButtonsOffset.set(0);
                if (inited) repositionElements();
            });
        });
        craftingTabList.resetSelectedTab();
        inited = true;
        addCraftingButtons();
        dyeTabList.add(0,0,0,43,0,new ResourceLocation("leather_chestplate"),DyeableLeatherItem.dyeArmor(Items.LEATHER_CHESTPLATE.getDefaultInstance(),List.of((DyeItem) Items.GREEN_DYE)).getTag(), Component.translatable("legacy.container.tab.armour"),null, b-> repositionElements());
        dyeTabList.add(0,0,0,43,0,groupTabList.tabButtons.get(1).icon,null, Component.translatable("entity.minecraft.item"),null, b-> repositionElements());
        Consumer<CustomCraftingIconHolder> dyeArmorUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (dyeArmorButtons.isEmpty()) return;
            ItemStack armor = dyeArmorButtons.get(0).itemIcon.isEmpty() ? Items.LEATHER_HELMET.getDefaultInstance() : dyeArmorButtons.get(0).itemIcon;
            updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(dyeArmorButtons.get(1).itemIcon),Ingredient.EMPTY, LegacyMinecraftPlatform.getStrictNBTIngredient(armor)),gridDimension,2,2,2);
            DyeItem dye = dyeItemButtons.get(1).itemIcon.getItem() instanceof DyeItem i ? i : (DyeItem) Items.WHITE_DYE;
            h.applyAddedIngredients();
            resultStack = dyeArmorButtons.get(0).itemIcon.isEmpty() || !dyeArmorButtons.get(1).hasItem ? armor : DyeableLeatherItem.dyeArmor(armor,h.addedIngredients != null ? Stream.concat(Stream.of(dye), h.addedIngredients.stream().map(ing-> (DyeItem)ing.getItems()[0].getItem())).toList() : List.of(dye));
            canCraft(ingredientsGrid,true);
        };
        Consumer<CustomCraftingIconHolder> dyeItemUpdateRecipe = h->{
            clearIngredients(ingredientsGrid);
            if (dyeItemButtons.isEmpty()) return;
            ItemStack item = dyeItemButtons.get(0).itemIcon.isEmpty() ? Items.WHITE_BED.getDefaultInstance() : dyeItemButtons.get(0).itemIcon;
            updateShapedIngredients(ingredientsGrid,List.of(Ingredient.EMPTY,Ingredient.of(dyeItemButtons.get(1).itemIcon),Ingredient.EMPTY, LegacyMinecraftPlatform.getStrictNBTIngredient(item)),gridDimension,2,2,2);
            DyeItem dye = dyeItemButtons.get(1).itemIcon.getItem() instanceof DyeItem i ? i : (DyeItem) Items.WHITE_DYE;
            ResourceLocation location = BuiltInRegistries.ITEM.getKey(item.getItem());
            String path = location.getPath();
            Item result = BuiltInRegistries.ITEM.get(location.withPath(path.contains(dye.getDyeColor().getName()) ? path : dye.getDyeColor().getName() + "_" + (Arrays.stream(DyeColor.values()).anyMatch(s-> path.contains(s.getName())) ? path.substring(path.indexOf("_") + 1) : path)));
            resultStack = dyeItemButtons.get(0).itemIcon.isEmpty() || result == Items.AIR ? item : result.getDefaultInstance();
            resultStack.setTag(item.getTag());
            canCraft(ingredientsGrid,true);
        };
        dyeArmorButtons.add(craftingButtonByPredicate(i-> i.getItem() instanceof DyeableLeatherItem,dyeArmorUpdateRecipe));
        dyeArmorButtons.add(craftingButtonByList(Arrays.stream(DyeColor.values()).map(c-> DyeItem.byColor(c).getDefaultInstance()).toList(),dyeArmorUpdateRecipe).enableAddIngredients());
        dyeItemButtons.add(craftingButtonByPredicate(i-> i.getItem() instanceof BedItem || (i.getItem() instanceof BlockItem b &&  b.getBlock() instanceof ShulkerBoxBlock),dyeItemUpdateRecipe));
        dyeItemButtons.add(craftingButtonByList(Arrays.stream(DyeColor.values()).map(c-> DyeItem.byColor(c).getDefaultInstance()).toList(),dyeItemUpdateRecipe));
    }
    protected CustomCraftingIconHolder craftingButtonByList(List<ItemStack> itemStacks, Consumer<CustomCraftingIconHolder> updateRecipe){
        return new CustomCraftingIconHolder(itemStacks.get(0)){
            ItemStack nextItem() {
                return itemStacks.get(Stocker.cyclic(0, itemStacks.indexOf(itemIcon) + 1, itemStacks.size()));
            }
            ItemStack previousItem() {
                return itemStacks.get(Stocker.cyclic(0, itemStacks.indexOf(itemIcon) - 1, itemStacks.size()));
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
    protected CustomCraftingIconHolder craftingButtonByPredicate(Predicate<ItemStack> isValid, Consumer<CustomCraftingIconHolder> updateRecipe){
        return new CustomCraftingIconHolder(){
            ItemStack nextItem() {
                for (int i = Math.max(0,inventory.items.indexOf(itemIcon)); i < inventory.items.size(); i++)
                    if (itemIcon != inventory.items.get(i) && isValid.test(inventory.items.get(i))) return inventory.items.get(i);
                for (int i = 0; i < Math.max(0,inventory.items.indexOf(itemIcon)); i++)
                    if (itemIcon != inventory.items.get(i) && isValid.test(inventory.items.get(i))) return inventory.items.get(i);
                return ItemStack.EMPTY;
            }
            ItemStack previousItem() {
                for (int i = Math.max(0,inventory.items.indexOf(itemIcon)); i >= 0; i--)
                    if (itemIcon != inventory.items.get(i) && isValid.test(inventory.items.get(i))) return inventory.items.get(i);
                for (int i = inventory.items.size() - 1; i >= Math.max(0,inventory.items.indexOf(itemIcon)); i--)
                    if (itemIcon != inventory.items.get(i) && isValid.test(inventory.items.get(i))) return inventory.items.get(i);
                return ItemStack.EMPTY;
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
        Component title = getTabList() == dyeTabList && selectedCraftingButton == 1 ? COLOR_TAB : getTabList().tabButtons.get(getTabList().selectedTab).getMessage();
        guiGraphics.drawString(this.font, title,((groupTabList.selectedTab == 0 ? imageWidth : imageWidth / 2) - font.width(title)) / 2,17, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, (355 + 160 - font.width(playerInventoryTitle))/ 2, 114, 4210752, false);
        guiGraphics.pose().translate(-leftPos,-topPos,0);
        getCraftingButtons().forEach(b-> b.render(guiGraphics,i,j,0));
        if (selectedCraftingButton < getCraftingButtons().size()) getCraftingButtons().get(selectedCraftingButton).renderSelection(guiGraphics, i, j, 0);
        guiGraphics.pose().translate(leftPos,topPos,0);
    }
    public void repositionElements() {
        lastFocused = getFocused() instanceof LegacyIconHolder h ? getCraftingButtons().indexOf(h) : -1;
        super.repositionElements();
    }
    @Override
    public void componentTick(ComponentState state) {
        if (state.pressed && state.canClick()){
            if (state.is(ControllerComponent.LEFT_TRIGGER) || state.is(ControllerComponent.RIGHT_TRIGGER)) groupTabList.controlTab(state.is(ControllerComponent.LEFT_TRIGGER),state.is(ControllerComponent.RIGHT_TRIGGER));
            if (state.is(ControllerComponent.RIGHT_STICK) && state instanceof ComponentState.Stick s) controlPage(s.x < 0 && -s.x > Math.abs(s.y),s.x > 0 && s.x > Math.abs(s.y));
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
        if (lastFocused >= 0 && lastFocused < getCraftingButtons().size()) setInitialFocus(getCraftingButtons().get(lastFocused));
        else if (!getCraftingButtons().isEmpty()) setInitialFocus(getCraftingButtons().get(0));
        if (groupTabList.selectedTab == 0) {
            craftingButtonsOffset.max = Math.max(0,recipesByTab.get(page.get() * 7 + craftingTabList.selectedTab).size() - 12);
            craftingButtons.forEach(b->{
                b.setPos(leftPos + 13 + craftingButtons.indexOf(b) * 27,topPos + 38);
                addWidget(b);
            });
        }else {
            getCraftingButtons().forEach(b->{
                b.setPos(leftPos + 51 + getCraftingButtons().indexOf(b) * 62,topPos + 38);
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
        if (groupTabList.selectedTab == 1) return dyeTabList;
        return craftingTabList;
    }
    protected boolean canCraft(List<Ingredient> ingredients, boolean isFocused) {
        List<Ingredient> ings = isFocused ? ingredientsGrid : ingredients;
        boolean canCraft = true;
        for (int i1 = 0; i1 < ings.size(); i1++) {
            Ingredient ing = ings.get(i1);
            if (ing.isEmpty()) continue;
            int itemCount = inventory.items.stream().filter(ing).mapToInt(ItemStack::getCount).sum();
            long ingCount = ings.stream().filter(i -> !i.isEmpty() && i.equals(ing)).count();
            if (itemCount >= ingCount || PagedList.occurrenceOf(ings, ing, i1) < itemCount) {
                if (isFocused && ingredientsGrid.contains(ing)) warningSlots[i1] = false;
            } else {
                canCraft = false;
                if (!isFocused || !ingredientsGrid.contains(ing)) break;
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
            h.offset = new Offset(0.5,0.5,0);
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
    protected abstract class CustomCraftingIconHolder extends LegacyIconHolder{
        abstract ItemStack nextItem();
        abstract ItemStack previousItem();
        abstract int findInventoryMatchSlot();
        abstract void updateRecipe();
        ItemStack nextItem = ItemStack.EMPTY;
        ItemStack previousItem = ItemStack.EMPTY;
        List<Ingredient> addedIngredients = null;
        boolean hasItem = false;
        final ItemStack defaultItem;
        public CustomCraftingIconHolder(ItemStack defaultItem){
            super(27,27);
            this.defaultItem = itemIcon = defaultItem;
        }
        public CustomCraftingIconHolder(){
            this(ItemStack.EMPTY);
        }
        public void init() {
            itemIcon = defaultItem;
        }

        public void applyAddedIngredients(){
            if (addedIngredients == null || addedIngredients.isEmpty()) return;
            int index = 0;
            for (int i1 = 0; i1 < ingredientsGrid.size(); i1++) {
                if (index >= addedIngredients.size()) break;
                Ingredient ing = ingredientsGrid.get(i1);
                if (!ing.isEmpty()) continue;
                ingredientsGrid.set(i1,addedIngredients.get(index));
                index++;
            }
        }
        public CustomCraftingIconHolder enableAddIngredients(){
            addedIngredients = new ArrayList<>();
            return this;
        }
        @Override
        public void setFocused(boolean bl) {
            if (bl){
                updateRecipe();
            }
            super.setFocused(bl);
        }
        public boolean canCraft(){
            return LegacyCraftingScreen.this.canCraft(ingredientsGrid,false);
        }

        @Override
        public void onPress() {
            if (isFocused()){
                if (canCraft()){
                    ScreenUtil.playSimpleUISound(LegacySoundEvents.CRAFT.get(),1.0f);
                    LegacyMinecraft.NETWORK.sendToServer(new ServerInventoryCraftPacket(ingredientsGrid, resultStack,-1, Screen.hasShiftDown() || ControllerComponent.LEFT_STICK_BUTTON.componentState.pressed));
                    updateRecipe();
                }else ScreenUtil.playSimpleUISound(LegacySoundEvents.CRAFT_FAIL.get(),1.0f);
            }
        }
        public boolean mouseScrolled(double d, double e, double f, double g) {
            int i = (int)Math.signum(g);
            if (isFocused() && !nextItem.isEmpty() && i > 0 || !previousItem.isEmpty() && i < 0 ){
                ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(), 1.0f);
                itemIcon = i > 0 ? nextItem : previousItem;
                updateRecipe();
                return true;
            }
            return false;
        }
        @Override
        public void render(GuiGraphics graphics, int i, int j, float f) {
            if (isFocused()) selectedCraftingButton = getCraftingButtons().indexOf(this);
            if (itemIcon.isEmpty() || isFocused()) {
                nextItem = nextItem();
                previousItem = previousItem();
            }
            if (itemIcon.isEmpty() && !nextItem.isEmpty()){
                itemIcon = nextItem;
                updateRecipe();
            }
            hasItem = hasItem(itemIcon);
            super.render(graphics, i, j, f);
        }
        protected boolean hasItem(ItemStack stack) {
            return inventory.items.stream().filter(s-> s.is(stack.getItem())).mapToInt(ItemStack::getCount).sum() >= stack.getCount();
        }
        @Override
        public void renderItem(GuiGraphics graphics, int i, int j, float f) {
             ScreenUtil.secureTranslucentRender(graphics,!itemIcon.isEmpty() && !hasItem(itemIcon),0.5f,()-> renderItem(graphics,itemIcon,getX(),getY(),false));
        }
        public boolean canAddIngredient(){
            return hasItem && addedIngredients != null && ingredientsGrid.contains(Ingredient.EMPTY);
        }
        @Override
        public boolean keyPressed(int i, int j, int k) {
            if (i == InputConstants.KEY_O && canAddIngredient()) {
                addedIngredients.add(Ingredient.of(itemIcon));
                updateRecipe();
                return true;
            }
            if (!nextItem.isEmpty() && i == 265 || !previousItem.isEmpty() && i == 264){
                ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(), 1.0f);
                itemIcon = i == 265 ? nextItem : previousItem;
                updateRecipe();
                return true;
            }
            return super.keyPressed(i, j, k);
        }

        @Override
        public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
            super.renderSelection(graphics, i, j, f);
            if (!itemIcon.isEmpty() && hasItem){
                Slot s = menu.getSlot(findInventoryMatchSlot());
                ScreenUtil.iconHolderRenderer.slotBounds(leftPos,topPos,s).renderHighlight(graphics);
            }
            if (!previousItem.isEmpty() || !nextItem.isEmpty()){
                scrollRenderer.renderScroll(graphics,ScreenDirection.UP,getX() + 5,getY() - 14);
                scrollRenderer.renderScroll(graphics,ScreenDirection.DOWN,getX() + 5,getY() + 31);
            }
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
        if (groupTabList.selectedTab != 0) ScreenUtil.renderSquareRecessedPanel(guiGraphics, leftPos + 176, topPos + 8, 162, 93, 2f);
        guiGraphics.blitSprite(SMALL_ARROW_SPRITE, leftPos + (is2x2 ? 87 : 97), topPos + 161, 16, 13);
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
        if (groupTabList.selectedTab == 1) return dyeTabList.selectedTab == 0 ? dyeArmorButtons : dyeItemButtons;
        return craftingButtons;
    }
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBackground(guiGraphics, i, j, f);
        super.render(guiGraphics, i, j, f);
        for (int index = 0; index < ingredientsGrid.size(); index++)
            ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (is2x2 ? 33 : 21) + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23, 23, 23, getActualItem(ingredientsGrid.get(index)), (!onlyCraftableRecipes || groupTabList.selectedTab != 0) && !getActualItem(ingredientsGrid.get(index)).isEmpty() &&  warningSlots[index], new Offset(0.5, is2x2 ? 0 : 0.5, 0)).render(guiGraphics, i, j, f);;
        ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (is2x2 ? 113 : 124), topPos + 151, 36, 36, resultStack, (!onlyCraftableRecipes || groupTabList.selectedTab != 0) && !canCraft(ingredientsGrid,false), new Offset(0.5, 0, 0)).render(guiGraphics, i, j, f);
        if (!resultStack.isEmpty()) {
            Component resultName = resultStack.getHoverName();
            ScreenUtil.renderScrollingString(guiGraphics, font, resultName, leftPos + 11 + Math.max(163 - font.width(resultName), 0) / 2, topPos + 114, leftPos + 170, topPos + 125, 4210752, false);
            if (groupTabList.selectedTab != 0){
                List<Component> list = resultStack.getTooltipLines(minecraft.player, TooltipFlag.NORMAL);
                for (int i1 = 0; i1 < list.size(); i1++) {
                    Component c = list.get(i1);
                    ScreenUtil.renderScrollingString(guiGraphics, font, c.copy().withStyle(ChatFormatting.DARK_GRAY), leftPos + 180, topPos + 15 + i1 * 13, leftPos + 342, topPos + 26 + i1 * 13, 4210752, false);
                }
            }
            if (ScreenUtil.isMouseOver(i,j,leftPos + (is2x2 ? 113 : 124), topPos + 151,36,36)) guiGraphics.renderTooltip(font, resultStack,i,j);
        }
        for (int index = 0; index < ingredientsGrid.size(); index++) if (!getActualItem(ingredientsGrid.get(index)).isEmpty() && ScreenUtil.isMouseOver(i,j,leftPos + (is2x2 ? 33 : 21) + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23,23,23)) guiGraphics.renderTooltip(font,getActualItem(ingredientsGrid.get(index)),i,j);
        getCraftingButtons().forEach(h -> h.renderTooltip(minecraft, guiGraphics, i, j));

        renderTooltip(guiGraphics, i, j);
    }
}
