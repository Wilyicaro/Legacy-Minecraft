package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.entity.PlayerHostPrivileges;
import wily.legacy.entity.PlayerTrustPermissions;
import wily.legacy.entity.PlayerTrustPolicy;
import wily.legacy.network.PlayerHostPrivilegesUpdatePayload;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.network.PlayerTrustUpdatePayload;
import wily.legacy.network.ServerHostOptionsPayload;
import wily.legacy.util.LegacySprites;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static wily.legacy.client.screen.LoadSaveScreen.GAME_MODEL_LABEL;

public class PlayerHostOptionsScreen extends PanelVListScreen {
    protected final PlayerInfo playerInfo;
    protected final Map<AbstractWidget, Runnable> commandsOnClose = new HashMap<>();
    protected final List<TickBox> hostPrivilegeOptions = new ArrayList<>();
    protected final PlayerTrustPermissions initialTrustPermissions;
    protected final PlayerHostPrivileges initialHostPrivileges;
    protected final boolean initialModerator;
    protected final boolean hasAdditionalTrustOptions;
    protected PlayerTrustPermissions trustPermissions;
    protected PlayerHostPrivileges hostPrivileges;
    protected boolean moderator;
    protected boolean changesApplied;

    public PlayerHostOptionsScreen(Screen parent, PlayerInfo playerInfo, Minecraft minecraft) {
        super(parent, s -> Panel.createPanel(s, p -> p.appearance(LegacySprites.PANEL, 280, ((PlayerHostOptionsScreen) s).getPanelHeight())), HostOptionsScreen.HOST_OPTIONS);
        this.playerInfo = playerInfo;

        boolean isSurvival = playerInfo.getGameMode().isSurvival();
        boolean canManageCheats = minecraft.player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        boolean self = minecraft.player.getUUID().equals(playerInfo.getProfile().id());
        LegacyPlayerInfo targetInfo = LegacyPlayerInfo.of(playerInfo);
        boolean fullAuthority = HostOptionsScreen.hasFullTrustAuthority(minecraft);
        boolean moderatorAuthority = HostOptionsScreen.isModerator(minecraft);
        PlayerTrustPolicy.Management management = PlayerTrustPolicy.management(Legacy4JClient.trustPlayers, self, fullAuthority, moderatorAuthority, targetInfo.hasFullTrustAuthority());
        boolean canManageTrust = management.canManageTrust();
        boolean canSetModerator = management.canSetModerator();
        boolean canKick = management.canKick();
        boolean canManageHostPrivileges = (canManageCheats || Legacy4JClient.allowHostCheats && moderatorAuthority) && !self && !targetInfo.hasFullTrustAuthority();
        boolean canManagePlayerOptions = canManageCheats || Legacy4JClient.allowHostCheats && (moderatorAuthority || self && targetInfo.getHostPrivileges().hasPlayerOptions(isSurvival));
        hasAdditionalTrustOptions = canManageTrust || canManageHostPrivileges || canSetModerator || canKick;

        accessor.addStatic(UIDefinition.createBeforeInit(a -> {
            a.putStaticElement("isSurvival", isSurvival);
            a.putStaticElement("hasAdditionalTrustOptions", hasAdditionalTrustOptions);
        }));

        initialTrustPermissions = targetInfo.getTrustPermissions();
        initialHostPrivileges = targetInfo.getHostPrivileges();
        initialModerator = targetInfo.isModerator();
        trustPermissions = initialTrustPermissions;
        hostPrivileges = initialHostPrivileges;
        moderator = initialModerator;

        TickBox[] invisibleOption = new TickBox[1];
        TickBox[] flyingOption = new TickBox[1];
        TickBox[] exhaustionOption = new TickBox[1];

        if (canManageTrust) {
            getRenderableVList().addRenderable(new TickBox(0, 0, trustPermissions.canBuildAndMine(), b -> Component.translatable("legacy.menu.host_options.player.canBuildAndMine"), b -> null, b -> trustPermissions = trustPermissions.withCanBuildAndMine(b.selected)));
            getRenderableVList().addRenderable(new TickBox(0, 0, trustPermissions.canUseDoorsAndSwitches(), b -> Component.translatable("legacy.menu.host_options.player.canUseDoorsAndSwitches"), b -> null, b -> trustPermissions = trustPermissions.withCanUseDoorsAndSwitches(b.selected)));
            getRenderableVList().addRenderable(new TickBox(0, 0, trustPermissions.canOpenContainers(), b -> Component.translatable("legacy.menu.host_options.player.canOpenContainers"), b -> null, b -> trustPermissions = trustPermissions.withCanOpenContainers(b.selected)));
            getRenderableVList().addRenderable(new TickBox(0, 0, trustPermissions.canAttackPlayers(), b -> Component.translatable("legacy.menu.host_options.player.canAttackPlayers"), b -> null, b -> trustPermissions = trustPermissions.withCanAttackPlayers(b.selected)));
            getRenderableVList().addRenderable(new TickBox(0, 0, trustPermissions.canAttackAnimals(), b -> Component.translatable("legacy.menu.host_options.player.canAttackAnimals"), b -> null, b -> trustPermissions = trustPermissions.withCanAttackAnimals(b.selected)));
        }
        if (canManageHostPrivileges) {
            addHostPrivilege(new TickBox(0, 0, hostPrivileges.canBecomeInvisible(), b -> Component.translatable("legacy.menu.host_options.player.canBecomeInvisible"), b -> null, b -> {
                hostPrivileges = hostPrivileges.withCanBecomeInvisible(b.selected);
                setPlayerOptionEnabled(invisibleOption[0], b.selected);
            }));
            if (isSurvival) {
                addHostPrivilege(new TickBox(0, 0, hostPrivileges.canFly(), b -> Component.translatable("legacy.menu.host_options.player.canFly"), b -> null, b -> {
                    hostPrivileges = hostPrivileges.withCanFly(b.selected);
                    setPlayerOptionEnabled(flyingOption[0], b.selected);
                }));
                addHostPrivilege(new TickBox(0, 0, hostPrivileges.canDisableExhaustion(), b -> Component.translatable("legacy.menu.host_options.player.canDisableExhaustion"), b -> null, b -> {
                    hostPrivileges = hostPrivileges.withCanDisableExhaustion(b.selected);
                    setPlayerOptionEnabled(exhaustionOption[0], b.selected);
                }));
            }
            addHostPrivilege(new TickBox(0, 0, hostPrivileges.canTeleport(), b -> Component.translatable("legacy.menu.host_options.player.canTeleport"), b -> null, b -> hostPrivileges = hostPrivileges.withCanTeleport(b.selected)));
        }
        if (canSetModerator)
            getRenderableVList().addRenderable(new TickBox(0, 0, moderator, b -> Component.translatable("legacy.menu.host_options.player.moderator"), b -> null, b -> {
                moderator = b.selected;
                hostPrivileges = moderator ? PlayerHostPrivileges.ALL : PlayerHostPrivileges.NONE;
                hostPrivilegeOptions.forEach(option -> option.selected = moderator);
                for (TickBox option : Arrays.asList(invisibleOption[0], flyingOption[0], exhaustionOption[0]))
                    setPlayerOptionEnabled(option, moderator);
            }));
        if (canKick)
            getRenderableVList().addRenderable(new LegacyButton(Component.translatable("legacy.menu.host_options.player.kick"), b -> minecraft.setScreen(new ConfirmationScreen(this, Component.translatable("legacy.menu.host_options.player.kick"), Component.translatable("legacy.menu.host_options.player.kick.confirmation"), screen -> kickPlayer()))));

        if (!canManagePlayerOptions) return;

        boolean initialVisibility = !targetInfo.isVisible();

        invisibleOption[0] = new TickBox(0, 0, initialVisibility, b1 -> Component.translatable("legacy.menu.host_options.player.invisible"), b1 -> null, b1 -> {
            if (initialVisibility != b1.selected) {
                commandsOnClose.put(b1, () -> {
                    if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(PlayerInfoSync.invisibility(b1.selected, playerInfo.getProfile()));
                });
            } else commandsOnClose.remove(b1);
        });
        invisibleOption[0].active = self && canManageCheats || hostPrivileges.canBecomeInvisible();
        getRenderableVList().addRenderable(invisibleOption[0]);
        if (playerInfo.getGameMode().isSurvival()) {
            boolean initialMayFly = targetInfo.mayFlySurvival();
            flyingOption[0] = new TickBox(0, 0, initialMayFly, b1 -> Component.translatable("legacy.menu.host_options.player.mayFly"), b1 -> null, b1 -> {
                if (initialMayFly != b1.selected) commandsOnClose.put(b1, () -> CommonNetwork.sendToServer(PlayerInfoSync.mayFlySurvival(b1.selected, playerInfo.getProfile())));
                else commandsOnClose.remove(b1);
            });
            flyingOption[0].active = self && canManageCheats || hostPrivileges.canFly();
            getRenderableVList().addRenderable(flyingOption[0]);
            boolean initialExhaustionDisabled = targetInfo.isExhaustionDisabled();
            exhaustionOption[0] = new TickBox(0, 0, initialExhaustionDisabled, b1 -> Component.translatable("legacy.menu.host_options.player.disableExhaustion"), b1 -> null, b1 -> {
                if (initialExhaustionDisabled != b1.selected) commandsOnClose.put(b1, () -> CommonNetwork.sendToServer(PlayerInfoSync.disableExhaustion(b1.selected, playerInfo.getProfile())));
                else commandsOnClose.remove(b1);
            });
            exhaustionOption[0].active = self && canManageCheats || hostPrivileges.canDisableExhaustion();
            getRenderableVList().addRenderable(exhaustionOption[0]);
        }
        if (!canManageCheats && !(Legacy4JClient.allowHostCheats && moderatorAuthority)) return;
        List<GameType> gameTypes = Arrays.stream(GameType.values()).toList();
        getRenderableVList().addRenderable(new LegacySliderButton<>(0, 0, 230, 16, b1 -> b1.getDefaultMessage(GAME_MODEL_LABEL, b1.getObjectValue().getShortDisplayName()), (b1) -> Tooltip.create(Component.translatable("selectWorld.gameMode." + playerInfo.getGameMode().getName() + ".info")), playerInfo.getGameMode(), () -> gameTypes, b1 -> commandsOnClose.put(b1, () -> {
            if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(ServerHostOptionsPayload.gameMode(b1.getObjectValue(), playerInfo.getProfile().id()));
            else minecraft.getConnection().sendCommand("gamemode %s %s".formatted(b1.getObjectValue().getName(), playerInfo.getProfile().name()));
        })));
        getRenderableVList().addRenderable(new LegacyButton(0, 0, 215, 20, Component.translatable("legacy.menu.host_options.set_player_spawn"), b1 -> commandsOnClose.put(b1, () -> {
            if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(ServerHostOptionsPayload.playerSpawn(playerInfo.getProfile().id()));
            else minecraft.player.connection.sendCommand("spawnpoint %s ~ ~ ~".formatted(playerInfo.getProfile().name()));
        })));
    }

