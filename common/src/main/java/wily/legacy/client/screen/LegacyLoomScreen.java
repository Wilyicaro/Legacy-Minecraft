package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.jetbrains.annotations.Nullable;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JPlatform;
import wily.legacy.client.LoomTabListing;
import wily.legacy.client.Offset;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.network.ServerInventoryCraftPacket;
import wily.legacy.util.PagedList;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.*;
import java.util.stream.Collectors;

import static wily.legacy.util.LegacySprites.SMALL_ARROW;
import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.LegacyCraftingScreen.clearIngredients;
import static wily.legacy.client.screen.RecipeIconHolder.getActualItem;

public class LegacyLoomScreen extends AbstractContainerScreen<LegacyCraftingMenu> implements Controller.Event {
    private final Inventory inventory;
    protected final List<ItemStack> compactInventoryList = new ArrayList<>();
    protected final List<Ingredient> ingredientsGrid = new ArrayList<>(Collections.nCopies(9,Ingredient.EMPTY));
    protected final List<Ingredient> selectedIngredients = new ArrayList<>();
    protected ItemStack resultStack = ItemStack.EMPTY;
    protected ItemStack selectedStack = ItemStack.EMPTY;
    protected ItemStack previewStack = ItemStack.EMPTY;
    public static final Component SELECT_BANNER_TAB = Component.translatable("legacy.container.tab.select_banner");
    public static final Component PREVIEW = Component.translatable("legacy.container.preview");
    public static final Ingredient CREEPER_BANNER_PATTERN = Ingredient.of(Items.CREEPER_BANNER_PATTERN);
    public static final Ingredient FLOWER_BANNER_PATTERN = Ingredient.of(Items.FLOWER_BANNER_PATTERN);
    public static final Ingredient PIGLIN_BANNER_PATTERN = Ingredient.of(Items.PIGLIN_BANNER_PATTERN);
    public static final Ingredient GLOBE_BANNER_PATTERN = Ingredient.of(Items.GLOBE_BANNER_PATTERN);
    public static final Ingredient SKULL_BANNER_PATTERN = Ingredient.of(Items.SKULL_BANNER_PATTERN);
    public static final Ingredient MOJANG_BANNER_PATTERN = Ingredient.of(Items.MOJANG_BANNER_PATTERN);
    protected final List<RecipeIconHolder<BannerRecipe>> craftingButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> selectBannerButton = List.of(new CustomRecipeIconHolder(){

        public Component getDisplayName() {
            return SELECT_BANNER_TAB;
        }

        ItemStack nextItem() {
            return nextItem(inventory, LegacyLoomScreen::canItemAcceptPatterns);
        }
        ItemStack previousItem() {
            return previousItem(inventory, LegacyLoomScreen::canItemAcceptPatterns);
        }
        public boolean applyNextItemIfAbsent() {
            return true;
        }

        public boolean canCraft() {
            return false;
        }

        public List<Ingredient> getIngredientsGrid() {
            return ingredientsGrid;
        }
        public ItemStack getResultStack() {
            return resultStack;
        }
        int findInventoryMatchSlot() {
            for (int i = 0; i < menu.slots.size(); i++)
                if (menu.slots.get(i).getItem() == itemIcon) return i;
            itemIcon = nextItem;
            return itemIcon.isEmpty() ? 0 : findInventoryMatchSlot();
        }

        @Override
        public void render(GuiGraphics graphics, int i, int j, float f) {
            if (isFocused()) selectedCraftingButton = getCraftingButtons().indexOf(this);
            super.render(graphics, i, j, f);
        }

        @Override
        public boolean keyPressed(int i, int j, int k) {
            if (i == InputConstants.KEY_O && !itemIcon.isEmpty() && hasItem()) {
                updateRecipe();
                selectedStack = itemIcon.copyWithCount(1);
                craftingTabList.tabButtons.get(1).onPress();
                return true;
            }
            return super.keyPressed(i, j, k);
        }

        void updateRecipe() {
            clearIngredients(ingredientsGrid);
            resultStack = itemIcon.copyWithCount(1);
            ingredientsGrid.set(4, Legacy4JPlatform.getStrictNBTIngredient(resultStack));
        }

        @Override
        LegacyScrollRenderer getScrollRenderer() {
            return scrollRenderer;
        }
    });
    protected List<BannerRecipe> selectedPatterns = new ArrayList<>();
    protected List<List<BannerRecipe>> recipesByGroup = new ArrayList<>();
    protected final Stocker.Sizeable page =  new Stocker.Sizeable(0);
    protected final Stocker.Sizeable craftingButtonsOffset =  new Stocker.Sizeable(0);
    protected final TabList craftingTabList = new TabList(new PagedList<>(page,7));
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    private final boolean[] warningSlots = new boolean[9];
    protected final ContainerListener listener = new ContainerListener() {
        public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
            recipesByGroup.clear();
            selectedIngredients.clear();
            if (craftingTabList.selectedTab != 0 && !selectedStack.isEmpty()) {
                previewStack = selectedStack.copy();
                selectedIngredients.add(Legacy4JPlatform.getStrictNBTIngredient(selectedStack));
                if (!selectedPatterns.isEmpty()) {
                    CompoundTag beTag = previewStack.getOrCreateTagElement("BlockEntityTag");
                    ListTag patternsTag = beTag.getList("Patterns", 10);
                    if (!beTag.contains("Patterns", 9)) beTag.put("Patterns", patternsTag);
                    selectedPatterns.forEach(rcp -> {
                        CompoundTag patternTag = new CompoundTag();
                        patternsTag.add(patternTag);
                        patternTag.putString("Pattern", BuiltInRegistries.BANNER_PATTERN.get(rcp.pattern).getHashname());
                        patternTag.putInt("Color", rcp.color.getId());
                        for (int i1 = 1; i1 < rcp.ingredients.size(); i1++) {
                            Ingredient ing = rcp.ingredients.get(i1);
                            if (ing.isEmpty()) continue;
                            selectedIngredients.add(ing);
                        }
                    });
                }
                Ingredient previewIng = Legacy4JPlatform.getStrictNBTIngredient(previewStack);
                LoomTabListing.list.get(craftingTabList.selectedTab - 1).patterns.forEach(p -> {
                    BannerPattern pattern = BuiltInRegistries.BANNER_PATTERN.get(p);
                    recipesByGroup.add(Arrays.stream(DyeColor.values()).map(color -> {
                        ItemStack result = previewStack.copy();
                        CompoundTag beTag = result.getOrCreateTagElement("BlockEntityTag");
                        ListTag patternsTag = beTag.getList("Patterns", 10);
                        if (!beTag.contains("Patterns", 9)) beTag.put("Patterns", patternsTag);
                        CompoundTag addPattern = new CompoundTag();
                        patternsTag.add(addPattern);
                        addPattern.putString("Pattern", pattern.getHashname());
                        addPattern.putInt("Color", color.getId());
                        Ingredient dye = Ingredient.of(DyeItem.byColor(color));
                        List<Ingredient> previewIngs = new ArrayList<>(selectedIngredients);
                        previewIngs.add(dye);
                        List<Ingredient> displayIngs = new ArrayList<>(List.of(previewIng, dye));
                        Ingredient extraIng = getPatternExtraIngredient(p);
                        if (!extraIng.isEmpty()){
                            displayIngs.add(1,extraIng);
                            previewIngs.add(extraIng);
                        }
                        NonNullList<Ingredient> ings = NonNullList.create();
                        ings.addAll(displayIngs);
                        ings.set(0,selectedIngredients.get(0));
                        return new BannerRecipe(previewIngs,displayIngs,ings,result, p, color);
                    }).collect(Collectors.collectingAndThen(Collectors.toList(), l -> {
                        Collections.reverse(l);
                        return l;
                    })));
                });
                craftingButtons.get(selectedCraftingButton).updateRecipeDisplay();
            }else {
                if (getCraftingButtons().size() > selectedCraftingButton && getCraftingButtons().get(selectedCraftingButton) instanceof LegacyCraftingScreen.CustomCraftingIconHolder h) h.updateRecipe();
            }
        }

