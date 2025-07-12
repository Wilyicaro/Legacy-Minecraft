package wily.legacy.mixin.base.client.gui;

//? if >=1.21.1 {
//?}
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
        import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
        import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
        import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Mixin(SubtitleOverlay.class)
public class SubtitleOverlayMixin {

    @Shadow @Final private Minecraft minecraft;

    @Shadow private boolean isListening;

    //? if >=1.20.5 {
    @Shadow @Final private List<SubtitleOverlay.Subtitle> audibleSubtitles;
    //?}

    @Shadow @Final private List<SubtitleOverlay.Subtitle> subtitles;

    @Unique
    private SubtitleOverlay self(){
        return (SubtitleOverlay) (Object) this;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();
        SoundManager soundManager = this.minecraft.getSoundManager();
        if (!this.isListening && this.minecraft.options.showSubtitles().get()) {
            soundManager.addListener(self());
            this.isListening = true;
        } else if (this.isListening && !this.minecraft.options.showSubtitles().get()) {
            soundManager.removeListener(self());
            this.isListening = false;
        }

        if (this.isListening) {
            var list = /*? if <1.20.5 {*//*subtitles*//*?} else {*/audibleSubtitles/*?}*/;
            Vec3 vec3 = /*? if <1.20.5 {*//*new Vec3(this.minecraft.player.getX(), this.minecraft.player.getEyeY(), this.minecraft.player.getZ())*//*?} else {*/soundManager.getListenerTransform().position()/*?}*/;
            Vec3 vec32 = /*? if <1.20.5 {*//*(new Vec3(0.0F, 0.0F, -1.0F)).xRot(-this.minecraft.player.getXRot() * ((float)Math.PI / 180F)).yRot(-this.minecraft.player.getYRot() * ((float)Math.PI / 180F))*//*?} else {*/soundManager.getListenerTransform().forward()/*?}*/;
            Vec3 vec33 = /*? if <1.20.5 {*//*vec32.cross((new Vec3((double)0.0F, (double)1.0F, (double)0.0F)).xRot(-this.minecraft.player.getXRot() * ((float)Math.PI / 180F)).yRot(-this.minecraft.player.getYRot() * ((float)Math.PI / 180F)))*//*?} else {*/soundManager.getListenerTransform().right()/*?}*/;

            //? if >=1.20.5 {
            this.audibleSubtitles.clear();
            for(SubtitleOverlay.Subtitle subtitle : this.subtitles) {
                if (subtitle.isAudibleFrom(vec3)) {
                    this.audibleSubtitles.add(subtitle);
                }
            }
            //?}

            if (!list.isEmpty()) {
                int j = 0;
                double d = this.minecraft.options.notificationDisplayTime().get();
                Iterator<SubtitleOverlay.Subtitle> iterator = list.iterator();

                while(iterator.hasNext()) {
                    SubtitleOverlay.Subtitle subtitle2 = iterator.next();
                    //? if >=1.20.5 {
                    subtitle2.purgeOldInstances(3000.0 * d);
                    //?}
                    if (/*? if <1.20.5 {*//*subtitle2.getTime() + (double)3000.0F * d <= (double)Util.getMillis()*//*?} else {*/!subtitle2.isStillActive()/*?}*/) {
                        iterator.remove();
                    } else {
                        j = Math.max(j, this.minecraft.font.width(subtitle2.getText()));
                    }
                }

                j += 36;
                int lineHeight = 12;
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(guiGraphics.guiWidth() - 10, (float)(guiGraphics.guiHeight() - 35));
                int height = list.size() * 12;
                LegacyRenderUtil.renderPointerPanel(guiGraphics, -j, -height, j, height + 10);
                guiGraphics.pose().translate( - (j / 2.0f) - 2.0f, 0 );
                for(SubtitleOverlay.Subtitle subtitle2 : list) {
                    Component component = subtitle2.getText();
                    //? if >=1.20.5 {
                    SubtitleOverlay.SoundPlayedAt soundPlayedAt = subtitle2.getClosest(vec3);
                    if (soundPlayedAt == null) continue;
                    //?}

                    Vec3 vec34 = /*? if <1.20.5 {*//*subtitle2.getLocation()*//*?} else {*/soundPlayedAt.location()/*?}*/.subtract(vec3).normalize();
                    double e = vec33.dot(vec34);
                    double f = vec32.dot(vec34);
                    boolean bl = f > (double)0.5F;
                    int l = j / 2;
                    Objects.requireNonNull(this.minecraft.font);
                    int n = lineHeight / 2;
                    int o = this.minecraft.font.width(component);
                    int p = Mth.floor(Mth.clampedLerp(255.0F, 75.0F, (Util.getMillis() - /*? if <1.20.5 {*//*subtitle2.getTime()*//*?} else {*/soundPlayedAt.time()/*?}*/) / 3000.0 * d));
                    int q = p << 16 | p << 8 | p;

                    int r = q - 16777216;
                    if (!bl && e != 0) {
                        FactoryGuiGraphics.of(guiGraphics).setBlitColor(1.0f,1.0f, 1.0f, p / 255f);
                        FactoryGuiGraphics.of(guiGraphics).blitSprite(e > 0 ? LegacySprites.SCROLL_RIGHT : LegacySprites.SCROLL_LEFT, e > 0 ? l - 8 : -l + 4, -n - 2, 6, 11);
                        FactoryGuiGraphics.of(guiGraphics).clearBlitColor();
                    }
                    guiGraphics.drawString(this.minecraft.font, component, -o / 2, -n, r);
                    guiGraphics.pose().translate(0, -(lineHeight + 1));
                }
                guiGraphics.pose().popMatrix();
            }
        }
    }
}
