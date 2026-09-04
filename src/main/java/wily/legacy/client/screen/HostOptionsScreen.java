package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Mth;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.network.CommonNetwork;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.client.control.BindingState;
import wily.legacy.client.control.ControlType;
import wily.legacy.client.control.LegacyKeyMapping;
import wily.legacy.client.control.tooltip.ControlTooltip;
import wily.legacy.client.control.tooltip.ControlTooltipList;
import wily.legacy.client.screen.compat.WorldHostFriendsScreen;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.util.LegacySprites;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.entity.PlayerTrustPolicy;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.*;
import java.util.function.BiConsumer;

public class HostOptionsScreen extends PanelVListScreen {
    public static final Component HOST_OPTIONS = Component.translatable("legacy.menu.host_options");
    public static final Component PLAYERS_INVITE = Component.translatable("legacy.menu.players_invite");
    protected final Component title;
    protected float oldAlpha = getDefaultOpacity();
    protected float alpha = getDefaultOpacity();
    protected boolean shouldFade = false;
    protected Button hostOptionsButton;

    public HostOptionsScreen(Component title) {
        super(s -> Panel.centered(s, LegacySprites.PANEL, 250, 190, 0, 20), HOST_OPTIONS);
        this.title = title;
        addPlayerButtons();
        renderableVList.layoutSpacing(l -> 0);
    }

    public HostOptionsScreen() {
        this(PLAYERS_INVITE);
        LegacyOptions.resetAdvancedWorldOptions();
    }

    public static void drawPlayerIcon(LegacyPlayerInfo info, GuiGraphicsExtractor GuiGraphicsExtractor, int x, int y) {
        drawPlayerIcon(info, GuiGraphicsExtractor, x, y, 20, 20);
    }

