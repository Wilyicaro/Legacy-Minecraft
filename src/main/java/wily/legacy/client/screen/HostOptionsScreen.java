package wily.legacy.client.screen;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.PlayerIdentifier;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.client.screen.compat.WorldHostFriendsScreen;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.LegacySprites;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.util.ScreenUtil;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static wily.legacy.client.screen.LoadSaveScreen.GAME_MODEL_LABEL;

public class HostOptionsScreen extends PanelVListScreen {
    public static final Component HOST_OPTIONS = Component.translatable("legacy.menu.host_options");
    public static final Component PLAYERS_INVITE = Component.translatable("legacy.menu.players_invite");
    protected final Component title;
    protected float alpha = getDefaultOpacity();
    protected boolean shouldFade = false;

    public static final List<GameRules.Key<GameRules.BooleanValue>> WORLD_RULES = new ArrayList<>(List.of(GameRules.RULE_DOFIRETICK,LegacyGameRules.TNT_EXPLODES,GameRules.RULE_DAYLIGHT,GameRules.RULE_KEEPINVENTORY,GameRules.RULE_DOMOBSPAWNING,GameRules.RULE_MOBGRIEFING, LegacyGameRules.GLOBAL_MAP_PLAYER_ICON));
    public static final List<GameRules.Key<GameRules.BooleanValue>> OTHER_RULES = new ArrayList<>(List.of(GameRules.RULE_WEATHER_CYCLE,GameRules.RULE_DOMOBLOOT,GameRules.RULE_DOBLOCKDROPS,GameRules.RULE_NATURAL_REGENERATION,GameRules.RULE_DO_IMMEDIATE_RESPAWN));

