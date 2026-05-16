package wily.legacy.client.screen.globalleaderboards.screen;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stat;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.LeaderboardsScreen;
import wily.legacy.client.screen.globalleaderboards.GlobalLeaderboardsFeature;
import wily.legacy.client.screen.globalleaderboards.board.GlobalLeaderboardBoardRegistry;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardBoardSnapshot;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardEntry;
import wily.legacy.client.screen.globalleaderboards.storage.GlobalLeaderboardStatCodec;

public final class GlobalLeaderboardsScreen extends LeaderboardsScreen {
   private static final Component VIEW_AROUND_ME = Component.translatable("legacy.menu.leaderboard.view.around_me");
   private static final Component VIEW_TOP = Component.translatable("legacy.menu.leaderboard.view.top");
   private static final Component TOGGLE_VIEW = Component.translatable("legacy.menu.leaderboard.toggle_view");
   private int seenCacheVersion = -1;
   private ViewMode viewMode = ViewMode.AROUND_ME;

   public GlobalLeaderboardsScreen(Screen parent) {
      super(parent, false);
      GlobalLeaderboardsFeature.ensureStarted(Minecraft.getInstance());
      this.refreshBoardDefinitions();
      this.selectFirstNonEmptyBoard();
      this.rebuildRenderableVList(Minecraft.getInstance());
      this.renderableVList.layoutSpacing(layout -> 1);
   }

   @Override
   protected List<LeaderboardsScreen.StatsBoard> statsBoards() {
      return GlobalLeaderboardBoardRegistry.statsBoards();
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
      this.viewMode = this.viewMode == ViewMode.AROUND_ME ? ViewMode.TOP : ViewMode.AROUND_ME;
      this.page = 0;
   }

   @Override
   protected Component filterText() {
      return this.viewMode.label();
   }

   @Override
   protected void selectFirstNonEmptyBoard() {
      int defaultBoard = GlobalLeaderboardBoardRegistry.defaultBoardIndex();
      List<LeaderboardsScreen.StatsBoard> boards = this.statsBoards();
      if (defaultBoard >= 0 && defaultBoard < boards.size() && !boards.get(defaultBoard).statsList.isEmpty()) {
         this.selectedStatBoard = defaultBoard;
         return;
      }

      super.selectFirstNonEmptyBoard();
   }

   @Override
   protected List<LeaderboardsScreen.LeaderboardEntry> resolveRankBoard(Minecraft minecraft) {
      String boardId = this.boardId();
      List<GlobalLeaderboardEntry> cachedEntries = GlobalLeaderboardsFeature.entries(boardId, this.viewMode);
      if (!cachedEntries.isEmpty()) {
         this.requestBoard(boardId);
         List<LeaderboardsScreen.LeaderboardEntry> entries = new ArrayList<>();
         cachedEntries.forEach(entry -> entries.add(this.displayedEntry(entry.rank(), entry.playerUuid(), entry.playerName(), GlobalLeaderboardStatCodec.decodeMap(entry.statValues()))));
         this.applyLocalPlayerRow(boardId, entries);
         entries.sort(Comparator.comparingInt(LeaderboardsScreen.LeaderboardEntry::rank));
         return entries;
      }

      this.requestBoard(boardId);
      GlobalLeaderboardBoardSnapshot snapshot = GlobalLeaderboardsFeature.boardSnapshot(boardId);
      if (snapshot == null || snapshot.totalScore() <= 0) {
         return List.of();
      }

      return List.of(this.localEntry(1, snapshot));
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
   protected boolean refreshesOnlineStats() {
      return false;
   }

   private void refreshBoardDefinitions() {
      GlobalLeaderboardBoardRegistry.ensureStatsBoards(this.minecraft);
      GlobalLeaderboardBoardRegistry.ensureTrackedBoardStats(this.minecraft);
   }

   private String boardId() {
      return GlobalLeaderboardsFeature.boardId(this.selectedStatBoard);
   }

   private void requestBoard(String boardId) {
      GlobalLeaderboardsFeature.requestBoard(boardId, this.viewMode);
   }

   private void applyLocalPlayerRow(String boardId, List<LeaderboardsScreen.LeaderboardEntry> entries) {
      GlobalLeaderboardBoardSnapshot snapshot = GlobalLeaderboardsFeature.boardSnapshot(boardId);
      if (snapshot == null || snapshot.totalScore() <= 0) {
         return;
      }

      String localName = GlobalLeaderboardsFeature.playerName().isBlank() ? this.minecraft.getUser().getName() : GlobalLeaderboardsFeature.playerName();
      String localUuid = GlobalLeaderboardsFeature.playerUuid();
      for (int i = 0; i < entries.size(); i++) {
         LeaderboardsScreen.LeaderboardEntry entry = entries.get(i);
         if (this.isLocalPlayer(entry, localUuid, localName)) {
            entries.set(i, this.localEntry(entry.rank(), snapshot));
            return;
         }
      }

      if (this.viewMode == ViewMode.AROUND_ME) {
         int nextRank = entries.stream().mapToInt(LeaderboardsScreen.LeaderboardEntry::rank).max().orElse(0) + 1;
         entries.add(this.localEntry(nextRank, snapshot));
      }
   }

   private LeaderboardsScreen.LeaderboardEntry localEntry(int rank, GlobalLeaderboardBoardSnapshot snapshot) {
      String localName = GlobalLeaderboardsFeature.playerName().isBlank() ? this.minecraft.getUser().getName() : GlobalLeaderboardsFeature.playerName();
      return this.displayedEntry(rank, GlobalLeaderboardsFeature.playerUuid(), localName, GlobalLeaderboardStatCodec.decodeMap(snapshot.statValues()));
   }

   private LeaderboardsScreen.LeaderboardEntry displayedEntry(int rank, String uuid, String name, Object2IntOpenHashMap<Stat<?>> stats) {
      return new LeaderboardsScreen.LeaderboardEntry(rank, uuid, name, stats);
   }

   private boolean isLocalPlayer(LeaderboardsScreen.LeaderboardEntry entry, String localUuid, String localName) {
      if (!localUuid.isBlank() && entry.uuid().equals(localUuid)) {
         return true;
      }

      return entry.uuid().isBlank() && entry.name().equals(localName);
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
