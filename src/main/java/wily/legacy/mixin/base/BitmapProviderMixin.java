package wily.legacy.mixin.base;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.providers.BitmapProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;

import java.io.IOException;
import java.io.InputStream;

@Mixin(BitmapProvider.Definition.class)
public abstract class BitmapProviderMixin {
    @Shadow protected abstract int getActualGlyphWidth(NativeImage nativeImage, int i, int j, int k, int l);

    @Shadow @Final private int[][] codepointGrid;

    @Shadow @Final private int ascent;

    @Shadow @Final private int height;

    @Shadow @Final private ResourceLocation file;

    @Inject(method = "load", at = @At("HEAD"), cancellable = true)
    private void load(ResourceManager resourceManager, CallbackInfoReturnable<GlyphProvider> cir) throws IOException {
        ResourceLocation resourceLocation = this.file.withPrefix("textures/");
        InputStream inputStream = resourceManager.open(resourceLocation);

        BitmapProvider var22;
        try {
            NativeImage nativeImage = NativeImage.read(NativeImage.Format.RGBA, inputStream);
            int i = nativeImage.getWidth();
            int j = nativeImage.getHeight();
            int k = i / this.codepointGrid[0].length;
            int l = j / this.codepointGrid.length;
            float f = (float)this.height / (float)l;
            CodepointMap<BitmapProvider.Glyph> codepointMap = new CodepointMap<>(BitmapProvider.Glyph[]::new, BitmapProvider.Glyph[][]::new);
            int m = 0;

            while(true) {
                if (m >= this.codepointGrid.length) {
                    var22 = new BitmapProvider(nativeImage, codepointMap);
                    break;
                }

                int n = 0;
                int[] var13 = this.codepointGrid[m];
                int var14 = var13.length;

                for(int var15 = 0; var15 < var14; ++var15) {
                    int o = var13[var15];
                    int p = n++;
                    if (o != 0) {
                        int q = this.getActualGlyphWidth(nativeImage, k, l, p, m);
                        BitmapProvider.Glyph glyph = codepointMap.put(o, new BitmapProvider.Glyph(f, nativeImage, p * k, m * l, k, l, (int)(0.5 + (double)((float)q * f)) + 1, this.ascent){
                            @Override
                            public float getAdvance() {
                                return (q + 1) * f;
                            }
                        });
                        if (glyph != null) {
                            Legacy4J.LOGGER.warn("Codepoint '{}' declared multiple times in {}", Integer.toHexString(o), resourceLocation);
                        }
                    }
                }

                ++m;
            }
        } catch (Throwable var21) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable var20) {
                    var21.addSuppressed(var20);
                }
            }
            cir.setReturnValue(null);
            throw var21;
        }

        if (inputStream != null) {
            inputStream.close();
        }

        cir.setReturnValue(var22);
    }
}
