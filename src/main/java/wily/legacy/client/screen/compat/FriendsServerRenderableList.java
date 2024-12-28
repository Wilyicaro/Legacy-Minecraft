package wily.legacy.client.screen.compat;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.gaming32.worldhost.FriendsListUpdate;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.mixin.ServerStatusPingerAccessor;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.JoinGameScreen;
import wily.legacy.client.screen.PlayGameScreen;
import wily.legacy.client.screen.ServerRenderableList;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.*;

public class FriendsServerRenderableList extends ServerRenderableList {
    boolean ping = false;
    protected final FriendsListUpdate friendsListUpdate = friends -> updateServers();

    public FriendsServerRenderableList(UIDefinition.Accessor accessor) {
        super(accessor);
    }

    public void added() {
        WorldHost.ONLINE_FRIEND_UPDATES.add(friendsListUpdate);
    }
    @Override
    public void removed() {
        super.removed();
        WorldHost.ONLINE_FRIEND_UPDATES.remove(friendsListUpdate);
    }

    public boolean hasOnlineFriends(){
        return !WorldHost.ONLINE_FRIENDS.isEmpty();
    }
    @Override
    public void updateServers() {
        if (!ping) {
            WorldHost.pingFriends();
            ping = true;
        }
        super.updateServers();
        Util.backgroundExecutor().execute(()-> {
            WorldHost.ONLINE_FRIENDS.forEach(((uuid, id) -> {
                AbstractButton onlineButton;
                GameProfile profile = WorldHost.fetchProfile(minecraft.getMinecraftSessionService(), uuid);
                addRenderable(onlineButton = new AbstractButton(0, 0, 270, 30, Component.literal(profile.getName())) {
                    final ServerData serverData = new ServerData("", "", /*? if >1.20.2 {*/ServerData.Type.OTHER/*?} else {*//*false*//*?}*/);
                    final FaviconTexture icon = FaviconTexture.forServer(minecraft.getTextureManager(), serverData.ip);
                    private byte @Nullable [] lastIconBytes;

                    private boolean uploadServerIcon(@Nullable byte[] bs) {
                        if (bs == null) {
                            icon.clear();
                        } else {
                            try {
                                icon.upload(NativeImage.read(bs));
                            } catch (Throwable throwable) {
                                LOGGER.error("Invalid icon for server {} ({})", serverData.name, serverData.ip, throwable);
                                return false;
                            }
                        }
                        return true;
                    }

                    @Override
                    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                        ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + 35, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), j, true);
                    }

                    @Override
                    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float f) {
                        super.renderWidget(guiGraphics, mouseX, mouseY, f);
                        updateServerInfo();
                        byte[] bs = serverData.getIconBytes();
                        if (!Arrays.equals(bs, this.lastIconBytes)) {
                            if (this.uploadServerIcon(bs)) this.lastIconBytes = bs;
                            else serverData.setIconBytes(null);
                        }
                        if (serverData.getIconBytes() == null) PlayerFaceRenderer.draw(guiGraphics, minecraft.getSkinManager()./*? if >1.20.1 {*/getInsecureSkin/*?} else {*//*getInsecureSkinLocation*//*?}*/(profile),getX() + 5, getY() + 5, 20);
                        else drawIcon(guiGraphics, getX(), getY(),icon.textureLocation());
                        if (minecraft.options.touchscreen().get().booleanValue() || isHovered) {
                            guiGraphics.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);
                            int u = mouseX - getX();
                            FactoryGuiGraphics.of(guiGraphics).blitSprite(u < 30 && u > 5 ? LegacySprites.JOIN_HIGHLIGHTED : LegacySprites.JOIN, getX(), getY(), 32, 32);
                        }
                    }


                    @Override
                    public void onPress() {
                        if (isFocused()) {
                            minecraft.setScreen(new JoinGameScreen(getScreen(), serverData,b-> {
                                WorldHost.LOGGER.info("Requesting to join {}", profile.getId());
                                if (WorldHost.protoClient != null) id.joinWorld(getScreen());
                            }));
                        }
                    }

                    private void updateServerInfo() {
                        serverData.name = profile.getName();
                        final var metadata = WorldHost.ONLINE_FRIEND_PINGS.get(profile.getId());
                        if (metadata == null) {
                            serverData.status = Component.empty();
                            serverData.motd = Component.empty();
                            return;
                        }

                        serverData.motd = metadata.description();
                        metadata.version().ifPresentOrElse(version -> {
                            serverData.version = Component.literal(version.name());
                            serverData.protocol = version.protocol();
                        }, () -> {
                            serverData.version = Component.translatable("multiplayer.status.old");
                            serverData.protocol = 0;
                        });
                        metadata.players().ifPresentOrElse(players -> {
                            serverData.status = Component.translatable("multiplayer.status.player_count", Component.literal(players.online()+"").withStyle(ChatFormatting.GRAY), Component.literal(players.max()+"").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY);
                            serverData.players = players;
                            if (!players.sample().isEmpty()) {
                                final List<Component> playerList = new ArrayList<>(players.sample().size());

                                for (GameProfile gameProfile : players.sample()) {
                                    playerList.add(Component.literal(gameProfile.getName()));
                                }

                                if (players.sample().size() < players.online()) {
                                    playerList.add(Component.translatable(
                                            "multiplayer.status.and_more",
                                            players.online() - players.sample().size()
                                    ));
                                }

                                serverData.playerList = playerList;
                            } else {
                                serverData.playerList = List.of();
                            }
                        }, () -> serverData.status = Component.translatable("multiplayer.status.unknown").withStyle(ChatFormatting.DARK_GRAY));
                        metadata.favicon().ifPresent(favicon -> {
                            if (!Arrays.equals(favicon.iconBytes(), serverData.getIconBytes())) {
                                serverData.setIconBytes(favicon.iconBytes());
                            }
                        });
                    }

                    @Override
                    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                        defaultButtonNarrationText(narrationElementOutput);
                    }
                });
                if (accessor.getChildren().contains(onlineButton))
                    this.minecraft.getNarrator().say(Component.translatable("multiplayer.lan.server_found", onlineButton.getMessage()));
            }
            ));
            if (!WorldHost.ONLINE_FRIENDS.isEmpty()) accessor.reloadUI();
        });
    }
}
