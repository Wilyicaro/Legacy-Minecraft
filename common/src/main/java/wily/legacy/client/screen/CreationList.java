package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.LegacyMinecraft;
import wily.legacy.LegacyMinecraftClient;

import java.io.IOException;
import java.util.function.Consumer;

public class CreationList extends RenderableVList{
    static final String TUTORIAL_FOLDER_NAME = "Tutorial";
    private PlayGameScreen screen;
    protected final Minecraft minecraft;

    public CreationList() {
        layoutSpacing(l->0);
        minecraft = Minecraft.getInstance();
        addCreationButton(new ResourceLocation(LegacyMinecraft.MOD_ID,"creation_list/create_world"),Component.translatable("legacy.menu.create_world"),c-> LegacyCreateWorldScreen.openFresh(this.minecraft, screen));
        addCreationButton(new ResourceLocation(LegacyMinecraft.MOD_ID,"creation_list/tutorial"),Component.translatable("legacy.menu.play_tutorial"),c-> {
            try {
                minecraft.createWorldOpenFlows().loadLevel(screen,LegacyMinecraftClient.importSaveFile(minecraft,minecraft.getResourceManager().getResourceOrThrow(new ResourceLocation(LegacyMinecraft.MOD_ID,"tutorial/tutorial.mcsave")).open(),TUTORIAL_FOLDER_NAME));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        addCreationButton(new ResourceLocation(LegacyMinecraft.MOD_ID,"creation_list/add_server"),Component.translatable("legacy.menu.add_server"),c-> {
            this.minecraft.setScreen(new ServerEditScreen(screen, new ServerData(I18n.get("selectServer.defaultName"), "", ServerData.Type.OTHER), true));
        });
    }

    @Override
    public void init(Screen screen, int leftPos, int topPos, int listWidth, int listHeight) {
        if (screen instanceof PlayGameScreen s) this.screen = s;
        super.init(screen, leftPos, topPos, listWidth, listHeight);
    }

    private void addCreationButton(ResourceLocation iconSprite, Component message, Consumer<AbstractButton> onPress){
        addRenderable(new AbstractButton(0,0,270,30,message) {
            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                super.renderWidget(guiGraphics, i, j, f);
                RenderSystem.enableBlend();
                guiGraphics.blitSprite(iconSprite, getX() + 5, getY() + 5, 20, 20);
                RenderSystem.disableBlend();
                if (minecraft.options.touchscreen().get().booleanValue() || isHovered) {
                    guiGraphics.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);
                }
            }
            @Override
            protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                int k = this.getX() + 35;
                int l = this.getX() + this.getWidth();
                TickBox.renderScrollingString(guiGraphics, font, this.getMessage(), k, this.getY(), l, this.getY() + this.getHeight(), j, true);
            }
            @Override
            public void onPress() {
                onPress.accept(this);
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                defaultButtonNarrationText(narrationElementOutput);
            }
        });
    }

    public PlayGameScreen getScreen() {
        return this.screen;
    }


}