    public static void drawPlayerIcon(LegacyPlayerInfo info, GuiGraphicsExtractor GuiGraphicsExtractor, int x, int y, int width, int height) {
        float[] color = Legacy4JClient.getVisualPlayerColor(info);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).setBlitColor(color[0], color[1], color[2], 1.0f);
        FactoryScreenUtil.enableBlend();
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(PlayerIdentifier.of(info.getIdentifierIndex()).optionsMapSprite(), x, y, width, height);
        FactoryScreenUtil.disableBlend();
        FactoryGuiGraphics.of(GuiGraphicsExtractor).clearBlitColor();
    }

    public static List<PlayerInfo> getActualPlayerInfos() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && !minecraft.player.connection.getOnlinePlayers().isEmpty())
            return minecraft.player.connection.getOnlinePlayers().stream().sorted(Comparator.comparingInt((p -> minecraft.hasSingleplayerServer() && minecraft.player.getGameProfile().equals(p.getProfile()) ? 0 : ((LegacyPlayerInfo) p).getIdentifierIndex()))).toList();
        return Collections.emptyList();
    }

    public static LegacyPlayerInfo getLocalPlayerInfo(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.getConnection() == null) return null;
        PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
        return playerInfo instanceof LegacyPlayerInfo info ? info : null;
    }

    public static boolean hasFullTrustAuthority(Minecraft minecraft) {
        if (minecraft.player == null) return false;
        if (minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer().getSingleplayerProfile() != null && Objects.equals(minecraft.getSingleplayerServer().getSingleplayerProfile().id(), minecraft.player.getUUID())) return true;
        LegacyPlayerInfo info = getLocalPlayerInfo(minecraft);
        return info != null && info.hasFullTrustAuthority();
    }

    public static boolean isModerator(Minecraft minecraft) {
        LegacyPlayerInfo info = getLocalPlayerInfo(minecraft);
        return info != null && info.isModerator();
    }

    public static boolean canTeleport(Minecraft minecraft) {
        if (minecraft.player == null) return false;
        LegacyPlayerInfo info = getLocalPlayerInfo(minecraft);
        return minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
                || Legacy4JClient.allowHostCheats && info != null && PlayerTrustPolicy.canTeleport(info);
    }

    protected static float getDefaultOpacity() {
        return 0.5f;
    }

    @Override
    public void addControlTooltips(ControlTooltipList list) {
        super.addControlTooltips(list);
        list.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.getIcon(), () -> minecraft.hasSingleplayerServer() ? !minecraft.getSingleplayerServer().isPublished() ? PublishScreen.getPublishComponent() : PublishScreen.hasWorldHost() ? WorldHostFriendsScreen.FRIENDS : null : null);
        list.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon(), () -> minecraft.computeChatAbilities().canSendMessages() ? LegacyKeyMapping.of(Minecraft.getInstance().options.keyChat).getDisplayName() : null);
    }

    public void reloadPlayerButtons() {
        int i = renderableVList.renderables.indexOf(getFocused());
        renderableVList.renderables.clear();
        addPlayerButtons();
        rebuildWidgets();
        if (i >= 0 && i < renderableVList.renderables.size())
            setFocused((GuiEventListener) renderableVList.renderables.get(i));
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state.is(LegacyKeyMapping.of(Legacy4JClient.keyHostOptions).getBinding()) && state.onceClick(true))
            onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (Legacy4JClient.keyHostOptions.matches(keyEvent)) {
            onClose();
            return true;
        }
        if (keyEvent.key() == InputConstants.KEY_O) {
            minecraft.setScreen(new ChatScreen("", false));
            return true;
        }
        if (keyEvent.key() == InputConstants.KEY_X && minecraft.hasSingleplayerServer()) {
            if (!minecraft.getSingleplayerServer().isPublished())
                minecraft.setScreen(new PublishScreen(this, minecraft.getSingleplayerServer().getDefaultGameType(), s -> s.publish(minecraft.getSingleplayerServer())));
            else if (PublishScreen.hasWorldHost()) minecraft.setScreen(new WorldHostFriendsScreen(this));
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    protected void addPlayerButtons() {
        addPlayerButtons(true, (playerInfo, b) -> {
            LegacyPlayerInfo target = LegacyPlayerInfo.of(playerInfo);
            boolean self = minecraft.player.getUUID().equals(playerInfo.getProfile().id());
            boolean canManageTarget = PlayerTrustPolicy.management(Legacy4JClient.trustPlayers, self, hasFullTrustAuthority(minecraft), isModerator(minecraft), target.hasFullTrustAuthority()).canKick();
            boolean canUseOwnOptions = self && Legacy4JClient.allowHostCheats && (target.isModerator() || target.getHostPrivileges().hasPlayerOptions(playerInfo.getGameMode().isSurvival()));
            if (!minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) && !canManageTarget && !canUseOwnOptions) return;
            minecraft.setScreen(new PlayerHostOptionsScreen(this, playerInfo, minecraft));
        });
    }

    protected void addPlayerButtons(boolean includeLocal, BiConsumer<PlayerInfo, AbstractButton> onPress) {
        for (PlayerInfo playerInfo : getActualPlayerInfos()) {
            if (!includeLocal && Objects.equals(playerInfo.getProfile().name(), Minecraft.getInstance().player.getGameProfile().name()))
                continue;
            renderableVList.addRenderable(new PlayerButton(0, 0, 230, 30, playerInfo) {
                @Override
                public void onPress(InputWithModifiers input) {
                    onPress.accept(playerInfo, this);
                }
            });
        }
    }

    @Override
    protected void init() {
        if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(PlayerInfoSync.askAll(minecraft.player));
        super.init();
        hostOptionsButton = null;
        addHostOptionsButton();
    }

    @Override
    protected void panelInit() {
        panel.init();
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 10, panel.y + 22, panel.width - 20, panel.height - 26);
    }

    protected void addHostOptionsButton() {
        hostOptionsButton = addRenderableWidget(accessor.putWidget("hostOptionsButton", Button.builder(HOST_OPTIONS, this::pressHostOptionsButton).bounds(panel.x, panel.y - 36, 250, 20).build()));
        refreshHostOptionsButton();
    }

    public void refreshHostOptionsButton() {
        if (hostOptionsButton == null) return;
        boolean enabled = hasFullTrustAuthority(minecraft) || isModerator(minecraft) || canTeleport(minecraft);
        hostOptionsButton.active = hostOptionsButton.visible = enabled;
        if (!enabled && getFocused() == hostOptionsButton) setFocused(null);
    }

    protected void pressHostOptionsButton(Button b) {
        minecraft.setScreen(new GameHostOptionsScreen(this, minecraft));
    }

    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderDefaultBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        oldAlpha = alpha;
        alpha = Mth.lerp(f * 0.18f, oldAlpha, shouldFade ? 1.0f : getDefaultOpacity());
        shouldFade = false;
        FactoryGuiGraphics.of(GuiGraphicsExtractor).setBlitColor(1.0f, 1.0f, 1.0f, alpha);
        FactoryScreenUtil.enableBlend();
        panel.extractRenderState(GuiGraphicsExtractor, i, j, f);
        FactoryScreenUtil.disableBlend();
        FactoryGuiGraphics.of(GuiGraphicsExtractor).setBlitColor(1.0f, 1.0f, 1.0f, 1.0f);
        LegacyFontUtil.applySDFont(sd -> GuiGraphicsExtractor.text(font, title, panel.x + accessor.getInteger("title.x", 11), panel.y + accessor.getInteger("title.y", 8), CommonColor.GRAY_TEXT.get(), false));
    }

    protected abstract class PlayerButton extends ListButton implements RenderableVListEntry, ControlTooltip.ActionHolder {
        public final PlayerInfo playerInfo;

        public PlayerButton(int x, int y, int width, int height, PlayerInfo playerInfo) {
            super(null, x, y, width, height, Component.literal(playerInfo.getProfile().name()));
            this.playerInfo = playerInfo;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
            if (isHoveredOrFocused()) shouldFade = true;
            super.extractContents(GuiGraphicsExtractor, i, j, f);
            drawPlayerIcon((LegacyPlayerInfo) playerInfo, GuiGraphicsExtractor,
                    getX() + list.accessor.getInteger(list.name + ".playerIcon.x", 6),
                    getY() + list.accessor.getInteger(list.name + ".playerIcon.y", 5),
                    list.accessor.getInteger(list.name + ".playerIcon.width", 20),
                    list.accessor.getInteger(list.name + ".playerIcon.height", 20));
        }

        @Override
        protected void renderScrollingString(GuiGraphicsExtractor GuiGraphicsExtractor, Font font, int i, int j) {
            LegacyFontUtil.applySDFont(sd -> LegacyRenderUtil.renderScrollingString(GuiGraphicsExtractor, font, this.getMessage(), getX() + list.accessor.getInteger(list.name + ".buttonMessage.x", 68), this.getY(), getX() + getWidth() - i, this.getY() + this.getHeight(), j, true));
        }

        @Override
        public void initRenderable(RenderableVList list) {
            this.list = list;
            setHeight(list.accessor.getInteger("buttonsHeight", 30));
        }

        @Override
        public Component getAction(Object context) {
            return context instanceof KeyContext c ? c.key() == InputConstants.KEY_RETURN && isHovered() ? LegacyComponents.PRIVILEGES : ControlTooltip.getSelectAction(this, c) : null;
        }
    }

}
