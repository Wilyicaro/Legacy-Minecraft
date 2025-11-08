package wily.legacy.mixin.base.client.gui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    @Shadow
    @Nullable
    private Component footer;

    @Shadow
    @Nullable
    private Component header;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    protected abstract List<PlayerInfo> getPlayerInfos();

    @Shadow
    public abstract Component getNameForDisplay(PlayerInfo playerInfo);

    @Inject(method = "render", at = @At("HEAD"))
    public void render(GuiGraphics guiGraphics, int i, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
        int width = 0;
        int height = 0;


        List<PlayerInfo> list = this.getPlayerInfos();
        int j = this.minecraft.font.width(" ");
        int k = 0;
        int l = 0;

        for (PlayerInfo playerInfo : list) {
            Component component = this.getNameForDisplay(playerInfo);
            k = Math.max(k, this.minecraft.font.width(component));
            int n;
            if (objective != null) {
                ScoreHolder scoreHolder = ScoreHolder.fromGameProfile(playerInfo.getProfile());
                ReadOnlyScoreInfo readOnlyScoreInfo = scoreboard.getPlayerScoreInfo(scoreHolder, objective);

                if (objective.getRenderType() != ObjectiveCriteria.RenderType.HEARTS) {
                    NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.PLAYER_LIST_DEFAULT);
                    n = this.minecraft.font.width(ReadOnlyScoreInfo.safeFormatValue(readOnlyScoreInfo, numberFormat));
                    l = Math.max(l, n > 0 ? j + n : 0);
                }
            }
        }

        int o = list.size();
        int p = o;

        int q;
        for (q = 1; p > 20; p = (o + q - 1) / q) {
            ++q;
        }

        boolean bl = this.minecraft.isLocalServer() || this.minecraft.getConnection().getConnection().isEncrypted();
        int r;
        if (objective != null) {
            if (objective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
                r = 90;
            } else {
                r = l;
            }
        } else {
            r = 0;
        }

        int n = Math.min(q * ((bl ? 9 : 0) + k + r + 13), i - 50) / q;
        int u = n * q + (q - 1) * 5;
        List<FormattedCharSequence> list3 = null;
        if (this.header != null) {
            list3 = this.minecraft.font.split(this.header, i - 50);

            for (FormattedCharSequence formattedCharSequence : list3) {
                u = Math.max(u, this.minecraft.font.width(formattedCharSequence));
            }
        }

        List<FormattedCharSequence> list4 = null;
        if (this.footer != null) {
            list4 = this.minecraft.font.split(this.footer, i - 50);

            for (FormattedCharSequence formattedCharSequence2 : list4) {
                u = Math.max(u, this.minecraft.font.width(formattedCharSequence2));
            }
        }

        if (list3 != null) {
            height += list3.size() * 9 + 1;
        }

        height += p * 9 + 1;
        width += u + 10;

        if (list4 != null) {
            height += list4.size() * 9 + 1;
        }

        LegacyRenderUtil.blitTranslucentSprite(guiGraphics, LegacySprites.POINTER_PANEL, (guiGraphics.guiWidth() - width) / 2, 6, width, height + 8);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0))
    public void noHeaderBackground(GuiGraphics instance, int i, int j, int k, int l, int m) {
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 1))
    public void renderBackground(GuiGraphics instance, int i, int j, int k, int l, int m) {
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 2))
    public void noPlayerBackground(GuiGraphics instance, int i, int j, int k, int l, int m) {
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 3))
    public void noFooterBackground(GuiGraphics instance, int i, int j, int k, int l, int m) {
    }
}
