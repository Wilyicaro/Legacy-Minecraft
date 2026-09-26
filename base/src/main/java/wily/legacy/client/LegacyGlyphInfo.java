package wily.legacy.client;

import com.mojang.blaze3d.font.GlyphInfo;

public class LegacyGlyphInfo implements GlyphInfo {

    private final float advance;
    private final float boldOffset;

    public LegacyGlyphInfo(float advance, float boldOffset) {
        this.advance = advance;
        this.boldOffset = boldOffset;
    }

    @Override
    public float getAdvance() {
        return advance;
    }

    @Override
    public float getBoldOffset() {
        return boldOffset;
    }
}
