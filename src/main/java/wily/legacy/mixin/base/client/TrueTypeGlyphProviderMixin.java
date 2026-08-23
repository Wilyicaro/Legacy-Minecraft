//? if <=1.20.4 {
/*package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import org.lwjgl.stb.STBTTFontinfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.client.LegacyFontUtil;

import java.nio.ByteBuffer;

@Mixin(TrueTypeGlyphProvider.class)
public abstract class TrueTypeGlyphProviderMixin {
    @Unique
    private long l4j$fontAddress;

    @Inject(method = "<init>(Ljava/nio/ByteBuffer;Lorg/lwjgl/stb/STBTTFontinfo;FFFFLjava/lang/String;)V", at = @At("RETURN"), require = 0)
    private void captureLegacyFont(ByteBuffer buffer, STBTTFontinfo font, float size, float oversample, float shiftX, float shiftY, String skip, CallbackInfo ci) {
        if (buffer.capacity() != 4041932) return;
        l4j$fontAddress = font.address();
        LegacyFontUtil.registerLegacyFont(l4j$fontAddress);
    }

    @Inject(method = "close", at = @At("HEAD"), remap = false)
    private void releaseLegacyFont(CallbackInfo ci) {
        if (l4j$fontAddress != 0) LegacyFontUtil.unregisterLegacyFont(l4j$fontAddress);
    }
}
*///?}
