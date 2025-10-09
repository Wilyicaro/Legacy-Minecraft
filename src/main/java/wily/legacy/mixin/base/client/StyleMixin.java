package wily.legacy.mixin.base.client;

import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.client.LegacyFontUtil;

@Mixin(Style.class)
public class StyleMixin {
    @Inject(method = "getFont", at = @At("RETURN"), cancellable = true)
    private void getFont(CallbackInfoReturnable<FontDescription> cir){
        if(LegacyFontUtil.defaultFontOverride != null && cir.getReturnValue().equals(FontDescription.DEFAULT)) cir.setReturnValue(LegacyFontUtil.defaultFontOverride);
    }
}
