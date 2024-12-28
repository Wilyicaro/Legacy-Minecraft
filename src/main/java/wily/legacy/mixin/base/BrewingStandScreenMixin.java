package wily.legacy.mixin.base;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;

import static wily.legacy.util.LegacySprites.BREWING_FUEL_SLOT;

@Mixin(BrewingStandScreen.class)
public abstract class BrewingStandScreenMixin extends AbstractContainerScreen<BrewingStandMenu> {
    @Shadow @Final private static int[] BUBBLELENGTHS;

    private static final Vec3 BREWING_SLOT_OFFSET = new Vec3(0,0.5,0);

    private static final LegacySlotDisplay FIRST_BREWING_SLOT_DISPLAY = createBrewingSlotDisplay(BREWING_SLOT_OFFSET);
    private static final LegacySlotDisplay SECOND_BREWING_SLOT_DISPLAY = createBrewingSlotDisplay(new Vec3(0.5,0,0));
    private static final LegacySlotDisplay THIRD_BREWING_SLOT_DISPLAY = createBrewingSlotDisplay(BREWING_SLOT_OFFSET);
    private static final LegacySlotDisplay FOURTH_BREWING_SLOT_DISPLAY = createBrewingSlotDisplay(new Vec3(0.5,0.5,0));

    private static LegacySlotDisplay createBrewingSlotDisplay(Vec3 offset){
        return new LegacySlotDisplay(){
            public Vec3 getOffset() {
                return offset;
            }
            public int getWidth() {
                return 27;
            }
            public ArbitrarySupplier<ResourceLocation> getIconHolderOverride() {
                return EMPTY_OVERRIDE;
            }
        };
    }

    public BrewingStandScreenMixin(BrewingStandMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }
    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        imageWidth = 213;
        imageHeight = 225;
        inventoryLabelX = 13;
        inventoryLabelY = 115;
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        titleLabelY = 11;
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0){
                LegacySlotDisplay.override(s,60, 76, FIRST_BREWING_SLOT_DISPLAY);
            } else if (i == 1) {
                LegacySlotDisplay.override(s,94, 87, SECOND_BREWING_SLOT_DISPLAY);
            } else if (i == 2) {
                LegacySlotDisplay.override(s,129, 76, THIRD_BREWING_SLOT_DISPLAY);
            } else if (i == 3) {
                LegacySlotDisplay.override(s, 94, 25, FOURTH_BREWING_SLOT_DISPLAY);
            } else if (i == 4) {
                LegacySlotDisplay.override(s,19,25, new LegacySlotDisplay(){
                    public Vec3 getOffset() {
                        return BREWING_SLOT_OFFSET;
                    }
                    public int getWidth() {
                        return 27;
                    }
                    public ResourceLocation getIconSprite() {
                        return s.getItem().isEmpty() ? BREWING_FUEL_SLOT : null;
                    }
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, 14 + (s.getContainerSlot() - 9) % 9 * 21,126 + (s.getContainerSlot() - 9) / 9 * 21);
            } else {
                LegacySlotDisplay.override(s, 14 + s.getContainerSlot() * 21,195);
            }
        }
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
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        super.renderLabels(guiGraphics, i, j);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.BREWING_COIL_FLAME, 43, 42,51, 33);
        int fuel = this.menu.getFuel();
        int n = Mth.clamp((27 * fuel + 20 - 1) / 20, 0, 27);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(65.5f,66, 0f);
        if (n > 0) {
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.FUEL_LENGTH_SPRITE, 27, 6, 0, 0, 0, 0, 0, n, 6);
        }
        guiGraphics.pose().popPose();
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 58.5f,topPos + 22.5, 0f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.BREWING_SLOTS, 0, 0,96, 96);
        guiGraphics.pose().popPose();
        int o;
        if ((o = this.menu.getBrewingTicks()) > 0) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(leftPos + 121.5f,topPos + 22.5, 0f);
            guiGraphics.pose().scale(0.5f,0.5f,0.5f);
            int p = (int)(84.0f * (1.0f - (float)o / 400.0f));
            if (p > 0)
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.BREW_PROGRESS_SPRITE, 27, 84, 0, 0, 0, 0, 0,27, p);
            guiGraphics.pose().popPose();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(leftPos + 71f,topPos + 21, 0f);
            guiGraphics.pose().scale(1.5f,1.5f,1.5f);
            if ((p = BUBBLELENGTHS[o / 2 % 7]) > 0) {
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.BUBBLES_SPRITE, 12, 29, 0, 29 - p, 0, 29 - p, 0,12, p);
            }
            guiGraphics.pose().popPose();
        }
    }
}
