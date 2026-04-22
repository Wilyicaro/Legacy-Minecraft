package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.font.GlyphInfo;
import net.minecraft.client.gui.font.providers.BitmapProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.MutableBitmapGlyph;

@Mixin(BitmapProvider.Glyph.class)
public class BitmapGlyphMixin implements MutableBitmapGlyph {
    @Unique
    private GlyphInfo l4j$info;


    @Override
    public void setGlyphInfo(GlyphInfo info) {
        this.l4j$info = info;
    }

    @ModifyReturnValue(method = "info", at = @At("RETURN"))
    private GlyphInfo info(GlyphInfo original) {
        return l4j$info == null ? original : l4j$info;
    }
}
