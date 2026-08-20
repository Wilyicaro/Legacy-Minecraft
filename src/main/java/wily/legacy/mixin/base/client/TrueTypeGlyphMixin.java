package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.platform.NativeImage;
//? if <=1.20.4 {
/*import org.lwjgl.stb.STBTTFontinfo;
*///?}
import org.lwjgl.system.MemoryUtil;
//? if >=1.21 {
import org.lwjgl.util.freetype.FT_Face;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.util.client.LegacyFontUtil;

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

    //? if <=1.20.4 {
    /*@Redirect(method = "upload", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;copyFromFont(Lorg/lwjgl/stb/STBTTFontinfo;IIIFFFFII)V"))
    private void applyGamma(NativeImage image, STBTTFontinfo font, int glyphIndex, int width, int height, float scaleX, float scaleY, float shiftX, float shiftY, int startX, int startY) {
        image.copyFromFont(font, glyphIndex, width, height, scaleX, scaleY, shiftX, shiftY, startX, startY);
        if (LegacyFontUtil.isLegacyFont(font.address())) l4j$applyGamma(image);
    }
    *///?}

    //? if >=1.21 {
    @Redirect(method = "upload", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;copyFromFont(Lorg/lwjgl/util/freetype/FT_Face;I)Z"), require = 0)
    private boolean applyGamma(NativeImage image, FT_Face face, int glyphIndex) {
        if (!image.copyFromFont(face, glyphIndex)) return false;
        if (L4J$FONT_FAMILIES.contains(face.family_nameString())) l4j$applyGamma(image);
        return true;
    }
    //?}

    @Unique
    private static void l4j$applyGamma(NativeImage image) {
        long pointer = ((NativeImageAccessor) (Object) image).l4j$getPixels();
        int length = image.getWidth() * image.getHeight();
        for (int i = 0; i < length; i++) {
            int coverage = MemoryUtil.memGetByte(pointer + i) & 255;
            MemoryUtil.memPutByte(pointer + i, L4J$GAMMA[coverage]);
        }
    }
}
