package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(ClientTextTooltip.class)

public class ClientTextTooltipMixin {
    @Shadow
    @Final
    private FormattedCharSequence text;

    @Inject(method = "extractText", at = @At("HEAD"), cancellable = true)
    public void renderText(GuiGraphicsExtractor GuiGraphicsExtractor, Font font, int i, int j, CallbackInfo ci) {
        if (!LegacyOptions.legacyItemTooltips.get()) return;
        ci.cancel();
        Integer color = LegacyRenderUtil.tooltipTextColorOverride;
        boolean hasStyleOverride = CommonColor.BLUE.isOverridden() || CommonColor.DARK_PURPLE.isOverridden() || CommonColor.RED.isOverridden() || CommonColor.GRAY.isOverridden();
        FormattedCharSequence text = color == null && !hasStyleOverride ? this.text : sink -> this.text.accept((index, style, codePoint) ->
                sink.accept(index, tooltipStyle(style, color, LegacyRenderUtil.tooltipTextColorOverrideForcesStyle), codePoint));
        GuiGraphicsExtractor.text(font, text, i, j, color == null ? -1 : color, false);
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    public void getHeight(Font font, CallbackInfoReturnable<Integer> cir) {
        if (LegacyOptions.legacyItemTooltips.get()) cir.setReturnValue(LegacyOptions.getUIMode().isSD() ? 9 : 12);
    }

    private static Style tooltipStyle(Style style, Integer color, boolean force) {
        if (force && color != null) return style.withColor(color & 0x00FFFFFF);
        CommonColor commonColor = commonStyleColor(style);
        if (commonColor != null && commonColor.isOverridden()) return style.withColor(commonColor.get() & 0x00FFFFFF);
        return color != null && style.getColor() == null ? style.withColor(color & 0x00FFFFFF) : style;
    }

    private static CommonColor commonStyleColor(Style style) {
        TextColor textColor = style.getColor();
        if (textColor == null) return null;
        int color = textColor.getValue() & 0x00FFFFFF;
        if (isCommonColor(color, CommonColor.BLUE, 0x5555FF)) return CommonColor.BLUE;
        if (isCommonColor(color, CommonColor.DARK_PURPLE, 0xAA00AA)) return CommonColor.DARK_PURPLE;
        if (isCommonColor(color, CommonColor.RED, 0xFF5555)) return CommonColor.RED;
        if (isCommonColor(color, CommonColor.GRAY, 0xAAAAAA)) return CommonColor.GRAY;
        return null;
    }

    private static boolean isCommonColor(int value, CommonColor color, int vanillaColor) {
        return value == vanillaColor || value == (color.defaultValue & 0x00FFFFFF) || color.isOverridden() && value == (color.get() & 0x00FFFFFF);
    }
}
