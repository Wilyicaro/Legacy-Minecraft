package wily.legacy.client.screen;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import wily.legacy.Legacy4J;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import static wily.legacy.client.screen.CreationList.addIconButton;

public class ServerRenderableList extends RenderableVList {
    static final ResourceLocation INCOMPATIBLE = ResourceLocation.withDefaultNamespace("server_list/incompatible");
    static final ResourceLocation UNREACHABLE = ResourceLocation.withDefaultNamespace("server_list/unreachable");
    static final ResourceLocation PING_1 = ResourceLocation.withDefaultNamespace("server_list/ping_1");
    static final ResourceLocation PING_2 = ResourceLocation.withDefaultNamespace("server_list/ping_2");
    static final ResourceLocation PING_3 = ResourceLocation.withDefaultNamespace("server_list/ping_3");
    static final ResourceLocation PING_4 = ResourceLocation.withDefaultNamespace("server_list/ping_4");
    static final ResourceLocation PING_5 = ResourceLocation.withDefaultNamespace("server_list/ping_5");
    static final ResourceLocation PINGING_1 = ResourceLocation.withDefaultNamespace("server_list/pinging_1");
    static final ResourceLocation PINGING_2 = ResourceLocation.withDefaultNamespace("server_list/pinging_2");
    static final ResourceLocation PINGING_3 = ResourceLocation.withDefaultNamespace("server_list/pinging_3");
    static final ResourceLocation PINGING_4 = ResourceLocation.withDefaultNamespace("server_list/pinging_4");
    static final ResourceLocation PINGING_5 = ResourceLocation.withDefaultNamespace("server_list/pinging_5");
    static final Logger LOGGER = LogUtils.getLogger();
    static final ThreadPoolExecutor THREAD_POOL = new ScheduledThreadPoolExecutor(5, new ThreadFactoryBuilder().setNameFormat("Server Pinger #%d").setDaemon(true).setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER)).build());
    private static final ResourceLocation ICON_MISSING = ResourceLocation.withDefaultNamespace("textures/misc/unknown_server.png");
    static final Component SCANNING_LABEL = Component.translatable("lanServer.scanning");
    static final Component CANT_RESOLVE_TEXT = Component.translatable("multiplayer.status.cannot_resolve").withStyle(style -> style.withColor(-65536));
    static final Component CANT_CONNECT_TEXT = Component.translatable("multiplayer.status.cannot_connect").withStyle(style -> style.withColor(-65536));
    static final Component INCOMPATIBLE_STATUS = Component.translatable("multiplayer.status.incompatible");
    static final Component NO_CONNECTION_STATUS = Component.translatable("multiplayer.status.no_connection");
    static final Component PINGING_STATUS = Component.translatable("multiplayer.status.pinging");
    static final Component ONLINE_STATUS = Component.translatable("multiplayer.status.online");
    private static final Component LAN_SERVER_HEADER = Component.translatable("lanServer.title");
    private static final Component HIDDEN_ADDRESS_TEXT = Component.translatable("selectServer.hiddenAddress");
    private PlayGameScreen screen;
    private final Minecraft minecraft;

    public final ServerList servers;
    @Nullable
    public LanServerDetection.LanServerDetector lanServerDetector;
    public final LanServerDetection.LanServerList lanServerList;
    public List<LanServer> lanServers;

    public ServerRenderableList() {
        layoutSpacing(l->0);
        minecraft = Minecraft.getInstance();
        servers = new ServerList(minecraft);
        servers.load();
        lanServerList = new LanServerDetection.LanServerList();
        updateServers();
    }
    @Override
    public void init(Screen screen, int leftPos, int topPos, int listWidth, int listHeight) {
        if (screen instanceof PlayGameScreen s) this.screen = s;
        try {
            this.lanServerDetector = new LanServerDetection.LanServerDetector(this.lanServerList);
            this.lanServerDetector.start();
        } catch (Exception exception) {
            Legacy4J.LOGGER.warn("Unable to start LAN server detection: {}", exception.getMessage());
        }
        super.init(screen, leftPos, topPos, listWidth, listHeight);
    }
    private Component getMultiplayerDisabledReason() {
        if (this.minecraft.allowsMultiplayer()) {
            return null;
        } else if (this.minecraft.isNameBanned()) {
            return Component.translatable("title.multiplayer.disabled.banned.name");
        } else {
            BanDetails banDetails = this.minecraft.multiplayerBan();
            if (banDetails != null) {
                return banDetails.expires() != null ? Component.translatable("title.multiplayer.disabled.banned.temporary") : Component.translatable("title.multiplayer.disabled.banned.permanent");
            } else {
                return Component.translatable("title.multiplayer.disabled");
            }
        }
    }
    public void updateServers(){
        renderables.clear();
        addIconButton(this,ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID,"creation_list/add_server"),Component.translatable("legacy.menu.add_server"), c-> this.minecraft.setScreen(new ServerEditScreen(screen, new ServerData(I18n.get("selectServer.defaultName"), "", ServerData.Type.OTHER), true)));
        Component component = this.getMultiplayerDisabledReason();
        Tooltip tooltip = component != null ? Tooltip.create(component) : null;
        addIconButton(this,ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID,"creation_list/realms"), Component.translatable("menu.online"), b-> minecraft.setScreen(new RealmsMainScreen(screen)),tooltip);
        for (int i = 0; i < servers.size(); i++) {
            int index = i;
            ServerData server = servers.get(i);
            FaviconTexture icon = FaviconTexture.forServer(this.minecraft.getTextureManager(), server.ip);
            addRenderable(new AbstractButton(0,0,270,30,Component.literal(server.name)) {
                private byte @Nullable [] lastIconBytes;
                @Nullable
                private List<Component> onlinePlayersTooltip;
                @Nullable
                private ResourceLocation statusIcon;
                @Nullable
                private Component statusIconTooltip;
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
                    List<Component> list2;
                    Component component2;
                    ResourceLocation resourceLocation;
                    if (server.state() == ServerData.State.INITIAL) {
                        server.setState(ServerData.State.PINGING);
                        server.motd = CommonComponents.EMPTY;
                        server.status = CommonComponents.EMPTY;
                        THREAD_POOL.submit(() -> {
                            try {
                                screen.getPinger().pingServer(server, () -> minecraft.execute(this::updateServerList), () -> {
                                    server.setState(server.protocol == SharedConstants.getCurrentVersion().getProtocolVersion() ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE);
                                    minecraft.execute(this::refreshStatus);
                                });
                            } catch (UnknownHostException unknownHostException) {
                               server.setState(ServerData.State.UNREACHABLE);
                               server.motd = CANT_RESOLVE_TEXT;
                                minecraft.execute(this::refreshStatus);
                            } catch (Exception exception) {
                                server.setState(ServerData.State.UNREACHABLE);
                                server.motd = CANT_CONNECT_TEXT;
                                minecraft.execute(this::refreshStatus);
                            }
                        });
                    }
                    boolean bl2 = !this.isCompatible();
                    guiGraphics.drawString(minecraft.font, getMessage(), getX() + 32 + 3, getY() + 3, 0xFFFFFF);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(getX() + 35,  getY() + 10,0);
                    guiGraphics.pose().scale(2/3f,2/3f,2/3f);
                    List<FormattedCharSequence> list = minecraft.font.split(server.motd, Math.max(234,minecraft.font.width(server.motd) / 2 + 20));
                    for (int p = 0; p < Math.min(2,list.size()); ++p) {
                        ScreenUtil.renderScrollingString(guiGraphics,minecraft.font, list.get(p), 0,  minecraft.font.lineHeight * p,234 , 11 + minecraft.font.lineHeight * p, -8355712, false,minecraft.font.width(list.get(p))* 2/3);
                    }
                    guiGraphics.pose().popPose();
                    Component component = bl2 ? server.version.copy().withStyle(ChatFormatting.RED) : server.status;
                    int q = minecraft.font.width(component);
                    guiGraphics.drawString(minecraft.font, component, getX() + 270 - q - 15 - 2, getY() + 3, -8355712, false);
                    if (server.state() == ServerData.State.PINGING) {
                        int p = (int)(Util.getMillis() / 100L + (long)(index * 2) & 7L);
                        if (p > 4) {
                            p = 8 - p;
                        }
                        this.statusIcon = switch (p) {
                            default -> PINGING_1;
                            case 1 -> PINGING_2;
                            case 2 -> PINGING_3;
                            case 3 -> PINGING_4;
                            case 4 -> PINGING_5;
                        };
                    }
                    if (statusIcon != null) guiGraphics.blitSprite(statusIcon, getX() + width - 15, getY() + 3, 10, 8);
                    byte[] bs = server.getIconBytes();
                    if (!Arrays.equals(bs, this.lastIconBytes)) {
                        if (this.uploadServerIcon(bs)) {
                            this.lastIconBytes = bs;
                        } else {
                            server.setIconBytes(null);
                            this.updateServerList();
                        }
                    }
                    this.drawIcon(guiGraphics, getX(), getY(), icon.textureLocation());
                    int s = mouseX - getX();
                    int t = mouseY - getY();
                    if (statusIconTooltip != null && s >= width - 15 && s <= width - 5 && t >= 2 && t <= 10) {
                        guiGraphics.renderTooltip(minecraft.font,statusIconTooltip, mouseX,mouseY);
                    } else if (onlinePlayersTooltip != null && s >= width - q - 15 - 2 && s <= width - 15 - 2 && t >= 2 && t <= 10) {
                        guiGraphics.renderComponentTooltip(minecraft.font,onlinePlayersTooltip, mouseX,mouseY);
                    }
                    if (minecraft.options.touchscreen().get().booleanValue() || isHovered) {
                        guiGraphics.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);
                        int u = mouseX - getX();
                        int v = mouseY - getY();
                        if (u < 32 && u > 16) {
                            guiGraphics.blitSprite(LegacySprites.JOIN_HIGHLIGHTED, getX(), getY(), 32, 32);
                        } else {
                            guiGraphics.blitSprite(LegacySprites.JOIN, getX(), getY(), 32, 32);
                        }
                        if (index > 0) {
                            if (u < 16 && v < 16) {
                                guiGraphics.blitSprite(LegacySprites.MOVE_UP_HIGHLIGHTED, getX(), getY(), 32, 32);
                            } else {
                                guiGraphics.blitSprite(LegacySprites.MOVE_UP, getX(), getY(), 32, 32);
                            }
                        }
                        if (index < screen.getServers().size() - 1) {
                            if (u < 16 && v > 16) {
                                guiGraphics.blitSprite(LegacySprites.MOVE_DOWN_HIGHLIGHTED, getX(), getY(), 32, 32);
                            } else {
                                guiGraphics.blitSprite(LegacySprites.MOVE_DOWN, getX(), getY(), 32, 32);
                            }
                        }
                    }
                }
                private void refreshStatus() {
                    this.onlinePlayersTooltip = null;
                    switch (server.state()) {
                        case INITIAL:
                        case PINGING: {
                            this.statusIcon = PING_1;
                            this.statusIconTooltip = PINGING_STATUS;
                            break;
                        }
                        case INCOMPATIBLE: {
                            this.statusIcon = INCOMPATIBLE;
                            this.statusIconTooltip = INCOMPATIBLE_STATUS;
                            this.onlinePlayersTooltip = server.playerList;
                            break;
                        }
                        case UNREACHABLE: {
                            this.statusIcon = UNREACHABLE;
                            this.statusIconTooltip = NO_CONNECTION_STATUS;
                            break;
                        }
                        case SUCCESSFUL: {
                            this.statusIcon = server.ping < 150L ? PING_5 : (server.ping < 300L ? PING_4 : (server.ping < 600L ? PING_3 : (server.ping < 1000L ? PING_2: PINGING_1)));
                            this.statusIconTooltip = Component.translatable("multiplayer.status.ping", server.ping);
                            this.onlinePlayersTooltip = server.playerList;
                        }
                    }
                }

                private boolean isCompatible() {
                    return server.protocol == SharedConstants.getCurrentVersion().getProtocolVersion();
                }

                public void updateServerList() {
                    screen.getServers().save();
                }

                protected void drawIcon(GuiGraphics guiGraphics, int i, int j, ResourceLocation resourceLocation) {
                    RenderSystem.enableBlend();
                    guiGraphics.blit(resourceLocation, i + 5, j + 5, 0.0f, 0.0f, 20, 20, 20, 20);
                    RenderSystem.disableBlend();
                }

                private boolean uploadServerIcon(@Nullable byte[] bs) {
                    if (bs == null) {
                        icon.clear();
                    } else {
                        try {
                            icon.upload(NativeImage.read(bs));
                        } catch (Throwable throwable) {
                            LOGGER.error("Invalid icon for server {} ({})", server.name, server.ip, throwable);
                            return false;
                        }
                    }
                    return true;
                }
                protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                }

                @Override
                public void onClick(double d, double e) {
                    double f = d - (double) getX();
                    double g = e - (double) getY();
                    if (f <= 32.0) {
                        if (f < 16.0 && g < 16.0 && index > 0) {
                            this.swap(index, index - 1);
                            return;
                        }
                        if (f < 16.0 && g > 16.0 && index < servers.size() - 1) {
                            this.swap(index, index + 1);
                            return;
                        }
                        join(server);
                    }else {
                        super.onClick(d,e);
                    }
                }

                @Override
                public void onPress() {
                    if (isFocused()) join(server);
                }
                private void swap(int i, int j) {
                    servers.swap(i, j);
                    updateServers();
                    screen.repositionElements();
                    if (j < renderables.size() && renderables.get(j) instanceof GuiEventListener l) screen.setFocused(l);
                }
                public boolean keyPressed(int i, int j, int k) {
                    if (Screen.hasShiftDown()) {
                        if (i == 264 && index < servers.size() - 1 || i == 265 && index > 0) {
                            this.swap(index, i == 264 ? index + 1 : index - 1);
                            return true;
                        }
                    }
                    if (i == InputConstants.KEY_O) {
                        minecraft.setScreen(new ServerOptionsScreen(screen,server));
                        screen.setFocused(this);
                        return true;
                    }
                    return super.keyPressed(i, j, k);
                }

                @Override
                protected MutableComponent createNarrationMessage() {
                    MutableComponent mutableComponent = Component.empty();
                    mutableComponent.append(Component.translatable("narrator.select", server.name));
                    mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                    switch (server.state()) {
                        case INCOMPATIBLE: {
                            mutableComponent.append(INCOMPATIBLE_STATUS);
                            mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                            mutableComponent.append(Component.translatable("multiplayer.status.version.narration", server.version));
                            mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                            mutableComponent.append(Component.translatable("multiplayer.status.motd.narration", server.motd));
                            break;
                        }
                        case UNREACHABLE: {
                            mutableComponent.append(NO_CONNECTION_STATUS);
                            break;
                        }
                        case PINGING: {
                            mutableComponent.append(PINGING_STATUS);
                            break;
                        }
                        default: {
                            mutableComponent.append(ONLINE_STATUS);
                            mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                            mutableComponent.append(Component.translatable("multiplayer.status.ping.narration", server.ping));
                            mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                            mutableComponent.append(Component.translatable("multiplayer.status.motd.narration", server.motd));
                            if (server.players == null) break;
                            mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                            mutableComponent.append(Component.translatable("multiplayer.status.player_count.narration", server.players.online(), server.players.max()));
                            mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                            mutableComponent.append(ComponentUtils.formatList(server.playerList, Component.literal(", ")));
                        }
                    }
                    return mutableComponent;
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    defaultButtonNarrationText(narrationElementOutput);
                }
            });
        }
        if (lanServers != null){
            for (LanServer lanServer : lanServers) {
                AbstractButton lanButton;
                addRenderable(lanButton = new AbstractButton(0,0,270,30,Component.literal(lanServer.getMotd())) {
                    @Override
                    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                        guiGraphics.drawString(minecraft.font, LAN_SERVER_HEADER, getX() + 32 + 3, getY() + 1, 0xFFFFFF, false);
                        guiGraphics.drawString(minecraft.font, lanServer.getMotd(), getX() + 32 + 3, getY() + 12, -8355712, false);
                        if (minecraft.options.hideServerAddress) {
                            guiGraphics.drawString(minecraft.font, HIDDEN_ADDRESS_TEXT, getX() + 32 + 3,getY() + 12 + 11, 0x303030, false);
                        } else {
                            guiGraphics.drawString(minecraft.font, lanServer.getAddress(), getX() + 32 + 3, getY() + 12 + 11, 0x303030, false);
                        }
                    }

                    @Override
                    public void onPress() {
                        if (isFocused()) joinLanServer(lanServer);
                    }

                    @Override
                    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                        defaultButtonNarrationText(narrationElementOutput);
                    }
                });
                if (screen.children.contains(lanButton))
                    this.minecraft.getNarrator().say(Component.translatable("multiplayer.lan.server_found", Component.empty().append(LAN_SERVER_HEADER).append(CommonComponents.SPACE).append(lanServer.getMotd())));
            }
        }else {
            addRenderable(SimpleLayoutRenderable.create(270,30,(r)-> ((guiGraphics, i, j, f) -> {
                int p = r.y + (r.height - minecraft.font.lineHeight) / 2;
                guiGraphics.drawString(this.minecraft.font, SCANNING_LABEL, this.minecraft.screen.width / 2 - this.minecraft.font.width(SCANNING_LABEL) / 2, p, 0xFFFFFF, false);
                String string = LoadingDotsText.get(Util.getMillis());
                guiGraphics.drawString(this.minecraft.font, string, this.minecraft.screen.width / 2 - this.minecraft.font.width(string) / 2, p + this.minecraft.font.lineHeight, -8355712, false);
            })));
        }
    }
    public void joinSelectedServer() {
        int i;
        if (screen.getFocused() instanceof Renderable r && (i = renderables.indexOf(r)) > 0){
            if (servers.size() > i){
                join(servers.get(i));
            }else if (lanServers != null)
                joinLanServer(lanServers.get(i - servers.size()));
        }
    }
    private void joinLanServer(LanServer lanServer) {
        join(new ServerData(lanServer.getMotd(),lanServer.getAddress(),ServerData.Type.LAN));
    }
    private void join(ServerData serverData) {
        ConnectScreen.startConnecting(screen, this.minecraft, ServerAddress.parseString(serverData.ip), serverData, false,null);
    }

}