        public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int j) {

        }
    };
    protected int selectedCraftingButton;
    protected boolean inited = false;
    public LegacyLoomScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().tooltips.set(0,create(()->getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_RETURN,true) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(true),()->  getFocused() instanceof RecipeIconHolder<?> && canCraft() ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.create") : null));
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().add(()-> getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_O,true) : ControllerBinding.UP_BUTTON.bindingState.getIcon(true), ()->getFocused() instanceof RecipeIconHolder<?> r && r.canCraft() || getFocused() instanceof CustomRecipeIconHolder h && h.hasItem() ? CONTROL_ACTION_CACHE.getUnchecked(getFocused() instanceof CustomRecipeIconHolder ? "mco.template.button.select" : "legacy.action.add") : null);
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().add(()-> getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_X,true) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(true), ()-> getFocused() instanceof RecipeIconHolder<?> r && r.getFocusedRecipe() != null && selectedPatterns.contains(r.getFocusedRecipe()) ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.remove") : null);
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().addCompound(()-> new Component[]{ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET,true) : ControllerBinding.LEFT_BUMPER.bindingState.getIcon(true),ControlTooltip.SPACE,ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET,true) : ControllerBinding.RIGHT_BUMPER.bindingState.getIcon(true)},()->CONTROL_ACTION_CACHE.getUnchecked("legacy.action.group"));
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().add(()-> page.max > 0 ? ControlTooltip.getActiveType().isKeyboard() ? COMPOUND_COMPONENT_FUNCTION.apply(new Component[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT,true),ControlTooltip.PLUS,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT,true),ControlTooltip.SPACE,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT,true)}) : ControllerBinding.RIGHT_STICK.bindingState.getIcon(true) : null,()->CONTROL_ACTION_CACHE.getUnchecked("legacy.action.page"));
        this.inventory = inventory;
        craftingTabList.addTabButton(43,0,new ResourceLocation("white_banner"),null,Component.empty(),t-> resetElements());
        for (LoomTabListing listing : LoomTabListing.list) {
            if (!listing.isValid()) continue;

            craftingTabList.addTabButton(43,0,listing.icon,listing.itemIconTag,listing.displayName, t->resetElements());
        }
        craftingTabList.resetSelectedTab();
        inited = true;
        addCraftingButtons();
    }
    public void resetElements(){
        listener.slotChanged(menu,-1,ItemStack.EMPTY);
        selectedCraftingButton = 0;
        craftingButtonsOffset.set(0);
        if (inited) repositionElements();
    }
    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        if (guiEventListener instanceof TabList) return;
        super.setFocused(guiEventListener);
    }
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        Component title = craftingTabList.selectedTab != 0 ? craftingTabList.tabButtons.get(craftingTabList.selectedTab).getMessage() : getFocused() instanceof CustomRecipeIconHolder h ? h.getDisplayName() : Component.empty();
        guiGraphics.drawString(this.font, title,((craftingTabList.selectedTab != 0 ? imageWidth : imageWidth / 2) - font.width(title)) / 2,17, 0x383838, false);
        if (menu.inventoryActive) guiGraphics.drawString(this.font, this.playerInventoryTitle, (355 + 160 - font.width(playerInventoryTitle))/ 2, 114, 0x383838, false);
        else guiGraphics.drawString(this.font, PREVIEW, (355 + 160 - font.width(PREVIEW))/ 2, 114, 0x383838, false);
        guiGraphics.pose().translate(-leftPos,-topPos,0);
        getCraftingButtons().forEach(b-> b.render(guiGraphics,i,j,0));
        if (selectedCraftingButton < getCraftingButtons().size()) getCraftingButtons().get(selectedCraftingButton).renderSelection(guiGraphics, i, j, 0);
        guiGraphics.pose().translate(leftPos,topPos,0);
    }
    @Override
    public void bindingStateTick(BindingState state) {
        if (state.pressed && state.canClick() && state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis s) controlPage(s.x < 0 && -s.x > Math.abs(s.y),s.x > 0 && s.x > Math.abs(s.y));
    }
    public static Ingredient getPatternExtraIngredient(ResourceKey<BannerPattern> pattern){
        Holder<BannerPattern> holder = BuiltInRegistries.BANNER_PATTERN.getHolderOrThrow(pattern);
        if (holder.is(BannerPatternTags.PATTERN_ITEM_CREEPER)) return CREEPER_BANNER_PATTERN;
        else if (holder.is(BannerPatternTags.PATTERN_ITEM_FLOWER)) return FLOWER_BANNER_PATTERN;
        else if (holder.is(BannerPatternTags.PATTERN_ITEM_SKULL)) return SKULL_BANNER_PATTERN;
        else if (holder.is(BannerPatternTags.PATTERN_ITEM_GLOBE)) return GLOBE_BANNER_PATTERN;
        else if (holder.is(BannerPatternTags.PATTERN_ITEM_PIGLIN)) return PIGLIN_BANNER_PATTERN;
        else if (holder.is(BannerPatternTags.PATTERN_ITEM_MOJANG)) return MOJANG_BANNER_PATTERN;
        return Ingredient.EMPTY;
    }
    @Override
    protected void init() {
        resultStack = ItemStack.EMPTY;
        imageWidth = 348;
        imageHeight = 215;
        super.init();
        topPos+=18;
        menu.addSlotListener(listener);
        craftingTabList.selectedTab = selectedStack.isEmpty() ? 0 : Math.max(craftingTabList.selectedTab,1);
        menu.inventoryActive = selectedStack.isEmpty();
        if (selectedCraftingButton < getCraftingButtons().size()) setFocused(getCraftingButtons().get(selectedCraftingButton));
        if (craftingTabList.selectedTab != 0) {
            craftingButtonsOffset.max = Math.max(0,LoomTabListing.list.get(page.get() * 7 + craftingTabList.selectedTab - 1).patterns.size() - 12);
            craftingButtons.forEach(b->{
                b.setPos(leftPos + 13 + craftingButtons.indexOf(b) * 27,topPos + 38);
                addWidget(b);
            });
        }else {
            getCraftingButtons().forEach(b->{
                b.setPos(leftPos + 77 + getCraftingButtons().indexOf(b) * 55,topPos + 39);
                b.offset = new Offset(getCraftingButtons().indexOf(b) * 0.5,0,0);
                b.init();
                addWidget(b);
            });
        }

        addWidget(craftingTabList);
        craftingTabList.init(leftPos, topPos - 37, imageWidth, (t, i) -> {
            int index = craftingTabList.tabButtons.indexOf(t);
            t.active = index == 0 && selectedStack.isEmpty() || index != 0 && !selectedStack.isEmpty();
            t.type = index == 0 ? 0 : index >= 6 ? 2 : 1;
            t.setWidth(51);
            t.offset = (t1) -> new Offset(-1.5 * craftingTabList.tabButtons.indexOf(t), t1.active ? t1.selected ? 0 : 4.5: 26.5, 0);
        });
    }
    protected boolean canCraft(){
        return selectedIngredients.size() > 1 && LegacyLoomScreen.this.canCraft(selectedIngredients,false);
    }

    protected boolean canCraft(List<Ingredient> ingredients, boolean isFocused) {
        boolean canCraft = true;
        compactInventoryList.clear();
        ServerInventoryCraftPacket.handleCompactInventoryList(compactInventoryList,minecraft.player.getInventory(),menu.getCarried());
        for (int i1 = 0; i1 < ingredients.size(); i1++) {
            Ingredient ing = ingredients.get(i1);
            if (ing.isEmpty()) continue;
            Optional<ItemStack> match = compactInventoryList.stream().filter(i-> !i.isEmpty() && ing.test(i.copyWithCount(1))).findFirst();
            if (match.isPresent()) {
                match.get().shrink(1);
                if (isFocused) warningSlots[i1] = false;
            } else {
                canCraft = false;
                if (!isFocused) break;
                else warningSlots[i1] = true;
            }
        }
        return canCraft;
    }
    public static boolean canItemAcceptPatterns(ItemStack stack){
        CompoundTag beTag = stack.getTagElement("BlockEntityTag");
        return stack.getItem() instanceof BannerItem && (beTag == null || !beTag.contains("Patterns") || beTag.getList("Patterns",10).size() < 6);
    }
    protected void addCraftingButtons(){
        for (int i = 0; i < 12; i++) {
            int index = i;

            RecipeIconHolder<BannerRecipe> h;
            craftingButtons.add(h = new RecipeIconHolder<>(leftPos + 13 + i * 27, topPos + 38) {
                @Override
                public void render(GuiGraphics graphics, int i, int j, float f) {
                    if (isFocused()) selectedCraftingButton = index;
                    super.render(graphics, i, j, f);
                }

                protected boolean canCraft(BannerRecipe rcp) {
                    if (rcp == null) return true;
                    return canItemAcceptPatterns(previewStack) && LegacyLoomScreen.this.canCraft(rcp.getIngredients(),isFocused() && getFocusedRecipe() == rcp) && LegacyLoomScreen.this.canCraft(rcp.previewIngredients,false);
                }

                protected List<BannerRecipe> getRecipes() {
                    return recipesByGroup.size() <= craftingButtonsOffset.get() + index ? Collections.emptyList() : recipesByGroup.get(craftingButtonsOffset.get() + index);
                }

                @Override
                protected void toggleCraftableRecipes() {
                }

                @Override
                public boolean keyPressed(int i, int j, int k) {
                    if ((i == InputConstants.KEY_O || i == InputConstants.KEY_X) && isValidIndex()){
                        if (i == InputConstants.KEY_O){
                            if (this.canCraft()){
                                selectedPatterns.add(getFocusedRecipe());
                                ScreenUtil.playSimpleUISound(SoundEvents.UI_LOOM_SELECT_PATTERN,1.0f);
                            }
                        } else selectedPatterns.remove(getFocusedRecipe());
                        int cycle = getFocusedRecipes().indexOf(getFocusedRecipe()) - getRecipes().indexOf(getFocusedRecipe());
                        focusedRecipes = null;
                        listener.slotChanged(menu,-1,ItemStack.EMPTY);
                        Collections.rotate(getFocusedRecipes(),cycle);
                        updateRecipeDisplay();
                        return true;
                    }
                    if (controlCyclicNavigation(i, index, craftingButtons, craftingButtonsOffset, scrollRenderer, LegacyLoomScreen.this))
                        return true;
                    return super.keyPressed(i, j, k);
                }

                protected void updateRecipeDisplay(BannerRecipe rcp) {
                    resultStack = getFocusedResult();
                    clearIngredients(ingredientsGrid);
                    if (rcp == null){
                        return;
                    }
                    for (int i1 = 0; i1 < rcp.displayIngredients.size(); i1++)
                        ingredientsGrid.set(i1,rcp.displayIngredients.get(i1));
                }
                public void onPress(){
                    if (isFocused() && isValidIndex()){
                        if (LegacyLoomScreen.this.canCraft()){
                            ScreenUtil.playSimpleUISound(SoundEvents.UI_LOOM_TAKE_RESULT,1.0f);
                            Legacy4J.NETWORK.sendToServer(new ServerInventoryCraftPacket(selectedIngredients,previewStack,-1, Screen.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.bindingState.pressed));
                            selectedPatterns.clear();
                            selectedStack = ItemStack.EMPTY;
                            previewStack = ItemStack.EMPTY;
                            craftingTabList.tabButtons.get(0).onPress();
                        } else ScreenUtil.playSimpleUISound(LegacySoundEvents.CRAFT_FAIL.get(),1.0f);
                    }
                }

            });
            h.offset = new Offset(0.5,0.5,0);
        }
    }
    record BannerRecipe(List<Ingredient> previewIngredients,List<Ingredient> displayIngredients, NonNullList<Ingredient> ingredients, ItemStack resultStack, ResourceKey<BannerPattern> pattern, DyeColor color) implements Recipe<CraftingContainer>{

        @Override
        public boolean matches(CraftingContainer container, Level level) {
            return true;
        }
        @Override
        public NonNullList<Ingredient> getIngredients() {
            return ingredients;
        }
        @Override
        public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
            return resultStack;
        }
        @Override
        public boolean canCraftInDimensions(int i, int j) {
            return true;
        }
        @Override
        public ItemStack getResultItem(RegistryAccess registryAccess) {
            return resultStack;
        }
        @Override
        public RecipeSerializer<?> getSerializer() {
            return null;
        }
        @Override
        public RecipeType<?> getType() {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof BannerRecipe rcp && rcp.color == color && rcp.pattern == pattern;
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        craftingTabList.render(guiGraphics, i, j, f);
        ScreenUtil.renderPanel(guiGraphics, leftPos, topPos, imageWidth, imageHeight, 2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics, leftPos + 9, topPos + 103, 163, 105, 2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics, leftPos + 176, topPos + 103, 163, 105, 2f);
        if (!menu.inventoryActive && !previewStack.isEmpty()){
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(leftPos + 220.5, topPos + 130.5,0);
            guiGraphics.pose().scale(4.25f,4.25f,4.25f);
            guiGraphics.renderItem(previewStack,0,0);
            guiGraphics.pose().popPose();
        }
        if (craftingTabList.selectedTab == 0) ScreenUtil.renderSquareRecessedPanel(guiGraphics, leftPos + 176, topPos + 8, 163, 93, 2f);
        guiGraphics.blitSprite(SMALL_ARROW, leftPos + 97, topPos + 161, 16, 13);
        if (craftingTabList.selectedTab != 0) {
            if (craftingButtonsOffset.get() > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, leftPos + 5, topPos + 45);
            if (craftingButtonsOffset.max > 0 && craftingButtonsOffset.get() < craftingButtonsOffset.max)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + 337, topPos + 45);
        }
    }


    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (hasShiftDown() && controlPage(i == 263, i == 262)) return true;
        if (craftingTabList.controlTab(i)) return true;
        return super.keyPressed(i, j, k);
    }
    protected boolean controlPage(boolean left, boolean right){
        if ((left || right) && page.max > 0 && craftingTabList.selectedTab != 0){
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
        return craftingTabList.selectedTab == 0 ? selectBannerButton : craftingButtons;
    }
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBackground(guiGraphics, i, j, f);
        super.render(guiGraphics, i, j, f);
        for (int index = 0; index < ingredientsGrid.size(); index++)
            ScreenUtil.iconHolderRenderer.itemHolder(leftPos + 21 + index % 3 * 23, topPos +  133 + index / 3 * 23, 23, 23, getActualItem(ingredientsGrid.get(index)), !getActualItem(ingredientsGrid.get(index)).isEmpty() &&  warningSlots[index], new Offset(0.5, 0.5, 0)).render(guiGraphics, i, j, f);;
        ScreenUtil.iconHolderRenderer.itemHolder(leftPos +  124, topPos + 151, 36, 36, resultStack, getFocused() instanceof RecipeIconHolder<?> r && r.isValidIndex() && !r.canCraft(), new Offset(0.5, 0, 0)).render(guiGraphics, i, j, f);
        if (!resultStack.isEmpty()) {
            Component resultName = resultStack.getHoverName();
            ScreenUtil.renderScrollingString(guiGraphics, font, resultName, leftPos + 11 + Math.max(163 - font.width(resultName), 0) / 2, topPos + 114, leftPos + 170, topPos + 125, 0x383838, false);
            if (craftingTabList.selectedTab == 0){
                List<Component> list = resultStack.getTooltipLines(minecraft.player, TooltipFlag.NORMAL);
                for (int i1 = 0; i1 < list.size(); i1++) {
                    if (26 + i1 * 13 >= 93) break;
                    Component c = list.get(i1);
                    ScreenUtil.renderScrollingString(guiGraphics, font, c.copy().withColor(0x383838), leftPos + 180, topPos + 15 + i1 * 13, leftPos + 335, topPos + 26 + i1 * 13, 0x383838, false);
                }
            }
            if (ScreenUtil.isMouseOver(i,j,leftPos + 124, topPos + 151,36,36)) guiGraphics.renderTooltip(font, resultStack,i,j);
        }
        for (int index = 0; index < ingredientsGrid.size(); index++) if (!getActualItem(ingredientsGrid.get(index)).isEmpty() && ScreenUtil.isMouseOver(i,j,leftPos +  21 + index % 3 * 23, topPos + 133 + index / 3 * 23,23,23)) guiGraphics.renderTooltip(font,getActualItem(ingredientsGrid.get(index)),i,j);
        getCraftingButtons().forEach(h -> h.renderTooltip(minecraft, guiGraphics, i, j));

        renderTooltip(guiGraphics, i, j);
    }
}
