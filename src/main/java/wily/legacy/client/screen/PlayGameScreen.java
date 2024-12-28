package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
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
    protected final TabList tabList = new TabList().add(30,0,Component.translatable("legacy.menu.load"), b-> repositionElements()).add(30,1,Component.translatable("legacy.menu.create"), b-> repositionElements()).add(30,2,t-> (guiGraphics, i, j, f) -> t.renderString(guiGraphics,font,canNotifyOnlineFriends() ? 0xFFFFFF : CommonColor.INVENTORY_GRAY_TEXT.get(),canNotifyOnlineFriends()),Component.translatable("legacy.menu.join"), b-> {
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
                renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"),b-> {if (okAction.test(b)) onClose();}).bounds(panel.x + (panel.width - 200) / 2, panel.y + panel.height - 30,200,20).build());
            }
        });
    });
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    public final SaveRenderableList saveRenderableList = new SaveRenderableList(accessor);
    public final CreationList creationList = new CreationList(accessor);
    protected final ServerRenderableList serverRenderableList = PublishScreen.hasWorldHost() ? new FriendsServerRenderableList(accessor) : new ServerRenderableList(accessor);
    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(),()->ControlTooltip.getKeyMessage(InputConstants.KEY_O,this));
        renderer.add(()-> tabList.selectedTab != 2 ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(),()->DIRECT_CONNECTION);
    }
    public PlayGameScreen(Screen parent, int initialTab) {
        super(s-> Panel.centered(s,300,256,()-> 0, ()-> UIDefinition.Accessor.of(s).getBoolean("hasTabList",true) ? 12 : 0),Component.translatable("legacy.menu.play_game"));
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

    public PlayGameScreen(Screen parent) {
        this(parent,0);
    }

    protected boolean canNotifyOnlineFriends(){
        return serverRenderableList.hasOnlineFriends() && Util.getMillis() % 1000 < 500;
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
        panel.height = Math.min(256,height-52);
        if (hasTabList()) addWidget(tabList);
        panel.init();
        renderableVListInit();
        if (hasTabList()) tabList.init(panel.x,panel.y - 24,panel.width);
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init("renderableVList",panel.x + 15,panel.y + 15,270, panel.height - 30 - (tabList.selectedTab == 0 ? 21 : 0));
        if (!hasTabList()) serverRenderableList.init("serverRenderableVList",panel.x + 15,panel.y + 15,270, panel.height - 30 - (tabList.selectedTab == 0 ? 21 : 0));
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
        if (hasTabList()) tabList.render(guiGraphics,i,j,f);
        panel.render(guiGraphics,i,j,f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS, accessor.getInteger("panelRecess.x",panel.x + 9), accessor.getInteger("panelRecess.y",panel.y + 9), accessor.getInteger("panelRecess.width",panel.width - 18), accessor.getInteger("panelRecess.height",panel.height - 18 - (tabList.selectedTab == 0 ? 21 : 0)));
        if (hasTabList() && tabList.selectedTab == 0){
            if (saveRenderableList.currentlyDisplayedLevels != null) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(panel.x + 11.25f, panel.y + panel.height - 22.75, 0);
                long storage = new File("/").getTotalSpace();
                guiGraphics.enableScissor(0, 0, panel.width - 24, panel.height - 10);
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
        if (isLoading)
            ScreenUtil.drawGenericLoading(guiGraphics, panel.x + 112 ,
                    panel.y + 66);
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
        if (hasTabList() && (tabList.controlTab(i) || tabList.directionalControlTab(i)))  return true;
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
            minecraft.setScreen(new ConfirmationScreen(this, serverBox.getMessage(),Component.translatable("addServer.enterIp"), b1->  ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(serverBox.getValue()), new ServerData("","",/*? if >1.20.2 {*/ ServerData.Type.OTHER/*?} else {*//*false*//*?}*/), false/*? if >=1.20.5 {*/,null/*?}*/)){
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
                    serverBox.setPosition(panel.getX() + 15, panel.getY() + 45);
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
