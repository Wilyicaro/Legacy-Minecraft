package wily.legacy.mixin.base.client.enchantment;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.ArrayList;
import java.util.Optional;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
    public EnchantmentScreenMixin(EnchantmentMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Shadow
    protected abstract void renderBook(GuiGraphics arg, int m, int n);

    @Inject(method = "init", at = @At("HEAD"))
    public void init(CallbackInfo ci) {
        boolean sd = LegacyOptions.getUIMode().isSD();
        imageWidth = sd ? 130 : 215;
        imageHeight = sd ? 140 : 217;
        inventoryLabelX = sd ? 7 : 14;
        inventoryLabelY = sd ? 67 : 104;
        titleLabelX = sd ? 7 : 14;
        titleLabelY = sd ? 5 : 10;
        int slotsSize = sd ? 13 : 21;
        LegacySlotDisplay defaultSlotsDisplay = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return slotsSize;
            }
        };
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, sd ? 10 : 19, sd ? 49 : 66, new LegacySlotDisplay() {
                    @Override
                    public ResourceLocation getIconSprite() {
                        return s.hasItem() ? null : LegacySprites.ENCHANTING_SLOT;
                    }

                    @Override
                    public int getWidth() {
                        return sd ? 16 : 21;
                    }
                });
            } else if (i == 1) {
                LegacySlotDisplay.override(s, sd ? 31 : 50, sd ? 49 : 66, new LegacySlotDisplay() {
                    @Override
                    public int getWidth() {
                        return sd ? 16 : 21;
                    }
                });
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, inventoryLabelX + (s.getContainerSlot() - 9) % 9 * slotsSize, (sd ? 77 : 115) + (s.getContainerSlot() - 9) / 9 * slotsSize, defaultSlotsDisplay);
            } else {
                LegacySlotDisplay.override(s, inventoryLabelX + s.getContainerSlot() * slotsSize, sd ? 122 : 185, defaultSlotsDisplay);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void mouseClicked(MouseButtonEvent event, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        boolean sd = LegacyOptions.getUIMode().isSD();
        int buttonHeight = (sd ? 15 : 21);
        for (int l = 0; l < 3; ++l) {
            double f = event.x() - (leftPos + (sd ? 52.5 : 80.5));
            double g = event.y() - (topPos + (sd ? 17.5 : 23.5) + buttonHeight * l);
            if (!(f >= 0.0) || !(g >= 0.0) || !(f < (sd ? 70 : 120)) || !(g < buttonHeight) || !this.menu.clickMenuButton(this.minecraft.player, l))
                continue;
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, l);
            cir.setReturnValue(true);
            return;
        }
        cir.setReturnValue(super.mouseClicked(event, bl));
    }

    @ModifyArg(method = "renderBook", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;submitBookModelRenderState(Lnet/minecraft/client/model/BookModel;Lnet/minecraft/resources/ResourceLocation;FFFIIII)V"), index = 2)
    public float changeBookScale(float original) {
        return LegacyOptions.getUIMode().isSD() ? original : original * 1.25f;
    }

    @ModifyArg(method = "renderBook", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;submitBookModelRenderState(Lnet/minecraft/client/model/BookModel;Lnet/minecraft/resources/ResourceLocation;FFFIIII)V"), index = 7)
    public int changeBookX1(int original) {
        return original + (LegacyOptions.getUIMode().isSD() ? 0 : 6);
    }

    @ModifyArg(method = "renderBook", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;submitBookModelRenderState(Lnet/minecraft/client/model/BookModel;Lnet/minecraft/resources/ResourceLocation;FFFIIII)V"), index = 8)
    public int changeBookY1(int original) {
        return original + (LegacyOptions.getUIMode().isSD() ? 0 : 9);
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIAccessor.of(this).getResourceLocation("imageSprite", sd ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + (sd ? 51 : 79), topPos + (sd ? 16 : 22), sd ? 73 : 123, sd ? 48 : 66);
        this.renderBook(guiGraphics, leftPos + (sd ? -5 : 8), topPos + (sd ? 0 : 12));
        EnchantmentNames.getInstance().initSeed(this.menu.getEnchantmentSeed());
        int m = this.menu.getGoldCount();
        int buttonWidth = sd ? 70 : 120;
        int buttonHeight = sd ? 15 : 21;
        int levelSize = sd ? 16 : 24;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(leftPos + (LegacyRenderUtil.hasHorizontalArtifacts() ? sd ? 52.4f : 80.4f : sd ? 52.5f : 80.5f), topPos + (sd ? 17.4f : 23.4f) - buttonHeight);
        for (int n = 0; n < 3; ++n) {
            guiGraphics.pose().translate(0f, buttonHeight);
            int enchantCost = this.menu.costs[n];
            FactoryGuiGraphics.of(guiGraphics).blitSprite(sd ? LegacySprites.SMALL_ENCHANTMENT_BUTTON_EMPTY : LegacySprites.ENCHANTMENT_BUTTON_EMPTY, 0, 0, buttonWidth, buttonHeight);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.DISABLED_LEVEL_SPRITES[n], sd ? 0 : -1, sd ? 0 : -1, levelSize, levelSize);
            if (enchantCost == 0) {
                continue;
            }
            String string = "" + enchantCost;
            int r = (sd ? 58 : 86) - this.font.width(string);
            FormattedText formattedText = EnchantmentNames.getInstance().getRandomName(this.font, r);
            int s = CommonColor.ENCHANTMENT_TEXT.get();
            if (!(m >= n + 1 && this.minecraft.player.experienceLevel >= enchantCost || this.minecraft.player.getAbilities().instabuild)) {
                guiGraphics.drawWordWrap(this.font, formattedText, sd ? 16 : 24, sd ? 2 : 3, r, (s & 0xFEFEFE) >> 1, false);
                s = CommonColor.INSUFFICIENT_EXPERIENCE_TEXT.get();
            } else {
                double t = i - (leftPos + (sd ? 52.5 : 80.5));
                double u = j - (topPos + (sd ? 17.5 : 23.5) + buttonHeight * n);
                if (t >= 0 && u >= 0 && t < buttonWidth && u < buttonHeight) {
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(sd ? LegacySprites.SMALL_ENCHANTMENT_BUTTON_SELECTED : LegacySprites.ENCHANTMENT_BUTTON_SELECTED, 0, 0, buttonWidth, buttonHeight);
                    s = CommonColor.HIGHLIGHTED_ENCHANTMENT_TEXT.get();
                } else {
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(sd ? LegacySprites.SMALL_ENCHANTMENT_BUTTON_ACTIVE : LegacySprites.ENCHANTMENT_BUTTON_ACTIVE, 0, 0, buttonWidth, buttonHeight);
                }
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ENABLED_LEVEL_SPRITES[n], sd ? 0 : -1, sd ? 0 : -1, levelSize, levelSize);
                guiGraphics.drawWordWrap(this.font, formattedText, sd ? 16 : 24, sd ? 2 : 3, r, s, false);
                s = CommonColor.EXPERIENCE_TEXT.get();
            }
            int color = s;
            LegacyFontUtil.applySDFont(b -> guiGraphics.drawString(this.font, string, buttonWidth - this.font.width(string) - (sd ? 2 : 0), sd ? 8 : 12, color));
        }
        guiGraphics.pose().popMatrix();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        LegacyFontUtil.applySDFont(b -> super.renderLabels(guiGraphics, i, j));
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        boolean sd = LegacyOptions.getUIMode().isSD();
        super.render(guiGraphics, i, j, f);
        this.renderTooltip(guiGraphics, i, j);
        boolean bl = this.minecraft.player.getAbilities().instabuild;
        int k = this.menu.getGoldCount();
        int buttonHeight = sd ? 15 : 21;
        for (int l = 0; l < 3; ++l) {
            int m = this.menu.costs[l];
            Optional<Holder.Reference<Enchantment>> optional = this.minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(this.menu.enchantClue[l]);
            int n = this.menu.levelClue[l];
            int o = l + 1;
            double t = i - (leftPos + (sd ? 52.5 : 80.5));
            double u = j - (topPos + (sd ? 17.5 : 23.5) + buttonHeight * l);
            if (!(t >= 0 && u >= 0 && t < (sd ? 70 : 120) && u < buttonHeight) || m <= 0 || n < 0 || optional.isEmpty()) continue;
            ArrayList<Component> list = Lists.newArrayList();
            optional.get().value();
            list.add(Component.translatable("container.enchant.clue", Enchantment.getFullname(optional.get(), n)).withStyle(ChatFormatting.WHITE));
            if (!bl) {
                list.add(CommonComponents.EMPTY);
                if (this.minecraft.player.experienceLevel < m) {
                    list.add(Component.translatable("container.enchant.level.requirement", this.menu.costs[l]).withStyle(ChatFormatting.RED));
                } else {
                    MutableComponent mutableComponent = o == 1 ? Component.translatable("container.enchant.lapis.one") : Component.translatable("container.enchant.lapis.many", o);
                    list.add(mutableComponent.withStyle(k >= o ? ChatFormatting.GRAY : ChatFormatting.RED));
                    MutableComponent mutableComponent2 = o == 1 ? Component.translatable("container.enchant.level.one") : Component.translatable("container.enchant.level.many", o);
                    list.add(mutableComponent2.withStyle(ChatFormatting.GRAY));
                }
            }
            guiGraphics.setComponentTooltipForNextFrame(this.font, list, i, j);
            break;
        }
    }

}
