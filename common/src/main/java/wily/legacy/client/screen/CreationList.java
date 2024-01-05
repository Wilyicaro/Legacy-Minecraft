package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import wily.legacy.LegacyMinecraft;
import wily.legacy.LegacyMinecraftClient;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

public class CreationList extends SlotButtonList<CreationList.CreationListEntry> {
    static final String TUTORIAL_FOLDER_NAME = "Tutorial";
    static final Logger LOGGER = LogUtils.getLogger();
    private final PlayGameScreen screen;

    public CreationList(PlayGameScreen playGameScreen, Minecraft minecraft, int i, int j, int k, int l, int m) {
        super(()->playGameScreen.tabList.selectedTab == 1,minecraft, i, j, k, l, m);
        this.screen = playGameScreen;
        addEntry(new CreationListEntry(this,new ResourceLocation(LegacyMinecraft.MOD_ID,"creation_list/create_world"),Component.translatable("legacy.menu.create_world"),c-> LegacyCreateWorldScreen.openFresh(this.minecraft, screen)));
        addEntry(new CreationListEntry(this,new ResourceLocation(LegacyMinecraft.MOD_ID,"creation_list/tutorial"),Component.translatable("legacy.menu.play_tutorial"),c-> {
            try {
                minecraft.createWorldOpenFlows().loadLevel(screen,LegacyMinecraftClient.importSaveFile(minecraft,minecraft.getResourceManager().getResourceOrThrow(new ResourceLocation(LegacyMinecraft.MOD_ID,"tutorial/tutorial.mcsave")).open(),TUTORIAL_FOLDER_NAME));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
        addEntry(new CreationListEntry(this,new ResourceLocation(LegacyMinecraft.MOD_ID,"creation_list/add_server"),Component.translatable("legacy.menu.add_server"),c-> {
            this.minecraft.setScreen(new ServerEditScreen(screen, new ServerData(I18n.get("selectServer.defaultName"), "", ServerData.Type.OTHER), true));
        }));
        setRenderBackground(false);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (!active.get()) return false;
        Optional<CreationListEntry> optional;
        if (CommonInputs.selected(i) && (optional = this.getSelectedOpt()).isPresent()) {
            optional.get().onPress.accept(optional.get());
            return true;
        }
        return super.keyPressed(i, j, k);
    }


    @Override
    protected int getScrollbarPosition() {
        return super.getScrollbarPosition() + 20;
    }

    @Override
    public int getRowWidth() {
        return 270;
    }


    public Optional<CreationListEntry> getSelectedOpt() {
       Entry<CreationListEntry> entry = this.getSelected();
        if (entry instanceof CreationListEntry e)
            return Optional.of(e);
        return Optional.empty();
    }

    public PlayGameScreen getScreen() {
        return this.screen;
    }



    @Environment(value=EnvType.CLIENT)
    public final class CreationListEntry extends SlotButtonList.SlotEntry<CreationListEntry> {
        protected final Minecraft minecraft;
        protected final PlayGameScreen screen;
        protected final ResourceLocation icon;
        private final Consumer<CreationListEntry> onPress;
        private long lastClickTime;
        private final Component message;
        public CreationListEntry(CreationList worldSelectionList2, ResourceLocation icon, Component message, Consumer<CreationListEntry> onPress) {
            this.minecraft = worldSelectionList2.minecraft;
            this.screen = worldSelectionList2.getScreen();
            this.message = message;
            this.icon = icon;
            this.onPress = onPress;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", message);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
            guiGraphics.drawString(this.minecraft.font, message, k + 35,j + (itemHeight - 7) / 2, 0xFFFFFF, true);
            RenderSystem.enableBlend();
            guiGraphics.blitSprite(this.icon, k + 5, j + 5, 20, 20);
            RenderSystem.disableBlend();
            if (this.minecraft.options.touchscreen().get().booleanValue() || bl) {
                guiGraphics.fill(k + 5, j + 5, k + 25, j + 25, -1601138544);
            }
        }

        @Override
        public boolean mouseClicked(double d, double e, int i) {
            if (!active.get()) return false;
            CreationList.this.setSelected(this);
            if (d - (double) CreationList.this.getRowLeft() <= 32.0) {
                onPress.accept(this);
                return true;
            }
            if (Util.getMillis() - this.lastClickTime < 250L) {
                onPress.accept(this);
                return true;
            }
            this.lastClickTime = Util.getMillis();
            return true;
        }
    }

}
