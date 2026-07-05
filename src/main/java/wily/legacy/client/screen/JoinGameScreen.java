package wily.legacy.client.screen;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.ScreenUtil;

public class JoinGameScreen extends PanelVListScreen{
    public static final Component JOIN_GAME = Component.translatable("legacy.menu.join_game");
    protected ScrollableRenderer scrollableRenderer =  new ScrollableRenderer(new LegacyScrollRenderer());
    protected MultiLineLabel label;
    private final ServerData data;
    private final Button.OnPress connect;
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, () -> accessor.getInteger("tooltipBox.width", 186));

    public JoinGameScreen(Screen parent, ServerData data, Button.OnPress connect) {
        super(parent, s-> Panel.centered(s,225, 177,0,34), JOIN_GAME);
        this.data = data;
        this.connect = connect;
        if (data.players == null) return;
        for (GameProfile gameProfile : data.players.sample()) {
            renderableVList.addRenderable(new AbstractButton(0,0,210,30,Component.literal(gameProfile.getName())) {
                @Override
                public void onPress() {
                }

                @Override
                protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                    ScreenUtil.applySDFont(ignored -> ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + 5, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), j, true));
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
        int buttonWidth = accessor.getInteger("joinButton.width", panel.width);
        int buttonHeight = accessor.getInteger("joinButton.height", 20);
        addRenderableWidget(accessor.putWidget("joinButton", Button.builder(JOIN_GAME,connect).bounds(accessor.getInteger("joinButton.x", (width - buttonWidth) / 2), accessor.getInteger("joinButton.y", panel.getY() - 25), buttonWidth, buttonHeight).build()));
    }

    @Override
    public void initRenderableVListEntry(RenderableVList renderableVList, Renderable renderable) {
        if (renderable instanceof AbstractButton widget) {
            //? if <=1.20.1 {
            /*((wily.factoryapi.base.client.WidgetAccessor) widget).setHeight(accessor.getInteger("buttonsHeight", 30));
            *///?} else {
            widget.setHeight(accessor.getInteger("buttonsHeight", 30));
            //?}
        }
    }

    @Override
    public void renderableVListInit() {
        tooltipBox.init();
        ScreenUtil.applySDFont(ignored -> label = MultiLineLabel.create(Minecraft.getInstance().font, data.motd, tooltipBox.getWidth() - 10));
        super.renderableVListInit();
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderDefaultBackground(guiGraphics, i, j, f);
        tooltipBox.render(guiGraphics,i,j,f);
        int lineHeight = LegacyOptions.getUIMode().isSD() ? 8 : 12;
        int visibleHeight = tooltipBox.getHeight() - (LegacyOptions.getUIMode().isSD() ? 20 : 44);
        scrollableRenderer.lineHeight = lineHeight;
        scrollableRenderer.scrolled.max = Math.max(0, label.getLineCount() - visibleHeight / lineHeight);
        ScreenUtil.applySDFont(ignored -> scrollableRenderer.render(guiGraphics,panel.x + panel.width + 3, panel.y + 13,tooltipBox.width - 10, visibleHeight, ()-> label.renderLeftAligned(guiGraphics, panel.x + panel.width + 3, panel.y + 13, lineHeight, 0xFFFFFF)));
    }
}
