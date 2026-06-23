package wily.legacy.client.screen.globalleaderboards.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardBoard;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardColumn;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardDifficulty;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardRow;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardViewMode;
import wily.legacy.api.client.leaderboards.LegacyLeaderboards;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.LeaderboardsScreen;
import wily.legacy.client.screen.globalleaderboards.GlobalLeaderboardsFeature;
import wily.legacy.client.screen.globalleaderboards.board.GlobalLeaderboardBoardRegistry;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardBoardSnapshot;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

public final class GlobalLeaderboardsScreen extends LeaderboardsScreen {
   private static final Component VIEW_AROUND_ME = Component.translatable("legacy.menu.leaderboard.view.around_me");
   private static final Component VIEW_TOP = Component.translatable("legacy.menu.leaderboard.view.top");
   private static final Component TOGGLE_VIEW = Component.translatable("legacy.menu.leaderboard.toggle_view");
   private final Map<String, List<SimpleLayoutRenderable>> columnRenderables = new HashMap<>();
   private int seenCacheVersion = -1;
   private int seenBoardsVersion = -1;
   private GlobalLeaderboardViewMode viewMode = GlobalLeaderboardViewMode.TOP;
   private GlobalLeaderboardDifficulty difficulty = GlobalLeaderboardDifficulty.NORMAL;
   private List<GlobalLeaderboardRow> rows = List.of();

   public GlobalLeaderboardsScreen(Screen parent) {
      super(parent, false);
      GlobalLeaderboardsFeature.ensureStarted(Minecraft.getInstance());
      this.refreshBoardDefinitions();
      this.selectFirstNonEmptyBoard();
      this.rebuildRenderableVList(Minecraft.getInstance());
      this.renderableVList.layoutSpacing(layout -> 1);
   }

   @Override
   protected ControlTooltip.Icon filterControlIcon() {
      return ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon();
   }

   @Override
   protected Component filterControlTooltip() {
      return TOGGLE_VIEW;
   }

   @Override
   protected int filterKey() {
      return InputConstants.KEY_O;
   }

   @Override
   protected void cycleFilter() {
      this.viewMode = this.viewMode == GlobalLeaderboardViewMode.AROUND_ME ? GlobalLeaderboardViewMode.TOP : GlobalLeaderboardViewMode.AROUND_ME;
      this.resetPageAndScroll();
   }

   private void cycleDifficulty(boolean left) {
      GlobalLeaderboardDifficulty[] values = GlobalLeaderboardDifficulty.values();
      GlobalLeaderboardBoard board = this.selectedGlobalBoard();
      int index = this.difficulty.ordinal();
      do {
         index = wily.factoryapi.base.Stocker.cyclic(0, index + (left ? -1 : 1), values.length);
         this.difficulty = values[index];
      } while (board != null && !GlobalLeaderboardBoardRegistry.supportsDifficulty(board.id(), this.difficulty));
      this.resetPageAndScroll();
   }

   @Override
   protected Component filterText() {
      return Component.translatable("legacy.menu.leaderboard.filter", this.viewMode == GlobalLeaderboardViewMode.AROUND_ME ? VIEW_AROUND_ME : VIEW_TOP);
   }

   @Override
   protected void selectFirstNonEmptyBoard() {
      List<GlobalLeaderboardBoard> boards = GlobalLeaderboardsFeature.boards();
      for (int i = 0; i < boards.size(); i++) {
         GlobalLeaderboardBoard board = boards.get(i);
         if (GlobalLeaderboardBoardRegistry.defaultBoardId().equals(board.id()) && !board.columns().isEmpty()) {
            this.selectedStatBoard = i;
            return;
         }
      }

      for (int i = 0; i < boards.size(); i++) {
         if (!boards.get(i).columns().isEmpty()) {
            this.selectedStatBoard = i;
            return;
         }
      }
      this.selectedStatBoard = 0;
   }

