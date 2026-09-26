package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(targets = "com.mojang.blaze3d.font.TrueTypeGlyphProvider$Glyph$1")
public class TrueTypeGlyphMixin {
    @Unique
    private static final byte[] L4J$GAMMA = l4j$createGamma();
    @Unique
    private static final Set<String> L4J$FONT_FAMILIES = Set.of("DFDotDotGothic16", "MJNgai PRC", "DFYuanMedium-B5", "KoreanERCCRegular");

    @Unique
    private static byte[] l4j$createGamma() {
        byte[] gamma = new byte[256];
        for (int i = 0; i < gamma.length; i++) gamma[i] = (byte) Math.round(255.0 * Math.pow(i / 255.0, 0.26));
        return gamma;
    }

    @Redirect(method = "upload", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;copyFromFont(Lorg/lwjgl/util/freetype/FT_Face;I)Z"))
    private boolean applyGamma(NativeImage image, FT_Face face, int glyphIndex) {
        if (!image.copyFromFont(face, glyphIndex)) return false;
        if (!L4J$FONT_FAMILIES.contains(face.family_nameString())) return true;
        long pointer = image.getPointer();
        int length = image.getWidth() * image.getHeight();
        for (int i = 0; i < length; i++) {
            int coverage = MemoryUtil.memGetByte(pointer + i) & 255;
            MemoryUtil.memPutByte(pointer + i, L4J$GAMMA[coverage]);
        }
        return true;
    }
}
