package wily.legacy.client.screen;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Screen;
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
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.LegacySprites;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import static wily.legacy.client.screen.CreationList.addCreationButton;

public class ServerSelectionList extends RenderableVList {
    static final ResourceLocation INCOMPATIBLE_SPRITE = new ResourceLocation("server_list/incompatible");
    static final ResourceLocation UNREACHABLE_SPRITE = new ResourceLocation("server_list/unreachable");
    static final ResourceLocation PING_1_SPRITE = new ResourceLocation("server_list/ping_1");
    static final ResourceLocation PING_2_SPRITE = new ResourceLocation("server_list/ping_2");
    static final ResourceLocation PING_3_SPRITE = new ResourceLocation("server_list/ping_3");
    static final ResourceLocation PING_4_SPRITE = new ResourceLocation("server_list/ping_4");
    static final ResourceLocation PING_5_SPRITE = new ResourceLocation("server_list/ping_5");
    static final ResourceLocation PINGING_1_SPRITE = new ResourceLocation("server_list/pinging_1");
    static final ResourceLocation PINGING_2_SPRITE = new ResourceLocation("server_list/pinging_2");
    static final ResourceLocation PINGING_3_SPRITE = new ResourceLocation("server_list/pinging_3");
    static final ResourceLocation PINGING_4_SPRITE = new ResourceLocation("server_list/pinging_4");
    static final ResourceLocation PINGING_5_SPRITE = new ResourceLocation("server_list/pinging_5");
    static final Logger LOGGER = LogUtils.getLogger();
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
    private PlayGameScreen screen;
    private final Minecraft minecraft;

    public final ServerList servers;
    @Nullable
    public LanServerDetection.LanServerDetector lanServerDetector;
    public final LanServerDetection.LanServerList lanServerList;
    public List<LanServer> lanServers;

    public ServerSelectionList() {
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
            LegacyMinecraft.LOGGER.warn("Unable to start LAN server detection: {}", exception.getMessage());
        }
        super.init(screen, leftPos, topPos, listWidth, listHeight);
    }

    public void updateServers(){
        renderables.clear();
        addCreationButton(this,new ResourceLocation(LegacyMinecraft.MOD_ID,"creation_list/add_server"),Component.translatable("legacy.menu.add_server"),c-> {
            this.minecraft.setScreen(new ServerEditScreen(screen, new ServerData(I18n.get("selectServer.defaultName"), "", ServerData.Type.OTHER), true));
        });
        for (int i = 0; i < servers.size(); i++) {
            int index = i;
            ServerData server = servers.get(i);
            FaviconTexture icon = FaviconTexture.forServer(this.minecraft.getTextureManager(), server.ip);
            addRenderable(new AbstractButton(0,0,270,30,Component.literal(server.name)) {
                private byte @Nullable [] lastIconBytes;
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
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
                    guiGraphics.drawString(minecraft.font, getMessage(), getX() + 32 + 3, getY() + 1, 0xFFFFFF);
                    List<FormattedCharSequence> list = minecraft.font.split(server.motd, 236);
                    for (int p = 0; p < Math.min(list.size(), 2); ++p) {
                        guiGraphics.drawString(minecraft.font, list.get(p), getX() + 32 + 3, getY() + 12 + minecraft.font.lineHeight * p, -8355712, false);
                    }
                    Component component = bl2 ? server.version.copy().withStyle(ChatFormatting.RED) : server.status;
                    int q = minecraft.font.width(component);
                    guiGraphics.drawString(minecraft.font, component, getX() + 270 - q - 15 - 2, getY() + 1, -8355712, false);
                    if (bl2) {
                        resourceLocation = INCOMPATIBLE_SPRITE;
                        component2 = INCOMPATIBLE_STATUS;
                        list2 = server.playerList;
                    } else if (this.pingCompleted()) {
                        resourceLocation = server.ping < 0L ? UNREACHABLE_SPRITE : (server.ping < 150L ? PING_5_SPRITE : (server.ping < 300L ? PING_4_SPRITE : (server.ping < 600L ? PING_3_SPRITE : (server.ping < 1000L ? PING_2_SPRITE : PING_1_SPRITE))));
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
                            default -> PINGING_1_SPRITE;
                            case 1 -> PINGING_2_SPRITE;
                            case 2 -> PINGING_3_SPRITE;
                            case 3 -> PINGING_4_SPRITE;
                            case 4 -> PINGING_5_SPRITE;
                        };
                        component2 = PINGING_STATUS;
                        list2 = Collections.emptyList();
                    }
                    guiGraphics.blitSprite(resourceLocation, getX() + width - 15, getY(), 10, 8);
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
                    if (s >= width - 15 && s <= width - 5 && t >= 0 && t <= 8) {
                        guiGraphics.renderTooltip(minecraft.font,component2, mouseX,mouseY);
                    } else if (s >= width - q - 15 - 2 && s <= width - 15 - 2 && t >= 0 && t <= 8) {
                        guiGraphics.renderComponentTooltip(minecraft.font,list2, mouseX,mouseY);
                    }
                    if (minecraft.options.touchscreen().get().booleanValue() || isHovered) {
                        guiGraphics.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);
                        int u = mouseX - getX();
                        int v = mouseY - getY();
                        if (u < 32 && u > 16) {
                            guiGraphics.blitSprite(LegacySprites.JOIN_HIGHLIGHTED_SPRITE, getX(), getY(), 32, 32);
                        } else {
                            guiGraphics.blitSprite(LegacySprites.JOIN_SPRITE, getX(), getY(), 32, 32);
                        }
                        if (index > 0) {
                            if (u < 16 && v < 16) {
                                guiGraphics.blitSprite(LegacySprites.MOVE_UP_HIGHLIGHTED_SPRITE, getX(), getY(), 32, 32);
                            } else {
                                guiGraphics.blitSprite(LegacySprites.MOVE_UP_SPRITE, getX(), getY(), 32, 32);
                            }
                        }
                        if (index < screen.getServers().size() - 1) {
                            if (u < 16 && v > 16) {
                                guiGraphics.blitSprite(LegacySprites.MOVE_DOWN_HIGHLIGHTED_SPRITE, getX(), getY(), 32, 32);
                            } else {
                                guiGraphics.blitSprite(LegacySprites.MOVE_DOWN_SPRITE, getX(), getY(), 32, 32);
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
        ConnectScreen.startConnecting(screen, this.minecraft, ServerAddress.parseString(serverData.ip), serverData, false);
    }

}