   @Override
   public void changeStatBoard(boolean left) {
      List<GlobalLeaderboardBoard> boards = GlobalLeaderboardsFeature.boards();
      if (boards.isEmpty()) {
         return;
      }
      int initialSelectedStatBoard = this.selectedStatBoard;
      while (this.selectedStatBoard != (this.selectedStatBoard = wily.factoryapi.base.Stocker.cyclic(0, this.selectedStatBoard + (left ? -1 : 1), boards.size())) && this.selectedStatBoard != initialSelectedStatBoard) {
         if (!boards.get(this.selectedStatBoard).columns().isEmpty()) {
            if (!GlobalLeaderboardBoardRegistry.supportsDifficulty(boards.get(this.selectedStatBoard).id(), this.difficulty)) {
               this.difficulty = GlobalLeaderboardDifficulty.EASY;
            }
            this.resetPageAndScroll();
            this.rebuildRenderableVList(this.minecraft);
            this.repositionElements();
            return;
         }
      }
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      if (keyEvent.key() == this.filterKey()) {
         this.cycleFilter();
         this.rebuildRenderableVList(this.minecraft);
         this.repositionElements();
         return true;
      }
      if (keyEvent.key() == InputConstants.KEY_D) {
         this.cycleDifficulty(false);
         this.rebuildRenderableVList(this.minecraft);
         this.repositionElements();
         return true;
      }
      if (keyEvent.isLeft() || keyEvent.isRight()) {
         this.changeStatBoard(keyEvent.isLeft());
         return true;
      }
      if (keyEvent.key() == InputConstants.KEY_LBRACKET || keyEvent.key() == InputConstants.KEY_RBRACKET) {
         this.cycleDifficulty(keyEvent.key() == InputConstants.KEY_LBRACKET);
         this.rebuildRenderableVList(this.minecraft);
         this.repositionElements();
         return true;
      }
      if (this.renderableVList.keyPressed(keyEvent.key())) {
         return true;
      }
      return super.keyPressed(keyEvent);
   }

