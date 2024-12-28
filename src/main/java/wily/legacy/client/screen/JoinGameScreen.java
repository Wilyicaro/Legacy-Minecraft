package wily.legacy.client.screen;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import wily.legacy.util.ScreenUtil;

public class JoinGameScreen extends PanelVListScreen{
    public static final Component JOIN_GAME = Component.translatable("legacy.menu.join_game");
    protected ScrollableRenderer scrollableRenderer =  new ScrollableRenderer(new LegacyScrollRenderer());
    protected final MultiLineLabel label;
    private final Button.OnPress connect;
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel,186);

    public JoinGameScreen(Screen parent, ServerData data, Button.OnPress connect) {
        super(parent, s-> Panel.centered(s,225, 177,0,34), JOIN_GAME);
        label = MultiLineLabel.create(Minecraft.getInstance().font,data.motd,tooltipBox.getWidth() - 10);
        this.connect = connect;
        if (data.players == null) return;
        for (GameProfile gameProfile : data.players.sample()) {
            renderableVList.addRenderable(new AbstractButton(0,0,210,30,Component.literal(gameProfile.getName())) {
                @Override
                public void onPress() {
                }

                @Override
                protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                    ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + 5, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), j, true);
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
        addRenderableWidget(Button.builder(JOIN_GAME,connect).bounds((width - 225) / 2, panel.getY() - 25,225,20).build());
    }

    @Override
    public void renderableVListInit() {
        tooltipBox.init();
        super.renderableVListInit();
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderDefaultBackground(guiGraphics, i, j, f);
        tooltipBox.render(guiGraphics,i,j,f);
        scrollableRenderer.render(guiGraphics,panel.x + panel.width + 3, panel.y + 13,tooltipBox.width - 10, tooltipBox.getHeight() - 44, ()-> label.renderLeftAligned(guiGraphics, panel.x + panel.width + 3, panel.y + 13, 12, 0xFFFFFF));
    }
}