    public HostOptionsScreen(Component title) {
        super(s-> Panel.centered(s, LegacySprites.PANEL,250,190, 0, 20),HOST_OPTIONS);
        this.title = title;
        addPlayerButtons();
        renderableVList.layoutSpacing(l->0);
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), ()-> minecraft.hasSingleplayerServer() ? !minecraft.getSingleplayerServer().isPublished() ? PublishScreen.PUBLISH : PublishScreen.hasWorldHost() ? WorldHostFriendsScreen.FRIENDS : null : null);
        renderer.add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(),()-> minecraft.getChatStatus().isChatAllowed(minecraft.isLocalServer()) ? LegacyKeyMapping.of(Minecraft.getInstance().options.keyChat).getDisplayName() : null);
    }

    public HostOptionsScreen() {
        this(PLAYERS_INVITE);
    }
    public void reloadPlayerButtons(){
        int i = renderableVList.renderables.indexOf(getFocused());
        renderableVList.renderables.clear();
        addPlayerButtons();
        rebuildWidgets();
        if (i >= 0 &&  i < renderableVList.renderables.size()) setFocused((GuiEventListener) renderableVList.renderables.get(i));
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state.is(LegacyKeyMapping.of(Legacy4JClient.keyHostOptions).getBinding()) && state.onceClick(true)) onClose();
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (Legacy4JClient.keyHostOptions.matches(i,j)){
            onClose();
            return true;
        }
        if (i== InputConstants.KEY_O){
            minecraft.setScreen(new ChatScreen(""){
                boolean released = false;
                public boolean charTyped(char c, int i) {
                    if (!released) return false;
                    return super.charTyped(c, i);
                }
                @Override
                public boolean keyReleased(int i2, int j, int k) {
                    if (i2 == i) released = true;
                    return super.keyReleased(i2, j, k);
                }
            });
            return true;
        }
        if (i == InputConstants.KEY_X && minecraft.hasSingleplayerServer()){
            if (!minecraft.getSingleplayerServer().isPublished()) minecraft.setScreen(new PublishScreen(this, minecraft.getSingleplayerServer().getDefaultGameType(), s -> s.publish(minecraft.getSingleplayerServer())){
                boolean released = false;
                public boolean charTyped(char c, int i) {
                    if (!released) return false;
                    return super.charTyped(c, i);
                }
                @Override
                public boolean keyReleased(int i2, int j, int k) {
                    if (i2 == i) released = true;
                    return super.keyReleased(i2, j, k);
                }
            });
            else if (PublishScreen.hasWorldHost()) minecraft.setScreen(new WorldHostFriendsScreen(this));
            return true;
        }
        return super.keyPressed(i, j, k);
    }

    public static void drawPlayerIcon(GameProfile profile, GuiGraphics guiGraphics, int x, int y){
        LegacyPlayerInfo info = (LegacyPlayerInfo)Minecraft.getInstance().getConnection().getPlayerInfo(profile.getId());
        float[] color = Legacy4JClient.getVisualPlayerColor(info);
        FactoryGuiGraphics.of(guiGraphics).setColor(color[0],color[1],color[2],1.0f);
        RenderSystem.enableBlend();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(PlayerIdentifier.of(info.getIdentifierIndex()).optionsMapSprite(),x,y, 20,20);
        RenderSystem.disableBlend();
        FactoryGuiGraphics.of(guiGraphics).clearColor();
    }
    protected void addPlayerButtons(){
        addPlayerButtons(true,(profile, b)->{
            if (!minecraft.player.hasPermissions(2)) return;
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(profile.getId());
            Map<AbstractWidget,Runnable> COMMAND_MAP = new HashMap<>();
            boolean initialVisibility = !((LegacyPlayerInfo)playerInfo).isVisible();
            PanelVListScreen screen = new PanelVListScreen(this, s-> Panel.centered(s, LegacySprites.PANEL,280, playerInfo.getGameMode().isSurvival() ? 120 : 88), HOST_OPTIONS){
                @Override
                protected void init() {
                    panel.init();
                    renderableVList.init(panel.x + 8,panel.y + 27,panel.width - 16,panel.height - 16);
                }

                @Override
                public void onClose() {
                    COMMAND_MAP.values().forEach(Runnable::run);
                    super.onClose();
                }

                @Override
                public boolean isPauseScreen() {
                    return false;
                }
                @Override
                public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
                    panel.render(guiGraphics,i,j,f);
                    drawPlayerIcon(profile,guiGraphics, panel.x + 7,panel.y + 5);
                    guiGraphics.drawString(font,profile.getName(),panel.x + 31, panel.y + 12, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
                }
            };
            List<GameType> gameTypes = Arrays.stream(GameType.values()).toList();
            screen.renderableVList.addRenderable(new TickBox(0,0,initialVisibility,b1-> Component.translatable("legacy.menu.host_options.player.invisible"),b1-> null, b1->{
                if (initialVisibility != b1.selected){
                    COMMAND_MAP.put(b1,()->{
                    if (b1.selected) {
                        minecraft.player.connection.sendCommand("effect give %s minecraft:invisibility infinite 255 true".formatted(profile.getName()));
                        minecraft.player.connection.sendCommand("effect give %s minecraft:resistance infinite 255 true".formatted(profile.getName()));
                    }else {
                        minecraft.player.connection.sendCommand("effect clear %s minecraft:invisibility".formatted(profile.getName()));
                        minecraft.player.connection.sendCommand("effect clear %s minecraft:resistance".formatted(profile.getName()));
                    }});
                }
            }));
            if (playerInfo.getGameMode().isSurvival()){
                screen.renderableVList.addRenderable(new TickBox(0,0,((LegacyPlayerInfo) playerInfo).mayFlySurvival(),b1-> Component.translatable("legacy.menu.host_options.player.mayFly"),b1-> null, b1-> CommonNetwork.sendToServer(new PlayerInfoSync(b1.selected ? 5 : 6, profile))));
                screen.renderableVList.addRenderable(new TickBox(0,0,((LegacyPlayerInfo) playerInfo).isExhaustionDisabled(),b1-> Component.translatable("legacy.menu.host_options.player.disableExhaustion"),b1-> null, b1-> CommonNetwork.sendToServer(new PlayerInfoSync(b1.selected ? 3 : 4, profile))));
            }
            screen.renderableVList.addRenderable(new LegacySliderButton<>(0, 0, 230,16, b1 -> b1.getDefaultMessage(GAME_MODEL_LABEL,b1.getObjectValue().getLongDisplayName()),(b1)->Tooltip.create(Component.translatable("selectWorld.gameMode."+playerInfo.getGameMode().getName()+ ".info")),playerInfo.getGameMode(),()->gameTypes, b1->COMMAND_MAP.put(b1,()-> minecraft.getConnection().sendCommand("gamemode %s %s".formatted(b1.getObjectValue().getName(),profile.getName())))));
            screen.renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.host_options.set_player_spawn"),b1-> COMMAND_MAP.put(b1,()-> minecraft.player.connection.sendCommand("spawnpoint %s ~ ~ ~".formatted(profile.getName())))).bounds(0,0,215,20).build());
            minecraft.setScreen(screen);
        });
    }
    protected void addPlayerButtons(boolean includeClient, BiConsumer<GameProfile, AbstractButton> onPress){
        for (GameProfile profile : getActualGameProfiles()) {
            if (!includeClient && Objects.equals(profile.getName(), Minecraft.getInstance().player.getGameProfile().getName())) continue;
            renderableVList.addRenderable(new AbstractButton(0,0, 230, 30,Component.literal(profile.getName())) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                    if (isHoveredOrFocused()) shouldFade = true;
                    super.renderWidget(guiGraphics, i, j, f);
                    drawPlayerIcon(profile, guiGraphics,getX() + 6, getY() + 5);
                }
                @Override
                protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                    ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), getX() + 68, this.getY(), getX() + getWidth(), this.getY() + this.getHeight(), j,true);
                }
                @Override
                public void onPress() {
                    onPress.accept(profile,this);
                }
                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    defaultButtonNarrationText(narrationElementOutput);
                }
            });
        }
    }
    public static List<GameProfile> getActualGameProfiles(){
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hasSingleplayerServer()) return minecraft.getSingleplayerServer().getPlayerList().getPlayers().stream().map(Player::getGameProfile).toList();
        if (minecraft.player != null && !minecraft.player.connection.getOnlinePlayers().isEmpty())
            return minecraft.player.connection.getOnlinePlayers().stream().sorted(Comparator.comparingInt((p -> /*? if >1.20.1 {*/minecraft.isLocalPlayer/*?} else {*//*minecraft.player.getUUID().equals*//*?}*/(p.getProfile().getId()) ? 0 : ((LegacyPlayerInfo)p).getIdentifierIndex()))).map(PlayerInfo::getProfile).toList();
        return Collections.emptyList();
    }

    @Override
    protected void init() {
        CommonNetwork.sendToServer(new PlayerInfoSync(0,minecraft.player));
        panel.init();
        addHostOptionsButton();
        renderableVList.init(panel.x + 10,panel.y + 22,panel.width - 20,panel.height - 20);
    }
    protected void addHostOptionsButton(){
        if (!minecraft.player.hasPermissions(2) && !minecraft.hasSingleplayerServer()) return;
        addRenderableWidget(Button.builder(HOST_OPTIONS,this::pressHostOptionsButton).bounds(panel.x, panel.y - 36,250,20).build());
    }
    protected void pressHostOptionsButton(Button b){
        Map<String,Object> nonOpGamerules = new HashMap<>();
        Map<AbstractWidget,Runnable> COMMAND_MAP = new HashMap<>();

        PanelVListScreen screen = new PanelVListScreen(this,s-> Panel.centered(s, LegacySprites.PANEL, 265,minecraft.player.hasPermissions(2) ? 200 : 118), HOST_OPTIONS){
            @Override
            protected void init() {
                panel.init();
                renderableVList.init(panel.x + 8,panel.y + 8,panel.width - 16,panel.height - 16);
            }
            public void onClose() {
                super.onClose();
                COMMAND_MAP.values().forEach(Runnable::run);
                if (!nonOpGamerules.isEmpty()) CommonNetwork.sendToServer(new PlayerInfoSync.All(nonOpGamerules,PlayerInfoSync.All.ID_S2C));
            }
            public boolean isPauseScreen() {
                return false;
            }

            @Override
            public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
                panel.render(guiGraphics,i,j,f);
            }
        };
        if (!minecraft.player.hasPermissions(2)){
            for (GameRules.Key<GameRules.BooleanValue> key : PlayerInfoSync.All.NON_OP_GAMERULES)
                screen.renderableVList.addRenderable(new TickBox(0,0,Legacy4JClient.gameRules.getRule(key).get(),b1-> Component.translatable(key.getDescriptionId()),b1-> null, b1-> nonOpGamerules.put(key.getId(),b1.selected)));
            minecraft.setScreen(screen);
            return;
        }
        List<String> weathers = List.of("clear","rain","thunder");
        int initialWeather = minecraft.level.isThundering() ? 2 : minecraft.level.isRaining() ? 1 : 0;
        screen.renderableVList.layoutSpacing(l-> 2);

        for (GameRules.Key<GameRules.BooleanValue> key : WORLD_RULES)
            screen.renderableVList.addRenderable(new TickBox(0,0,Legacy4JClient.gameRules.getRule(key).get(),b1-> Component.translatable(key.getDescriptionId()),b1-> null, b1-> COMMAND_MAP.put(b1,()->minecraft.player.connection.sendCommand("gamerule %s %s".formatted(key.getId(), b1.selected)))));
        screen.renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.host_options.set_day"),b1-> minecraft.player.connection.sendCommand("time set day")).bounds(0,0,215,20).build());
        screen.renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.host_options.set_night"),b1-> minecraft.player.connection.sendCommand("time set 14000")).bounds(0,0,215,20).build());
        screen.renderableVList.addRenderable(new LegacySliderButton<>(0,0, 230,16, b1 -> b1.getDefaultMessage(Component.translatable("options.difficulty"),b1.getObjectValue().getDisplayName()),b1-> Tooltip.create(minecraft.level.getDifficulty().getInfo()),minecraft.level.getDifficulty(),()-> Arrays.asList(Difficulty.values()), b1->COMMAND_MAP.put(b1,()->minecraft.getConnection().sendCommand("difficulty " + b1.getObjectValue().getKey()))));
        Supplier<GameType> gameType = ()->Legacy4JClient.defaultServerGameType == null ? minecraft.gameMode.getPlayerMode() : Legacy4JClient.defaultServerGameType;
        List<GameType> gameTypes = Arrays.stream(GameType.values()).toList();
        screen.renderableVList.addRenderable(new LegacySliderButton<>(0, 0, 230,16, b1 -> b1.getDefaultMessage(GAME_MODEL_LABEL,b1.getObjectValue().getLongDisplayName()),b1->Tooltip.create(Component.translatable("selectWorld.gameMode."+gameType.get().getName()+ ".info")),gameType.get(),()->gameTypes, b1->COMMAND_MAP.put(b1,()->minecraft.getConnection().sendCommand("defaultgamemode " + b1.getObjectValue().getName()))));
        screen.renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.host_options.set_world_spawn"),b1-> COMMAND_MAP.put(b1,()->minecraft.player.connection.sendCommand("setworldspawn"))).bounds(0,0,215,20).build());
        screen.renderableVList.addRenderables(SimpleLayoutRenderable.create(240, 12, (l -> ((graphics, i, j, f) -> {}))),SimpleLayoutRenderable.create(240, 12, (l -> ((graphics, i, j, f) -> graphics.drawString(font, Component.translatable("soundCategory.weather"), l.x + 1, l.y + 4, CommonColor.INVENTORY_GRAY_TEXT.get(),false)))));
        screen.renderableVList.addRenderable(new LegacySliderButton<>(0, 0, 230,16, b1 -> Component.translatable( "legacy.weather_state." + b1.getObjectValue()),b1->null,weathers.get(initialWeather),()->weathers, b1->{
            if (!Objects.equals(b1.getObjectValue(), weathers.get(initialWeather))) COMMAND_MAP.put(b1,()-> minecraft.getConnection().sendCommand("weather " + b1.getObjectValue()));
        }));
        for (GameRules.Key<GameRules.BooleanValue> key : OTHER_RULES)
            screen.renderableVList.addRenderable(new TickBox(0,0,Legacy4JClient.gameRules.getRule(key).get(),b1-> Component.translatable(key.getDescriptionId()),b1-> null, b1->COMMAND_MAP.put(b1,()->minecraft.player.connection.sendCommand("gamerule %s %s".formatted(key.getId(), b1.selected)))));
        minecraft.setScreen(screen);
        if (minecraft.hasSingleplayerServer() && !minecraft.getSingleplayerServer().isPublished()) return;
        BiFunction<Boolean,Component,AbstractButton> teleportButton = (bol,title)-> Button.builder(title, b1-> minecraft.setScreen(new HostOptionsScreen(title) {
            @Override
            protected void addHostOptionsButton() {
            }

            @Override
            protected void addPlayerButtons() {
                addPlayerButtons(false,(profile,b1)->{
                    if (bol) minecraft.player.connection.sendCommand("tp %s".formatted(profile.getName()));
                    else minecraft.player.connection.sendCommand("tp %s ~ ~ ~".formatted(profile.getName()));
                });
            }

            public boolean isPauseScreen() {
                return false;
            }
        })).bounds(0,0,215,20).build();
        screen.renderableVList.addRenderable(teleportButton.apply(true,Component.translatable("legacy.menu.host_options.teleport_player")));
        screen.renderableVList.addRenderable(teleportButton.apply(false,Component.translatable("legacy.menu.host_options.teleport_me")));
    }
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (shouldFade) {
            alpha = Math.min(1.0f, alpha * 1.04f);
            shouldFade = false;
        }else {
            if (alpha > getDefaultOpacity())  alpha = Math.max(getDefaultOpacity(), alpha / 1.04f);
        }
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f, Math.max(getDefaultOpacity(), Math.min(alpha * (shouldFade ? 1 + f * 0.04f : 1 / (1 + f * 0.04f)),1.0f)));
        RenderSystem.enableBlend();
        panel.render(guiGraphics,i,j,f);
        RenderSystem.disableBlend();
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,1.0f);
        guiGraphics.drawString(font,title,panel.x + 11, panel.y + 8, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
    }

    protected static float getDefaultOpacity() {
        return 0.5f;
    }
}
