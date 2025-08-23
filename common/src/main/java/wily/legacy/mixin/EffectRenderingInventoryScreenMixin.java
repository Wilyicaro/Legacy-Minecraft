package wily.legacy.mixin;

import com.google.common.collect.Ordering;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.util.ScreenUtil;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mixin(EffectRenderingInventoryScreen.class)
public abstract class EffectRenderingInventoryScreenMixin extends AbstractContainerScreen {

    @Shadow protected abstract Component getEffectName(MobEffectInstance mobEffectInstance);

    public EffectRenderingInventoryScreenMixin(AbstractContainerMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Inject(method = "render",at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        super.render(guiGraphics, i, j, f);
        int x = this.leftPos + this.imageWidth + 3;
        int l = this.width - x;
        Collection<MobEffectInstance> collection = this.minecraft.player.getActiveEffects();
        if (collection.isEmpty() || l < 32) {
            return;
        }
        boolean bl = l >= 129;
        int m = 31;
        if (imageHeight < collection.size() * 28) {
            m = imageHeight / collection.size();
        }
        List<MobEffectInstance> iterable = Ordering.natural().sortedCopy(collection);
        int y = topPos + imageHeight - 28;
        for (MobEffectInstance mobEffectInstance : iterable) {
            ScreenUtil.renderPointerPanel(guiGraphics,x,y,bl ? 129 : 28, 28);
            if (bl) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(x + 25, y + 7,0);
                Legacy4JClient.applyFontOverrideIf(minecraft.getWindow().getHeight() <= 720, LegacyIconHolder.MOJANGLES_11_FONT, b->{
                    Component effect = this.getEffectName(mobEffectInstance);
                    if (!b) guiGraphics.pose().scale(2/3f,2/3f,2/3f);
                    guiGraphics.drawString(this.font, effect, 0, 0, 0xFFFFFF);
                    guiGraphics.pose().translate(0, 10 * (b ? 1 : 1.5f),0);
                    guiGraphics.drawString(this.font, MobEffectUtil.formatDuration(mobEffectInstance, 1.0f, this.minecraft.level.tickRateManager().tickrate()), 0,0, 0x7F7F7F);
                });
                guiGraphics.pose().popPose();
            }
            guiGraphics.blit(x + (bl ? 3 : 5), y + 5, 0, 18, 18, minecraft.getMobEffectTextures().get(mobEffectInstance.getEffect()));
            y -= m;
        }
        if (!bl && i >= x && i <= x + 28) {
            int n = topPos + imageHeight - 28;
            MobEffectInstance mobEffectInstance = null;
            for (MobEffectInstance mobEffectInstance2 : iterable) {
                if (j >= n && j <= n + m) {
                    mobEffectInstance = mobEffectInstance2;
                }
                n -= m;
            }
            if (mobEffectInstance != null) {
                List<Component> list = List.of(this.getEffectName(mobEffectInstance), MobEffectUtil.formatDuration(mobEffectInstance, 1.0f, this.minecraft.level.tickRateManager().tickrate()));
                guiGraphics.renderTooltip(this.font, list, Optional.empty(), i, j);
            }
        }

    }
}
