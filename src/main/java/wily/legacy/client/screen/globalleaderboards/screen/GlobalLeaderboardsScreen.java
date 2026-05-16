package wily.legacy.client.screen.globalleaderboards.screen;

import com.mojang.blaze3d.platform.InputConstants;
import wily.legacy.client.screen.globalleaderboards.GlobalLeaderboardsFeature;
import wily.legacy.client.screen.globalleaderboards.board.GlobalLeaderboardBoardRegistry;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardBoardSnapshot;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardEntry;
import wily.legacy.client.screen.globalleaderboards.storage.GlobalLeaderboardStatCodec;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.stats.Stat;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;
import wily.legacy.client.screen.RenderableVList;
import wily.legacy.client.screen.LeaderboardsScreen;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

public final class GlobalLeaderboardsScreen extends PanelVListScreen {
   private static final Component RANK = Component.translatable("legacy.menu.leaderboard.rank");
   private static final Component USERNAME = Component.translatable("legacy.menu.leaderboard.username");
   private static final Component NO_RESULTS = Component.translatable("legacy.menu.leaderboard.no_results");
   private static final Component VIEW_AROUND_ME = Component.translatable("legacy.menu.leaderboard.view.around_me");
   private static final Component VIEW_TOP = Component.translatable("legacy.menu.leaderboard.view.top");
   private static final Component TOGGLE_VIEW = Component.translatable("legacy.menu.leaderboard.toggle_view");
   private int selectedStatBoard;
   private int statsInScreen;
   private int lastStatsInScreen;
   private int page;
   private int seenCacheVersion = -1;
   private GlobalLeaderboardsScreen.ViewMode viewMode = GlobalLeaderboardsScreen.ViewMode.AROUND_ME;
   private List<GlobalLeaderboardsScreen.DisplayedEntry> displayedEntries = Collections.emptyList();

   public GlobalLeaderboardsScreen(Screen parent) {
      super(parent, screen -> Panel.createPanel(screen, panel -> panel.appearance(568, 275)), CommonComponents.EMPTY);
      GlobalLeaderboardsFeature.ensureStarted(Minecraft.getInstance());
      this.refreshBoardDefinitions();
      this.selectFirstNonEmptyBoard();
      this.rebuildRenderableVList(Minecraft.getInstance());
      this.renderableVList.layoutSpacing(layout -> 1);
   }

   @Override
   public void addControlTooltips(ControlTooltip.Renderer renderer) {
      super.addControlTooltips(renderer);
      renderer.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon(), () -> TOGGLE_VIEW);
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      if (keyEvent.key() == InputConstants.KEY_O) {
         this.toggleViewMode();
         return true;
      }

      if (keyEvent.isLeft() || keyEvent.isRight()) {
         this.changeStatBoard(keyEvent.isLeft());
         return true;
      }

      if (keyEvent.key() == InputConstants.KEY_LBRACKET || keyEvent.key() == InputConstants.KEY_RBRACKET) {
         LeaderboardsScreen.StatsBoard board = this.selectedBoard();
         if (board != null && !board.renderables.isEmpty()) {
            int newPage = this.changedPage(keyEvent.key() == InputConstants.KEY_LBRACKET ? -this.lastStatsInScreen : this.statsInScreen);
            if (newPage != this.page) {
               this.lastStatsInScreen = this.statsInScreen;
               this.page = newPage;
               return true;
            }
         }
      }

