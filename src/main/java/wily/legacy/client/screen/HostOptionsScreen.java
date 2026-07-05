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
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.base.client.WidgetAccessor;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.network.CommonNetwork;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.PlayerIdentifier;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.client.screen.compat.WorldHostFriendsScreen;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.util.LegacySprites;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.network.ServerHostOptionsPayload;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static wily.legacy.client.screen.LoadSaveScreen.GAME_MODEL_LABEL;

public class HostOptionsScreen extends PanelVListScreen {
    public static final Component HOST_OPTIONS = Component.translatable("legacy.menu.host_options");
    public static final Component PLAYERS_INVITE = Component.translatable("legacy.menu.players_invite");
    protected final Component title;
    protected float oldAlpha = getDefaultOpacity();
    protected float alpha = getDefaultOpacity();
    protected boolean shouldFade = false;

    public static final List<GameRules.Key<GameRules.BooleanValue>> WORLD_RULES = new ArrayList<>(List.of(GameRules.RULE_DOFIRETICK,LegacyGameRules.getTntExplodes(),GameRules.RULE_DAYLIGHT,GameRules.RULE_KEEPINVENTORY,GameRules.RULE_DOMOBSPAWNING,GameRules.RULE_MOBGRIEFING,LegacyGameRules.GLOBAL_MAP_PLAYER_ICON,LegacyGameRules.LEGACY_SWIMMING,LegacyGameRules.LEGACY_FLIGHT,LegacyGameRules.LEGACY_SHIELD_CONTROLS,LegacyGameRules.LEGACY_OFFHAND_LIMITS));
    public static final List<GameRules.Key<GameRules.BooleanValue>> OTHER_RULES = new ArrayList<>(List.of(GameRules.RULE_WEATHER_CYCLE,GameRules.RULE_DOMOBLOOT,GameRules.RULE_DOBLOCKDROPS,GameRules.RULE_NATURAL_REGENERATION,GameRules.RULE_DO_IMMEDIATE_RESPAWN));
    public static final List<GameRules.Key<GameRules.BooleanValue>> LEGACY_WORLD_RULES = List.of(GameRules.RULE_DOFIRETICK,LegacyGameRules.getTntExplodes(),GameRules.RULE_DAYLIGHT,GameRules.RULE_KEEPINVENTORY,GameRules.RULE_DOMOBSPAWNING,GameRules.RULE_MOBGRIEFING);
    public static final List<GameRules.Key<GameRules.BooleanValue>> LEGACY_OTHER_RULES = List.of(GameRules.RULE_WEATHER_CYCLE,GameRules.RULE_DOMOBLOOT,GameRules.RULE_DOBLOCKDROPS,GameRules.RULE_NATURAL_REGENERATION);

