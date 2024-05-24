package wily.legacy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.List;

@Mixin(BeaconScreen.class)
public abstract class BeaconScreenMixin extends AbstractContainerScreen<BeaconMenu> {
    @Shadow @Final private List<BeaconScreen.BeaconButton> beaconButtons;

    @Shadow protected abstract <T extends AbstractWidget & BeaconScreen.BeaconButton> void addBeaconButton(T abstractWidget);

    @Mutable
    @Shadow @Final private static ResourceLocation CONFIRM_SPRITE;

    @Shadow @Final private static Component PRIMARY_EFFECT_LABEL;

    @Shadow @Final private static Component SECONDARY_EFFECT_LABEL;

    public BeaconScreenMixin(BeaconMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }
    private BeaconScreen self(){
        return (BeaconScreen)(Object) this;
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        CONFIRM_SPRITE = new ResourceLocation(Legacy4J.MOD_ID,"container/beacon_check");
        imageWidth = 260;
        imageHeight = 255;
        super.init();
        this.beaconButtons.clear();
        this.addBeaconButton(self().new BeaconConfirmButton(this.leftPos + 202, this.topPos + 127){
            @Override
            protected void renderIcon(GuiGraphics guiGraphics) {
                RenderSystem.enableBlend();
                super.renderIcon(guiGraphics);
                RenderSystem.disableBlend();
            }
        });

        int j;
        int l;
        MobEffect mobEffect;
        BeaconScreen.BeaconPowerButton beaconPowerButton;
        for(int i = 0; i <= 2; ++i) {
            j = BeaconBlockEntity.BEACON_EFFECTS[i].length;
            for(l = 0; l < j; ++l) {
                mobEffect = BeaconBlockEntity.BEACON_EFFECTS[i][l];
                beaconPowerButton = self().new BeaconPowerButton(this.leftPos + 59 + (j > 1 ? l * 27 : 13), this.topPos + 38 + i * 30, mobEffect, true, i);
                beaconPowerButton.active = false;
                this.addBeaconButton(beaconPowerButton);
            }
        }

        j = BeaconBlockEntity.BEACON_EFFECTS[3].length + 1;
        int k = j > 1 ? 0 : 13;

        for(l = 0; l < j - 1; ++l) {
            mobEffect = BeaconBlockEntity.BEACON_EFFECTS[3][l];
            beaconPowerButton = self().new BeaconPowerButton(this.leftPos + 164 + l * 27 + k, this.topPos + 68, mobEffect, false, 3);
            beaconPowerButton.active = false;
            this.addBeaconButton(beaconPowerButton);
        }

        BeaconScreen.BeaconPowerButton beaconPowerButton2 = self().new BeaconUpgradePowerButton(this.leftPos + 165 + (j - 1) * 24 - k / 2, this.topPos + 68, BeaconBlockEntity.BEACON_EFFECTS[0][0]);
        beaconPowerButton2.visible = false;
        this.addBeaconButton(beaconPowerButton2);
    }

    @Inject(method = "renderLabels",at = @At("HEAD"), cancellable = true)
    public void renderLabels(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        ci.cancel();
        guiGraphics.drawString(this.font, PRIMARY_EFFECT_LABEL, 9 + (121 - font.width(PRIMARY_EFFECT_LABEL)) /2, 13, 0x383838, false);
        guiGraphics.drawString(this.font, SECONDARY_EFFECT_LABEL, 133 + (121 - font.width(SECONDARY_EFFECT_LABEL)) /2, 13, 0x383838, false);
    }
    private static final Item[] DISPLAY_ITEMS = new Item[]{Items.NETHERITE_INGOT,Items.EMERALD,Items.DIAMOND,Items.GOLD_INGOT,Items.IRON_INGOT};
    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        ScreenUtil.renderPanel(guiGraphics,leftPos,topPos,imageWidth,imageHeight,2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics,leftPos + 8,topPos + 9,120, 115,2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics,leftPos + 132,topPos + 9,120, 115,2f);
        guiGraphics.blitSprite(LegacySprites.BEACON_1,leftPos + 32, topPos + 39, 20, 19);
        guiGraphics.blitSprite(LegacySprites.BEACON_2,leftPos + 32, topPos + 69, 20, 19);
        guiGraphics.blitSprite(LegacySprites.BEACON_3,leftPos + 32, topPos + 97, 20, 19);
        guiGraphics.blitSprite(LegacySprites.BEACON_4,leftPos + 180, topPos + 42, 20, 19);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 15, topPos + 129, 100.0F);
        guiGraphics.pose().scale(7/6f, 7/6f,7/6f);
        for (Item displayItem : DISPLAY_ITEMS) {
            guiGraphics.renderItem(new ItemStack(displayItem), 0, 0);
            guiGraphics.pose().translate(18,0,0);
        }
        guiGraphics.pose().popPose();
    }
}
