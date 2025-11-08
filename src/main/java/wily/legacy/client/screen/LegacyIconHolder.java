package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

public class LegacyIconHolder extends SimpleLayoutRenderable implements GuiEventListener, NarratableEntry, ControlTooltip.ActionHolder {
    public Vec2 offset = Vec2.ZERO;
    public ResourceLocation iconSprite = null;
    public ArbitrarySupplier<ResourceLocation> iconHolderOverride = null;
    @NotNull
    public ItemStack itemIcon = ItemStack.EMPTY;

    public boolean allowItemDecorations = true;
    public boolean allowFocusedItemTooltip = false;
    public boolean isHovered;
    private boolean isWarning = false;
    private boolean focused = false;

    public LegacyIconHolder() {
    }

    public LegacyIconHolder(int leftPos, int topPos, Slot slot) {
        slotBounds(leftPos, topPos, slot);
    }


    public LegacyIconHolder(int x, int y, int width, int height) {
        this(width, height);
        setPos(x, y);
    }

    public LegacyIconHolder(int width, int height) {
        setBounds(width, height);
    }

    public static LegacyIconHolder fromSlot(Slot slot) {
        return new LegacyIconHolder() {
            @Override
            public void render(GuiGraphics graphics, int i, int j, float f) {
                slotBoundsWithItem(0, 0, slot);
                super.render(graphics, i, j, f);
            }
        };
    }