   @Override
   public void rebuildRenderableVList(Minecraft minecraft) {
      this.renderableVList.renderables.clear();
      GlobalLeaderboardBoard board = this.selectedGlobalBoard();
      if (board == null || board.columns().isEmpty()) {
         this.rows = List.of();
         return;
      }

      this.rows = this.resolveRows(board);
      for (GlobalLeaderboardRow row : this.rows) {
         String rank = Integer.toString(row.rank() > 0 ? row.rank() : this.rows.indexOf(row) + 1);
         this.renderableVList.renderables.add(new AbstractWidget(0, 0, 551, 20, Component.literal(row.playerName())) {
            @Override
            protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
               int y = getY() + (getHeight() - font.lineHeight) / 2 + 1;
               FactoryGuiGraphics.of(graphics).blitSprite(isHoveredOrFocused() ? LegacySprites.LEADERBOARD_BUTTON_HIGHLIGHTED : LegacySprites.LEADERBOARD_BUTTON, getX(), getY(), getWidth(), getHeight());
               LegacyFontUtil.applySDFont(fontOverride -> {
                  graphics.text(font, rank, getX() + accessor.getInteger("renderableVList.buttonRank.x", 40) - font.width(rank) / 2, y, LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()));
                  graphics.text(font, getMessage(), getX() + accessor.getInteger("renderableVList.buttonUsername.x", 120) - font.width(getMessage()) / 2, y, LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()));

                  int added = 0;
                  List<GlobalLeaderboardColumn> columns = board.columns();
                  List<SimpleLayoutRenderable> renderables = columnRenderables(board);
                  for (int index = page; index < columns.size(); index++) {
                     if (added >= statsInScreen) {
                        break;
                     }
                     GlobalLeaderboardColumn column = columns.get(index);
                     Component value = column.format(row.columnValue(column.id()));
                     SimpleLayoutRenderable renderable = renderables.get(index);
                     int w = font.width(value);
                     int valueX = renderable.getX() + (renderable.getWidth() - w) / 2;
                     graphics.text(font, value, valueX, y, LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()));
                     added++;
                  }
               });
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
               this.defaultButtonNarrationText(narrationElementOutput);
            }
         });
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.refreshBoardDefinitions();
      int cacheVersion = GlobalLeaderboardsFeature.cacheVersion();
      int boardsVersion = GlobalLeaderboardsFeature.boards().hashCode();
      if (cacheVersion != this.seenCacheVersion || boardsVersion != this.seenBoardsVersion) {
         this.seenCacheVersion = cacheVersion;
         this.seenBoardsVersion = boardsVersion;
         this.rebuildRenderableVList(this.minecraft);
         this.repositionElements();
      }
   }

   @Override
   protected boolean refreshesOnlineStats() {
      return false;
   }

   @Override
   protected void panelInit() {
      this.addRenderableOnly(this.panel);
      this.panel.init();
      this.addRenderableOnly((graphics, mouseX, mouseY, delta) -> {
         int topTooltipHeight = this.accessor.getInteger("topTooltip.height", 18);
         int topTooltipY = this.panel.y + this.accessor.getInteger("topTooltip.y", -topTooltipHeight);
         int filterTooltipWidth = this.accessor.getInteger("filterTooltip.width", 166);
         int filterTooltipX = this.panel.x + this.accessor.getInteger("filterTooltip.x", 8);
         int boardTooltipWidth = this.accessor.getInteger("boardTooltip.width", 211);
         int boardTooltipX = this.panel.x + this.accessor.getInteger("boardTooltip.x", (this.panel.width - boardTooltipWidth) / 2);
         int entriesTooltipWidth = this.accessor.getInteger("entriesTooltip.width", 166);
         int entriesTooltipX = this.panel.x + this.panel.width - entriesTooltipWidth + this.accessor.getInteger("entriesTooltip.x", -8);
         LegacyRenderUtil.renderPointerPanel(graphics, filterTooltipX, topTooltipY, filterTooltipWidth, topTooltipHeight);
         LegacyRenderUtil.renderPointerPanel(graphics, boardTooltipX, topTooltipY, boardTooltipWidth, topTooltipHeight);
         LegacyRenderUtil.renderPointerPanel(graphics, entriesTooltipX, topTooltipY, entriesTooltipWidth, topTooltipHeight);
         GlobalLeaderboardBoard board = this.selectedGlobalBoard();
         if (board == null) {
            return;
         }
         LegacyFontUtil.applyFontOverrideIf(LegacyOptions.getUIMode().isHD(), LegacyFontUtil.MOJANGLES_11_FONT, fontOverride -> {
            float topTooltipScale = this.accessor.getFloat("topTooltip.scale", LegacyOptions.getUIMode().isFHD() ? 2 / 3f : 1.0f);
            int topTextColor = CommonColor.ITEM_NAME_TEXT.get();
            graphics.pose().pushMatrix();
            Component filter = this.filterText();
            graphics.pose().translate(filterTooltipX + (filterTooltipWidth - this.font.width(filter) * topTooltipScale) / 2, topTooltipY + this.accessor.getInteger("filterText.y", 6));
            if (!fontOverride) {
               graphics.pose().scale(topTooltipScale);
            }
            graphics.text(this.font, filter, 0, 0, topTextColor);
            graphics.pose().popMatrix();
            graphics.pose().pushMatrix();
            Component boardTitle = this.boardTitle(board);
            graphics.pose().translate(boardTooltipX + (boardTooltipWidth - this.font.width(boardTitle) * topTooltipScale) / 2, topTooltipY + this.accessor.getInteger("boardText.y", 6));
            if (!fontOverride) {
               graphics.pose().scale(topTooltipScale);
            }
            graphics.text(this.font, boardTitle, 0, 0, topTextColor);
            graphics.pose().popMatrix();
            graphics.pose().pushMatrix();
            Component entries = Component.translatable("legacy.menu.leaderboard.entries", this.rows.size());
            graphics.pose().translate(entriesTooltipX + (entriesTooltipWidth - this.font.width(entries) * topTooltipScale) / 2, topTooltipY + this.accessor.getInteger("entriesText.y", 6));
            if (!fontOverride) {
               graphics.pose().scale(topTooltipScale);
            }
            graphics.text(this.font, entries, 0, 0, topTextColor);
            graphics.pose().popMatrix();
         });
         if (board.columns().isEmpty()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(this.panel.x + (this.panel.width - this.font.width(NO_RESULTS) * 1.5f) / 2f, this.panel.y + (this.panel.height - 13.5f) / 2f);
            graphics.pose().scale(1.5f, 1.5f);
            graphics.text(this.font, NO_RESULTS, 0, 0, CommonColor.GRAY_TEXT.get(), false);
            graphics.pose().popMatrix();
            return;
         }
         LegacyFontUtil.applySDFont(fontOverride -> {
            graphics.text(this.font, RANK, this.panel.x + this.accessor.getInteger("rankText.x", 40), this.panel.y + this.accessor.getInteger("rankText.y", 20), CommonColor.GRAY_TEXT.get(), false);
            graphics.text(this.font, USERNAME, this.panel.x + this.accessor.getInteger("usernameText.x", 108), this.panel.y + this.accessor.getInteger("usernameText.y", 20), CommonColor.GRAY_TEXT.get(), false);
         });

         int statsBoardX = this.accessor.getInteger("statsBoard.x", 182);
         int statsBoardY = this.accessor.getInteger("statsBoard.y", 22);
         int totalStatsWidth = this.accessor.getInteger("statsBoard.width", 351);
         List<SimpleLayoutRenderable> renderables = this.columnRenderables(board);
         int totalWidth = 0;
         this.statsInScreen = 0;
         for (int index = this.page; index < renderables.size(); index++) {
            int newWidth = totalWidth + renderables.get(index).getWidth();
            if (newWidth > totalStatsWidth) {
               break;
            }
            this.statsInScreen++;
            totalWidth = newWidth;
         }
         int boardControlTooltipY = topTooltipY + this.accessor.getInteger("boardControlTooltip.y", 6);
         graphics.pose().pushMatrix();
         graphics.pose().translate(boardTooltipX + this.accessor.getInteger("boardControlTooltip.x", 2), boardControlTooltipY);
         graphics.pose().scale(LegacyOptions.getUIMode().isSD() ? 1.2f : 0.6f);
         (ControlType.getActiveType().isKbm() ? ControlTooltip.CompoundComponentIcon.of(ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)) : ControllerBinding.LEFT_STICK.getIcon()).render(graphics, 4, 0, false);
         graphics.pose().popMatrix();
         ControlTooltip.Icon difficultyControl = ControlTooltip.CompoundComponentIcon.of(ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.getIcon(), ControlTooltip.SPACE_ICON, ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.getIcon());
         graphics.pose().pushMatrix();
         graphics.pose().translate(boardTooltipX + boardTooltipWidth + this.accessor.getInteger("boardPageTooltip.x", -4), boardControlTooltipY);
         if (!LegacyOptions.getUIMode().isSD()) {
            graphics.pose().scale(0.5f, 0.5f);
         }
         difficultyControl.render(graphics, -difficultyControl.getWidth(), 0, false);
         graphics.pose().popMatrix();
         if (this.statsInScreen == 0) {
            return;
         }
         int x = (totalStatsWidth - totalWidth) / (this.statsInScreen + 1);
         Integer hovered = null;
         for (int index = this.page; index < this.page + this.statsInScreen; index++) {
            SimpleLayoutRenderable renderable = renderables.get(index);
            renderable.setPosition(this.panel.x + statsBoardX + x, this.panel.y + statsBoardY - renderable.height / 2);
            renderable.extractRenderState(graphics, mouseX, mouseY, delta);
            if (renderable.isHovered(mouseX, mouseY)) {
               hovered = index;
            }
            x += renderable.getWidth() + (totalStatsWidth - totalWidth) / this.statsInScreen;
         }
         if (hovered != null) {
            graphics.setTooltipForNextFrame(this.font, board.columns().get(hovered).displayName(), mouseX, mouseY);
         }
      });
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.EMPTY;
   }

   private void refreshBoardDefinitions() {
      GlobalLeaderboardsFeature.refreshBoards(this.minecraft);
   }

   private GlobalLeaderboardBoard selectedGlobalBoard() {
      return GlobalLeaderboardsFeature.board(this.selectedStatBoard);
   }

   private List<GlobalLeaderboardRow> resolveRows(GlobalLeaderboardBoard board) {
      List<GlobalLeaderboardRow> cachedEntries = GlobalLeaderboardsFeature.entries(board, this.viewMode, this.difficulty);
      if (!cachedEntries.isEmpty()) {
         this.requestBoard(board);
         List<GlobalLeaderboardRow> entries = new ArrayList<>(cachedEntries);
         this.applyLocalPlayerRow(board, entries);
         entries.sort(Comparator.comparingInt(GlobalLeaderboardRow::rank));
         return entries;
      }

      this.requestBoard(board);
      GlobalLeaderboardBoardSnapshot snapshot = GlobalLeaderboardsFeature.boardSnapshot(board, this.difficulty);
      if (snapshot == null || snapshot.totalScore() <= 0) {
         return List.of();
      }

      return List.of(this.localRow(1, snapshot));
   }

   private void requestBoard(GlobalLeaderboardBoard board) {
      GlobalLeaderboardsFeature.requestBoard(board, this.viewMode, this.difficulty);
   }

   private void applyLocalPlayerRow(GlobalLeaderboardBoard board, List<GlobalLeaderboardRow> entries) {
      GlobalLeaderboardBoardSnapshot snapshot = GlobalLeaderboardsFeature.boardSnapshot(board, this.difficulty);
      if (snapshot == null || snapshot.totalScore() <= 0) {
         return;
      }

      String localName = GlobalLeaderboardsFeature.playerName().isBlank() ? this.minecraft.getUser().getName() : GlobalLeaderboardsFeature.playerName();
      String localUuid = GlobalLeaderboardsFeature.playerUuid();
      for (int i = 0; i < entries.size(); i++) {
         GlobalLeaderboardRow entry = entries.get(i);
         if (this.isLocalPlayer(entry, localUuid, localName)) {
            entries.set(i, this.localRow(entry.rank(), snapshot));
            return;
         }
      }

      if (this.viewMode == GlobalLeaderboardViewMode.AROUND_ME) {
         int nextRank = entries.stream().mapToInt(GlobalLeaderboardRow::rank).max().orElse(0) + 1;
         entries.add(this.localRow(nextRank, snapshot));
      }
   }

   private GlobalLeaderboardRow localRow(int rank, GlobalLeaderboardBoardSnapshot snapshot) {
      String localName = GlobalLeaderboardsFeature.playerName().isBlank() ? this.minecraft.getUser().getName() : GlobalLeaderboardsFeature.playerName();
      return GlobalLeaderboardRow.fromNumbers(rank, GlobalLeaderboardsFeature.playerUuid(), localName, snapshot.totalScore(), snapshot.statValues());
   }

   private Component boardTitle(GlobalLeaderboardBoard board) {
      return LegacyLeaderboards.LEGACY_PROVIDER.equals(board.providerId()) ? Component.translatable("legacy.menu.leaderboard.board_difficulty", board.displayName(), this.difficulty.displayName()) : board.displayName();
   }

   private boolean isLocalPlayer(GlobalLeaderboardRow entry, String localUuid, String localName) {
      if (!localUuid.isBlank() && entry.playerUuid().equals(localUuid)) {
         return true;
      }

      return entry.playerUuid().isBlank() && entry.playerName().equals(localName);
   }

   private List<SimpleLayoutRenderable> columnRenderables(GlobalLeaderboardBoard board) {
      return this.columnRenderables.computeIfAbsent(board.key(), key -> board.columns().stream().map(column -> column.icon().create(24, 24)).toList());
   }
}
