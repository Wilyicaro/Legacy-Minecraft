package wily.legacy.mixin;

import com.mojang.blaze3d.font.GlyphInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import wily.legacy.Legacy4JClient;

@Mixin(GlyphInfo.class)
public interface GlyphInfoMixin {
    /**
     * @author Wilyicaro
     * @reason Legacy Edition Accuracy
     */
    @Overwrite
    default float getShadowOffset() {
        return Legacy4JClient.FONT_SHADOW_OFFSET;
    }
}
