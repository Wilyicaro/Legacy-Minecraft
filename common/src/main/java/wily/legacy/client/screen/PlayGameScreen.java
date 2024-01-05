package wily.legacy.client.screen;

import net.minecraft.FileUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.apache.commons.compress.utils.FileNameUtils;
import org.jetbrains.annotations.Nullable;
import wily.legacy.LegacyMinecraft;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.util.ScreenUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class PlayGameScreen extends PanelBackgroundScreen{
    protected PlayGameScreen(Screen parent) {
        super(300,256,Component.translatable("legacy.menu.play_game"));
        this.parent = parent;

    }
    public static final WorldOptions TEST_OPTIONS = new WorldOptions("test1".hashCode(), true, false);
    private LanServerDetection.LanServerList lanServerList;
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    protected ServerSelectionList serverSelectionList;
    private ServerList servers;
    @Nullable
    private LanServerDetection.LanServerDetector lanServerDetector;
    public SaveSelectionList saveSelectionList;
    private CreationList creationList;
    protected final TabList tabList = new TabList().addTabButton(30,0,Component.translatable("legacy.menu.load"),b->{}).addTabButton(30,1,Component.translatable("legacy.menu.create"),b->{}).addTabButton(30,2,Component.translatable("legacy.menu.join"),b->{});
    public boolean init = false;

    @Override
    protected void init() {
        panel.height = Math.min(256,height-52);
        addRenderableWidget(tabList);
        super.init();
        addRenderableOnly(((guiGraphics, i, j, f) -> ScreenUtil.renderPanelRecess(guiGraphics, panel.x + 9, panel.y + 9, panel.width - 18, panel.height - 18, 2)));
        if (!init) {
            init = true;
            this.servers = new ServerList(this.minecraft);
            this.servers.load();
            this.lanServerList = new LanServerDetection.LanServerList();
            this.serverSelectionList = new ServerSelectionList(this, this.minecraft, this.width, panel.height - 24, panel.y + 12, 30);
            this.serverSelectionList.updateOnlineServers(this.servers);
            this.creationList = new CreationList(this, this.minecraft, this.width, panel.height - 24, panel.y + 12, 30);
        }
        try {
            this.lanServerDetector = new LanServerDetection.LanServerDetector(this.lanServerList);
            this.lanServerDetector.start();
        } catch (Exception exception) {
            LegacyMinecraft.LOGGER.warn("Unable to start LAN server detection: {}", exception.getMessage());
        }
        this.addRenderableWidget(this.serverSelectionList).setRectangle(this.width, panel.height - 24, 0, panel.y + 12);
        this.addRenderableWidget(this.saveSelectionList = new SaveSelectionList(this, this.minecraft, this.width, panel.height - 24, panel.y + 12, 30, "", this.saveSelectionList));
        this.addRenderableWidget(this.creationList).setRectangle(this.width, panel.height - 24, 0, panel.y + 12);
        tabList.init(panel.x,panel.y - 24,panel.width);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        children().forEach(c->{
            if (c.isMouseOver(d,e))c.mouseScrolled(d,e,f,g);
        });
        return false;
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics,false);
    }

    @Override
    public boolean charTyped(char c, int i) {
        return super.charTyped(c, i);
    }




    @Override
    public void removed() {
        if (this.saveSelectionList != null) {
            this.saveSelectionList.children().forEach(SaveSelectionList.Entry::close);
        }
        if (this.lanServerDetector != null) {
            this.lanServerDetector.interrupt();
            this.lanServerDetector = null;
        }
        this.pinger.removeAll();
        this.serverSelectionList.removed();
    }
    @Override
    public void tick() {
        super.tick();
        List<LanServer> list = this.lanServerList.takeDirtyServers();
        if (list != null) {
            this.serverSelectionList.updateNetworkServers(list);
        }
        this.pinger.tick();
    }
    

    private void refreshServerList() {
        rebuildWidgets();
    }

    private void deleteCallback(boolean bl) {
        ServerSelectionList.Entry entry = this.serverSelectionList.getSelected();
        if (bl && entry instanceof ServerSelectionList.OnlineServerEntry) {
            this.servers.remove(((ServerSelectionList.OnlineServerEntry)entry).getServerData());
            this.servers.save();
            this.serverSelectionList.setSelected(null);
            this.serverSelectionList.updateOnlineServers(this.servers);
        }
        this.minecraft.setScreen(this);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        tabList.controlTab(i,j,k);
        if (super.keyPressed(i, j, k)) {
            return true;
        }
        if (i == 294) {
            this.refreshServerList();
            return true;
        }
        if (this.serverSelectionList.getSelected() != null) {
            if (CommonInputs.selected(i)) {
                this.joinSelectedServer();
                return true;
            }
            return this.serverSelectionList.keyPressed(i, j, k);
        }
        return false;
    }
    public void joinSelectedServer() {
        ServerSelectionList.Entry entry = this.serverSelectionList.getSelected();
        if (entry instanceof ServerSelectionList.OnlineServerEntry) {
            this.join(((ServerSelectionList.OnlineServerEntry)entry).getServerData());
        } else if (entry instanceof ServerSelectionList.NetworkServerEntry) {
            LanServer lanServer = ((ServerSelectionList.NetworkServerEntry)entry).getServerData();
            this.join(new ServerData(lanServer.getMotd(), lanServer.getAddress(), ServerData.Type.LAN));
        }
    }

    private void join(ServerData serverData) {
        ConnectScreen.startConnecting(this, this.minecraft, ServerAddress.parseString(serverData.ip), serverData, false);
    }

    public void setSelected(ServerSelectionList.Entry entry) {
        this.serverSelectionList.setSelected(entry);
    }

    public ServerStatusPinger getPinger() {
        return this.pinger;
    }

    public ServerList getServers() {
        return servers;
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
                saveSelectionList.reloadWorldList();
            }));
        }
    }
}
