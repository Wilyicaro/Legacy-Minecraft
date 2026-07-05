package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.commons.compress.utils.FileNameUtils;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.WidgetAccessor;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.compat.FriendsServerRenderableList;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class PlayGameScreen extends PanelVListScreen implements ControlTooltip.Event, TabList.Access{
    private static final Component SAFETY_TITLE = Component.translatable("multiplayerWarning.header").withStyle(ChatFormatting.BOLD);
    private static final Component SAFETY_CONTENT = Component.translatable("multiplayerWarning.message");
    private static final Component SAFETY_CHECK = Component.translatable("multiplayerWarning.check");
    public static final Component DIRECT_CONNECTION = Component.translatable("selectServer.direct");
    public boolean isLoading = false;
    protected final TabList tabList = new TabList(accessor).add(31, LegacyTabButton.Type.LEFT, this::renderTabString,Component.translatable("legacy.menu.load"), b-> repositionElements()).add(31, LegacyTabButton.Type.MIDDLE, this::renderTabString,Component.translatable("legacy.menu.create"), b-> repositionElements()).add(31, LegacyTabButton.Type.RIGHT, this::renderJoinTabString,Component.translatable("legacy.menu.join"), b-> {
        if (this.minecraft.options.skipMultiplayerWarning)
            repositionElements();
        else minecraft.setScreen(new ConfirmationScreen(this,SAFETY_TITLE,Component.translatable("legacy.menu.multiplayer_warning").append("\n").append(SAFETY_CONTENT)){
            @Override
            protected void addButtons() {
                renderableVList.addRenderable(Button.builder(SAFETY_CHECK, b-> {
                    this.minecraft.options.skipMultiplayerWarning = true;
                    this.minecraft.options.save();
                    onClose();
                }).bounds(panel.x + (panel.width - 200) / 2, panel.y + panel.height - 52,200,20).build());
                renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"),b-> okAction.accept(this)).bounds(panel.x + (panel.width - 200) / 2, panel.y + panel.height - 30,200,20).build());
            }
        });
    });
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    public final SaveRenderableList saveRenderableList = new SaveRenderableList(accessor);
    public final CreationList creationList = new CreationList(accessor);
    protected final Panel panelRecess;
    protected final ServerRenderableList serverRenderableList = PublishScreen.hasWorldHost() ? new FriendsServerRenderableList(accessor) : new ServerRenderableList(accessor);
    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon(),()->ControlTooltip.getKeyMessage(InputConstants.KEY_O,this));
        renderer.add(()-> tabList.selectedTab != 2 ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.getIcon(),()->DIRECT_CONNECTION);
    }
    public PlayGameScreen(Screen parent, int initialTab) {
        super(s-> Panel.createPanel(s, p-> p.appearance(300, Math.min(256, s.height - 52)), p-> p.pos(p.centeredLeftPos(s), p.centeredTopPos(s) + (UIAccessor.of(s).getBoolean("hasTabList",true) ? 12 : 0))),Component.translatable("legacy.menu.play_game"));
        panelRecess = Panel.createPanel(this, p -> p.appearance(LegacySprites.PANEL_RECESS, panel.width - 18, panel.height - 18 - (hasStorageBar() ? 21 : 0)), p -> p.pos(panel.x + 9, panel.y + 9));
        this.parent = parent;
        tabList.selectedTab = initialTab;
        renderableVLists.clear();
        renderableVLists.add(saveRenderableList);
        renderableVLists.add(creationList);
        renderableVLists.add(serverRenderableList);
    }

    public boolean hasTabList(){
        return accessor.getBoolean("hasTabList",true);
    }

    public boolean hasStorageBar() {
        return tabList.selectedTab == 0 && accessor.getBoolean("storageBar.isVisible", LegacyOptions.getUIMode().isFHD());
    }

    public PlayGameScreen(Screen parent) {
        this(parent,0);
    }

    public static Screen createAndCheckNewerVersions(Screen parent){
        PlayGameScreen screen = new PlayGameScreen(parent);
        if (Legacy4JClient.isNewerVersion) {
            Legacy4JClient.isNewerVersion = false;
            return PatchNotesScreen.createNewerVersion(createAndCheckNewerVersions(parent));
        }
        else if (Legacy4JClient.isNewerMinecraftVersion) {
            Legacy4JClient.isNewerMinecraftVersion = false;
            return PatchNotesScreen.createNewerMinecraftVersion(createAndCheckNewerVersions(parent));
        }
        return screen;
    }

    protected boolean canNotifyOnlineFriends(){
        return serverRenderableList.hasOnlineFriends() && Util.getMillis() % 1000 < 500;
    }

    protected void renderTabString(LegacyTabButton tab, GuiGraphics guiGraphics, int i, int j, float f) {
        tab.renderString(guiGraphics, font, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
    }

    protected void renderJoinTabString(LegacyTabButton tab, GuiGraphics guiGraphics, int i, int j, float f) {
        boolean notify = canNotifyOnlineFriends();
        tab.renderString(guiGraphics, font, notify ? 0xFFFFFFFF : CommonColor.INVENTORY_GRAY_TEXT.get(), notify);
    }

    @Override
    public TabList getTabList() {
        return tabList;
    }

    @Override
    public void added() {
        super.added();
        serverRenderableList.added();
    }

    @Override
    protected void init() {
        if (hasTabList()) addWidget(tabList);
        panel.init();
        panelRecess.init("panelRecess");
        renderableVListInit();
        if (hasTabList()) tabList.init(panel.x, panel.y - 25, panel.width, 31);
    }

    @Override
    public void renderableVListInit() {
        String listName = hasTabList() && tabList.selectedTab == 2 ? "serverRenderableVList" : "renderableVList";
        getRenderableVList().init(listName, panel.x + 15,panel.y + 15,panel.width - 30, panel.height - 30 - (hasStorageBar() ? 21 : 0));
        if (!hasTabList()) serverRenderableList.init("serverRenderableVList",panel.x + 15,panel.y + 15,panel.width - 30, panel.height - 30 - (hasStorageBar() ? 21 : 0));
    }

    @Override
    public void initRenderableVListEntry(RenderableVList renderableVList, Renderable renderable) {
        if (renderable instanceof AbstractWidget widget) {
            //? if <=1.20.1 {
            /*((WidgetAccessor) widget).setHeight(accessor.getInteger("buttonsHeight", 30));
            *///?} else {
            widget.setHeight(accessor.getInteger("buttonsHeight", 30));
            //?}
        }
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
        if (hasTabList()) tabList.render(guiGraphics,i,j,f);
        panel.render(guiGraphics,i,j,f);
        if (hasTabList()) tabList.renderSelected(guiGraphics,i,j,f);
        panelRecess.render(guiGraphics,i,j,f);
        if (hasStorageBar()){
            if (saveRenderableList.currentlyDisplayedLevels != null) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(panel.x + 11.25f, panel.y + panel.height - 22.75, 0);
                long storage = new File("/").getTotalSpace();
                guiGraphics.enableScissor(panel.x + 11, panel.y + panel.height - 23, panel.x + panel.width - 13, panel.y + panel.height - 10);
                for (LevelSummary level : saveRenderableList.currentlyDisplayedLevels) {
                    Long size;
                    if ((size = SaveRenderableList.sizeCache.getIfPresent(level)) == null) continue;
                    float scaledSize = Math.max(1,size * (panel.width - 21f)/ storage);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().scale(scaledSize,1,1);
                    guiGraphics.fill(0, 0, 1, 11,getFocused() instanceof AbstractButton b && saveRenderableList.renderables.contains(b) && saveRenderableList.renderables.indexOf(b) == saveRenderableList.currentlyDisplayedLevels.indexOf(level) ? CommonColor.SELECTED_STORAGE_SAVE.get() : CommonColor.STORAGE_SAVE.get());
                    guiGraphics.pose().popPose();
                    guiGraphics.pose().translate(scaledSize, 0, 0);
                }
                guiGraphics.pose().popPose();
                guiGraphics.disableScissor();
            }
            ScreenUtil.renderPanelTranslucentRecess(guiGraphics, panel.x + 9, panel.y + panel.height - 25, panel.width - 18 , 16);
        }
        if (isLoading) {
            int blockSize = accessor.getInteger("loadingIcon.blockSize", LegacyOptions.getUIMode().isSD() ? 6 : 21);
            int spacing = accessor.getInteger("loadingIcon.spacing", LegacyOptions.getUIMode().isSD() ? 3 : 6);
            int size = blockSize * 3 + spacing * 2;
            ScreenUtil.drawGenericLoading(guiGraphics, panelRecess.x + (panelRecess.width - size) / 2, panelRecess.y + (panelRecess.height - size) / 2, blockSize, spacing);
        }
    }

    @Override
    public RenderableVList getRenderableVList() {
        return getRenderableVLists().get(hasTabList() ? tabList.selectedTab : 0);
    }

    @Override
    public void removed() {
        if (this.saveRenderableList != null) {
            SaveRenderableList.resetIconCache();
        }
        serverRenderableList.removed();
        this.pinger.removeAll();
    }

    @Override
    public void tick() {
        super.tick();
        if (serverRenderableList.syncOptionDrivenButtons()) {
            rebuildWidgets();
        }
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
        this.pinger.tick();
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (hasTabList() && (tabList.controlTab(i) || tabList.directionalControlTab(i))) return true;
        if (super.keyPressed(i, j, k)) {
            return true;
        }
        if (i == InputConstants.KEY_F5) {
            if (tabList.selectedTab == 0) {
                saveRenderableList.reloadSaveList();
            } else if (tabList.selectedTab == 2) {
                serverRenderableList.servers.load();
                serverRenderableList.updateServers();
            }
            this.rebuildWidgets();
            return true;
        }
        if (i == InputConstants.KEY_X && tabList.selectedTab == 2){
            EditBox serverBox = new EditBox(Minecraft.getInstance().font, 0,0,200,20,DIRECT_CONNECTION);
            minecraft.setScreen(new ConfirmationScreen(this, ConfirmationScreen::getPanelWidth, () -> LegacyOptions.getUIMode().isSD() ? 92 : 120, serverBox.getMessage(), Component.translatable("addServer.enterIp"), b1->  ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(serverBox.getValue()), new ServerData("","",/*? if >1.20.2 {*/ ServerData.Type.OTHER/*?} else {*//*false*//*?}*/), false/*? if >=1.20.5 {*/,null/*?}*/)){
                boolean released = false;
                @Override
                protected void addButtons() {
                    super.addButtons();
                    okButton.active = false;
                }

                @Override
                public boolean charTyped(char c, int i) {
                    if (!released) return false;
                    return super.charTyped(c, i);
                }

                @Override
                public boolean keyReleased(int i2, int j, int k) {
                    if (i2 == i) released = true;
                    return super.keyReleased(i2, j, k);
                }

                @Override
                protected void init() {
                    super.init();
                    serverBox.setWidth(renderableVList.listWidth);
                    //? if <=1.20.1 {
                    /*((WidgetAccessor)serverBox).setHeight(LegacyOptions.getUIMode().isSD() ? 16 : 20);
                    *///?} else {
                    serverBox.setHeight(LegacyOptions.getUIMode().isSD() ? 16 : 20);
                    //?}
                    serverBox.setPosition(panel.getX() + (panel.getWidth() - serverBox.getWidth()) / 2, panel.getY() + (LegacyOptions.getUIMode().isSD() ? 32 : 45));
                    serverBox.setResponder(s-> okButton.active = ServerAddress.isValidAddress(s));
                    addRenderableWidget(serverBox);
                }
            });
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
                        Legacy4JClient.importSaveFile(new FileInputStream(p.toFile()),minecraft.getLevelSource(), FileNameUtils.getBaseName(p.getFileName().toString()));
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
