package wily.legacy.mixin;

import com.mojang.blaze3d.font.GlyphInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import wily.legacy.LegacyMinecraftClient;

@Mixin(GlyphInfo.class)
public interface GlyphInfoMixin {
    @Overwrite
    default float getShadowOffset() {
        return LegacyMinecraftClient.FONT_SHADOW_OFFSET;
    }
}
