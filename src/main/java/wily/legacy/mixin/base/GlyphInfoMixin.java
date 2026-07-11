package wily.legacy.mixin.base;

import com.mojang.blaze3d.font.GlyphInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;

@Mixin(GlyphInfo.class)
public interface GlyphInfoMixin extends GlyphInfo {
    /**
     * @author Wilyicaro
     * @reason Legacy Edition Accuracy
     */
    @Overwrite
    default float getShadowOffset() {
        return Legacy4JClient.getFontShadowOffset();
    }
    /**
     * @author Wilyicaro
     * @reason Legacy Edition Accuracy
     */
    @Overwrite
    default float getAdvance(boolean bl) {
        return this.getAdvance() + (bl ? this.getBoldOffset() : 0.0f) - (Legacy4JClient.legacyFont && LegacyOptions.legacyFont.get() || !LegacyOptions.legacyFont.get() ? 0 : 0.5f);
    }
}