    protected void addHostPrivilege(TickBox option) {
        hostPrivilegeOptions.add(option);
        getRenderableVList().addRenderable(option);
    }

    protected void setPlayerOptionEnabled(TickBox option, boolean enabled) {
        if (option == null) return;
        if (!enabled && option.selected) option.onPress(null);
        option.active = enabled;
    }

    protected int getPanelHeight() {
        if (!hasAdditionalTrustOptions) return playerInfo.getGameMode().isSurvival() ? 120 : 88;
        int contentHeight = 35;
        for (Renderable renderable : getRenderableVList().renderables) {
            if (renderable instanceof LayoutElement element) {
                contentHeight += renderable instanceof TickBox ? TickBox.getDefaultHeight() : renderable instanceof LegacyButton ? LegacyButton.getDefaultHeight() : element.getHeight();
                contentHeight += getRenderableVList().layoutSeparation.apply(element);
            }
        }
        return Math.min(LegacyOptions.getUIMode().isSD() ? 120 : 200, contentHeight);
    }

    protected void applyChanges() {
        if (changesApplied) return;
        changesApplied = true;
        if (Legacy4JClient.hasModOnServer() && (!trustPermissions.equals(initialTrustPermissions) || moderator != initialModerator))
            CommonNetwork.sendToServer(PlayerTrustUpdatePayload.forPlayer(playerInfo.getProfile(), trustPermissions, moderator));
        PlayerHostPrivileges expectedPrivileges = moderator == initialModerator ? initialHostPrivileges : moderator ? PlayerHostPrivileges.ALL : PlayerHostPrivileges.NONE;
        if (Legacy4JClient.hasModOnServer() && !hostPrivileges.equals(expectedPrivileges))
            CommonNetwork.sendToServer(PlayerHostPrivilegesUpdatePayload.forPlayer(playerInfo.getProfile(), hostPrivileges));
        commandsOnClose.values().forEach(Runnable::run);
    }

