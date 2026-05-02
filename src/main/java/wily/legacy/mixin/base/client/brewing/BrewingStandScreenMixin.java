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
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import static wily.legacy.util.LegacySprites.BREWING_FUEL_SLOT;

@Mixin(BrewingStandScreen.class)
public abstract class BrewingStandScreenMixin extends AbstractContainerScreen<BrewingStandMenu> {

private static final Vec2 BREWING_SLOT_OFFSET = new Vec2(0, 0.5f);
    private static final LegacySlotDisplay FIRST_BREWING_SLOT_DISPLAY = createBrewingSlotDisplay(BREWING_SLOT_OFFSET);
    private static final LegacySlotDisplay SECOND_BREWING_SLOT_DISPLAY = createBrewingSlotDisplay(new Vec2(0.5f, 0f));
    private static final LegacySlotDisplay THIRD_BREWING_SLOT_DISPLAY = createBrewingSlotDisplay(BREWING_SLOT_OFFSET);
    private static final LegacySlotDisplay FOURTH_BREWING_SLOT_DISPLAY = createBrewingSlotDisplay(new Vec2(0.5f, 0.5f));
    @Shadow
    @Final
    private static int[] BUBBLELENGTHS;

    public BrewingStandScreenMixin(BrewingStandMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    private static LegacySlotDisplay createBrewingSlotDisplay(Vec2 offset) {
        return new LegacySlotDisplay() {
            public Vec2 getOffset() {
                return LegacyOptions.getUIMode().isSD() ? Vec2.ZERO : offset;
            }

            public int getWidth() {
                return LegacyOptions.getUIMode().isSD() ? 18 : 27;
            }

            public ArbitrarySupplier<Identifier> getIconHolderOverride() {
                return EMPTY_OVERRIDE;
            }
        };
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageWidth(sd ? 130 : 213);
        ((wily.legacy.mixin.base.client.AbstractContainerScreenAccessor) this).legacy$setImageHeight(sd ? 145 : 225);
        inventoryLabelX = sd ? 7 : 13;
        inventoryLabelY = sd ? 74 : 115;
        LegacyFontUtil.applySDFont(b -> this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2);
        titleLabelY = sd ? 4 : 11;
        int slotsSize = sd ? 13 : 21;
        LegacySlotDisplay defaultDisplay = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return slotsSize;
            }
        };
        super.init();
        if (!sd)
            topPos -= 20;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, sd ? 34 : 60, sd ? 48 : 76, FIRST_BREWING_SLOT_DISPLAY);
            } else if (i == 1) {
                LegacySlotDisplay.override(s, sd ? 57 : 94, sd ? 55 : 87, SECOND_BREWING_SLOT_DISPLAY);
            } else if (i == 2) {
                LegacySlotDisplay.override(s, sd ? 80 : 129, sd ? 48 : 76, THIRD_BREWING_SLOT_DISPLAY);
            } else if (i == 3) {
                LegacySlotDisplay.override(s, sd ? 57 : 94, sd ? 14 : 25, FOURTH_BREWING_SLOT_DISPLAY);
            } else if (i == 4) {
                LegacySlotDisplay.override(s, sd ? 7 : 19, sd ? 16 : 25, new LegacySlotDisplay() {
                    public Vec2 getOffset() {
                        return sd ? Vec2.ZERO : BREWING_SLOT_OFFSET;
                    }

                    public int getWidth() {
                        return sd ? 18 : 27;
                    }

                    public Identifier getIconSprite() {
                        return s.getItem().isEmpty() ? BREWING_FUEL_SLOT : null;
                    }
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 84 : 126) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsSize, sd ? 126 : 195, defaultDisplay);
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
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.BREWING_COIL_FLAME, sd ? 23 : 43, sd ? 25 : 42, sd ? 34 : 51, sd ? 22 : 33);
        int fuel = this.menu.getFuel();
        int fuelWidth = sd ? 18 : 27;
        int fuelHeight = sd ? 4 : 6;
        int n = Mth.clamp((fuelWidth * fuel + 20 - 1) / 20, 0, fuelWidth);
        if (n > 0) {
            GuiGraphicsExtractor.pose().pushMatrix();
            GuiGraphicsExtractor.pose().translate(sd ? 38 : LegacyRenderUtil.hasHorizontalArtifacts() ? 65.4f : 65.5f, sd ? 41 : 66);
            FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.FUEL_LENGTH_SPRITE, fuelWidth, fuelHeight, 0, 0, 0, 0, 0, n, fuelHeight);
            GuiGraphicsExtractor.pose().popMatrix();
        }
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(leftPos + (sd ? 33 : LegacyRenderUtil.hasHorizontalArtifacts() ? 58.4f : 58.5f), topPos + (sd ? 12 : 22.4f));
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.BREWING_SLOTS, 0, 0, sd ? 64 : 96, sd ? 64 : 96);
        GuiGraphicsExtractor.pose().popMatrix();
        int o;
        if ((o = this.menu.getBrewingTicks()) > 0) {
            int guiScale = minecraft.getWindow().getGuiScale();
            int brewWidth = 9 * guiScale;
            int brewHeight = 27 * guiScale;
            int p = (int) (brewHeight * (1.0f - (float) o / 400.0f));
            if (p > 0) {
                GuiGraphicsExtractor.pose().pushMatrix();
                GuiGraphicsExtractor.pose().translate(leftPos + (sd ? 75 : LegacyRenderUtil.hasHorizontalArtifacts() ? 121.4f : 121.5f), topPos + (sd ? 12 : 22.4f));
                if (!sd) GuiGraphicsExtractor.pose().scale(1.5f, 1.5f);
                GuiGraphicsExtractor.pose().scale(1f / guiScale, 1f / guiScale);
                FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.BREW_PROGRESS_SPRITE, brewWidth, brewHeight, 0, 0, 0, 0, 0, brewWidth, p);
                GuiGraphicsExtractor.pose().popMatrix();
            }
            if ((p = BUBBLELENGTHS[o / 2 % 7]) > 0) {
                GuiGraphicsExtractor.pose().pushMatrix();
                GuiGraphicsExtractor.pose().translate(leftPos + (sd ? 41 : 71), topPos + (sd ? 11 : 21));
                if (!sd) GuiGraphicsExtractor.pose().scale(1.5f, 1.5f);
                FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.BUBBLES_SPRITE, 12, 29, 0, 29 - p, 0, 29 - p, 0, 12, p);
                GuiGraphicsExtractor.pose().popMatrix();
            }
        }
    }
}
