package wily.legacy.client.screen.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.screen.RenderableVList;
import wily.legacy.util.ScreenUtil;

import java.lang.reflect.Constructor;

public class BisectModCompat {
    private static final ResourceLocation BISECT_ICON = FactoryAPI.createLocation("bhmenu", "textures/gui/serverlistbanner/icon.png");
    private static final Component TITLE = Component.literal("Need a server?");
    private static final String ORDER_SCREEN = "com.bisecthosting.mods.bhmenu.modules.servercreatorbanner.screens.BHOrderScreen";

    public static AbstractButton createButton(RenderableVList list) {
        return new AbstractButton(0, 0, 270, 30, TITLE) {
            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                if (!list.accessor.getBoolean("allowButtonsWithIcons", true)) return;
                FactoryScreenUtil.enableBlend();
                FactoryGuiGraphics.of(guiGraphics).blit(BISECT_ICON, getX() + 5, getY() + 5, 0.0f, 0.0f, 20, 20, 20, 20);
                FactoryScreenUtil.disableBlend();
                if (Minecraft.getInstance().options.touchscreen().get() || isHovered) {
                    guiGraphics.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);
                }
            }

            @Override
            protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int color, int packedLight) {
                ScreenUtil.applySDFont(ignored -> ScreenUtil.renderScrollingString(guiGraphics, font, getMessage(), getX() + 35, getY(), getX() + getWidth(), getY() + getHeight(), packedLight, true));
            }

            @Override
            public void onPress() {
                Screen screen = createOrderScreen(list.getScreen());
                if (screen != null) Minecraft.getInstance().setScreen(screen);
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                defaultButtonNarrationText(narrationElementOutput);
            }
        };
    }

    private static Screen createOrderScreen(Screen parent) {
        try {
            Class<?> screen = Class.forName(ORDER_SCREEN);
            Constructor<?> constructor = screen.getConstructor(Screen.class);
            Object result = constructor.newInstance(parent);
            return result instanceof Screen s ? s : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}
