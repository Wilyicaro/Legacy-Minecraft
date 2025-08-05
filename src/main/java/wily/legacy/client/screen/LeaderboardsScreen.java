package wily.legacy.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.base.network.CommonNetwork;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.util.JsonUtil;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

public class LeaderboardsScreen extends PanelVListScreen {
    public static final List<StatsBoard> statsBoards = new ArrayList<>();
    public int selectedStatBoard = 0;
    protected int statsInScreen = 0;
    protected int lastStatsInScreen = 0;
    protected int page = 0;
    protected int updateTimer = 0;
    public static final Component RANK = Component.translatable("legacy.menu.leaderboard.rank");
    public static final Component USERNAME = Component.translatable("legacy.menu.leaderboard.username");
    public static final Component OVERALL = Component.translatable("legacy.menu.leaderboard.filter.overall");
    public static final Component MY_SCORE = Component.translatable("legacy.menu.leaderboard.filter.my_score");
    public static final Component NO_RESULTS = Component.translatable("legacy.menu.leaderboard.no_results");
    protected final Stocker.Sizeable filter = new Stocker.Sizeable(0,1);
    protected List<LegacyPlayerInfo> actualRankBoard = Collections.emptyList();
    public LeaderboardsScreen(Screen parent) {
        super(parent, s -> Panel.createPanel(s, p-> p.appearance(568, 275)), CommonComponents.EMPTY);
        rebuildRenderableVList(Minecraft.getInstance());
        renderableVList.layoutSpacing(l-> 1);
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.getIcon(),()-> LegacyComponents.CHANGE_FILTER);
    }

    public static void refreshStatsBoards(Minecraft minecraft){
        if (minecraft.getConnection() == null) return;
        statsBoards.forEach(StatsBoard::clear);
        if (Legacy4JClient.hasModOnServer()) {
            minecraft.getConnection().getOnlinePlayers().stream().map(p -> ((LegacyPlayerInfo) p).getStatsMap()).forEach(o -> o.forEach((s, i) -> {
                if (i <= 0) return;
                for (StatsBoard statsBoard : statsBoards) if (statsBoard.add(s)) break;

            }));
        }else {
            minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
            minecraft.player.getStats().stats.forEach((s, i) -> {
                for (StatsBoard statsBoard : statsBoards) if (statsBoard.add(s)) break;
            });
        }
    }

    public int changedPage(int count){
        return Math.max(0,(page + count) >= statsBoards.get(selectedStatBoard).renderables.size() ? page : page + count);
    }

    public void changeStatBoard(boolean left){
        int initialSelectedStatBoard = selectedStatBoard;
        while (selectedStatBoard != (selectedStatBoard = Stocker.cyclic(0,selectedStatBoard + (left ? -1 : 1), statsBoards.size())) && selectedStatBoard != initialSelectedStatBoard){
            if (!statsBoards.get(selectedStatBoard).statsList.isEmpty()) {
                page = 0;
                rebuildRenderableVList(minecraft);
                repositionElements();
                return;
            }
        }
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_X){
            filter.add(1,true);
            rebuildRenderableVList(minecraft);
            repositionElements();
        }
        if (i == InputConstants.KEY_LEFT || i == InputConstants.KEY_RIGHT){
            changeStatBoard(i == InputConstants.KEY_LEFT);
            return true;
        }
        if (i == InputConstants.KEY_LBRACKET || i == InputConstants.KEY_RBRACKET){
            if (selectedStatBoard < statsBoards.size() && !statsBoards.get(selectedStatBoard).renderables.isEmpty()){
                int newPage = changedPage(i == InputConstants.KEY_LBRACKET ? -lastStatsInScreen : statsInScreen);
                if (newPage != page){
                    lastStatsInScreen = statsInScreen;
                    page = newPage;
                    return true;
                }
            }
        }
        if (renderableVList.keyPressed(i)) return true;
        return super.keyPressed(i, j, k);
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        renderableVList.mouseScrolled(g);
        return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
    }

    public void rebuildRenderableVList(Minecraft minecraft){
        renderableVList.renderables.clear();
        if (minecraft.getConnection() == null ||  statsBoards.get(selectedStatBoard).statsList.isEmpty()) return;
        actualRankBoard = Legacy4JClient.hasModOnServer() && filter.get() != 1 ? minecraft.getConnection().getOnlinePlayers().stream().map(p-> ((LegacyPlayerInfo)p)).filter(info-> info.getStatsMap().object2IntEntrySet().stream().filter(s-> statsBoards.get(selectedStatBoard).statsList.contains(s.getKey())).mapToInt(Object2IntMap.Entry::getIntValue).sum() > 0).sorted(filter.get() == 0 ? Comparator.comparingInt(info-> ((LegacyPlayerInfo)info).getStatsMap().object2IntEntrySet().stream().filter(s-> statsBoards.get(selectedStatBoard).statsList.contains(s.getKey())).mapToInt(Object2IntMap.Entry::getIntValue).sum()).reversed() : Comparator.comparing((LegacyPlayerInfo l) -> l.legacyMinecraft$getProfile().getName())).toList() : List.of((LegacyPlayerInfo) minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID()));
        for (int i = 0; i < actualRankBoard.size(); i++) {
            LegacyPlayerInfo info = actualRankBoard.get(i);
            String rank = i + 1 + "";
            renderableVList.renderables.add(new AbstractWidget(0,0,551,20,Component.literal(info.legacyMinecraft$getProfile().getName())) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                    int y = getY() + (getHeight() - font.lineHeight) / 2 + 1;
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(isHoveredOrFocused() ? LegacySprites.LEADERBOARD_BUTTON_HIGHLIGHTED : LegacySprites.LEADERBOARD_BUTTON,getX(),getY(),getWidth(),getHeight());
                    guiGraphics.drawString(font,rank,getX() + 40 - font.width(rank) / 2, y, LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()));
                    guiGraphics.drawString(font,getMessage(),getX() + 120 -(font.width(getMessage())) / 2, y, LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()));
                    guiGraphics.drawString(font,getMessage(),getX() + 120 -(font.width(getMessage())) / 2, y, LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()));
                    int added = 0;
                    Component hoveredValue = null;
                    for (int index = page; index < statsBoards.get(selectedStatBoard).statsList.size(); index++) {
                        if (added >= statsInScreen)break;
                        Stat<?> stat = statsBoards.get(selectedStatBoard).statsList.get(index);
                        Component value = ControlTooltip.CONTROL_ICON_FUNCTION.apply(stat.format((Legacy4JClient.hasModOnServer() ? info.getStatsMap() : minecraft.player.getStats().stats).getInt(stat)), Style.EMPTY).getComponent();
                        SimpleLayoutRenderable renderable = statsBoards.get(selectedStatBoard).renderables.get(index);
                        int w = font.width(value);
                        LegacyRenderUtil.renderScrollingString(guiGraphics,font, value,renderable.getX() + Math.max(0,renderable.getWidth() - w) / 2, getY(),renderable.getX() + Math.min(renderable.getWidth(),(renderable.getWidth() - w)/ 2 + getWidth()), getY() + getHeight(), LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()),true);
                        if (LegacyRenderUtil.isMouseOver(i,j,renderable.getX() + Math.max(0,renderable.getWidth() - w) / 2, getY(),Math.min(renderable.getWidth(),w), getHeight())) hoveredValue = value;
                        added++;
                    }
                    if (hoveredValue != null) guiGraphics.setTooltipForNextFrame(font,hoveredValue,i,j);
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    defaultButtonNarrationText(narrationElementOutput);
                }
            });
        }
    }


    @Override
    public void tick() {
        super.tick();
        if (updateTimer <= 0){
            updateTimer = 20;
            if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(PlayerInfoSync.askAll(minecraft.player));
            else minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
        } else updateTimer--;
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        addRenderableOnly((guiGraphics, i, j, f) -> {
            LegacyRenderUtil.renderPointerPanel(guiGraphics,panel.x + 8,panel.y - 18,166,18);
            LegacyRenderUtil.renderPointerPanel(guiGraphics,panel.x + (panel.width - 211) / 2,panel.y - 18,211,18);
            LegacyRenderUtil.renderPointerPanel(guiGraphics,panel.x +  panel.width - 174 ,panel.y - 18,166,18);
            if (!statsBoards.isEmpty() && selectedStatBoard < statsBoards.size()){
                StatsBoard board = statsBoards.get(selectedStatBoard);
                LegacyFontUtil.applyFontOverrideIf(LegacyRenderUtil.is720p(), LegacyFontUtil.MOJANGLES_11_FONT, b-> {
                    guiGraphics.pose().pushMatrix();
                    Component filter = Component.translatable("legacy.menu.leaderboard.filter", this.filter.get() == 0 ? OVERALL :  MY_SCORE);
                    guiGraphics.pose().translate(panel.x + 91 - font.width(filter) / 3f, panel.y - 12);
                    if (!b) guiGraphics.pose().scale(2/3f,2/3f);
                    guiGraphics.drawString(font, filter, 0, 0, 0xFFFFFFFF);
                    guiGraphics.pose().popMatrix();
                    guiGraphics.pose().pushMatrix();
                    guiGraphics.pose().translate(panel.x + (panel.width - font.width(board.displayName) * 2/3f) / 2, panel.y - 12);
                    if (!b) guiGraphics.pose().scale(2/3f,2/3f);
                    guiGraphics.drawString(font,board.displayName,0, 0,0xFFFFFFFF);
                    guiGraphics.pose().popMatrix();
                    guiGraphics.pose().pushMatrix();
                    Component entries = Component.translatable("legacy.menu.leaderboard.entries",actualRankBoard.size());
                    guiGraphics.pose().translate(panel.x + 477 - font.width(entries) / 3f, panel.y - 12);
                    if (!b) guiGraphics.pose().scale(2/3f,2/3f);
                    guiGraphics.drawString(font,entries,0, 0,0xFFFFFFFF);
                    guiGraphics.pose().popMatrix();
                });
                if (board.statsList.isEmpty()) {
                    guiGraphics.pose().pushMatrix();
                    guiGraphics.pose().translate(panel.x + (panel.width - font.width(NO_RESULTS) * 1.5f)/2f, panel.y + (panel.height - 13.5f) / 2f);
                    guiGraphics.pose().scale(1.5f,1.5f);
                    guiGraphics.drawString(font,NO_RESULTS,0,0, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
                    guiGraphics.pose().popMatrix();
                    return;
                }
                guiGraphics.drawString(font,RANK,panel.x + 40, panel.y + 20,CommonColor.INVENTORY_GRAY_TEXT.get(),false);
                guiGraphics.drawString(font,USERNAME,panel.x + 108, panel.y + 20,CommonColor.INVENTORY_GRAY_TEXT.get(),false);
                int totalWidth = 0;
                statsInScreen = 0;
                for (int index = page; index < board.renderables.size(); index++) {
                    int newWidth= totalWidth + board.renderables.get(index).getWidth();
                    if (newWidth > 351) break;
                    statsInScreen++;
                    totalWidth = newWidth;
                }
                FactoryScreenUtil.enableBlend();
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(panel.x + (panel.width - 211) / 2f, panel.y - 12);
                guiGraphics.pose().scale(0.5f,0.5f);
                (ControlType.getActiveType().isKbm() ? ControlTooltip.CompoundComponentIcon.of(ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)) : ControllerBinding.LEFT_STICK.getIcon()).render(guiGraphics,4,0, false);
                if (statsInScreen < statsBoards.get(selectedStatBoard).renderables.size()) {
                    ControlTooltip.Icon pageControl = ControlTooltip.CompoundComponentIcon.of(ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.getIcon(), ControlTooltip.SPACE_ICON, ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.getIcon());
                    pageControl.render(guiGraphics,422 - pageControl.getWidth() - 8, 0, false);
                }
                guiGraphics.pose().popMatrix();
                FactoryScreenUtil.disableBlend();
                if (statsInScreen == 0) return;
                int x = (351 - totalWidth) / (statsInScreen + 1);
                Integer hovered = null;
                for (int index = page; index < page + statsInScreen; index++) {
                    SimpleLayoutRenderable r = board.renderables.get(index);
                    r.setPosition(panel.x + 182 + x,panel.y + 22 - r.height / 2);
                    r.render(guiGraphics,i,j,f);
                    if (r.isHovered(i,j)) hovered = index;
                    x+= r.getWidth() + (351 - totalWidth) / statsInScreen;
                }
                if (hovered != null) guiGraphics.setTooltipForNextFrame(font, board.statsList.get(hovered).getValue() instanceof EntityType<?> e ? e.getDescription() : board.statsList.get(hovered).getValue() instanceof ItemLike item && item.asItem() != Items.AIR ? Component.translatable(item.asItem().getDescriptionId()) : ControlTooltip.getAction("stat." + board.statsList.get(hovered).getValue().toString().replace(':', '.')), i, j);
            }
        });
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 9,panel.y + 39,551,226);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, false);
    }

    public void onStatsUpdated() {
        if (!Legacy4JClient.hasModOnServer()){
            refreshStatsBoards(minecraft);
            if (LeaderboardsScreen.statsBoards.get(selectedStatBoard).statsList.isEmpty()) minecraft.executeIfPossible(()-> changeStatBoard(false));
        }
    }

    public static class StatsBoard {
        public final Component displayName;
        public final StatType<?> type;
        public final List<Stat<?>> statsList = new ArrayList<>();
        public List<SimpleLayoutRenderable> renderables = new ArrayList<>();
        public final List<StatIconOverride<?>> statIconOverrides = new ArrayList<>();

        public void clear() {
            statsList.clear();
            renderables.clear();
        }

        public SimpleLayoutRenderable getRenderable(Stat<?> stat){
            for (var override : statIconOverrides) {
                if (override.test(stat)){
                    return override.icon();
                }
            }

            if (stat.getValue() instanceof ItemLike i) {
                LegacyIconHolder h = new LegacyIconHolder(24,24);
                h.itemIcon = i.asItem().getDefaultInstance();
                return h;
            } else if (stat.getValue() instanceof EntityType<?> e) {
                ResourceLocation entityIcon = Legacy4J.createModLocation("icon/leaderboards/entity/" + e.builtInRegistryHolder().key().location().getPath());
                if (Minecraft.getInstance().getGuiSprites().textureAtlas.texturesByName.containsKey(entityIcon)) {
                    LegacyIconHolder h = new LegacyIconHolder(24, 24);
                    h.iconSprite = entityIcon;
                    return h;
                }
                return LegacyIconHolder.entityHolder(0,0,24,24, e);
            }
            Component name = Component.translatable("stat." + stat.getValue().toString().replace(':', '.'));
            return SimpleLayoutRenderable.create(Minecraft.getInstance().font.width(name) * 2/3 + 8,7, (l)->((guiGraphics, i, j, f) ->
                LegacyFontUtil.applyFontOverrideIf(LegacyRenderUtil.is720p(), LegacyFontUtil.MOJANGLES_11_FONT, b-> {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(l.getX() + 4, l.getY());
                if (!b) guiGraphics.pose().scale(2/3f,2/3f);
                guiGraphics.drawString(Minecraft.getInstance().font,name,0,0,CommonColor.INVENTORY_GRAY_TEXT.get(),false);
                guiGraphics.pose().popMatrix();
            })));

        }

        public StatsBoard(StatType<?> type, Component displayName){
            this.type = type;
            this.displayName = displayName;
        }

        public static StatsBoard create(StatType<?> type, Component displayName){
            return new StatsBoard(type, displayName);
        }

        public static StatsBoard create(StatType<?> type, Component displayName, Predicate<Stat<?>> canAccept){
            return new StatsBoard(type, displayName){
                @Override
                public boolean canAdd(Stat<?> stat) {
                    return super.canAdd(stat) && canAccept.test(stat);
                }
            };
        }

        public boolean canAdd(Stat<?> stat){
            return stat.getType() == type;
        }

        public boolean add(Stat<?> stat){
            if (canAdd(stat)){
                if (!statsList.contains(stat)){
                    statsList.add(stat);
                    renderables.add(getRenderable(stat));
                }
                return true;
            } return false;
        }
    }

    public record StatIconOverride<T>(StatType<T> statType, Predicate<T> isValid, LegacyIconHolder icon) implements Predicate<Stat<?>> {

        @Override
        public boolean test(Stat<?> stat) {
            return stat.getType() == statType && isValid.test((T) stat.getValue());
        }
    }

    public static class Manager implements ResourceManagerReloadListener {
        public static final String LEADERBOARD_LISTING = "leaderboard_listing.json";

        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {

            JsonUtil.getOrderedNamespaces(resourceManager).forEach(name->resourceManager.getResource(FactoryAPI.createLocation(name, LEADERBOARD_LISTING)).ifPresent(((r) -> {
                try (BufferedReader bufferedReader = r.openAsReader()) {
                    JsonObject obj = GsonHelper.parse(bufferedReader);
                    JsonElement ioElement = obj.get("listing");
                    if (ioElement instanceof JsonArray array)
                        array.forEach(e-> {
                            if (e instanceof JsonObject o) statsBoards.add(statsBoardFromJson(o));
                        });
                } catch (IOException exception) {
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            })));
        }

        protected StatsBoard statsBoardFromJson(JsonObject o){
            var statType = FactoryAPIPlatform.getRegistryValue(ResourceLocation.tryParse(GsonHelper.getAsString(o,"type")),BuiltInRegistries.STAT_TYPE);
            Component name = o.has("displayName") ? Component.translatable(GsonHelper.getAsString(o,"displayName")) : statType.getDisplayName();
            StatsBoard statsBoard;
            if (o.get("predicate") instanceof JsonObject predObj){
                Predicate predicate = JsonUtil.registryMatches(statType.getRegistry(), predObj);
                statsBoard = StatsBoard.create(statType, name, s-> predicate.test(s.getValue()));
            } else statsBoard = StatsBoard.create(statType, name);
            if (o.get("overrides") instanceof JsonArray a) a.forEach(e-> parseOverride(statType, statsBoard, e));

            return statsBoard;
        }

        protected <T> void parseOverride(StatType<T> statType, StatsBoard statsBoard, JsonElement e){
            if (e instanceof JsonObject override && override.get("type") instanceof JsonPrimitive p) {
                String type = p.getAsString();
                var predicate = JsonUtil.registryMatches(statType.getRegistry(), override.getAsJsonObject("predicate"));
                switch (type) {
                    case "item" -> {
                        Item item =  FactoryAPIPlatform.getRegistryValue(ResourceLocation.tryParse(GsonHelper.getAsString(override, "id")),BuiltInRegistries.ITEM);
                        LegacyIconHolder h =  new LegacyIconHolder(24, 24);
                        h.itemIcon = item.getDefaultInstance();
                        statsBoard.statIconOverrides.add(new StatIconOverride<>(statType, predicate, h));
                    }
                    //Unused for now, as +1.21.6 gui rendering doesn't allow more than one entity rendering on the screen
                    //case "entity_type" -> {
                    //    EntityType<?> entityType = FactoryAPIPlatform.getRegistryValue(ResourceLocation.tryParse(GsonHelper.getAsString(override, "id")),BuiltInRegistries.ENTITY_TYPE);
                    //    statsBoard.statIconOverrides.put(predicate, LegacyIconHolder.entityHolder(0,0,24,24,entityType));
                    //}
                    case "sprite" -> {
                        ResourceLocation sprite = ResourceLocation.tryParse(GsonHelper.getAsString(override, "id"));
                        LegacyIconHolder h = new LegacyIconHolder(24, 24);
                        h.iconSprite = sprite;
                        statsBoard.statIconOverrides.add(new StatIconOverride<>(statType, predicate, h));
                    }
                }
            }
        }

        @Override
        public String getName() {
            return "legacy:leaderboards_listing";
        }
    }

}
