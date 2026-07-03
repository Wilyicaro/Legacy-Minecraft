package wily.legacy.mixin.base;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.ScreenUtil;

@Mixin(ClientTextTooltip.class)

public class ClientTextTooltipMixin {
    @Shadow @Final private FormattedCharSequence text;

    @Inject(method = "renderText", at = @At("HEAD"), cancellable = true)
    public void renderText(Font font, int i, int j, Matrix4f matrix4f, MultiBufferSource.BufferSource bufferSource, CallbackInfo ci) {
        if (!LegacyOptions.legacyItemTooltips.get()) return;
        ci.cancel();
        Integer color = ScreenUtil.tooltipTextColorOverride;
        boolean hasStyleOverride = CommonColor.BLUE.isOverridden() || CommonColor.DARK_PURPLE.isOverridden() || CommonColor.RED.isOverridden() || CommonColor.GRAY.isOverridden();
        FormattedCharSequence text = color == null && !hasStyleOverride ? this.text : sink -> this.text.accept((index, style, codePoint) -> sink.accept(index, tooltipStyle(style, color, ScreenUtil.tooltipTextColorOverrideForcesStyle), codePoint));
        font.drawInBatch(text, (float)i, (float)j, color == null ? -1 : color, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
    }
    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    public void getHeight(CallbackInfoReturnable<Integer> cir) {
        if (LegacyOptions.legacyItemTooltips.get()) cir.setReturnValue(12);
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
