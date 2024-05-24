package wily.legacy.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsUpdateListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.player.LegacyPlayerInfo;
import wily.legacy.util.JsonUtil;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

public class LeaderboardsScreen extends PanelBackgroundScreen implements StatsUpdateListener {
    public static final List<StatsBoard> statsBoards = new ArrayList<>();
    public int selectedStatBoard = 0;
    protected int statsInScreen = 0;
    protected int lastStatsInScreen = 0;
    protected int page = 0;
    public static final Component RANK = Component.translatable("legacy.menu.leaderboard.rank");
    public static final Component USERNAME = Component.translatable("legacy.menu.leaderboard.username");
    public static final Component MY_SCORE = Component.translatable("legacy.menu.leaderboard.filter.my_score");
    public static final Component NO_RESULTS = Component.translatable("legacy.menu.leaderboard.no_results");
    protected final RenderableVList renderableVList = new RenderableVList();
    protected final ControlTooltip.Renderer controlTooltipRenderer = ControlTooltip.defaultScreen(this);
    protected boolean myScore = false;
    protected List<LegacyPlayerInfo> actualRankBoard = Collections.emptyList();
    public LeaderboardsScreen(Screen parent) {
        super(568,275, CommonComponents.EMPTY);
        controlTooltipRenderer.add(()->ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X,true) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(true),()-> ControlTooltip.CONTROL_ACTION_CACHE.getUnchecked("legacy.action.change_filter"));
        this.parent = parent;
        rebuildRenderableVList(Minecraft.getInstance());
    }

    public static void refreshStatsBoards(Minecraft minecraft){
        if (minecraft.getConnection() == null) return;
        statsBoards.forEach(StatsBoard::clear);
        if (Legacy4JClient.isModEnabledOnServer()) {
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
            myScore = !myScore;
            rebuildRenderableVList(minecraft);
            repositionElements();
        }
        if (i == InputConstants.KEY_LEFT || i == InputConstants.KEY_RIGHT){
            changeStatBoard(i == InputConstants.KEY_LEFT);
            return true;
        }
        if (i == InputConstants.KEY_LBRACKET || i == InputConstants.KEY_RBRACKET){
            if (!statsBoards.get(selectedStatBoard).renderables.isEmpty()){
                int newPage = changedPage(i == InputConstants.KEY_LBRACKET ? -lastStatsInScreen : statsInScreen);
                if (newPage != page){
                    lastStatsInScreen = statsInScreen;
                    page = newPage;
                    return true;
                }
            }
        }
        return super.keyPressed(i, j, k);
    }

    public void rebuildRenderableVList(Minecraft minecraft){
        renderableVList.renderables.clear();
        if (minecraft.getConnection() == null ||  statsBoards.get(selectedStatBoard).statsList.isEmpty()) return;
        actualRankBoard = Legacy4JClient.isModEnabledOnServer() ? minecraft.getConnection().getOnlinePlayers().stream().map(p-> ((LegacyPlayerInfo)p)).filter(info-> info.getStatsMap().object2IntEntrySet().stream().filter(s-> statsBoards.get(selectedStatBoard).statsList.contains(s.getKey())).mapToInt(Object2IntMap.Entry::getIntValue).sum() > 0).sorted(Comparator.comparingInt(info->((LegacyPlayerInfo)info).getStatsMap().object2IntEntrySet().stream().filter(s-> statsBoards.get(selectedStatBoard).statsList.contains(s.getKey())).mapToInt(Object2IntMap.Entry::getIntValue).sum()).reversed()).toList() : List.of((LegacyPlayerInfo) minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID()));
        for (int i = 0; i < actualRankBoard.size(); i++) {
            LegacyPlayerInfo info = actualRankBoard.get(i);
            if (myScore && !minecraft.player.getUUID().equals(info.legacyMinecraft$getProfile().getId())) continue;
            String rank =i + 1 + "";
            renderableVList.renderables.add(new AbstractWidget(0,0,551,20,Component.literal(info.legacyMinecraft$getProfile().getName())) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                    int y = getY() + (getHeight() - font.lineHeight) / 2 + 1;
                    guiGraphics.blitSprite(isHoveredOrFocused() ? LegacySprites.LEADERBOARD_BUTTON_HIGHLIGHTED : LegacySprites.LEADERBOARD_BUTTON,getX(),getY(),getWidth(),getHeight());
                    guiGraphics.drawString(font,rank,getX() + 40 - font.width(rank) / 2, y, ScreenUtil.getDefaultTextColor(!isHoveredOrFocused()));
                    guiGraphics.drawString(font,getMessage(),getX() + 120 -(font.width(getMessage())) / 2, y, ScreenUtil.getDefaultTextColor(!isHoveredOrFocused()));
                    guiGraphics.drawString(font,getMessage(),getX() + 120 -(font.width(getMessage())) / 2, y, ScreenUtil.getDefaultTextColor(!isHoveredOrFocused()));
                    int added = 0;

                    for (int index = page; index < statsBoards.get(selectedStatBoard).statsList.size(); index++) {
                        if (added >= statsInScreen)break;
                        Stat<?> stat = statsBoards.get(selectedStatBoard).statsList.get(index);
                        String value = stat.format((Legacy4JClient.isModEnabledOnServer() ? info.getStatsMap() : minecraft.player.getStats().stats).getInt(stat));
                        SimpleLayoutRenderable renderable = statsBoards.get(selectedStatBoard).renderables.get(index);
                        guiGraphics.drawString(font,value,renderable.getX() + (renderable.getWidth() - font.width(value)) / 2, y, ScreenUtil.getDefaultTextColor(!isHoveredOrFocused()));

                        added++;
                    }
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

                }
            });
        }
    }
    @Override
    protected void init() {
        super.init();
        if (Legacy4JClient.isModEnabledOnServer()) Legacy4J.NETWORK.sendToServer(new PlayerInfoSync(0,minecraft.player));
        else minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
        addRenderableOnly((guiGraphics, i, j, f) -> {
            ScreenUtil.renderPointerPanel(guiGraphics,panel.x + 8,panel.y - 18,166,18);
            ScreenUtil.renderPointerPanel(guiGraphics,panel.x + (panel.width - 211) / 2,panel.y - 18,211,18);
            ScreenUtil.renderPointerPanel(guiGraphics,panel.x +  panel.width - 174 ,panel.y - 18,166,18);
            if (!statsBoards.isEmpty() && selectedStatBoard < statsBoards.size()){
                StatsBoard board = statsBoards.get(selectedStatBoard);
                guiGraphics.pose().pushPose();
                Component filter = Component.translatable("legacy.menu.leaderboard.filter", myScore ? MY_SCORE : LegacyKeyBindsScreen.NONE);
                guiGraphics.pose().translate(panel.x + 91 - font.width(filter) / 4f, panel.y - 12,0);
                guiGraphics.pose().scale(2/3f,2/3f,2/3f);
                guiGraphics.drawString(font,filter,0, 0,0xFFFFFF);
                guiGraphics.pose().popPose();
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(panel.x + (panel.width - font.width(board.displayName) / 2f) / 2, panel.y - 12,0);
                guiGraphics.pose().scale(2/3f,2/3f,2/3f);
                guiGraphics.drawString(font,board.displayName,0, 0,0xFFFFFF);
                guiGraphics.pose().popPose();
                guiGraphics.pose().pushPose();
                Component entries = Component.translatable("legacy.menu.leaderboard.entries",actualRankBoard.size());
                guiGraphics.pose().translate(panel.x + 477 - font.width(entries) / 4f, panel.y - 12,0);
                guiGraphics.pose().scale(2/3f,2/3f,2/3f);
                guiGraphics.drawString(font,entries,0, 0,0xFFFFFF);
                guiGraphics.pose().popPose();
                RenderSystem.enableBlend();
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(panel.x + (panel.width - 211) / 2f, panel.y - 12,0);
                guiGraphics.pose().scale(0.5f,0.5f,0.5f);
                guiGraphics.drawString(font,ControlTooltip.getActiveType().isKeyboard() ?ControlTooltip.COMPOUND_COMPONENT_FUNCTION.apply(new Component[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT,true),ControlTooltip.SPACE, ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT,true)}) : ControllerBinding.LEFT_STICK.bindingState.getIcon(true),4, 0,0xFFFFFF);
                if (statsInScreen < statsBoards.get(selectedStatBoard).renderables.size()) {
                    Component pageControl = ControlTooltip.COMPOUND_COMPONENT_FUNCTION.apply(new Component[]{ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET, true) : ControllerBinding.LEFT_BUMPER.bindingState.getIcon(true), ControlTooltip.SPACE, ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET, true) : ControllerBinding.RIGHT_BUMPER.bindingState.getIcon(true)});
                    guiGraphics.drawString(font, pageControl, 422 - font.width(pageControl) - 8, 0, 0xFFFFFF);
                }
                guiGraphics.pose().popPose();
                RenderSystem.disableBlend();
                if (board.statsList.isEmpty()) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(panel.x + (panel.width - font.width(NO_RESULTS) * 1.5f)/2f, panel.y + (panel.height - 13.5f) / 2f,0);
                    guiGraphics.pose().scale(1.5f,1.5f,1.5f);
                    guiGraphics.drawString(font,NO_RESULTS,0,0,0x383838,false);
                    guiGraphics.pose().popPose();
                    return;
                }
                guiGraphics.drawString(font,RANK,panel.x + 40, panel.y + 20,0x383838,false);
                guiGraphics.drawString(font,USERNAME,panel.x + 108, panel.y + 20,0x383838,false);
                int totalWidth = 0;
                statsInScreen = 0;
                for (int index = page; index < board.renderables.size(); index++) {
                    int newWidth= totalWidth + board.renderables.get(index).getWidth();
                    if (newWidth > 351) break;
                    statsInScreen++;
                    totalWidth = newWidth;
                }
                if (statsInScreen == 0) return;
                int x = (351 - totalWidth) / (statsInScreen + 1);
                for (int index = page; index < page + statsInScreen; index++) {
                    SimpleLayoutRenderable r = board.renderables.get(index);
                    r.setPosition(panel.x + 182 + x,panel.y + 22 - r.height / 2);
                    r.render(guiGraphics,i,j,f);
                    x+= r.getWidth() + (351 - totalWidth) / statsInScreen;
                }
            }
        });
        renderableVList.init(this,panel.x + 9,panel.y + 39,551,226);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderTransparentBackground(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        controlTooltipRenderer.render(guiGraphics, i, j, f);
    }

    @Override
    public void onStatsUpdated() {
        if (!Legacy4JClient.isModEnabledOnServer()){
            refreshStatsBoards(minecraft);
            if (minecraft.screen instanceof LeaderboardsScreen s && LeaderboardsScreen.statsBoards.get(s.selectedStatBoard).statsList.isEmpty()) minecraft.executeIfPossible(()-> s.changeStatBoard(false));
        }
    }

    public static class StatsBoard{
        public final Component displayName;
        public final StatType<?> type;
        public final List<Stat<?>> statsList = new ArrayList<>();
        public List<SimpleLayoutRenderable> renderables = new ArrayList<>();
        public final Map<Predicate, LegacyIconHolder> statIconOverrides = new HashMap<>();
        public void clear(){
            statsList.clear();
            renderables.clear();
        }

        public SimpleLayoutRenderable getRenderable(Stat<?> stat){
            for (Map.Entry<Predicate, LegacyIconHolder> entry : statIconOverrides.entrySet()) {
                if (entry.getKey().test(stat.getValue())){
                    return entry.getValue();
                }
            }

            if (stat.getValue() instanceof ItemLike i){
                LegacyIconHolder h = new LegacyIconHolder(24,24);
                h.itemIcon = i.asItem().getDefaultInstance();
                return h;
            }else if (stat.getValue() instanceof EntityType<?> e){
                return LegacyIconHolder.entityHolder(0,0,24,24, e);
            }
            Component name = Component.translatable("stat." + stat.getValue().toString().replace(':', '.'));
            return SimpleLayoutRenderable.create(Minecraft.getInstance().font.width(name) * 2/3 + 2,7, (l)->((guiGraphics, i, j, f) -> {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(l.getX() + 1,l.getY(),0);
                guiGraphics.pose().scale(2/3f,2/3f,2/3f);
                guiGraphics.drawString(Minecraft.getInstance().font,name,0,0,0x383838,false);
                guiGraphics.pose().popPose();
            }));

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
            }return false;
        }

    }
    public static class Manager extends SimplePreparableReloadListener<List<StatsBoard>> {
        public static final String LEADERBOARD_LISTING = "leaderboard_listing.json";

        @Override
        protected List<StatsBoard> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            List<StatsBoard> statsBoards = new ArrayList<>();
            ResourceManager manager = Minecraft.getInstance().getResourceManager();

            manager.getNamespaces().stream().sorted(Comparator.comparingInt(s-> s.equals("legacy") ? 0 : 1)).forEach(name->manager.getResource(new ResourceLocation(name, LEADERBOARD_LISTING)).ifPresent(((r) -> {
                try {
                    BufferedReader bufferedReader = r.openAsReader();
                    JsonObject obj = GsonHelper.parse(bufferedReader);
                    JsonElement ioElement = obj.get("listing");
                    if (ioElement instanceof JsonArray array)
                        array.forEach(e-> {
                            if (e instanceof JsonObject o) statsBoards.add(statsBoardFromJson(o));
                        });
                    else if (ioElement instanceof JsonObject o) statsBoards.add(statsBoardFromJson(o));
                    bufferedReader.close();
                } catch (IOException exception) {
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            })));
            return statsBoards;
        }
        protected StatsBoard statsBoardFromJson(JsonObject o){
            StatType<?> statType = BuiltInRegistries.STAT_TYPE.get(ResourceLocation.tryParse(GsonHelper.getAsString(o,"type")));
            Component name = o.has("displayName") ? Component.translatable(GsonHelper.getAsString(o,"displayName")) : statType.getDisplayName();
            StatsBoard statsBoard;
            if (o.get("predicate") instanceof JsonObject predObj){
                Predicate predicate = JsonUtil.registryMatches(statType.getRegistry(),predObj);
                statsBoard = StatsBoard.create(statType,name, s-> predicate.test(s.getValue()));
            }else statsBoard = StatsBoard.create(statType,name);
            if (o.get("overrides") instanceof JsonArray a) a.forEach(e->{
                if (e instanceof JsonObject override && override.get("type") instanceof JsonPrimitive p) {
                    String type = p.getAsString();
                    Predicate predicate = JsonUtil.registryMatches(statType.getRegistry(),override.getAsJsonObject("predicate"));
                    switch (type) {
                        case "item" -> {
                            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(GsonHelper.getAsString(override, "id")));
                            LegacyIconHolder h =  new LegacyIconHolder(24, 24);
                            h.itemIcon = item.getDefaultInstance();
                            statsBoard.statIconOverrides.put(predicate, h);
                        }
                        case "entity_type" -> {
                            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(GsonHelper.getAsString(override, "id")));
                            statsBoard.statIconOverrides.put(predicate, LegacyIconHolder.entityHolder(0,0,24,24,entityType));
                        }
                        case "sprite" -> {
                            ResourceLocation sprite = ResourceLocation.tryParse(GsonHelper.getAsString(override, "id"));
                            LegacyIconHolder h = new LegacyIconHolder(24, 24);
                            h.iconSprite = sprite;
                            statsBoard.statIconOverrides.put(predicate, h);
                        }
                    }
                }
            });

            return statsBoard;
        }
        @Override
        protected void apply(List<StatsBoard> list, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            statsBoards.clear();
            statsBoards.addAll(list);
        }
    }

}