    public void closeIfPlayerLeft() {
        if (minecraft.getConnection() == null || minecraft.getConnection().getPlayerInfo(playerInfo.getProfile().id()) == null)
            minecraft.setScreen(parent);
    }

    protected void kickPlayer() {
        applyChanges();
        if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(ServerHostOptionsPayload.kick(playerInfo.getProfile().id()));
        minecraft.setScreen(parent);
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 8, panel.y + 27, panel.width - 16, panel.height - (hasAdditionalTrustOptions ? 35 : 16));
    }

    @Override
    protected void panelInit() {
        panel.init();
    }

    @Override
    public void onClose() {
        applyChanges();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderDefaultBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        panel.extractRenderState(GuiGraphicsExtractor, i, j, f);
        HostOptionsScreen.drawPlayerIcon((LegacyPlayerInfo) playerInfo, GuiGraphicsExtractor, panel.x + 7, panel.y + 5);
        GuiGraphicsExtractor.text(font, playerInfo.getProfile().name(), panel.x + 31, panel.y + 12, CommonColor.GRAY_TEXT.get(), false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        super.extractRenderState(GuiGraphicsExtractor, i, j, f);
        if (LegacyOptions.useLegacyWorldOptions()) GuiGraphicsExtractor.deferredTooltip = null;
    }
}
