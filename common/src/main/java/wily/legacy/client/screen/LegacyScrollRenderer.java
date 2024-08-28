package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.util.LegacySprites;

public class LegacyScrollRenderer {
    public static final ResourceLocation[] SCROLLS = new ResourceLocation[]{LegacySprites.SCROLL_UP, LegacySprites.SCROLL_DOWN, LegacySprites.SCROLL_LEFT, LegacySprites.SCROLL_RIGHT};
    public final long[] lastScrolled = new long[4];
    public long lastScroll = 0;
    public ScreenDirection lastDirection;

    public void updateScroll(ScreenDirection direction){
        lastDirection = direction;
        lastScroll = (lastScrolled[direction.ordinal()] = Util.getMillis());
    }
    public void renderScroll(PoseStack graphics, ScreenDirection direction, int x, int y){
        boolean h =direction.getAxis() == ScreenAxis.HORIZONTAL;
        RenderSystem.enableBlend();
        long l = lastScrolled[direction.ordinal()];
        if (l > 0) {
            float f = (Util.getMillis() - l) / 320f;
            float fade = Math.min(1.0f,f < 0.5f ? 1 - f * 2f : (f - 0.5f) * 2f);
            RenderSystem.setShaderColor(1.0f,1.0f,1.0f, fade);
        }
        LegacyGuiGraphics.of(graphics).blitSprite(SCROLLS[direction.ordinal()],x,y, h ? 6 : 13, h ? 11 : 7);
        if (l > 0) RenderSystem.setShaderColor(1.0f,1.0f,1.0f,1.0f);
        RenderSystem.disableBlend();
    }

}
