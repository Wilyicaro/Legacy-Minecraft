package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.FactoryIngredient;
import wily.factoryapi.base.StackIngredient;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.network.CommonNetwork;
import wily.factoryapi.base.network.CommonRecipeManager;
import wily.factoryapi.util.FactoryItemUtil;
import wily.legacy.client.LegacyGuiItemRenderer;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.network.ServerMenuCraftPayload;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class CustomRecipeIconHolder extends LegacyIconHolder implements ControlTooltip.ActionHolder {
    final ItemStack defaultItem;
    private final Minecraft minecraft;
    ItemStack nextItem = ItemStack.EMPTY;
    ItemStack previousItem = ItemStack.EMPTY;
    List<ItemStack> addedIngredientsItems = null;
    Predicate<CustomRecipeIconHolder> canAddIngredient = h -> true;

    public CustomRecipeIconHolder(ItemStack defaultItem) {
        super(27, 27);
        minecraft = Minecraft.getInstance();
        allowItemDecorations = false;
        this.defaultItem = itemIcon = defaultItem;
    }

    public CustomRecipeIconHolder() {
        this(ItemStack.EMPTY);
    }

    public void init() {
        itemIcon = defaultItem;
    }

    public void applyAddedIngredients() {
        if (addedIngredientsItems == null || addedIngredientsItems.isEmpty()) return;
        int index = 0;
        for (int i1 = 0; i1 < getIngredientsGrid().size(); i1++) {
            if (index >= addedIngredientsItems.size()) break;
            Optional<Ingredient> ing = getIngredientsGrid().get(i1);
            if (ing.isPresent()) continue;
            getIngredientsGrid().set(i1, Optional.of(StackIngredient.of(false, addedIngredientsItems.get(index))));
            index++;
        }
    }

    public boolean applyNextItemIfAbsent() {
        return false;
    }

    public CustomRecipeIconHolder enableAddIngredients() {
        addedIngredientsItems = new ArrayList<>();
        return this;
    }

    public CustomRecipeIconHolder enableAddIngredients(Predicate<CustomRecipeIconHolder> canAddIngredient) {
        this.canAddIngredient = canAddIngredient;
        return this.enableAddIngredients();
    }

    public ItemStack nextItem(Inventory inventory, Predicate<ItemStack> isValid) {
        List<ItemStack> items = inventory.getNonEquipmentItems();
        for (int i = Math.max(0, items.indexOf(itemIcon)); i < items.size(); i++)
            if (itemIcon != items.get(i) && isValid.test(items.get(i))) return items.get(i);
        for (int i = 0; i < Math.max(0, items.indexOf(itemIcon)); i++)
            if (itemIcon != items.get(i) && isValid.test(items.get(i))) return items.get(i);
        return ItemStack.EMPTY;
    }

    public ItemStack previousItem(Inventory inventory, Predicate<ItemStack> isValid) {
        List<ItemStack> items = inventory.getNonEquipmentItems();
        for (int i = Math.max(0, items.indexOf(itemIcon)); i >= 0; i--)
            if (itemIcon != items.get(i) && isValid.test(items.get(i))) return items.get(i);
        for (int i = items.size() - 1; i >= Math.max(0, items.indexOf(itemIcon)); i--)
            if (itemIcon != items.get(i) && isValid.test(items.get(i))) return items.get(i);
        return ItemStack.EMPTY;
    }

    public ItemStack nextItem(List<ItemStack> itemStacks) {
        return itemStacks.get(Stocker.cyclic(0, itemStacks.indexOf(itemIcon) + 1, itemStacks.size()));
    }

    public ItemStack previousItem(List<ItemStack> itemStacks) {
        return itemStacks.get(Stocker.cyclic(0, itemStacks.indexOf(itemIcon) - 1, itemStacks.size()));
    }

    @Override
    public void setFocused(boolean bl) {
        if (bl) {
            updateRecipe();
        }
        super.setFocused(bl);
    }

    public abstract Component getDisplayName();

    abstract ItemStack nextItem();

    abstract ItemStack previousItem();

    abstract int findInventoryMatchSlot();

    abstract void updateRecipe();

    abstract LegacyScrollRenderer getScrollRenderer();

    public abstract boolean canCraft();

    public abstract List<Optional<Ingredient>> getIngredientsGrid();

    public ItemStack assembleCraftingResult(Level level, CraftingContainer container) {
        container.clearContent();
        for (int i = 0; i < getIngredientsGrid().size(); i++) {
            if (getIngredientsGrid().get(i).isPresent())
                container.setItem(i, FactoryIngredient.of(getIngredientsGrid().get(i).get()).getStacks()[0]);
        }
        //? if >=1.20.5
        CraftingInput input = container.asCraftInput();
        return CommonRecipeManager.getResultFor(RecipeType.CRAFTING, input, level).orElse(ItemStack.EMPTY);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (isFocused()) {
            if (canCraft()) {
                craft(input);
                updateRecipe();
            } else LegacySoundUtil.playSimpleUISound(LegacyRegistries.CRAFT_FAIL.get(), 1.0f);
        }
    }

    public void craft(InputWithModifiers input) {
        CommonNetwork.sendToServer(new ServerMenuCraftPayload(getRecipeId(), List.copyOf(getIngredientsGrid()), -1, input.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.state().pressed));
    }

    public Optional<ResourceLocation> getRecipeId() {
        return Optional.empty();
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        int i = (int) Math.signum(g);
        if (isFocused() && !nextItem.isEmpty() && i > 0 || !previousItem.isEmpty() && i < 0) {
            LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
            itemIcon = i > 0 ? nextItem : previousItem;
            updateRecipe();
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int i, int j, float f) {
        if (itemIcon.isEmpty() || (applyNextItemIfAbsent() && !hasItem(itemIcon)) || isFocused()) {
            nextItem = nextItem();
            previousItem = previousItem();
        }
        if ((itemIcon.isEmpty() || (applyNextItemIfAbsent() && !hasItem(itemIcon))) && !nextItem.isEmpty()) {
            itemIcon = nextItem;
            if (isFocused()) updateRecipe();
        }
        super.render(graphics, i, j, f);
    }

    protected boolean hasItem() {
        return hasItem(itemIcon);
    }

    protected boolean hasItem(ItemStack stack) {
        return !stack.isEmpty() && minecraft.player.getInventory()./*? if <1.21.5 {*//*items*//*?} else {*/getNonEquipmentItems()/*?}*/.stream().filter(s -> FactoryItemUtil.equalItems(s, stack)).mapToInt(ItemStack::getCount).sum() >= stack.getCount();
    }

    @Override
    public void renderItem(GuiGraphics graphics, int i, int j, float f) {
        LegacyGuiItemRenderer.secureTranslucentRender(!itemIcon.isEmpty() && !hasItem(itemIcon), 0.5f, (u) -> renderItem(graphics, itemIcon, getX(), getY(), false));
    }

    public boolean canAddIngredient() {
        return hasItem(itemIcon) && addedIngredientsItems != null && canAddIngredient.test(this) && getIngredientsGrid().stream().anyMatch(Optional::isEmpty);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == InputConstants.KEY_O && canAddIngredient()) {
            addedIngredientsItems.add(itemIcon.copyWithCount(1));
            updateRecipe();
            return true;
        }
        if (keyEvent.key() == InputConstants.KEY_X && addedIngredientsItems != null && !addedIngredientsItems.isEmpty()) {
            addedIngredientsItems.remove(addedIngredientsItems.size() - 1);
            updateRecipe();
            return true;
        }
        if (!nextItem.isEmpty() && keyEvent.isUp() || !previousItem.isEmpty() && keyEvent.isDown()) {
            LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
            itemIcon = keyEvent.isUp() ? nextItem : previousItem;
            updateRecipe();
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
        super.renderSelection(graphics, i, j, f);
        int matchSlot;
        if (!itemIcon.isEmpty() && hasItem(itemIcon) && minecraft.screen instanceof LegacyMenuAccess<?> a && (matchSlot = findInventoryMatchSlot()) > 0) {
            Slot s = a.getMenu().getSlot(matchSlot);
            LegacyIconHolder h = LegacyRenderUtil.iconHolderRenderer.slotBounds(a.getMenuRectangle().left(), a.getMenuRectangle().top(), s);
            h.render(graphics, i, j, f);
            h.itemIcon = s.getItem();
            h.renderHighlight(graphics);
            h.renderItem(graphics, i, j, f);
        }
        if (!previousItem.isEmpty() && previousItem != itemIcon || !nextItem.isEmpty() && nextItem != itemIcon) {
            renderScroll(graphics, getScrollRenderer());
        }
    }

    @Override
    public @Nullable Component getAction(Context context) {
        return context.actionOfContext(KeyContext.class, c -> c.key() == InputConstants.KEY_RETURN && isFocused() && canCraft() ? LegacyComponents.CREATE : c.key() == InputConstants.KEY_O && isFocused() && canAddIngredient() ? LegacyComponents.ADD : null);
    }
}