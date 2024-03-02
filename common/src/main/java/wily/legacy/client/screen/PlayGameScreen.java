package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.commons.compress.utils.FileNameUtils;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.util.ScreenUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class PlayGameScreen extends PanelVListScreen{
    public PlayGameScreen(Screen parent, int initialTab) {
        super(parent,300,256,Component.translatable("legacy.menu.play_game"));
    }
    public PlayGameScreen(Screen parent) {
        this(parent,0);
    }
    protected final TabList tabList = new TabList().add(30,0,Component.translatable("legacy.menu.load"), b-> repositionElements()).add(30,1,Component.translatable("legacy.menu.create"), b-> repositionElements()).add(30,2,Component.translatable("legacy.menu.join"), b-> repositionElements());
    public boolean isLoading = false;
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    protected final ServerSelectionList serverSelectionList = new ServerSelectionList();
    public final SaveSelectionList saveSelectionList = new SaveSelectionList(this);
    private final CreationList creationList = new CreationList();


    @Override
    protected void init() {
        panel.height = Math.min(256,height-52);
        addRenderableWidget(tabList);
        panel.init();
        addRenderableOnly(panel);
        addRenderableOnly(((guiGraphics, i, j, f) -> {
            ScreenUtil.renderPanelRecess(guiGraphics, panel.x + 9, panel.y + 9, panel.width - 18, panel.height - 18, 2);
            if (isLoading)
                ScreenUtil.drawGenericLoading(guiGraphics, panel.x + 112 , panel.y + 66);
        }));
        getRenderableVList().init(this,panel.x + 15,panel.y + 15,270, panel.height - 10);
        tabList.init(panel.x,panel.y - 24,panel.width);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics,false);
    }

    @Override
    public RenderableVList getRenderableVList() {
        if (tabList.selectedTab == 2) return serverSelectionList;
        else if (tabList.selectedTab == 1) return creationList;
        return saveSelectionList;
    }

    @Override
    public void removed() {
        if (this.saveSelectionList != null) {
            SaveSelectionList.resetIconCache();
        }
        if (serverSelectionList.lanServerDetector != null) {
            serverSelectionList.lanServerDetector.interrupt();
            serverSelectionList.lanServerDetector = null;
        }
        this.pinger.removeAll();
    }
    @Override
    public void tick() {
        super.tick();
        List<LevelSummary> summaries = saveSelectionList.pollLevelsIgnoreErrors();
        if (summaries != saveSelectionList.currentlyDisplayedLevels) {
            saveSelectionList.fillLevels("",summaries);
            repositionElements();
        }
        List<LanServer> list = serverSelectionList.lanServerList.takeDirtyServers();
        if (list != null) {
            if (serverSelectionList.lanServers == null || !new HashSet<>(serverSelectionList.lanServers).containsAll(list)) {
                serverSelectionList.lanServers = list;
                serverSelectionList.updateServers();
                rebuildWidgets();
            }
        }
        if (tabList.selectedTab == 2) minecraft.getRealms32BitWarningStatus().showRealms32BitWarningIfNeeded(this);
        this.pinger.tick();
    }




    @Override
    public boolean keyPressed(int i, int j, int k) {
        tabList.controlTab(i);
        tabList.directionalControlTab(i);
        if (super.keyPressed(i, j, k)) {
            return true;
        }
        if (i == 294) {
            if (tabList.selectedTab == 0) {
                saveSelectionList.reloadSaveList();
            } else if (tabList.selectedTab == 2) {
                serverSelectionList.updateServers();
            }
            this.rebuildWidgets();
            return true;
        }
        return false;
    }

    public ServerStatusPinger getPinger() {
        return this.pinger;
    }

    public ServerList getServers() {
        return serverSelectionList.servers;
    }
    public void onFilesDrop(List<Path> list) {
        if (tabList.selectedTab == 0) {
            for (Path path : list) {
                if (!path.getFileName().toString().endsWith(".mcsave") && !path.getFileName().toString().endsWith(".zip")) return;
            }
            String string = list.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.joining(", "));
            minecraft.setScreen(new ConfirmationScreen(this, Component.translatable("legacy.menu.import_save"), Component.translatable("legacy.menu.import_save_message", string), (b) -> {
                list.forEach(p -> {
                    try {
                        LegacyMinecraftClient.importSaveFile(minecraft, new FileInputStream(p.toFile()), FileNameUtils.getBaseName(p.getFileName().toString()));
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                });
                minecraft.setScreen(this);
                saveSelectionList.reloadSaveList();
            }));
        }
    }
}
