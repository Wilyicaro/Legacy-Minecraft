package wily.legacy.client.screen.compat;

import com.bisecthosting.mods.bhmenu.ModRef;
import com.bisecthosting.mods.bhmenu.ModRoot;
import com.bisecthosting.mods.bhmenu.modules.servercreatorbanner.screens.BHOrderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import wily.legacy.client.screen.IconButton;
import wily.legacy.client.screen.ServerRenderableList;

public class BisectModCompat {
    private static final Identifier BISECT_ICON = ModRef.res("textures/gui/serverlistbanner/icon.png");

    public static AbstractWidget createButton(ServerRenderableList list) {
        return new IconButton(list, 0, 0, 270, 30, ModRoot.INSTANCE.modules.serverCreatorBanner.getTitle()) {
            @Override
            public void onPress(InputWithModifiers input) {
                Minecraft.getInstance().setScreen(new BHOrderScreen(list.getScreen()));
            }

            @Override
            public void renderIcon(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int width, int height) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, BISECT_ICON, getX() + x, getY() + y, 0.0f, 0.0f, width, height, width, height);
            }

            @Override
            protected void renderScrollingString(GuiGraphicsExtractor GuiGraphicsExtractor, Font font, int i, int j) {
                super.renderScrollingString(GuiGraphicsExtractor, font, i, j);
            }
        };
    }
}
