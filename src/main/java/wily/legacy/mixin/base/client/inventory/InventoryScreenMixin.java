package wily.legacy.mixin.base.client.inventory;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.CreativeModeScreen;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.client.screen.ReplaceableScreen;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> implements ReplaceableScreen, LegacyMenuAccess<InventoryMenu> {
    private static final Vec2 EQUIP_SLOT_OFFSET = new Vec2(50, 0);
    private static final Vec2 EQUIP_SLOT_OFFSET_SD = new Vec2(31, 0);
    private static final ResourceLocation[] EQUIPMENT_SLOT_SPRITES = new ResourceLocation[]{LegacySprites.HEAD_SLOT, LegacySprites.CHEST_SLOT, LegacySprites.LEGS_SLOT, LegacySprites.FEET_SLOT};

    private boolean canReplace = true;

    public InventoryScreenMixin(InventoryMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @ModifyExpressionValue(method = "containerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
    public boolean containerTick(boolean original) {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        imageWidth = sd ? 130 : 215;
        imageHeight = sd ? 140 : 217;
        inventoryLabelX = sd ? 7 : 14;
        inventoryLabelY = sd ? 65 : 103;
        int slotsSize = sd ? 13 : 21;
        super.init();
        LegacySlotDisplay craftingDisplay = new LegacySlotDisplay() {
            public boolean isVisible() {
                return LegacyOptions.hasClassicCrafting();
            }

            @Override
            public int getWidth() {
                return slotsSize;
            }
        };
        LegacySlotDisplay defaultDisplay = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return slotsSize;
            }
        };
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, sd ? 110 : 180, sd ? 25 : 40, craftingDisplay);
            } else if (i < 5) {
                LegacySlotDisplay.override(s, (sd ? 64 : 111) + s.getContainerSlot() % 2 * slotsSize, (sd ? 19 : 30) + s.getContainerSlot() / 2 * slotsSize, craftingDisplay);
            } else if (i < 9) {
                int index = 39 - s.getContainerSlot();
                LegacySlotDisplay.override(s, inventoryLabelX, (sd ? 9 : 14) + index * slotsSize, new LegacySlotDisplay() {
                    public ResourceLocation getIconSprite() {
                        return s.getItem().isEmpty() ? EQUIPMENT_SLOT_SPRITES[index] : null;
                    }

                    public Vec2 getOffset() {
                        return LegacyOptions.hasClassicCrafting() ? Vec2.ZERO : sd ? EQUIP_SLOT_OFFSET_SD : EQUIP_SLOT_OFFSET;
                    }

                    @Override
                    public int getWidth() {
                        return slotsSize;
                    }
                });
            } else if (i < menu.slots.size() - 10) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 76 : 116) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultDisplay);
            } else if (i < menu.slots.size() - 1) {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsSize, sd ? 120 : 186, defaultDisplay);
            } else {
                LegacySlotDisplay.override(s, sd ? 64 : 111, sd ? 48 : 77, new LegacySlotDisplay() {
                    public Vec2 getOffset() {
                        return LegacyOptions.hasClassicCrafting() ? Vec2.ZERO : sd ? EQUIP_SLOT_OFFSET_SD : EQUIP_SLOT_OFFSET;
                    }

                    public ResourceLocation getIconSprite() {
                        return s.getItem().isEmpty() ? LegacySprites.SHIELD_SLOT : null;
                    }

                    @Override
                    public int getWidth() {
                        return slotsSize;
                    }
                });
            }
        }
    }

    @Inject(method = "getRecipeBookButtonPosition", at = @At("HEAD"), cancellable = true)
    protected void getRecipeBookButtonPosition(CallbackInfoReturnable<ScreenPosition> cir) {
        cir.setReturnValue(new ScreenPosition(this.leftPos + (LegacyOptions.getUIMode().isSD() ? 90 : 180), topPos + (LegacyOptions.getUIMode().isSD() ? 50 : 71)));
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics graphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        int playerPanelX = leftPos + (sd ? 23 : 40) + (LegacyOptions.hasClassicCrafting() ? 0 : sd ? 31 : 50);
        int playerPanelY = topPos + (sd ? 8 : 13);
        int playerPanelWidth = sd ? 39 : 63;
        int playerPanelHeight = sd ? 52 : 84;
        FactoryGuiGraphics.of(graphics).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        FactoryGuiGraphics.of(graphics).blitSprite(sd ? LegacySprites.SQUARE_ENTITY_PANEL : LegacySprites.ENTITY_PANEL, playerPanelX, playerPanelY, playerPanelWidth, playerPanelHeight);
        Pose pose = minecraft.player.getPose();
        minecraft.player.setPose(Pose.STANDING);
        LegacyRenderUtil.renderEntityInInventoryFollowsMouse(graphics, playerPanelX, playerPanelY, playerPanelX + playerPanelWidth, playerPanelY + playerPanelHeight, sd ? 20 : 35, 0.0625f, i, j, minecraft.player);
        minecraft.player.setPose(pose);
        if (LegacyOptions.hasClassicCrafting()) {
            LegacyFontUtil.applySDFont(b -> graphics.drawString(this.font, this.title, leftPos + (sd ? 64 : 111), topPos + (sd ? 9 : 16), CommonColor.INVENTORY_GRAY_TEXT.get(), false));
            FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.SMALL_ARROW, leftPos + (sd ? 92 : 158), topPos + (sd ? 24 : 42), 16, 14);
        }
    }

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    public void renderLabels(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        ci.cancel();
        LegacyFontUtil.applySDFont(b -> guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
    }

    public boolean canReplace() {
        return Legacy4JClient.playerHasInfiniteMaterials() && canReplace;
    }

    public void setCanReplace(boolean canReplace) {
        this.canReplace = canReplace;
    }

    public Screen getReplacement() {
        return CreativeModeScreen.getActualCreativeScreenInstance(minecraft);
    }

    @Override
    public int getTipXDiff() {
        return -186;
    }

    @Override
    public boolean allowItemPopping() {
        return true;
    }
}