    public static LegacyIconHolder entityHolder(int x, int y, int width, int height, EntityType<?> entityType) {
        return new LegacyIconHolder(x, y, width, height) {
            Entity entity;

            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                super.render(graphics, mouseX, mouseY, delta);
                if (entity == null && Minecraft.getInstance().level != null) {
                    entity = entityType.create(Minecraft.getInstance().level, EntitySpawnReason.EVENT);
                }
                if (entity != null) renderEntity(graphics, entity, mouseX, mouseY, delta);
            }
        };
    }

    public void setBounds(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setPos(int x, int y) {
        setX(x);
        setY(y);
    }

    public LegacyIconHolder slotBoundsWithItem(int leftPos, int topPos, Slot slot) {
        return slotBounds(leftPos, topPos, slot, slot.getItem());
    }

    public LegacyIconHolder slotBounds(Slot slot) {
        return slotBounds(0, 0, slot);
    }

    public LegacyIconHolder slotBounds(int leftPos, int topPos, Slot slot) {
        return slotBounds(leftPos, topPos, slot, ItemStack.EMPTY);
    }

    public LegacyIconHolder slotBounds(int leftPos, int topPos, Slot slot, ItemStack stack) {
        return itemHolder(leftPos + slot.x, topPos + slot.y, LegacySlotDisplay.of(slot).getWidth(), LegacySlotDisplay.of(slot).getHeight(), stack, LegacySlotDisplay.of(slot).isWarning(), LegacySlotDisplay.of(slot).getIconSprite(), LegacySlotDisplay.of(slot).getOffset(), LegacySlotDisplay.of(slot).getIconHolderOverride());
    }

    public LegacyIconHolder itemHolder(ItemStack itemIcon, boolean isWarning) {
        return itemHolder(itemIcon, isWarning, Vec2.ZERO);
    }

    public LegacyIconHolder itemHolder(ItemStack itemIcon, boolean isWarning, Vec2 offset) {
        return itemHolder(0, 0, 21, 21, itemIcon, isWarning, offset);
    }

    public LegacyIconHolder itemHolder(int x, int y, int width, int height, ItemStack itemIcon, boolean isWarning, Vec2 offset) {
        return itemHolder(x, y, width, height, itemIcon, isWarning, null, offset, null);
    }

    public LegacyIconHolder itemHolder(int x, int y, int width, int height, ItemStack itemIcon, boolean isWarning, ResourceLocation iconSprite, Vec2 offset, ArbitrarySupplier<ResourceLocation> override) {
        setPos(x, y);
        setBounds(width, height);
        this.iconSprite = iconSprite;
        this.iconHolderOverride = override;
        this.itemIcon = itemIcon;
        this.isWarning = isWarning;
        this.allowItemDecorations = true;
        this.offset = offset;
        return this;
    }

    public double getMiddleX() {
        return getXCorner() + offset.x + getWidth() / 2f;
    }

    public double getMiddleY() {
        return getYCorner() + offset.y + getHeight() / 2f;
    }

    public float getXCorner() {
        return getX() - (isSizeable() ? 1 : getWidth() / 20f);
    }

    public float getYCorner() {
        return getY() - (isSizeable() ? 1 : getHeight() / 20f);
    }

    public float getSelectableWidth() {
        return getWidth() - 2 * (isSizeable() ? 1 : getWidth() / 20f);
    }

    public float getSelectableHeight() {
        return getHeight() - 2 * (isSizeable() ? 1 : getHeight() / 20f);
    }

    public float getScaleX() {
        return getWidth() / 18f;
    }

    public float getScaleY() {
        return getHeight() / 18f;
    }

    public boolean isSizeable() {
        return Math.min(getWidth(), getHeight()) < 18 && LegacyOptions.getUIMode().isHD();
    }

    public boolean canSizeIcon() {
        return getMinSize() < 18 || getMinSize() > 21;
    }

    public void applyOffset(GuiGraphics graphics) {
        if (!offset.equals(Vec2.ZERO)) graphics.pose().translate(offset.x, offset.y);
    }

    public boolean isWarning() {
        return isWarning;
    }

    public void setWarning(boolean warning) {
        this.isWarning = warning;
    }

    public ResourceLocation getIconHolderSprite() {
        return iconHolderOverride == null ? isWarning() ? LegacySprites.RED_ICON_HOLDER : isSizeable() ? LegacySprites.SIZEABLE_ICON_HOLDER : LegacySprites.ICON_HOLDER : iconHolderOverride.get();
    }

    @Override
    public void render(GuiGraphics graphics, int i, int j, float f) {
        isHovered = isHovered(i, j);
        ResourceLocation sprite = getIconHolderSprite();
        if (sprite != null)
            renderChild(graphics, getXCorner(), getYCorner(), () -> FactoryGuiGraphics.of(graphics).blitSprite(sprite, 0, 0, getWidth(), getHeight()));
        if (iconSprite != null) {
            renderIcon(iconSprite, graphics, canSizeIcon(), 16, 16);
        }
        renderItem(graphics, i, j, f);
    }

    public void renderIcon(ResourceLocation location, GuiGraphics graphics, boolean scaled, int width, int height) {
        renderChild(graphics, getX(), getY(), () -> {
            if (scaled) {
                graphics.pose().scale(getSelectableWidth() / width, getSelectableHeight() / height);
            } else graphics.pose().translate((getSelectableWidth() - width) / 2, (getSelectableHeight() - height) / 2);
            FactoryGuiGraphics.of(graphics).blitSprite(location, 0, 0, width, height);
        });
    }

    public void renderItem(GuiGraphics graphics, int i, int j, float f) {
        renderItem(graphics, itemIcon, getX(), getY(), isWarning());
    }

    public void renderItem(GuiGraphics graphics, ItemStack item, int x, int y, boolean isWarning) {
        if (!item.isEmpty()) renderItem(graphics, () -> {
            graphics.renderFakeItem(item, 0, 0);
            if (allowItemDecorations)
                graphics.renderItemDecorations(Minecraft.getInstance().font, item, 0, 0);
        }, x, y, isWarning);
    }

    public void renderItem(GuiGraphics graphics, Runnable itemRender, int x, int y, boolean isWarning) {
        renderScaled(graphics, x, y, itemRender);
        if (isWarning) renderWarning(graphics);
    }

    public void renderWarning(GuiGraphics graphics) {
        renderChild(graphics, x, y, () -> FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.WARNING_ICON, 0, 0, 8, 8));
    }

    public void renderEntity(GuiGraphics graphics, Entity entity, int mouseX, int mouseY, float deltaTime) {
        entity.setYRot(180);
        entity.yRotO = entity.getYRot();
        entity.setXRot(entity.xRotO = 0);
        if (entity instanceof LivingEntity e) {
            e.yBodyRotO = e.yBodyRot = 180.0f;
            e.yHeadRot = 180;
            e.yHeadRotO = e.yHeadRot;
        }
        LegacyRenderUtil.renderEntity(graphics, getX(), getY(), getX() + Math.round(getSelectableWidth()), getY() + Math.round(getSelectableHeight() * 2), (int) Math.min(getSelectableWidth(), getSelectableHeight()), new Vector3f(), new Quaternionf().rotationXYZ(0.0f, (float) Math.PI / 4, (float) Math.PI), null, entity, true);
    }

    public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
        if (LegacyOptions.getUIMode().isSD() && getMinSize() == 20)
            renderChild(graphics, getXCorner() - (21f - getWidth()) / 2, getYCorner() - (21f - getHeight()) / 2, () -> FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SELECT_ICON_HIGHLIGHT_SMALL, 0, 0, 21, 21));
        else
            renderChild(graphics, getXCorner() + (getWidth() - 36f) / 2, getYCorner() + (getHeight() - 36f) / 2, () -> FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SELECT_ICON_HIGHLIGHT, 0, 0, 36, 36));
    }

    public void renderScroll(GuiGraphics graphics, LegacyScrollRenderer scrollRenderer) {
        if (LegacyOptions.getUIMode().isSD() && getMinSize() == 20) {
            renderChild(graphics, getXCorner() + (getWidth() - 7) / 2.0f, getYCorner() - 0.5f, () -> {
                scrollRenderer.renderSmallScroll(graphics, true, 0, -5);
                scrollRenderer.renderSmallScroll(graphics, false, 0, getHeight() + 2);
            });
        } else {
            renderChild(graphics, getXCorner() + (getWidth() - 13) / 2.0f, getYCorner(), () -> {
                scrollRenderer.renderScroll(graphics, ScreenDirection.UP, -1, -12);
                scrollRenderer.renderScroll(graphics, ScreenDirection.DOWN, -1, getHeight() + 5);
            });
        }
    }

    public void renderScaled(GuiGraphics graphics, float x, float y, Runnable render) {
        renderChild(graphics, x, y, () -> {
            graphics.pose().scale(getWidth() / 18f, getHeight() / 18f);
            render.run();
        });
    }

    public void renderChild(GuiGraphics graphics, float x, float y, Runnable render) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        applyOffset(graphics);
        render.run();
        graphics.pose().popMatrix();
    }

    public void renderHighlight(GuiGraphics graphics) {
        renderChild(graphics, x, y, () -> {
            graphics.pose().scale(getSelectableWidth() / 16f, getSelectableHeight() / 16f);
            FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SLOT_HIGHLIGHT, 0, 0, 16, 16);
        });
    }

    public void renderTooltip(Minecraft minecraft, GuiGraphics graphics, int i, int j) {
        if (isHovered || (allowFocusedItemTooltip && isFocused()))
            renderTooltip(minecraft, graphics, itemIcon, !isHovered ? (int) getMiddleX() : i, !isHovered ? (int) getMiddleY() : j);
    }

    public void renderTooltip(Minecraft minecraft, GuiGraphics graphics, ItemStack stack, int i, int j) {
        if (!stack.isEmpty())
            LegacyFontUtil.applySmallerFont(LegacyFontUtil.MOJANGLES_11_FONT, b -> graphics.setTooltipForNextFrame(minecraft.font, stack, i, j));
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        return LegacyRenderUtil.isMouseOver(mouseX, mouseY, getXCorner(), getYCorner(), width, height);
    }

    public boolean isHoveredOrFocused() {
        return isHovered || isFocused();
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.isSelection()) {
            onPress(keyEvent);
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        if (isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y()) && mouseButtonEvent.button() == 0) {
            onClick(mouseButtonEvent, bl);
            return !isFocused();
        }
        return false;
    }

    public void playClickSound() {
        if (!isFocused()) LegacySoundUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F);
    }

    public void onClick(MouseButtonEvent event, boolean bl) {
        playClickSound();
        onPress(event);
    }

    public void onPress(InputWithModifiers inputWithModifiers) {

    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public void setFocused(boolean bl) {
        focused = bl;
    }

    @Override
    public NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarrationPriority.FOCUSED;
        }
        if (this.isHovered) {
            return NarrationPriority.HOVERED;
        }
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }

    public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
        return !this.isFocused() ? ComponentPath.leaf(this) : null;
    }

    public ScreenRectangle getRectangle() {
        return new ScreenRectangle((int) (offset.x + getXCorner()), (int) (offset.y + getYCorner()), getWidth(), getHeight());
    }

    public int getMinSize() {
        return Math.min(getWidth(), getHeight());
    }

    @Override
    public @Nullable Component getAction(Context context) {
        return ControlTooltip.getSelectAction(this, context);
    }
}
