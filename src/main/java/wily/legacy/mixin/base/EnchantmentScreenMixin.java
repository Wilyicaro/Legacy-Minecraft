package wily.legacy.mixin.base;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.client.CommonColor;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.Optional;

import static wily.legacy.Legacy4J.MOD_ID;
import static wily.legacy.util.LegacySprites.SHIELD_SLOT;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
    @Shadow protected abstract void renderBook(GuiGraphics arg, int i, int j, float g);
    @Shadow private BookModel bookModel;

    public EnchantmentScreenMixin(EnchantmentMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        this.bookModel = new BookModel(this.minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));
        imageWidth = 215;
        imageHeight = 217;
        inventoryLabelX = 14;
        inventoryLabelY = 104;
        titleLabelX = 14;
        titleLabelY = 10;
        super.init();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0){
                LegacySlotDisplay.override(s, 19, 66, new LegacySlotDisplay() {
                    @Override
                    public ResourceLocation getIconSprite() {
                        return s.hasItem() ? null : LegacySprites.ENCHANTING_SLOT;
                    }
                });
            } else if (i == 1) {
                LegacySlotDisplay.override(s, 50, 66);
            } else if (i < menu.slots.size() - 9) {
                LegacySlotDisplay.override(s, 14 + (s.getContainerSlot() - 9) % 9 * 21,115 + (s.getContainerSlot() - 9) / 9 * 21);
            } else {
                LegacySlotDisplay.override(s, 14 + s.getContainerSlot() * 21,185);
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

    @Inject(method = "mouseClicked",at = @At("HEAD"), cancellable = true)
    public void mouseClicked(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        for (int l = 0; l < 3; ++l) {
            double f = d - (leftPos + 80.5);
            double g = e - (topPos + 23.5 + 21 * l);
            if (!(f >= 0.0) || !(g >= 0.0) || !(f < 120) || !(g < 21) || !this.menu.clickMenuButton(this.minecraft.player, l)) continue;
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, l);
            cir.setReturnValue(true);
            return;
        }
       cir.setReturnValue(super.mouseClicked(d, e, i));
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(UIDefinition.Accessor.of(this).getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos, imageWidth,imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 79,  topPos+ 22, 123, 66);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 2,topPos + 4,0);
        guiGraphics.pose().scale(1.25f,1.25f,1.25f);
        this.renderBook(guiGraphics, 0, 0, f);
        guiGraphics.pose().popPose();
        EnchantmentNames.getInstance().initSeed(this.menu.getEnchantmentSeed());
        int m = this.menu.getGoldCount();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 80.5f,topPos+ 2.5f,0f);
        for (int n = 0; n < 3; ++n) {
            guiGraphics.pose().translate(0f,21f,0f);
            int enchantCost = this.menu.costs[n];
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ENCHANTMENT_BUTTON_EMPTY, 0, 0, 120, 21);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.DISABLED_LEVEL_SPRITES[n], -1, -1, 24, 24);
            if (enchantCost == 0)
                continue;
            String string = "" + enchantCost;
            int r = 86 - this.font.width(string);
            FormattedText formattedText = EnchantmentNames.getInstance().getRandomName(this.font, r);
            int s = CommonColor.ENCHANTMENT_TEXT.get();
            if (!(m >= n + 1 && this.minecraft.player.experienceLevel >= enchantCost || this.minecraft.player.getAbilities().instabuild)) {
                guiGraphics.drawWordWrap(this.font, formattedText, 24, 3, r, (s & 0xFEFEFE) >> 1);
                s = CommonColor.INSUFFICIENT_EXPERIENCE_TEXT.get();
            } else {
                double t = i - (leftPos + 80.5);
                double u = j - (topPos + 23.5 + 21 * n);
                if (t >= 0 && u >= 0 && t < 120 && u < 21) {
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ENCHANTMENT_BUTTON_SELECTED, 0, 0, 120, 21);
                    s = CommonColor.HIGHLIGHTED_ENCHANTMENT_TEXT.get();
                } else {
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ENCHANTMENT_BUTTON_ACTIVE, 0, 0, 120, 21);
                }
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.ENABLED_LEVEL_SPRITES[n], -1, -1, 24, 24);
                guiGraphics.drawWordWrap(this.font, formattedText, 24, 3, r, s);
                s = CommonColor.EXPERIENCE_TEXT.get();
            }
            guiGraphics.drawString(this.font, string, 120 - this.font.width(string), 12, s);
        }
        guiGraphics.pose().popPose();
    }
    @Inject(method = "render",at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        super.render(guiGraphics, i, j, f);
        this.renderTooltip(guiGraphics, i, j);
        boolean bl = this.minecraft.player.getAbilities().instabuild;
        int k = this.menu.getGoldCount();
        for (int l = 0; l < 3; ++l) {
            int m = this.menu.costs[l];
            Optional<Holder.Reference<Enchantment>> optional = this.minecraft.level.registryAccess()./*? if <1.21.2 {*//*registryOrThrow(Registries.ENCHANTMENT).getHolder(this.menu.enchantClue[l])*//*?} else {*/lookupOrThrow(Registries.ENCHANTMENT).get(this.menu.enchantClue[l])/*?}*/;
            int n = this.menu.levelClue[l];
            int o = l + 1;
            double t = i - (leftPos + 80.5);
            double u = j - (topPos + 23.5 + 21 * l);
            if (!(t >= 0 && u >= 0 && t < 120 && u < 21) || m <= 0 || n < 0 || optional.isEmpty()) continue;
            ArrayList<Component> list = Lists.newArrayList();
            optional.get().value();
            list.add(Component.translatable("container.enchant.clue", /*? if <1.20.5 {*//*optional.get().value().getFullname(n)*//*?} else {*/Enchantment.getFullname(optional.get(), n)/*?}*/).withStyle(ChatFormatting.WHITE));
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
            guiGraphics.renderComponentTooltip(this.font, list, i, j);
            break;
        }
    }

}