    public HostOptionsScreen(Component title) {
        super(s-> Panel.centered(s, LegacySprites.PANEL,250,190, 0, 20),HOST_OPTIONS);
        this.title = title;
        addPlayerButtons();
        renderableVList.layoutSpacing(l->0);
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.getIcon(), ()-> minecraft.hasSingleplayerServer() ? !minecraft.getSingleplayerServer().isPublished() ? PublishScreen.getPublishComponent() : PublishScreen.hasWorldHost() ? WorldHostFriendsScreen.FRIENDS : null : null);
        renderer.add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon(),()-> minecraft.getChatStatus().isChatAllowed(minecraft.isLocalServer()) ? LegacyKeyMapping.of(Minecraft.getInstance().options.keyChat).getDisplayName() : null);
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

    protected abstract class PlayerButton extends AbstractButton {
        public final PlayerInfo playerInfo;

        public PlayerButton(int x, int y, int width, int height, PlayerInfo playerInfo) {
            super(x, y, width, height, Component.literal(playerInfo.getProfile().getName()));
            this.playerInfo = playerInfo;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            if (isHoveredOrFocused()) shouldFade = true;
            super.renderWidget(guiGraphics, i, j, f);
            String listName = renderableVList.name == null ? "renderableVList" : renderableVList.name;
            int iconWidth = accessor.getInteger(listName + ".playerIcon.width", 20);
            int iconHeight = accessor.getInteger(listName + ".playerIcon.height", 20);
            drawPlayerIcon((LegacyPlayerInfo) playerInfo, guiGraphics, getX() + accessor.getInteger(listName + ".playerIcon.x", 6), getY() + accessor.getInteger(listName + ".playerIcon.y", 5), iconWidth, iconHeight);
        }
        @Override
        protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
            String listName = renderableVList.name == null ? "renderableVList" : renderableVList.name;
            ScreenUtil.applySDFont(ignored -> ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), getX() + accessor.getInteger(listName + ".buttonMessage.x", 68), this.getY(), getX() + getWidth(), this.getY() + this.getHeight(), j,true));
        }
        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }

    public static void drawPlayerIcon(LegacyPlayerInfo info, GuiGraphics guiGraphics, int x, int y){
        drawPlayerIcon(info, guiGraphics, x, y, 20, 20);
    }

    public static void drawPlayerIcon(LegacyPlayerInfo info, GuiGraphics guiGraphics, int x, int y, int width, int height){
        float[] color = Legacy4JClient.getVisualPlayerColor(info);
        FactoryGuiGraphics.of(guiGraphics).setColor(color[0],color[1],color[2],1.0f);
        FactoryScreenUtil.enableBlend();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(PlayerIdentifier.of(info.getIdentifierIndex()).optionsMapSprite(),x,y, width,height);
        FactoryScreenUtil.disableBlend();
        FactoryGuiGraphics.of(guiGraphics).clearColor();
    }

    protected void addPlayerButtons(){
        addPlayerButtons(true,(playerInfo, b)->{
            if (!minecraft.player.hasPermissions(2)) return;
            minecraft.setScreen(new PlayerHostOptionsScreen(this, playerInfo, minecraft));
        });
    }

    protected void addPlayerButtons(boolean includeLocal, BiConsumer<PlayerInfo, AbstractButton> onPress){
        for (PlayerInfo playerInfo : getActualPlayerInfos()) {
            if (!includeLocal && Objects.equals(playerInfo.getProfile().getName(), Minecraft.getInstance().player.getGameProfile().getName())) continue;
            renderableVList.addRenderable(new PlayerButton(0,0, 230, 30, playerInfo) {
                @Override
                public void onPress() {
                    onPress.accept(playerInfo,this);
                }
            });
        }
    }

    public static List<PlayerInfo> getActualPlayerInfos(){
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && !minecraft.player.connection.getOnlinePlayers().isEmpty())
            return minecraft.player.connection.getOnlinePlayers().stream().sorted(Comparator.comparingInt((p -> minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer().isSingleplayerOwner(p.getProfile()) ? 0 : ((LegacyPlayerInfo)p).getIdentifierIndex()))).toList();
        return Collections.emptyList();
    }

    @Override
    protected void init() {
        CommonNetwork.sendToServer(PlayerInfoSync.askAll(minecraft.player));
        super.init();
        addHostOptionsButton();
    }

    @Override
    protected void panelInit() {
        panel.init();
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(
                accessor.getInteger("renderableVList.x", panel.x + 10),
                accessor.getInteger("renderableVList.y", panel.y + 22),
                accessor.getInteger("renderableVList.width", panel.width - 20),
                accessor.getInteger("renderableVList.height", panel.height - 26));
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

    protected void addHostOptionsButton(){
        if (!minecraft.player.hasPermissions(2) && !minecraft.hasSingleplayerServer()) return;
        addRenderableWidget(accessor.putWidget("hostOptionsButton", Button.builder(HOST_OPTIONS,this::pressHostOptionsButton).bounds(panel.x, panel.y - 36, 250, 20).build()));
    }

    protected void pressHostOptionsButton(Button b){
        minecraft.setScreen(new GameHostOptionsScreen(this, minecraft));
    }
    public boolean isPauseScreen() {
        return false;
    }


    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        oldAlpha = alpha;
        alpha = Mth.lerp(f * 0.1f, oldAlpha, shouldFade ? 1.0f : getDefaultOpacity());
        shouldFade = false;
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f, alpha);
        FactoryScreenUtil.enableBlend();
        panel.render(guiGraphics,i,j,f);
        FactoryScreenUtil.disableBlend();
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,1.0f);
        ScreenUtil.applySDFont(ignored -> guiGraphics.drawString(font,title,panel.x + accessor.getInteger("title.x", 11), panel.y + accessor.getInteger("title.y", 8), CommonColor.INVENTORY_GRAY_TEXT.get(), false));
    }

    protected static float getDefaultOpacity() {
        return 0.5f;
    }
}
