package wily.legacy.client.control.tooltip;

import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.base.client.MinecraftAccessor;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.control.ControlType;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.Map;
import java.util.function.BiConsumer;

public class ControlTooltipRenderer implements Renderable {
    public static final FactoryEvent<BiConsumer<Screen, ControlTooltips>> SCREEN_EVENT = new FactoryEvent<>(e -> (screen, event) -> e.invokeAll(c -> c.accept(screen, event)));
    public static final FactoryEvent<BiConsumer<Gui, ControlTooltips>> GUI_EVENT = new FactoryEvent<>(e -> (screen, event) -> e.invokeAll(c -> c.accept(screen, event)));

    private static final ControlTooltipRenderer INSTANCE = new ControlTooltipRenderer();
    public final ControlTooltips tooltips = new ControlTooltips();
    protected final Map<Component, Icon> renderTooltips = new Object2ReferenceLinkedOpenHashMap<>();
    private final Minecraft minecraft = Minecraft.getInstance();

    public static ControlTooltipRenderer getInstance() {
        return INSTANCE;
    }

    public static ControlTooltipRenderer of(Object o) {
        return o instanceof ControlTooltip.Listener e ? e.getRenderer() : getInstance();
    }

    public boolean allowPressed() {
        return minecraft.screen != null;
    }

    public ControlTooltips tooltips() {
        return tooltips;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int i, int j, float f) {
        boolean inGame = minecraft.screen == null;
        renderTooltips.clear();
        if (!LegacyOptions.displayControlTooltips.get() || inGame && (!LegacyOptions.displayHUD.get() || minecraft.options.hideGui || !LegacyOptions.inGameTooltips.get()))
            return;

        for (ControlTooltipList list : tooltips) {
            for (ControlTooltip tooltip : list.tooltips) {
                Component action;
                Icon icon;
                if ((action = tooltip.getAction()) == null || (icon = tooltip.getIcon()) == null) continue;

                if (LegacyOptions.getUIMode().isSD())
                    action = action.copy().withStyle(action.getStyle().withFont(LegacyFontUtil.MOJANGLES_11_FONT));
                renderTooltips.compute(action, (k, existingIcon) -> existingIcon == null ? icon : existingIcon.equals(icon) || !LegacyOptions.displayMultipleControlsFromAction.get() ? existingIcon : CompoundIcon.of(existingIcon, ControlTooltip.SPACE_ICON, icon));
            }
        }
        graphics.pose().pushMatrix();
        boolean left = LegacyOptions.controlTooltipDisplay.get().isLeft();
        float hudDistance = Math.max(0.0f, LegacyOptions.hudDistance.get().floatValue() - 0.5f) * 2;
        float hudDiff = 1.0f - hudDistance;
        float xDiff = 32 - 30 * hudDiff;
        graphics.pose().translate(left ? xDiff : graphics.guiWidth() - xDiff, graphics.guiHeight() - (29 - (15 - ControlType.getActiveType().iconHeight()) / 2 - 16 * hudDiff));

        renderTooltips.forEach((action, icon) -> {
            if (left) {
                int controlWidth = icon.render(graphics, 0, 0, allowPressed(), ColorUtil.withAlpha(0xFFFFFF, ControlTooltip.getAlpha()), false);
                if (controlWidth > 0) {
                    graphics.pose().translate(LegacyOptions.getUIMode().isSD() ? 0 : 2, 0.0f);
                    graphics.text(minecraft.font, action, controlWidth, 0, ColorUtil.withAlpha(CommonColor.ACTION_TEXT.get(), ControlTooltip.getAlpha()));
                    graphics.pose().translate(controlWidth + minecraft.font.width(action) + 10, 0);
                }
            } else {
                int controlWidth = icon.getWidth();
                if (controlWidth > 0) {
                    graphics.pose().translate(-controlWidth - minecraft.font.width(action), 0);
                    icon.render(graphics, 0, 0, allowPressed(), ColorUtil.withAlpha(0xFFFFFF, ControlTooltip.getAlpha()), false);
                    graphics.pose().translate(LegacyOptions.getUIMode().isSD() ? 0 : 2, 0.0f);
                    graphics.text(minecraft.font, action, controlWidth, 0, ColorUtil.withAlpha(CommonColor.ACTION_TEXT.get(), ControlTooltip.getAlpha()));
                    graphics.pose().translate(-12, 0);
                }
            }
        });
        graphics.pose().popMatrix();
    }

    public void press(MouseButtonEvent event, boolean clicked) {
        if (!MinecraftAccessor.getInstance().hasGameLoaded() || renderTooltips.isEmpty()) return;
        boolean left = LegacyOptions.controlTooltipDisplay.get().isLeft();
        float hudDistance = Math.max(0.0f, LegacyOptions.hudDistance.get().floatValue() - 0.5f) * 2;
        float hudDiff = 1.0f - hudDistance;
        float xDiff = 32 - 30 * hudDiff;
        float tooltipX = left ? xDiff : minecraft.getWindow().getGuiScaledWidth() - xDiff;
        float tooltipY = minecraft.getWindow().getGuiScaledHeight() - (29 - (15 - ControlType.getActiveType().iconHeight()) / 2 - 16 * hudDiff);
        for (Map.Entry<Component, Icon> e : renderTooltips.entrySet()) {
            int tooltipWidth = e.getValue().getWidth() + minecraft.font.width(e.getKey());
            if (!left) tooltipX -= tooltipWidth;
            if (LegacyRenderUtil.isMouseOver(event.x(), event.y(), tooltipX, tooltipY - 1, tooltipWidth, 9)) {
                if (clicked)
                    e.getValue().clickIfInside(tooltipX, event);
                else
                    e.getValue().release(event);
                return;
            }
            tooltipX += left ? tooltipWidth + 12 : -12;
        }
    }
}