      return this.renderableVList.keyPressed(keyEvent.key()) || super.keyPressed(keyEvent);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.renderableVList.mouseScrolled(verticalAmount);
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public void rebuildRenderableVList(Minecraft minecraft) {
      this.renderableVList.renderables.clear();
      this.displayedEntries = this.resolveEntries(this.boardId());
      this.ensureBoardStats(this.displayedEntries);
      LeaderboardsScreen.StatsBoard board = this.selectedBoard();
      if (board == null || board.statsList.isEmpty() || this.displayedEntries.isEmpty()) {
         return;
      }

      for (GlobalLeaderboardsScreen.DisplayedEntry entry : this.displayedEntries) {
         String rankLabel = Integer.toString(entry.rank());
         this.renderableVList.renderables.add(new AbstractWidget(0, 0, 551, 20, Component.literal(entry.name())) {
            @Override
            protected void extractWidgetRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float delta) {
               int y = this.getY() + (this.getHeight() - GlobalLeaderboardsScreen.this.font.lineHeight) / 2 + 1;
               FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(this.isHoveredOrFocused() ? LegacySprites.LEADERBOARD_BUTTON_HIGHLIGHTED : LegacySprites.LEADERBOARD_BUTTON, this.getX(), this.getY(), this.getWidth(), this.getHeight());
               LegacyFontUtil.applySDFont(enabled -> {
                  GuiGraphicsExtractor.text(GlobalLeaderboardsScreen.this.font, rankLabel, this.getX() + GlobalLeaderboardsScreen.this.accessor.getInteger("renderableVList.buttonRank.x", 40) - GlobalLeaderboardsScreen.this.font.width(rankLabel) / 2, y, LegacyRenderUtil.getDefaultTextColor(!this.isHoveredOrFocused()));
                  GuiGraphicsExtractor.text(GlobalLeaderboardsScreen.this.font, this.getMessage(), this.getX() + GlobalLeaderboardsScreen.this.accessor.getInteger("renderableVList.buttonUsername.x", 120) - GlobalLeaderboardsScreen.this.font.width(this.getMessage()) / 2, y, LegacyRenderUtil.getDefaultTextColor(!this.isHoveredOrFocused()));
                  int added = 0;
                  Component hoveredValue = null;

                  for (int index = GlobalLeaderboardsScreen.this.page; index < board.statsList.size(); index++) {
                     if (added >= GlobalLeaderboardsScreen.this.statsInScreen) {
                        break;
                     }

                     Stat<?> stat = board.statsList.get(index);
                     Component value = ControlTooltip.CONTROL_ICON_FUNCTION.apply(stat.format(entry.statsMap().getInt(stat)), Style.EMPTY).getComponent();
                     SimpleLayoutRenderable renderable = board.renderables.get(index);
                     int areaLeft = GlobalLeaderboardsScreen.this.statValueLeft(board, index);
                     int areaRight = GlobalLeaderboardsScreen.this.statValueRight(board, index);
                     GlobalLeaderboardsScreen.this.drawStatValue(GuiGraphicsExtractor, value, areaLeft, areaRight, this.getY(), this.getHeight(), LegacyRenderUtil.getDefaultTextColor(!this.isHoveredOrFocused()));
                     if (LegacyRenderUtil.isMouseOver(mouseX, mouseY, areaLeft, this.getY(), areaRight - areaLeft, this.getHeight())) {
                        hoveredValue = value;
                     }
                     added++;
                  }

                  if (hoveredValue != null) {
                     GuiGraphicsExtractor.setTooltipForNextFrame(GlobalLeaderboardsScreen.this.font, hoveredValue, mouseX, mouseY);
                  }
               });
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput narration) {
               this.defaultButtonNarrationText(narration);
            }
         });
      }
   }

   @Override
   public void tick() {
      super.tick();
      int cacheVersion = GlobalLeaderboardsFeature.cacheVersion();
      if (cacheVersion != this.seenCacheVersion) {
         this.seenCacheVersion = cacheVersion;
         this.refreshBoardDefinitions();
         this.rebuildRenderableVList(this.minecraft);
         this.repositionElements();
      }
   }

   @Override
   protected void panelInit() {
      super.panelInit();
      this.addRenderableOnly((GuiGraphicsExtractor, mouseX, mouseY, delta) -> {
         int topTooltipHeight = this.accessor.getInteger("topTooltip.height", 18);
         int topTooltipY = this.panel.y + this.accessor.getInteger("topTooltip.y", -topTooltipHeight);
         int filterTooltipWidth = this.accessor.getInteger("filterTooltip.width", 166);
         int filterTooltipX = this.panel.x + this.accessor.getInteger("filterTooltip.x", 8);
         int boardTooltipWidth = this.accessor.getInteger("boardTooltip.width", 211);
         int boardTooltipX = this.panel.x + this.accessor.getInteger("boardTooltip.x", (this.panel.width - boardTooltipWidth) / 2);
         int entriesTooltipWidth = this.accessor.getInteger("entriesTooltip.width", 166);
         int entriesTooltipX = this.panel.x + this.panel.width - entriesTooltipWidth + this.accessor.getInteger("entriesTooltip.x", -8);
         LegacyRenderUtil.renderPointerPanel(GuiGraphicsExtractor, filterTooltipX, topTooltipY, filterTooltipWidth, topTooltipHeight);
         LegacyRenderUtil.renderPointerPanel(GuiGraphicsExtractor, boardTooltipX, topTooltipY, boardTooltipWidth, topTooltipHeight);
         LegacyRenderUtil.renderPointerPanel(GuiGraphicsExtractor, entriesTooltipX, topTooltipY, entriesTooltipWidth, topTooltipHeight);
         LeaderboardsScreen.StatsBoard board = this.selectedBoard();
         if (board == null) {
            return;
         }

         LegacyFontUtil.applyFontOverrideIf(LegacyOptions.getUIMode().isHD(), LegacyFontUtil.MOJANGLES_11_FONT, enabled -> {
            float scale = this.accessor.getFloat("topTooltip.scale", LegacyOptions.getUIMode().isFHD() ? 2 / 3.0F : 1.0F);
            this.drawTopTooltip(GuiGraphicsExtractor, filterTooltipX, filterTooltipWidth, topTooltipY, this.viewMode.label(), scale, enabled, this.accessor.getInteger("filterText.y", 6));
            this.drawTopTooltip(GuiGraphicsExtractor, boardTooltipX, boardTooltipWidth, topTooltipY, board.displayName, scale, enabled, this.accessor.getInteger("boardText.y", 6));
            this.drawTopTooltip(GuiGraphicsExtractor, entriesTooltipX, entriesTooltipWidth, topTooltipY, Component.translatable("legacy.menu.leaderboard.entries", this.displayedEntries.size()), scale, enabled, this.accessor.getInteger("entriesText.y", 6));
         });

         if (board.statsList.isEmpty()) {
            GuiGraphicsExtractor.pose().pushMatrix();
            GuiGraphicsExtractor.pose().translate(this.panel.x + (this.panel.width - this.font.width(NO_RESULTS) * 1.5F) / 2.0F, this.panel.y + (this.panel.height - 13.5F) / 2.0F);
            GuiGraphicsExtractor.pose().scale(1.5F, 1.5F);
            GuiGraphicsExtractor.text(this.font, NO_RESULTS, 0, 0, CommonColor.GRAY_TEXT.get(), false);
            GuiGraphicsExtractor.pose().popMatrix();
            return;
         }

         LegacyFontUtil.applySDFont(enabled -> {
            GuiGraphicsExtractor.text(this.font, RANK, this.panel.x + this.accessor.getInteger("rankText.x", 40), this.panel.y + this.accessor.getInteger("rankText.y", 20), CommonColor.GRAY_TEXT.get(), false);
            GuiGraphicsExtractor.text(this.font, USERNAME, this.panel.x + this.accessor.getInteger("usernameText.x", 108), this.panel.y + this.accessor.getInteger("usernameText.y", 20), CommonColor.GRAY_TEXT.get(), false);
         });

         int statsBoardX = this.accessor.getInteger("statsBoard.x", 182);
         int statsBoardY = this.accessor.getInteger("statsBoard.y", 22);
         int totalStatsWidth = this.accessor.getInteger("statsBoard.width", 351);
         int totalWidth = 0;
         this.statsInScreen = 0;

         for (int index = this.page; index < board.renderables.size(); index++) {
            int newWidth = totalWidth + board.renderables.get(index).getWidth();
            if (newWidth > totalStatsWidth) {
               break;
            }

            this.statsInScreen++;
            totalWidth = newWidth;
         }

         int boardControlTooltipY = topTooltipY + this.accessor.getInteger("boardControlTooltip.y", 6);
         GuiGraphicsExtractor.pose().pushMatrix();
         GuiGraphicsExtractor.pose().translate(boardTooltipX + this.accessor.getInteger("boardControlTooltip.x", 2), boardControlTooltipY);
         if (!LegacyOptions.getUIMode().isSD()) {
            GuiGraphicsExtractor.pose().scale(0.5F, 0.5F);
         }

         (ControlType.getActiveType().isKbm() ? ControlTooltip.CompoundComponentIcon.of(ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)) : ControllerBinding.LEFT_STICK.getIcon()).render(GuiGraphicsExtractor, 4, 0, false);
         GuiGraphicsExtractor.pose().popMatrix();
         if (this.statsInScreen < board.renderables.size()) {
            ControlTooltip.Icon pageControl = ControlTooltip.CompoundComponentIcon.of(ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.getIcon(), ControlTooltip.SPACE_ICON, ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.getIcon());
            GuiGraphicsExtractor.pose().pushMatrix();
            GuiGraphicsExtractor.pose().translate(boardTooltipX + boardTooltipWidth + this.accessor.getInteger("boardPageTooltip.x", -4), boardControlTooltipY);
            if (!LegacyOptions.getUIMode().isSD()) {
               GuiGraphicsExtractor.pose().scale(0.5F, 0.5F);
            }

            pageControl.render(GuiGraphicsExtractor, -pageControl.getWidth(), 0, false);
            GuiGraphicsExtractor.pose().popMatrix();
         }

         if (this.statsInScreen == 0) {
            return;
         }

         int x = (totalStatsWidth - totalWidth) / (this.statsInScreen + 1);
         Integer hovered = null;
         for (int index = this.page; index < this.page + this.statsInScreen; index++) {
            SimpleLayoutRenderable renderable = board.renderables.get(index);
            renderable.setPosition(this.panel.x + statsBoardX + x, this.panel.y + statsBoardY - renderable.height / 2);
            renderable.extractRenderState(GuiGraphicsExtractor, mouseX, mouseY, delta);
            if (renderable.isHovered(mouseX, mouseY)) {
               hovered = index;
            }
            x += renderable.getWidth() + (totalStatsWidth - totalWidth) / this.statsInScreen;
         }

         if (hovered != null) {
            Stat<?> stat = board.statsList.get(hovered);
            GuiGraphicsExtractor.setTooltipForNextFrame(this.font, ControlTooltip.getAction("stat." + stat.getValue().toString().replace(':', '.')), mouseX, mouseY);
         }
      });
   }

   @Override
   public void initRenderableVListEntry(RenderableVList renderableVList, net.minecraft.client.gui.components.Renderable renderable) {
      if (renderable instanceof AbstractWidget widget) {
         widget.setHeight(this.accessor.getInteger("buttonsHeight", 20));
      }
   }

   @Override
   public void renderableVListInit() {
      this.getRenderableVList().init(this.panel.x + 9, this.panel.y + 39, this.panel.width - 17, this.panel.height - 49);
   }

   @Override
   public void renderDefaultBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float delta) {
      LegacyRenderUtil.renderDefaultBackground(this.accessor, GuiGraphicsExtractor, false);
   }

   private void changeStatBoard(boolean left) {
      List<LeaderboardsScreen.StatsBoard> boards = GlobalLeaderboardBoardRegistry.statsBoards();
      if (boards.isEmpty()) {
         return;
      }

      int initial = this.selectedStatBoard;
      while (this.selectedStatBoard != (this.selectedStatBoard = Stocker.cyclic(0, this.selectedStatBoard + (left ? -1 : 1), boards.size())) && this.selectedStatBoard != initial) {
         if (!boards.get(this.selectedStatBoard).statsList.isEmpty()) {
            this.page = 0;
            this.rebuildRenderableVList(this.minecraft);
            this.repositionElements();
            return;
         }
      }
   }

   private void toggleViewMode() {
      this.viewMode = this.viewMode == GlobalLeaderboardsScreen.ViewMode.AROUND_ME ? GlobalLeaderboardsScreen.ViewMode.TOP : GlobalLeaderboardsScreen.ViewMode.AROUND_ME;
      this.page = 0;
      this.rebuildRenderableVList(this.minecraft);
      this.repositionElements();
   }

   private int changedPage(int count) {
      LeaderboardsScreen.StatsBoard board = this.selectedBoard();
      return board == null ? 0 : Math.max(0, this.page + count >= board.renderables.size() ? this.page : this.page + count);
   }

   private void refreshBoardDefinitions() {
      GlobalLeaderboardBoardRegistry.ensureStatsBoards(this.minecraft);
      GlobalLeaderboardBoardRegistry.ensureTrackedBoardStats(this.minecraft);
   }

   private void selectFirstNonEmptyBoard() {
      int defaultBoard = GlobalLeaderboardBoardRegistry.defaultBoardIndex();
      List<LeaderboardsScreen.StatsBoard> boards = GlobalLeaderboardBoardRegistry.statsBoards();
      if (defaultBoard >= 0 && defaultBoard < boards.size() && !boards.get(defaultBoard).statsList.isEmpty()) {
         this.selectedStatBoard = defaultBoard;
         return;
      }

      for (int i = 0; i < boards.size(); i++) {
         if (!boards.get(i).statsList.isEmpty()) {
            this.selectedStatBoard = i;
            return;
         }
      }

      this.selectedStatBoard = 0;
   }

   private LeaderboardsScreen.StatsBoard selectedBoard() {
      return GlobalLeaderboardBoardRegistry.statsBoard(this.selectedStatBoard);
   }

   private String boardId() {
      return GlobalLeaderboardsFeature.boardId(this.selectedStatBoard);
   }

   private List<GlobalLeaderboardsScreen.DisplayedEntry> resolveEntries(String boardId) {
      List<GlobalLeaderboardEntry> cachedEntries = GlobalLeaderboardsFeature.entries(boardId, this.viewMode);
      if (!cachedEntries.isEmpty()) {
         this.requestBoard(boardId);
         List<GlobalLeaderboardsScreen.DisplayedEntry> resolved = new ArrayList<>();
         cachedEntries.forEach(entry -> resolved.add(new GlobalLeaderboardsScreen.DisplayedEntry(entry.rank(), entry.playerName(), GlobalLeaderboardStatCodec.decodeMap(entry.statValues()), entry.totalScore())));
         this.applyLocalPlayerRow(boardId, resolved);
         resolved.sort(Comparator.comparingInt(GlobalLeaderboardsScreen.DisplayedEntry::rank));
         return resolved;
      }

      this.requestBoard(boardId);

      GlobalLeaderboardBoardSnapshot snapshot = GlobalLeaderboardsFeature.boardSnapshot(boardId);
      if (snapshot == null || snapshot.totalScore() <= 0) {
         return List.of();
      }

      return List.of(new GlobalLeaderboardsScreen.DisplayedEntry(1, GlobalLeaderboardsFeature.playerName().isBlank() ? this.minecraft.getUser().getName() : GlobalLeaderboardsFeature.playerName(), GlobalLeaderboardStatCodec.decodeMap(snapshot.statValues()), snapshot.totalScore()));
   }

   private void requestBoard(String boardId) {
      GlobalLeaderboardsFeature.requestBoard(boardId, this.viewMode);
   }

   private void applyLocalPlayerRow(String boardId, List<GlobalLeaderboardsScreen.DisplayedEntry> resolved) {
      GlobalLeaderboardBoardSnapshot snapshot = GlobalLeaderboardsFeature.boardSnapshot(boardId);
      if (snapshot == null || snapshot.totalScore() <= 0) {
         return;
      }

      String localName = GlobalLeaderboardsFeature.playerName().isBlank() ? this.minecraft.getUser().getName() : GlobalLeaderboardsFeature.playerName();
      Object2IntOpenHashMap<Stat<?>> localStats = GlobalLeaderboardStatCodec.decodeMap(snapshot.statValues());
      for (int i = 0; i < resolved.size(); i++) {
         GlobalLeaderboardsScreen.DisplayedEntry entry = resolved.get(i);
         if (entry.name().equals(localName)) {
            resolved.set(i, new GlobalLeaderboardsScreen.DisplayedEntry(entry.rank(), localName, localStats, snapshot.totalScore()));
            return;
         }
      }

      if (this.viewMode != GlobalLeaderboardsScreen.ViewMode.AROUND_ME) {
         return;
      }

      int nextRank = resolved.stream().mapToInt(GlobalLeaderboardsScreen.DisplayedEntry::rank).max().orElse(0) + 1;
      resolved.add(new GlobalLeaderboardsScreen.DisplayedEntry(nextRank, localName, localStats, snapshot.totalScore()));
   }

   private void ensureBoardStats(List<GlobalLeaderboardsScreen.DisplayedEntry> entries) {
      LeaderboardsScreen.StatsBoard board = this.selectedBoard();
      if (board == null) {
         return;
      }

      entries.forEach(entry -> entry.statsMap().object2IntEntrySet().forEach(statEntry -> GlobalLeaderboardBoardRegistry.addStat(board, statEntry.getKey())));
   }

   private void drawTopTooltip(GuiGraphicsExtractor GuiGraphicsExtractor, int x, int width, int y, Component text, float scale, boolean enabled, int textOffsetY) {
      GuiGraphicsExtractor.pose().pushMatrix();
      GuiGraphicsExtractor.pose().translate(x + (width - this.font.width(text) * scale) / 2.0F, y + textOffsetY);
      if (!enabled) {
         GuiGraphicsExtractor.pose().scale(scale);
      }
      GuiGraphicsExtractor.text(this.font, text, 0, 0, 0xFFFFFFFF);
      GuiGraphicsExtractor.pose().popMatrix();
   }

   private int statValueLeft(LeaderboardsScreen.StatsBoard board, int index) {
      int boardLeft = this.panel.x + this.accessor.getInteger("statsBoard.x", 182);
      if (index <= this.page) {
         return boardLeft;
      }

      SimpleLayoutRenderable current = board.renderables.get(index);
      SimpleLayoutRenderable previous = board.renderables.get(index - 1);
      return (this.centerX(previous) + this.centerX(current)) / 2;
   }

   private int statValueRight(LeaderboardsScreen.StatsBoard board, int index) {
      int boardRight = this.panel.x + this.accessor.getInteger("statsBoard.x", 182) + this.accessor.getInteger("statsBoard.width", 351);
      if (index + 1 >= board.renderables.size() || index + 1 >= this.page + this.statsInScreen) {
         return boardRight;
      }

      SimpleLayoutRenderable current = board.renderables.get(index);
      SimpleLayoutRenderable next = board.renderables.get(index + 1);
      return (this.centerX(current) + this.centerX(next)) / 2;
   }

   private int centerX(SimpleLayoutRenderable renderable) {
      return renderable.getX() + renderable.getWidth() / 2;
   }

   private void drawStatValue(GuiGraphicsExtractor GuiGraphicsExtractor, Component value, int left, int right, int rowY, int rowHeight, int color) {
      int width = this.font.width(value);
      if (width <= 0) {
         return;
      }

      int x = left + (right - left - width) / 2;
      int y = rowY + (rowHeight - this.font.lineHeight) / 2 + 1;
      GuiGraphicsExtractor.text(this.font, value, x, y, color);
   }

   private record DisplayedEntry(int rank, String name, Object2IntOpenHashMap<Stat<?>> statsMap, int totalScore) {
   }

   public static enum ViewMode {
      AROUND_ME(VIEW_AROUND_ME),
      TOP(VIEW_TOP);

      private final Component label;

      private ViewMode(Component label) {
         this.label = label;
      }

      public Component label() {
         return this.label;
      }
   }
}

