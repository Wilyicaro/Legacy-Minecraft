package wily.legacy.client.screen;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerSelectionList extends SlotButtonList<ServerSelectionList.Entry> {
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
    static final ResourceLocation JOIN_HIGHLIGHTED_SPRITE = new ResourceLocation("server_list/join_highlighted");
    static final ResourceLocation JOIN_SPRITE = new ResourceLocation("server_list/join");
    static final ResourceLocation MOVE_UP_HIGHLIGHTED_SPRITE = new ResourceLocation("server_list/move_up_highlighted");
    static final ResourceLocation MOVE_UP_SPRITE = new ResourceLocation("server_list/move_up");
    static final ResourceLocation MOVE_DOWN_HIGHLIGHTED_SPRITE = new ResourceLocation("server_list/move_down_highlighted");
    static final ResourceLocation MOVE_DOWN_SPRITE = new ResourceLocation("server_list/move_down");
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
    private final PlayGameScreen screen;
    private final List<ServerSelectionList.OnlineServerEntry> onlineServers = Lists.newArrayList();
    private final Entry lanHeader = new ServerSelectionList.LANHeader();
    private final List<ServerSelectionList.NetworkServerEntry> networkServers = Lists.newArrayList();

    public ServerSelectionList(PlayGameScreen playGameScreen, Minecraft minecraft, int i, int j, int k, int l, int m) {
        super(()->playGameScreen.tabList.selectedTab == 2,minecraft, i, j, k, l, m);
        this.screen = playGameScreen;
        setRenderBackground(false);
    }
    private void refreshEntries() {
        this.clearEntries();
        this.onlineServers.forEach(this::addEntry);
        this.addEntry(this.lanHeader);
        this.networkServers.forEach(this::addEntry);
    }


    @Override
    public boolean keyPressed(int i, int j, int k) {
        Entry entry = this.getSelected();
        return entry != null && entry.keyPressed(i, j, k) || super.keyPressed(i, j, k);
    }

    public void updateOnlineServers(ServerList serverList) {
        this.onlineServers.clear();
        for (int i = 0; i < serverList.size(); ++i) {
            this.onlineServers.add(new ServerSelectionList.OnlineServerEntry(this.screen, serverList.get(i)));
        }
        this.refreshEntries();
    }

    public void updateNetworkServers(List<LanServer> list) {
        int i = list.size() - this.networkServers.size();
        this.networkServers.clear();
        for (LanServer lanServer : list) {
            this.networkServers.add(new ServerSelectionList.NetworkServerEntry(this.screen, lanServer));
        }
        this.refreshEntries();
        for (int j = this.networkServers.size() - i; j < this.networkServers.size(); ++j) {
            ServerSelectionList.NetworkServerEntry networkServerEntry = this.networkServers.get(j);
            int k = j - this.networkServers.size() + this.children().size();
            int l = this.getRowTop(k);
            int m = this.getRowBottom(k);
            if (m < this.y0 || l > this.y1) continue;
            this.minecraft.getNarrator().say(Component.translatable("multiplayer.lan.server_found", networkServerEntry.getServerNarration()));
        }
    }

    @Override
    protected int getScrollbarPosition() {
        return super.getScrollbarPosition() + 30;
    }

    public void removed() {
    }

    @Environment(value= EnvType.CLIENT)
    public static class LANHeader
            extends Entry {
        private final Minecraft minecraft = Minecraft.getInstance();

        @Override
        public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
            int p = j + m / 2 - this.minecraft.font.lineHeight / 2;
            guiGraphics.drawString(this.minecraft.font, SCANNING_LABEL, this.minecraft.screen.width / 2 - this.minecraft.font.width(SCANNING_LABEL) / 2, p, 0xFFFFFF, false);
            String string = LoadingDotsText.get(Util.getMillis());
            guiGraphics.drawString(this.minecraft.font, string, this.minecraft.screen.width / 2 - this.minecraft.font.width(string) / 2, p + this.minecraft.font.lineHeight, -8355712, false);
        }

        @Override
        public boolean hasSlotBackground() {
            return false;
        }

        @Override
        public Component getNarration() {
            return SCANNING_LABEL;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class Entry extends SlotButtonList.SlotEntry<Entry> implements AutoCloseable {
        @Override
        public void close() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    public class OnlineServerEntry extends Entry {
        private final PlayGameScreen screen;
        private final Minecraft minecraft;
        private final ServerData serverData;
        private final FaviconTexture icon;
        @Nullable
        private byte[] lastIconBytes;
        private long lastClickTime;

        protected OnlineServerEntry(PlayGameScreen joinMultiplayerScreen, ServerData serverData) {
            this.screen = joinMultiplayerScreen;
            this.serverData = serverData;
            this.minecraft = Minecraft.getInstance();
            this.icon = FaviconTexture.forServer(this.minecraft.getTextureManager(), serverData.ip);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
            List<Component> list2;
            Component component2;
            ResourceLocation resourceLocation;
            if (!this.serverData.pinged) {
                this.serverData.pinged = true;
                this.serverData.ping = -2L;
                this.serverData.motd = CommonComponents.EMPTY;
                this.serverData.status = CommonComponents.EMPTY;
                THREAD_POOL.submit(() -> {
                    try {
                        this.screen.getPinger().pingServer(this.serverData, () -> this.minecraft.execute(this::updateServerList));
                    } catch (UnknownHostException unknownHostException) {
                        this.serverData.ping = -1L;
                        this.serverData.motd = CANT_RESOLVE_TEXT;
                    } catch (Exception exception) {
                        this.serverData.ping = -1L;
                        this.serverData.motd = CANT_CONNECT_TEXT;
                    }
                });
            }
            boolean bl2 = !this.isCompatible();
            guiGraphics.drawString(this.minecraft.font, this.serverData.name, k + 32 + 3, j + 1, 0xFFFFFF, false);
            List<FormattedCharSequence> list = this.minecraft.font.split(this.serverData.motd, l - 32 - 2);
            for (int p = 0; p < Math.min(list.size(), 2); ++p) {
                guiGraphics.drawString(this.minecraft.font, list.get(p), k + 32 + 3, j + 12 + this.minecraft.font.lineHeight * p, -8355712, false);
            }
            Component component = bl2 ? this.serverData.version.copy().withStyle(ChatFormatting.RED) : this.serverData.status;
            int q = this.minecraft.font.width(component);
            guiGraphics.drawString(this.minecraft.font, component, k + l - q - 15 - 2, j + 1, -8355712, false);
            if (bl2) {
                resourceLocation = INCOMPATIBLE_SPRITE;
                component2 = INCOMPATIBLE_STATUS;
                list2 = this.serverData.playerList;
            } else if (this.pingCompleted()) {
                resourceLocation = this.serverData.ping < 0L ? UNREACHABLE_SPRITE : (this.serverData.ping < 150L ? PING_5_SPRITE : (this.serverData.ping < 300L ? PING_4_SPRITE : (this.serverData.ping < 600L ? PING_3_SPRITE : (this.serverData.ping < 1000L ? PING_2_SPRITE : PING_1_SPRITE))));
                if (this.serverData.ping < 0L) {
                    component2 = NO_CONNECTION_STATUS;
                    list2 = Collections.emptyList();
                } else {
                    component2 = Component.translatable("multiplayer.status.ping", this.serverData.ping);
                    list2 = this.serverData.playerList;
                }
            } else {
                int r = (int)(Util.getMillis() / 100L + (long)(i * 2) & 7L);
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
            guiGraphics.blitSprite(resourceLocation, k + l - 15, j, 10, 8);
            byte[] bs = this.serverData.getIconBytes();
            if (!Arrays.equals(bs, this.lastIconBytes)) {
                if (this.uploadServerIcon(bs)) {
                    this.lastIconBytes = bs;
                } else {
                    this.serverData.setIconBytes(null);
                    this.updateServerList();
                }
            }
            this.drawIcon(guiGraphics, k, j, this.icon.textureLocation());
            int s = n - k;
            int t = o - j;
            if (s >= l - 15 && s <= l - 5 && t >= 0 && t <= 8) {
                guiGraphics.renderTooltip(minecraft.font,component2, n,o);
            } else if (s >= l - q - 15 - 2 && s <= l - 15 - 2 && t >= 0 && t <= 8) {
                guiGraphics.renderComponentTooltip(minecraft.font,list2, n,o);
            }
            if (this.minecraft.options.touchscreen().get().booleanValue() || bl) {
                guiGraphics.fill(k + 5, j + 5, k + 25, j + 25, -1601138544);
                int u = n - k;
                int v = o - j;
                if (this.canJoin()) {
                    if (u < 32 && u > 16) {
                        guiGraphics.blitSprite(JOIN_HIGHLIGHTED_SPRITE, k, j, 32, 32);
                    } else {
                        guiGraphics.blitSprite(JOIN_SPRITE, k, j, 32, 32);
                    }
                }
                if (i > 0) {
                    if (u < 16 && v < 16) {
                        guiGraphics.blitSprite(MOVE_UP_HIGHLIGHTED_SPRITE, k, j, 32, 32);
                    } else {
                        guiGraphics.blitSprite(MOVE_UP_SPRITE, k, j, 32, 32);
                    }
                }
                if (i < this.screen.getServers().size() - 1) {
                    if (u < 16 && v > 16) {
                        guiGraphics.blitSprite(MOVE_DOWN_HIGHLIGHTED_SPRITE, k, j, 32, 32);
                    } else {
                        guiGraphics.blitSprite(MOVE_DOWN_SPRITE, k, j, 32, 32);
                    }
                }
            }
        }

        private boolean pingCompleted() {
            return this.serverData.pinged && this.serverData.ping != -2L;
        }

        private boolean isCompatible() {
            return this.serverData.protocol == SharedConstants.getCurrentVersion().getProtocolVersion();
        }

        public void updateServerList() {
            this.screen.getServers().save();
        }

        protected void drawIcon(GuiGraphics guiGraphics, int i, int j, ResourceLocation resourceLocation) {
            RenderSystem.enableBlend();
            guiGraphics.blit(resourceLocation, i + 5, j + 5, 0.0f, 0.0f, 20, 20, 20, 20);
            RenderSystem.disableBlend();
        }

        private boolean canJoin() {
            return true;
        }

        private boolean uploadServerIcon(@Nullable byte[] bs) {
            if (bs == null) {
                this.icon.clear();
            } else {
                try {
                    this.icon.upload(NativeImage.read(bs));
                } catch (Throwable throwable) {
                    LOGGER.error("Invalid icon for server {} ({})", this.serverData.name, this.serverData.ip, throwable);
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean keyPressed(int i, int j, int k) {
            if (Screen.hasShiftDown()) {
                ServerSelectionList serverSelectionList = this.screen.serverSelectionList;
                int l = serverSelectionList.children().indexOf(this);
                if (l == -1) {
                    return true;
                }
                if (i == 264 && l < this.screen.getServers().size() - 1 || i == 265 && l > 0) {
                    this.swap(l, i == 264 ? l + 1 : l - 1);
                    return true;
                }
            }
            if (i == InputConstants.KEY_E) {
                minecraft.setScreen(new ServerOptionsScreen(screen,serverData));
                return true;
            }
            return super.keyPressed(i, j, k);
        }

        private void swap(int i, int j) {
            this.screen.getServers().swap(i, j);
            this.screen.serverSelectionList.updateOnlineServers(this.screen.getServers());
            Entry entry = this.screen.serverSelectionList.children().get(j);
            this.screen.serverSelectionList.setSelected(entry);
            ServerSelectionList.this.ensureVisible(entry);
        }

        @Override
        public boolean mouseClicked(double d, double e, int i) {
            if (screen.tabList.selectedTab != 2) return false;
            double f = d - (double) ServerSelectionList.this.getRowLeft();
            double g = e - (double) ServerSelectionList.this.getRowTop(ServerSelectionList.this.children().indexOf(this));
            if (f <= 32.0) {
                if (f < 32.0 && f > 16.0 && this.canJoin()) {
                    this.screen.setSelected(this);
                    this.screen.joinSelectedServer();
                    return true;
                }
                int j = this.screen.serverSelectionList.children().indexOf(this);
                if (f < 16.0 && g < 16.0 && j > 0) {
                    this.swap(j, j - 1);
                    return true;
                }
                if (f < 16.0 && g > 16.0 && j < this.screen.getServers().size() - 1) {
                    this.swap(j, j + 1);
                    return true;
                }
            }
            this.screen.setSelected(this);
            if (Util.getMillis() - this.lastClickTime < 250L) {
                this.screen.joinSelectedServer();
            }
            this.lastClickTime = Util.getMillis();
            return true;
        }

        public ServerData getServerData() {
            return this.serverData;
        }

        @Override
        public Component getNarration() {
            MutableComponent mutableComponent = Component.empty();
            mutableComponent.append(Component.translatable("narrator.select", this.serverData.name));
            mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
            if (!this.isCompatible()) {
                mutableComponent.append(INCOMPATIBLE_STATUS);
                mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                mutableComponent.append(Component.translatable("multiplayer.status.version.narration", this.serverData.version));
                mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                mutableComponent.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
            } else if (this.serverData.ping < 0L) {
                mutableComponent.append(NO_CONNECTION_STATUS);
            } else if (!this.pingCompleted()) {
                mutableComponent.append(PINGING_STATUS);
            } else {
                mutableComponent.append(ONLINE_STATUS);
                mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                mutableComponent.append(Component.translatable("multiplayer.status.ping.narration", this.serverData.ping));
                mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                mutableComponent.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
                if (this.serverData.players != null) {
                    mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                    mutableComponent.append(Component.translatable("multiplayer.status.player_count.narration", this.serverData.players.online(), this.serverData.players.max()));
                    mutableComponent.append(CommonComponents.NARRATION_SEPARATOR);
                    mutableComponent.append(ComponentUtils.formatList(this.serverData.playerList, Component.literal(", ")));
                }
            }
            return mutableComponent;
        }

        @Override
        public void close() {
            this.icon.close();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class NetworkServerEntry extends Entry {
        private static final Component LAN_SERVER_HEADER = Component.translatable("lanServer.title");
        private static final Component HIDDEN_ADDRESS_TEXT = Component.translatable("selectServer.hiddenAddress");
        private final PlayGameScreen screen;
        protected final Minecraft minecraft;
        protected final LanServer serverData;
        private long lastClickTime;

        protected NetworkServerEntry(PlayGameScreen joinMultiplayerScreen, LanServer lanServer) {
            this.screen = joinMultiplayerScreen;
            this.serverData = lanServer;
            this.minecraft = Minecraft.getInstance();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
            guiGraphics.drawString(this.minecraft.font, LAN_SERVER_HEADER, k + 32 + 3, j + 1, 0xFFFFFF, false);
            guiGraphics.drawString(this.minecraft.font, this.serverData.getMotd(), k + 32 + 3, j + 12, -8355712, false);
            if (this.minecraft.options.hideServerAddress) {
                guiGraphics.drawString(this.minecraft.font, HIDDEN_ADDRESS_TEXT, k + 32 + 3, j + 12 + 11, 0x303030, false);
            } else {
                guiGraphics.drawString(this.minecraft.font, this.serverData.getAddress(), k + 32 + 3, j + 12 + 11, 0x303030, false);
            }
        }

        @Override
        public boolean mouseClicked(double d, double e, int i) {
            this.screen.setSelected(this);
            if (Util.getMillis() - this.lastClickTime < 250L) {
                this.screen.joinSelectedServer();
            }
            this.lastClickTime = Util.getMillis();
            return false;
        }

        public LanServer getServerData() {
            return this.serverData;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.getServerNarration());
        }

        public Component getServerNarration() {
            return Component.empty().append(LAN_SERVER_HEADER).append(CommonComponents.SPACE).append(this.serverData.getMotd());
        }
    }
}
