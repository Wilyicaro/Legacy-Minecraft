package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.jetbrains.annotations.Nullable;
import wily.legacy.Legacy4JPlatform;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.client.LoomTabListing;
import wily.legacy.network.CommonNetwork;
import wily.legacy.util.*;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.inventory.RecipeMenu;
import wily.legacy.network.ServerMenuCraftPacket;

import java.util.*;
import java.util.stream.Collectors;

import static wily.legacy.util.LegacySprites.SMALL_ARROW;
import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.LegacyCraftingScreen.clearIngredients;
import static wily.legacy.client.screen.RecipeIconHolder.getActualItem;

public class LegacyLoomScreen extends AbstractContainerScreen<LegacyCraftingMenu> implements Controller.Event,ControlTooltip.Event {
    private final Inventory inventory;
    protected final List<ItemStack> compactInventoryList = new ArrayList<>();
    protected final List<Ingredient> ingredientsGrid = new ArrayList<>(Collections.nCopies(9,Ingredient.EMPTY));
    protected final List<Ingredient> selectedIngredients = new ArrayList<>();
    protected ItemStack resultStack = ItemStack.EMPTY;
    protected ItemStack selectedStack = ItemStack.EMPTY;
    protected ItemStack previewStack = ItemStack.EMPTY;
    public static final Component SELECT_BANNER_TAB = Component.translatable("legacy.container.tab.select_banner");
    public static final Component PREVIEW = Component.translatable("legacy.container.preview");
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
                        Ingredient extraIng = LegacyCraftingMenu.getBannerPatternExtraIngredient(p);
                        if (!extraIng.isEmpty()){
                            displayIngs.add(1,extraIng);
                            previewIngs.add(extraIng);
                        }
                        NonNullList<Ingredient> ings = NonNullList.create();
                        ings.addAll(displayIngs);
                        ings.set(0,selectedIngredients.get(0));
                        return new BannerRecipe(p.location(),previewIngs,displayIngs,ings,result, p, color);
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
        this.inventory = inventory;
        craftingTabList.addTabButton(43,0,LegacyTabButton.iconOf(Items.WHITE_BANNER),Component.empty(),t-> resetElements());
        for (LoomTabListing listing : LoomTabListing.list) {
            if (!listing.isValid()) continue;

            craftingTabList.addTabButton(43,0, listing.icon,listing.displayName, t->resetElements());
        }
        craftingTabList.resetSelectedTab();
        inited = true;
        addCraftingButtons();
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        Event.super.addControlTooltips(renderer);
        renderer.
                set(0,create(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(),()->  getFocused() instanceof RecipeIconHolder<?> && canCraft() ? getAction("legacy.action.create") : null)).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), ()->getFocused() instanceof RecipeIconHolder<?> r && r.canCraft() || getFocused() instanceof CustomRecipeIconHolder h && h.hasItem() ? getAction(getFocused() instanceof CustomRecipeIconHolder ? "mco.template.button.select" : "legacy.action.add") : null).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), ()-> getFocused() instanceof RecipeIconHolder<?> r && r.getFocusedRecipe() != null && selectedPatterns.contains(r.getFocusedRecipe()) ? getAction("legacy.action.remove") : null).
                addCompound(()-> new ControlTooltip.Icon[]{ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.bindingState.getIcon(),ControlTooltip.SPACE_ICON, ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.bindingState.getIcon()},()->getAction("legacy.action.group")).
                add(()-> page.max > 0 ? ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new ControlTooltip.Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.RIGHT_STICK.bindingState.getIcon() : null,()->getAction("legacy.action.page"));
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
        guiGraphics.drawString(this.font, title,((craftingTabList.selectedTab != 0 ? imageWidth : imageWidth / 2) - font.width(title)) / 2,17, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        if (menu.inventoryActive) guiGraphics.drawString(this.font, this.playerInventoryTitle, (355 + 160 - font.width(playerInventoryTitle))/ 2, 114, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        else guiGraphics.drawString(this.font, PREVIEW, (355 + 160 - font.width(PREVIEW))/ 2, 114, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        guiGraphics.pose().translate(-leftPos,-topPos,0);
        getCraftingButtons().forEach(b-> b.render(guiGraphics,i,j,0));
        if (selectedCraftingButton < getCraftingButtons().size()) getCraftingButtons().get(selectedCraftingButton).renderSelection(guiGraphics, i, j, 0);
        guiGraphics.pose().translate(leftPos,topPos,0);
    }
    @Override
    public void bindingStateTick(BindingState state) {
        if (state.pressed && state.canClick() && state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis s) controlPage(s.x < 0 && -s.x > Math.abs(s.y),s.x > 0 && s.x > Math.abs(s.y));
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
        compactInventoryList.clear();
        RecipeMenu.handleCompactInventoryList(compactInventoryList,Minecraft.getInstance().player.getInventory(),menu.getCarried());
        return LegacyCraftingScreen.canCraft(compactInventoryList, ingredients, isFocused ? warningSlots : null);
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
                            selectedPatterns.forEach(b-> CommonNetwork.sendToServer(new ServerMenuCraftPacket(b.pattern.location(),b.displayIngredients,-1, Screen.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.bindingState.pressed)));
                            selectedPatterns.clear();
                            selectedStack = ItemStack.EMPTY;
                            previewStack = ItemStack.EMPTY;
                            craftingTabList.tabButtons.get(0).onPress();
                        } else ScreenUtil.playSimpleUISound(LegacyRegistries.CRAFT_FAIL.get(),1.0f);
                    }
                }

            });
            h.offset = new Offset(0.5,0.5,0);
        }
    }
    record BannerRecipe(ResourceLocation getId, List<Ingredient> previewIngredients, List<Ingredient> displayIngredients, NonNullList<Ingredient> ingredients, ItemStack resultStack, ResourceKey<BannerPattern> pattern, DyeColor color) implements Recipe<CraftingContainer>{

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
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        craftingTabList.render(guiGraphics, i, j, f);
        LegacyGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMALL_PANEL, leftPos, topPos, imageWidth, imageHeight);
        LegacyGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 9, topPos + 103, 163, 105);
        LegacyGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + 176, topPos + 103, 163, 105);
        if (!menu.inventoryActive && !previewStack.isEmpty()){
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(leftPos + 220.5, topPos + 130.5,0);
            guiGraphics.pose().scale(4.25f,4.25f,4.25f);
            guiGraphics.renderItem(previewStack,0,0);
            guiGraphics.pose().popPose();
        }
        if (craftingTabList.selectedTab == 0) LegacyGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + 176, topPos + 8, 163, 93);
        LegacyGuiGraphics.of(guiGraphics).blitSprite(SMALL_ARROW, leftPos + 97, topPos + 161, 16, 13);
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
        super.render(guiGraphics, i, j, f);
        for (int index = 0; index < ingredientsGrid.size(); index++)
            ScreenUtil.iconHolderRenderer.itemHolder(leftPos + 21 + index % 3 * 23, topPos +  133 + index / 3 * 23, 23, 23, getActualItem(ingredientsGrid.get(index)), !getActualItem(ingredientsGrid.get(index)).isEmpty() &&  warningSlots[index], new Offset(0.5, 0.5, 0)).render(guiGraphics, i, j, f);;
        ScreenUtil.iconHolderRenderer.itemHolder(leftPos +  124, topPos + 151, 36, 36, resultStack, getFocused() instanceof RecipeIconHolder<?> r && r.isValidIndex() && !r.canCraft(), new Offset(0.5, 0, 0)).render(guiGraphics, i, j, f);
        if (!resultStack.isEmpty()) {
            Component resultName = resultStack.getHoverName();
            ScreenUtil.renderScrollingString(guiGraphics, font, resultName, leftPos + 11 + Math.max(163 - font.width(resultName), 0) / 2, topPos + 114, leftPos + 170, topPos + 125, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            if (craftingTabList.selectedTab == 0){
                List<Component> list = resultStack.getTooltipLines(minecraft.player, TooltipFlag.NORMAL);
                for (int i1 = 0; i1 < list.size(); i1++) {
                    if (26 + i1 * 13 >= 93) break;
                    Component c = list.get(i1);
                    ScreenUtil.renderScrollingString(guiGraphics, font, c.copy().setStyle(Style.EMPTY), leftPos + 180, topPos + 15 + i1 * 13, leftPos + 335, topPos + 26 + i1 * 13, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                }
            }
            if (ScreenUtil.isMouseOver(i,j,leftPos + 124, topPos + 151,36,36)) guiGraphics.renderTooltip(font, resultStack,i,j);
        }
        for (int index = 0; index < ingredientsGrid.size(); index++) if (!getActualItem(ingredientsGrid.get(index)).isEmpty() && ScreenUtil.isMouseOver(i,j,leftPos +  21 + index % 3 * 23, topPos + 133 + index / 3 * 23,23,23)) guiGraphics.renderTooltip(font,getActualItem(ingredientsGrid.get(index)),i,j);
        getCraftingButtons().forEach(h -> h.renderTooltip(minecraft, guiGraphics, i, j));

        renderTooltip(guiGraphics, i, j);
    }
}
