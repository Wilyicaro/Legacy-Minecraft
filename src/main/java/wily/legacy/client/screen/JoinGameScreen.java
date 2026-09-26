package wily.legacy.client.screen;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import wily.legacy.util.client.LegacyRenderUtil;

public class JoinGameScreen extends PanelVListScreen {
    public static final Component JOIN_GAME = Component.translatable("legacy.menu.join_game");
    protected final MultiLineLabel label;
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, 186);
    private final Button.OnPress connect;
    protected ScrollableRenderer scrollableRenderer = new ScrollableRenderer(new LegacyScrollRenderer());

    public JoinGameScreen(Screen parent, ServerData data, Button.OnPress connect) {
        super(parent, s -> Panel.centered(s, 225, 177, 0, 34), JOIN_GAME);
        label = MultiLineLabel.create(Minecraft.getInstance().font, data.motd, tooltipBox.getWidth() - 10);
        this.connect = connect;
        if (data.players == null) return;
        for (NameAndId gameProfile : data.players.sample()) {
            renderableVList.addRenderable(new AbstractButton(0, 0, 210, 30, Component.literal(gameProfile.name())) {
                @Override
                public void onPress(InputWithModifiers input) {
                }

                @Override
                protected void extractContents(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
                    LegacyRenderUtil.renderScrollingString(GuiGraphicsExtractor, Minecraft.getInstance().font, this.getMessage(), this.getX() + 5, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()), true);
                }

                protected void renderScrollingString(GuiGraphicsExtractor GuiGraphicsExtractor, Font font, int i, int j) {
                    LegacyRenderUtil.renderScrollingString(GuiGraphicsExtractor, font, this.getMessage(), this.getX() + 5, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), j, true);
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    defaultButtonNarrationText(narrationElementOutput);
                }
            });
        }
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(JOIN_GAME, connect).bounds((width - 225) / 2, panel.getY() - 25, 225, 20).build());
    }

    @Override
    public void renderableVListInit() {
        tooltipBox.init();
        super.renderableVListInit();
    }

    @Override
    public void renderDefaultBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        super.renderDefaultBackground(GuiGraphicsExtractor, i, j, f);
        tooltipBox.extractRenderState(GuiGraphicsExtractor, i, j, f);
        scrollableRenderer.extractRenderState(GuiGraphicsExtractor, panel.x + panel.width + 3, panel.y + 13, tooltipBox.width - 10, tooltipBox.getHeight() - 44, () -> label.visitLines(net.minecraft.client.gui.TextAlignment.LEFT, panel.x + panel.width + 3, panel.y + 13, 12, GuiGraphicsExtractor.textRenderer()));
    }
}
