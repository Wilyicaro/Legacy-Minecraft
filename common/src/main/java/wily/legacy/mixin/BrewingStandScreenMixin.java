package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

@Mixin(BrewingStandScreen.class)
public abstract class BrewingStandScreenMixin extends AbstractContainerScreen<BrewingStandMenu> {
    private static final ResourceLocation FUEL_LENGTH_SPRITE = new ResourceLocation(Legacy4J.MOD_ID,"container/fuel_length");
    private static final ResourceLocation BREW_PROGRESS_SPRITE = new ResourceLocation(Legacy4J.MOD_ID,"container/brew_progress");
    private static final ResourceLocation BUBBLES_SPRITE = new ResourceLocation(Legacy4J.MOD_ID,"container/bubbles");

    @Shadow @Final private static int[] BUBBLELENGTHS;

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
    }

    @Override
    public void renderBackground(PoseStack poseStack) {
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int i, int j) {
        super.renderLabels(poseStack, i, j);
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.BREWING_COIL_FLAME, 43, 42,51, 33);
        int fuel = this.menu.getFuel();
        int n = Mth.clamp((27 * fuel + 20 - 1) / 20, 0, 27);
        poseStack.pose().pushPose();
        poseStack.pose().translate(65.5f,66, 0f);
        if (n > 0) {
            LegacyGuiGraphics.of(poseStack).blitSprite(FUEL_LENGTH_SPRITE, 27, 6, 0, 0, 0, 0, n, 6);
        }
        poseStack.pose().popPose();
    }

    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(PoseStack poseStack, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.SMALL_PANEL,leftPos,topPos,imageWidth,imageHeight);
        poseStack.pose().pushPose();
        poseStack.pose().translate(leftPos + 58.5f,topPos + 22.5, 0f);
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.BREWING_SLOTS, 0, 0,96, 96);
        poseStack.pose().popPose();
        int o;
        if ((o = this.menu.getBrewingTicks()) > 0) {
            poseStack.pose().pushPose();
            poseStack.pose().translate(leftPos + 121.5f,topPos + 22.5, 0f);
            poseStack.pose().scale(0.5f,0.5f,0.5f);
            int p = (int)(84.0f * (1.0f - (float)o / 400.0f));
            if (p > 0)
                LegacyGuiGraphics.of(poseStack).blitSprite(BREW_PROGRESS_SPRITE, 27, 84, 0, 0, 0, 0, 27, p);
            poseStack.pose().popPose();
            poseStack.pose().pushPose();
            poseStack.pose().translate(leftPos + 71f,topPos + 21, 0f);
            poseStack.pose().scale(1.5f,1.5f,1.5f);
            if ((p = BUBBLELENGTHS[o / 2 % 7]) > 0) {
                LegacyGuiGraphics.of(poseStack).blitSprite(BUBBLES_SPRITE, 12, 29, 0, 29 - p, 0, 29 - p, 12, p);
            }
            poseStack.pose().popPose();
        }
    }
}
