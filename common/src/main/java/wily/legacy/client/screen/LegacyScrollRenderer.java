package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.resources.ResourceLocation;

import static wily.legacy.LegacyMinecraftClient.*;

public class LegacyScrollRenderer {
    public static final ResourceLocation[] SCROLL_SPRITES = new ResourceLocation[]{SCROLL_UP,SCROLL_DOWN,SCROLL_LEFT,SCROLL_RIGHT};
    public final long[] lastScrolled = new long[4];

    public void updateScroll(ScreenDirection direction){
        lastScrolled[direction.ordinal()] = Util.getMillis();
    }
    public void renderScroll(GuiGraphics graphics, ScreenDirection direction, int x, int y){
        boolean h =direction.getAxis() == ScreenAxis.HORIZONTAL;
        RenderSystem.enableBlend();
        long l = lastScrolled[direction.ordinal()];
        if (l > 0) {
            float f = (Util.getMillis() - l) / 320f;
            float fade = f < 0.5f ? 1 - f * 2f : (f - 0.5f) * 2f;
            if (f >= 1.0f) lastScrolled[direction.ordinal()] = 0;
            RenderSystem.setShaderColor(1.0f,1.0f,1.0f, fade);
        }
        graphics.blitSprite(SCROLL_SPRITES[direction.ordinal()],x,y, h ? 6 : 13, h ? 11 : 7);
        if (l > 0) RenderSystem.setShaderColor(1.0f,1.0f,1.0f,1.0f);
        RenderSystem.disableBlend();
    }

}
