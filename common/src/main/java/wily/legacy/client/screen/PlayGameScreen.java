package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.commons.compress.utils.FileNameUtils;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.ScreenUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static wily.legacy.client.screen.ControlTooltip.CONTROL_ACTION_CACHE;

public class PlayGameScreen extends PanelVListScreen{
    public PlayGameScreen(Screen parent, int initialTab) {
        super(parent,300,256,Component.translatable("legacy.menu.play_game"));
        tabList.selectedTab = -1;
        tabList.tabButtons.get(initialTab).onPress();
        Supplier<Boolean> saveOptions = ()-> saveRenderableList.renderables.stream().anyMatch(r-> r instanceof GuiEventListener l && l.isFocused());
        controlTooltipRenderer.add(()->ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O,true) : ControllerBinding.UP_BUTTON.bindingState.getIcon(true),()->saveOptions.get() || serverRenderableList.renderables.stream().anyMatch(r-> serverRenderableList.renderables.indexOf(r) > 1 && r instanceof GuiEventListener l && l.isFocused()) ? CONTROL_ACTION_CACHE.getUnchecked(saveOptions.get() ? "legacy.menu.save_options" : "legacy.menu.server_options") : null);
    }
    public PlayGameScreen(Screen parent) {
        this(parent,0);
    }
    protected final TabList tabList = new TabList().add(30,0,Component.translatable("legacy.menu.load"), b-> repositionElements()).add(30,1,Component.translatable("legacy.menu.create"), b-> repositionElements()).add(30,2,Component.translatable("legacy.menu.join"), b-> repositionElements());
    public boolean isLoading = false;
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    protected final ServerRenderableList serverRenderableList = new ServerRenderableList();
    public final SaveRenderableList saveRenderableList = new SaveRenderableList(this);
    private final CreationList creationList = new CreationList();


    @Override
    protected void init() {
        panel.height = Math.min(256,height-52);
        addWidget(tabList);
        panel.init();
        getRenderableVList().init(this,panel.x + 15,panel.y + 15,270, panel.height - 10 - (tabList.selectedTab == 0 ? 21 : 0));
        tabList.init(panel.x,panel.y - 24,panel.width);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics,false);
        tabList.render(guiGraphics,i,j,f);
        panel.render(guiGraphics,i,j,f);
        ScreenUtil.renderPanelRecess(guiGraphics, panel.x + 9, panel.y + 9, panel.width - 18, panel.height - 18 - (tabList.selectedTab == 0 ? 21 : 0), 2);
        if (tabList.selectedTab == 0){
            if (saveRenderableList.currentlyDisplayedLevels != null) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(panel.x + 11.25f, panel.y + panel.height - 22.75, 0);
                long storage = new File("/").getTotalSpace();
                long fixedStorage = SaveRenderableList.sizeCache.asMap().values().stream().max(Comparator.comparingLong(l->l)).orElse(0L) * (saveRenderableList.currentlyDisplayedLevels.size() + 1);
                long storageSize = fixedStorage != 0 ? Math.min(storage,fixedStorage) : storage;
                for (LevelSummary level : saveRenderableList.currentlyDisplayedLevels) {
                    Long size;
                    if ((size = SaveRenderableList.sizeCache.getIfPresent(level)) == null) continue;
                    float scaledSize = size * (panel.width - 21f)/ storageSize;
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().scale(scaledSize,1,1);
                    guiGraphics.fill(0, 0, 1, 11,getFocused() instanceof AbstractButton b && saveRenderableList.renderables.contains(b) && saveRenderableList.renderables.indexOf(b) == saveRenderableList.currentlyDisplayedLevels.indexOf(level) ? 0xFFFFFF00 : 0xFF8C9DE2);
                    guiGraphics.pose().popPose();
                    guiGraphics.pose().translate(scaledSize, 0, 0);
                }
                guiGraphics.pose().popPose();
            }
            ScreenUtil.renderPanelTranslucentRecess(guiGraphics, panel.x + 9, panel.y + panel.height - 25, panel.width - 18 , 16, 2);
        }
        if (isLoading)
            ScreenUtil.drawGenericLoading(guiGraphics, panel.x + 112 ,
                    panel.y + 66);
    }

    @Override
    public RenderableVList getRenderableVList() {
        if (tabList.selectedTab == 2) return serverRenderableList;
        else if (tabList.selectedTab == 1) return creationList;
        return saveRenderableList;
    }

    @Override
    public void removed() {
        if (this.saveRenderableList != null) {
            SaveRenderableList.resetIconCache();
        }
        if (serverRenderableList.lanServerDetector != null) {
            serverRenderableList.lanServerDetector.interrupt();
            serverRenderableList.lanServerDetector = null;
        }
        this.pinger.removeAll();
    }
    @Override
    public void tick() {
        super.tick();
        List<LevelSummary> summaries = saveRenderableList.pollLevelsIgnoreErrors();
        if (summaries != saveRenderableList.currentlyDisplayedLevels) {
            saveRenderableList.fillLevels("",summaries);
            repositionElements();
        }
        List<LanServer> list = serverRenderableList.lanServerList.takeDirtyServers();
        if (list != null) {
            if (serverRenderableList.lanServers == null || !new HashSet<>(serverRenderableList.lanServers).containsAll(list)) {
                serverRenderableList.lanServers = list;
                serverRenderableList.updateServers();
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
                saveRenderableList.reloadSaveList();
            } else if (tabList.selectedTab == 2) {
                serverRenderableList.updateServers();
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
        return serverRenderableList.servers;
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
                        Legacy4JClient.importSaveFile(minecraft, new FileInputStream(p.toFile()), FileNameUtils.getBaseName(p.getFileName().toString()));
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                });
                minecraft.setScreen(this);
                saveRenderableList.reloadSaveList();
            }));
        }
    }
}
