package wily.legacy.client.screen;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyGuiGraphics;
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
    static final ResourceLocation INCOMPATIBLE = new ResourceLocation(Legacy4J.MOD_ID,"server_list/incompatible");
    static final ResourceLocation UNREACHABLE = new ResourceLocation(Legacy4J.MOD_ID,"server_list/unreachable");
    static final ResourceLocation PING_1 = new ResourceLocation(Legacy4J.MOD_ID,"server_list/ping_1");
    static final ResourceLocation PING_2 = new ResourceLocation(Legacy4J.MOD_ID,"server_list/ping_2");
    static final ResourceLocation PING_3 = new ResourceLocation(Legacy4J.MOD_ID,"server_list/ping_3");
    static final ResourceLocation PING_4 = new ResourceLocation(Legacy4J.MOD_ID,"server_list/ping_4");
    static final ResourceLocation PING_5 = new ResourceLocation(Legacy4J.MOD_ID,"server_list/ping_5");
    static final ResourceLocation PINGING_1 = new ResourceLocation(Legacy4J.MOD_ID,"server_list/pinging_1");
    static final ResourceLocation PINGING_2 = new ResourceLocation(Legacy4J.MOD_ID,"server_list/pinging_2");
    static final ResourceLocation PINGING_3 = new ResourceLocation(Legacy4J.MOD_ID,"server_list/pinging_3");
    static final ResourceLocation PINGING_4 = new ResourceLocation(Legacy4J.MOD_ID,"server_list/pinging_4");
    static final ResourceLocation PINGING_5 = new ResourceLocation(Legacy4J.MOD_ID,"server_list/pinging_5");
    protected static final Logger LOGGER = LogUtils.getLogger();
    static final ThreadPoolExecutor THREAD_POOL = new ScheduledThreadPoolExecutor(5, new ThreadFactoryBuilder().setNameFormat("Server Pinger #%d").setDaemon(true).setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER)).build());
    private static final ResourceLocation ICON_MISSING = new ResourceLocation("textures/misc/unknown_server.png");
    static final Component SCANNING_LABEL = Component.translatable("lanServer.scanning");
    static final Component CANT_RESOLVE_TEXT = Component.translatable("multiplayer.status.cannot_resolve").withStyle(style -> style.withColor(-65536));
    static final Component CANT_CONNECT_TEXT = Component.translatable("multiplayer.status.cannot_connect").withStyle(style -> style.withColor(-65536));
    static final Component INCOMPATIBLE_STATUS = Component.translatable("multiplayer.status.incompatible");
    static final Component NO_CONNECTION_STATUS = Component.translatable("multiplayer.status.no_connection");
    static final Component PINGING_STATUS = Component.translatable("multiplayer.status.pinging");
    static final Component ONLINE_STATUS = Component.translatable("multiplayer.status.online");
    private static final Component LAN_SERVER_HEADER = Component.translatable("lanServer.title");
    private static final Component HIDDEN_ADDRESS_TEXT = Component.translatable("selectServer.hiddenAddress");
    protected PlayGameScreen screen;
    protected final Minecraft minecraft;

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
    public boolean hasOnlineFriends(){
        return false;
    }
    private Component getMultiplayerDisabledReason() {
        if (this.minecraft.allowsMultiplayer()) {
            return null;
        } else {
            BanDetails banDetails = this.minecraft.multiplayerBan();
            if (banDetails != null) {
                return banDetails.expires() != null ? Component.translatable("title.multiplayer.disabled.banned.temporary") : Component.translatable("title.multiplayer.disabled.banned.permanent");
            } else {
                return Component.translatable("title.multiplayer.disabled");
            }
        }
    }
    public static void drawIcon(PoseStack poseStack, int i, int j, ResourceLocation resourceLocation) {
        RenderSystem.enableBlend();
        poseStack.blit(resourceLocation, i + 5, j + 5, 0.0f, 0.0f, 20, 20, 20, 20);
        RenderSystem.disableBlend();
    }
    public void updateServers(){
        renderables.clear();
        addIconButton(this,new ResourceLocation(Legacy4J.MOD_ID,"creation_list/add_server"),Component.translatable("legacy.menu.add_server"), c-> this.minecraft.setScreen(new ServerEditScreen(screen, new ServerData(I18n.get("selectServer.defaultName"), "", false), true)));
        Component component = this.getMultiplayerDisabledReason();
        Tooltip tooltip = component != null ? Tooltip.create(component) : null;
        addIconButton(this,new ResourceLocation(Legacy4J.MOD_ID,"creation_list/realms"), Component.translatable("menu.online"), b-> minecraft.setScreen(new RealmsMainScreen(screen)),tooltip);
        for (int i = 0; i < servers.size(); i++) {
            int index = i;
            ServerData server = servers.get(i);
            FaviconTexture icon = FaviconTexture.forServer(this.minecraft.getTextureManager(), server.ip);
            addRenderable(new AbstractButton(0,0,270,30,Component.literal(server.name)) {
                private byte @Nullable [] lastIconBytes;
                @Override
                protected void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
                    super.renderWidget(poseStack, mouseX, mouseY, partialTicks);
                    List<Component> list2;
                    Component component2;
                    ResourceLocation resourceLocation;
                    if (!server.pinged) {
                        server.pinged = true;
                        server.ping = -2L;
                        server.motd = CommonComponents.EMPTY;
                        server.status = CommonComponents.EMPTY;
                        THREAD_POOL.submit(() -> {
                            try {
                                screen.getPinger().pingServer(server, () -> minecraft.execute(this::updateServerList));
                            } catch (UnknownHostException unknownHostException) {
                                server.ping = -1L;
                                server.motd = CANT_RESOLVE_TEXT;
                            } catch (Exception exception) {
                                server.ping = -1L;
                                server.motd = CANT_CONNECT_TEXT;
                            }
                        });
                    }
                    boolean bl2 = !this.isCompatible();
                    poseStack.drawString(minecraft.font, getMessage(), getX() + 32 + 3, getY() + 3, 0xFFFFFF);
                    poseStack.pushPose();
                    poseStack.translate(getX() + 35,  getY() + 10,0);
                    poseStack.scale(2/3f,2/3f,2/3f);
                    List<FormattedCharSequence> list = minecraft.font.split(server.motd, Math.max(234,minecraft.font.width(server.motd) / 2 + 20));
                    for (int p = 0; p < Math.min(2,list.size()); ++p) {
                        ScreenUtil.renderScrollingString(poseStack,minecraft.font, list.get(p), 0,  minecraft.font.lineHeight * p,234 , 11 + minecraft.font.lineHeight * p, -8355712, false,minecraft.font.width(list.get(p))* 2/3);
                    }
                    poseStack.popPose();
                    Component component = bl2 ? server.version.copy().withStyle(ChatFormatting.RED) : server.status;
                    int q = minecraft.font.width(component);
                    poseStack.drawString(minecraft.font, component, getX() + 270 - q - 15 - 2, getY() + 3, -8355712, false);
                    if (bl2) {
                        resourceLocation = INCOMPATIBLE;
                        component2 = INCOMPATIBLE_STATUS;
                        list2 = server.playerList;
                    } else if (this.pingCompleted()) {
                        resourceLocation = server.ping < 0L ? UNREACHABLE : (server.ping < 150L ? PING_5 : (server.ping < 300L ? PING_4 : (server.ping < 600L ? PING_3 : (server.ping < 1000L ? PING_2 : PING_1))));
                        if (server.ping < 0L) {
                            component2 = NO_CONNECTION_STATUS;
                            list2 = Collections.emptyList();
                        } else {
                            component2 = Component.translatable("multiplayer.status.ping", server.ping);
                            list2 = server.playerList;
                        }
                    } else {
                        int r = (int)(Util.getMillis() / 100L + (long)(index * 2) & 7L);
                        if (r > 4) {
                            r = 8 - r;
                        }
                        resourceLocation = switch (r) {
                            default -> PINGING_1;
                            case 1 -> PINGING_2;
                            case 2 -> PINGING_3;
                            case 3 -> PINGING_4;
                            case 4 -> PINGING_5;
                        };
                        component2 = PINGING_STATUS;
                        list2 = Collections.emptyList();
                    }
                    LegacyGuiGraphics.of(poseStack).blitSprite(resourceLocation, getX() + width - 15, getY() + 3, 10, 8);
                    byte[] bs = server.getIconBytes();
                    if (!Arrays.equals(bs, this.lastIconBytes)) {
                        if (this.uploadServerIcon(bs)) {
                            this.lastIconBytes = bs;
                        } else {
                            server.setIconBytes(null);
                            this.updateServerList();
                        }
                    }
                    drawIcon(poseStack, getX(), getY(), icon.textureLocation());
                    int s = mouseX - getX();
                    int t = mouseY - getY();
                    if (s >= width - 15 && s <= width - 5 && t >= 2 && t <= 10) {
                        poseStack.renderTooltip(minecraft.font,component2, mouseX,mouseY);
                    } else if (s >= width - q - 15 - 2 && s <= width - 15 - 2 && t >= 2 && t <= 10) {
                        poseStack.renderComponentTooltip(minecraft.font,list2, mouseX,mouseY);
                    }
                    if (minecraft.options.touchscreen().get().booleanValue() || isHovered) {
                        poseStack.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);
                        int u = mouseX - getX();
                        int v = mouseY - getY();
                        if (u < 32 && u > 16) {
                            LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.JOIN_HIGHLIGHTED, getX(), getY(), 32, 32);
                        } else {
                            LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.JOIN, getX(), getY(), 32, 32);
                        }
                        if (index > 0) {
                            if (u < 16 && v < 16) {
                                LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.MOVE_UP_HIGHLIGHTED, getX(), getY(), 32, 32);
                            } else {
                                LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.MOVE_UP, getX(), getY(), 32, 32);
                            }
                        }
                        if (index < screen.getServers().size() - 1) {
                            if (u < 16 && v > 16) {
                                LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.MOVE_DOWN_HIGHLIGHTED, getX(), getY(), 32, 32);
                            } else {
                                LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.MOVE_DOWN, getX(), getY(), 32, 32);
                            }
                        }
                    }
                }
                private boolean pingCompleted() {
                    return server.pinged && server.ping != -2L;
                }

                private boolean isCompatible() {
                    return server.protocol == SharedConstants.getCurrentVersion().getProtocolVersion();
                }

                public void updateServerList() {
                    screen.getServers().save();
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
                protected void renderScrollingString(PoseStack poseStack, Font font, int i, int j) {
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
                    if (!this.isCompatible()) {
                        mutableComponent.append(INCOMPATIBLE_STATUS);
                        mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                        mutableComponent.append(Component.translatable("multiplayer.status.version.narration", server.version));
                        mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                        mutableComponent.append(Component.translatable("multiplayer.status.motd.narration", server.motd));
                    } else if (server.ping < 0L) {
                        mutableComponent.append(NO_CONNECTION_STATUS);
                    } else if (!this.pingCompleted()) {
                        mutableComponent.append(PINGING_STATUS);
                    } else {
                        mutableComponent.append(ONLINE_STATUS);
                        mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                        mutableComponent.append(Component.translatable("multiplayer.status.ping.narration", server.ping));
                        mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                            mutableComponent.append(Component.translatable("multiplayer.status.motd.narration", server.motd));
                        if (server.players != null) {
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
                    protected void renderScrollingString(PoseStack poseStack, Font font, int i, int j) {
                        poseStack.drawString(minecraft.font, LAN_SERVER_HEADER, getX() + 32 + 3, getY() + 1, 0xFFFFFF, false);
                        poseStack.drawString(minecraft.font, lanServer.getMotd(), getX() + 32 + 3, getY() + 12, -8355712, false);
                        if (minecraft.options.hideServerAddress) {
                            poseStack.drawString(minecraft.font, HIDDEN_ADDRESS_TEXT, getX() + 32 + 3,getY() + 12 + 11, 0x303030, false);
                        } else {
                            poseStack.drawString(minecraft.font, lanServer.getAddress(), getX() + 32 + 3, getY() + 12 + 11, 0x303030, false);
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
            addRenderable(SimpleLayoutRenderable.create(270,30,(r)-> ((poseStack, i, j, f) -> {
                int p = r.y + (r.height - minecraft.font.lineHeight) / 2;
                poseStack.drawString(this.minecraft.font, SCANNING_LABEL, this.minecraft.screen.width / 2 - this.minecraft.font.width(SCANNING_LABEL) / 2, p, 0xFFFFFF, false);
                String string = LoadingDotsText.get(Util.getMillis());
                poseStack.drawString(this.minecraft.font, string, this.minecraft.screen.width / 2 - this.minecraft.font.width(string) / 2, p + this.minecraft.font.lineHeight, -8355712, false);
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
        join(new ServerData(lanServer.getMotd(),lanServer.getAddress(),true));
    }
    private void join(ServerData serverData) {
        ConnectScreen.startConnecting(screen, this.minecraft, ServerAddress.parseString(serverData.ip), serverData, false);
    }
}
