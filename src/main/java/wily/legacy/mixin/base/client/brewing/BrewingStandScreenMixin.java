package wily.legacy.mixin.base.client.brewing;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import static wily.legacy.util.LegacySprites.BREWING_FUEL_SLOT;

@Mixin(BrewingStandScreen.class)
public abstract class BrewingStandScreenMixin extends AbstractContainerScreen<BrewingStandMenu> {
    private static final Vec2 BREWING_SLOT_OFFSET = new Vec2(0, 0.5f);
    @Shadow
    @Final
    private static int[] BUBBLELENGTHS;

    public BrewingStandScreenMixin(BrewingStandMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    public void added() {
        super.added();
        ControlTooltip.Renderer.of(this).replace(3, i -> i, this::getQuickMoveLabel);
    }

    private Component getQuickMoveLabel(Component fallback) {
        if (hoveredSlot == null || hoveredSlot.getItem().isEmpty() || hoveredSlot.container != minecraft.player.getInventory())
            return fallback;
        ItemStack item = hoveredSlot.getItem();
        if (menu.getSlot(4).mayPlace(item))
            return LegacyComponents.MOVE_FUEL;
        if (menu.getSlot(3).mayPlace(item) || menu.getSlot(0).mayPlace(item) || menu.getSlot(1).mayPlace(item) || menu.getSlot(2).mayPlace(item))
            return LegacyComponents.MOVE_INGREDIENT;
        return fallback;
    }

    private static Vec2 getOffset(UIAccessor accessor, String elementName, Vec2 fallback) {
        return new Vec2(accessor.getFloat(elementName + ".offset.x", fallback.x), accessor.getFloat(elementName + ".offset.y", fallback.y));
    }

    private static LegacySlotDisplay createBrewingSlotDisplay(UIAccessor accessor, String elementName, String offsetElementName, Vec2 offset) {
        return new LegacySlotDisplay() {
            public Vec2 getOffset() {
                return LegacyOptions.getUIMode().isSD() ? Vec2.ZERO : BrewingStandScreenMixin.getOffset(accessor, offsetElementName, offset);
            }

            public int getWidth() {
                return accessor.getInteger(elementName + ".width", LegacyOptions.getUIMode().isSD() ? 18 : 27);
            }

            public int getHeight() {
                return accessor.getInteger(elementName + ".height", getWidth());
            }

            public Identifier getIconSprite() {
                return accessor.getElement(elementName + ".iconSprite", Identifier.class).orElse(null);
            }

            public ArbitrarySupplier<Identifier> getIconHolderOverride() {
                return accessor.getBoolean(elementName + ".hideHolder", true) ? EMPTY_OVERRIDE : LegacySlotDisplay.super.getIconHolderOverride();
            }
        };
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        UIAccessor accessor = UIAccessor.of(this);
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageWidth(accessor.getInteger("imageWidth", sd ? 130 : 213));
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageHeight(accessor.getInteger("imageHeight", sd ? 145 : 225));
        inventoryLabelX = accessor.getInteger("inventoryLabel.x", sd ? 7 : 13);
        inventoryLabelY = accessor.getInteger("inventoryLabel.y", sd ? 74 : 115);
        LegacyFontUtil.applySDFont(b -> this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2);
        titleLabelY = accessor.getInteger("title.y", sd ? 4 : 11);
        int slotsSize = accessor.getInteger("inventorySlots.size", sd ? 13 : 21);
        LegacySlotDisplay defaultDisplay = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return slotsSize;
            }
        };
        super.init();
        if (!sd)
            topPos += accessor.getInteger("screenOffset.y", -20);
        LegacySlotDisplay outerBrewingSlotDisplay = createBrewingSlotDisplay(accessor, "brewingBottleSlot", "brewingBottleSlot.outer", BREWING_SLOT_OFFSET);
        LegacySlotDisplay centerBrewingSlotDisplay = createBrewingSlotDisplay(accessor, "brewingBottleSlot", "brewingBottleSlot.center", new Vec2(0.5f, 0f));
        LegacySlotDisplay ingredientBrewingSlotDisplay = createBrewingSlotDisplay(accessor, "brewingIngredientSlot", "brewingIngredientSlot", new Vec2(0.5f, 0.5f));
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, accessor.getInteger("brewingBottleSlot.left.x", sd ? 34 : 60), accessor.getInteger("brewingBottleSlot.left.y", sd ? 48 : 76), outerBrewingSlotDisplay);
            } else if (i == 1) {
                LegacySlotDisplay.override(s, accessor.getInteger("brewingBottleSlot.center.x", sd ? 57 : 94), accessor.getInteger("brewingBottleSlot.center.y", sd ? 55 : 87), centerBrewingSlotDisplay);
            } else if (i == 2) {
                LegacySlotDisplay.override(s, accessor.getInteger("brewingBottleSlot.right.x", sd ? 80 : 129), accessor.getInteger("brewingBottleSlot.right.y", sd ? 48 : 76), outerBrewingSlotDisplay);
            } else if (i == 3) {
                LegacySlotDisplay.override(s, accessor.getInteger("brewingIngredientSlot.x", sd ? 57 : 94), accessor.getInteger("brewingIngredientSlot.y", sd ? 14 : 25), ingredientBrewingSlotDisplay);
            } else if (i == 4) {
                LegacySlotDisplay.override(s, accessor.getInteger("brewingFuelSlot.x", sd ? 7 : 19), accessor.getInteger("brewingFuelSlot.y", sd ? 16 : 25), new LegacySlotDisplay() {
                    public Vec2 getOffset() {
                        return sd ? Vec2.ZERO : BrewingStandScreenMixin.getOffset(accessor, "brewingFuelSlot", BREWING_SLOT_OFFSET);
                    }

                    public int getWidth() {
                        return accessor.getInteger("brewingFuelSlot.width", sd ? 18 : 27);
                    }

                    public int getHeight() {
                        return accessor.getInteger("brewingFuelSlot.height", getWidth());
                    }

                    public Identifier getIconSprite() {
                        return s.getItem().isEmpty() ? accessor.getResourceLocation("brewingFuelSlot.iconSprite", BREWING_FUEL_SLOT) : null;
                    }
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, accessor.getInteger("inventorySlots.x", inventoryLabelX) + (s.getContainerSlot() - 9) % 9 * slotsSize, accessor.getInteger("inventorySlots.y", sd ? 84 : 126) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, accessor.getInteger("inventorySlots.x", inventoryLabelX) + s.getContainerSlot() * slotsSize, accessor.getInteger("hotbarSlots.y", sd ? 126 : 195), defaultDisplay);
            }
        }
    }

    //? if >1.20.1 {
    @Override
    public void extractBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        super.extractBackground(GuiGraphicsExtractor, i, j, f);
    }

    //?} else {
    /*@Override
    public void extractBackground(GuiGraphicsExtractor GuiGraphicsExtractor) {
    }
    *///?}
    @Override
    protected void extractLabels(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j) {
        LegacyFontUtil.applySDFont(b -> super.extractLabels(GuiGraphicsExtractor, i, j));
        boolean sd = LegacyOptions.getUIMode().isSD();
        UIAccessor accessor = UIAccessor.of(this);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.BREWING_COIL_FLAME, accessor.getInteger("brewingCoil.x", sd ? 23 : 43), accessor.getInteger("brewingCoil.y", sd ? 25 : 42), accessor.getInteger("brewingCoil.width", sd ? 34 : 51), accessor.getInteger("brewingCoil.height", sd ? 22 : 33));
        int fuel = this.menu.getFuel();
        int fuelWidth = accessor.getInteger("fuelLength.width", sd ? 18 : 27);
        int fuelHeight = accessor.getInteger("fuelLength.height", sd ? 4 : 6);
        int n = Mth.clamp((fuelWidth * fuel + 20 - 1) / 20, 0, fuelWidth);
        if (n > 0) {
            GuiGraphicsExtractor.pose().pushMatrix();
            GuiGraphicsExtractor.pose().translate(accessor.getFloat("fuelLength.x", sd ? 38 : LegacyRenderUtil.hasHorizontalArtifacts() ? 65.4f : 65.5f), accessor.getFloat("fuelLength.y", sd ? 41 : 66));
            FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.FUEL_LENGTH_SPRITE, fuelWidth, fuelHeight, 0, 0, 0, 0, 0, n, fuelHeight);
            GuiGraphicsExtractor.pose().popMatrix();
        }
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        UIAccessor accessor = UIAccessor.of(this);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(accessor.getResourceLocation("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(leftPos + accessor.getFloat("brewingSlots.x", sd ? 33 : LegacyRenderUtil.hasHorizontalArtifacts() ? 58.4f : 58.5f), topPos + accessor.getFloat("brewingSlots.y", sd ? 12 : 22.4f));
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.BREWING_SLOTS, 0, 0, accessor.getInteger("brewingSlots.width", sd ? 64 : 96), accessor.getInteger("brewingSlots.height", sd ? 64 : 96));
        GuiGraphicsExtractor.pose().popMatrix();
        int o;
        if ((o = this.menu.getBrewingTicks()) > 0) {
            int guiScale = minecraft.getWindow().getGuiScale();
            int brewWidth = 9 * guiScale;
            int brewHeight = 27 * guiScale;
            int p = (int) (brewHeight * (1.0f - (float) o / 400.0f));
            if (p > 0) {
                GuiGraphicsExtractor.pose().pushMatrix();
                GuiGraphicsExtractor.pose().translate(leftPos + accessor.getFloat("brewProgress.x", sd ? 75 : LegacyRenderUtil.hasHorizontalArtifacts() ? 121.4f : 121.5f), topPos + accessor.getFloat("brewProgress.y", sd ? 12 : 22.4f));
                if (!sd) GuiGraphicsExtractor.pose().scale(accessor.getFloat("brewProgress.scale", 1.5f), accessor.getFloat("brewProgress.scale", 1.5f));
                GuiGraphicsExtractor.pose().scale(1f / guiScale, 1f / guiScale);
                FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.BREW_PROGRESS_SPRITE, brewWidth, brewHeight, 0, 0, 0, 0, 0, brewWidth, p);
                GuiGraphicsExtractor.pose().popMatrix();
            }
            if ((p = BUBBLELENGTHS[o / 2 % 7]) > 0) {
                GuiGraphicsExtractor.pose().pushMatrix();
                GuiGraphicsExtractor.pose().translate(leftPos + accessor.getFloat("brewingBubbles.x", sd ? 41 : 71), topPos + accessor.getFloat("brewingBubbles.y", sd ? 11 : 21));
                if (!sd) GuiGraphicsExtractor.pose().scale(accessor.getFloat("brewingBubbles.scale", 1.5f), accessor.getFloat("brewingBubbles.scale", 1.5f));
                FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.BUBBLES_SPRITE, 12, 29, 0, 29 - p, 0, 29 - p, 0, 12, p);
                GuiGraphicsExtractor.pose().popMatrix();
            }
        }
    }
}
