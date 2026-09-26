package wily.legacy.api.client.leaderboards;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.screens.Screen;
import wily.legacy.client.screen.LeaderboardsScreen;
import wily.legacy.client.screen.globalleaderboards.GlobalLeaderboardsFeature;

public final class LegacyLeaderboards {
   public static final String LEGACY_PROVIDER = "legacy";
   private static final Object LOCK = new Object();
   private static final Map<String, GlobalLeaderboardProvider> PROVIDERS = new LinkedHashMap<>();
   private static final Map<String, GlobalLeaderboardBoard> BOARDS = new LinkedHashMap<>();
   private static final AtomicInteger VERSION = new AtomicInteger();

   private LegacyLeaderboards() {
   }

   public static void registerProvider(GlobalLeaderboardProvider provider) {
      Objects.requireNonNull(provider, "provider");
      String id = provider.id();
      if (id == null || id.isBlank()) {
         throw new IllegalArgumentException("Leaderboard provider id cannot be empty");
      }
      synchronized (LOCK) {
         GlobalLeaderboardProvider previous = PROVIDERS.put(id.trim(), provider);
         if (previous != provider) {
            VERSION.incrementAndGet();
         }
      }
   }

   public static void registerBoard(GlobalLeaderboardBoard board) {
      Objects.requireNonNull(board, "board");
      synchronized (LOCK) {
         GlobalLeaderboardBoard previous = BOARDS.put(board.key(), board);
         if (!sameBoard(previous, board)) {
            VERSION.incrementAndGet();
         }
      }
   }

   public static List<GlobalLeaderboardBoard> boards() {
      synchronized (LOCK) {
         ArrayList<GlobalLeaderboardBoard> boards = new ArrayList<>(BOARDS.values());
         boards.sort(Comparator.comparingInt(GlobalLeaderboardBoard::order).thenComparing(GlobalLeaderboardBoard::providerId).thenComparing(GlobalLeaderboardBoard::id));
         return List.copyOf(boards);
      }
   }

   public static GlobalLeaderboardProvider provider(String providerId) {
      synchronized (LOCK) {
         return PROVIDERS.get(providerId);
      }
   }

   public static GlobalLeaderboardBoard board(String providerId, String boardId) {
      synchronized (LOCK) {
         return BOARDS.get(GlobalLeaderboardBoard.key(providerId, boardId));
      }
   }

   public static Screen open(Screen parent) {
      return GlobalLeaderboardsFeature.createScreen(parent, () -> new LeaderboardsScreen(parent));
   }

   public static int version() {
      return VERSION.get();
   }

   private static boolean sameBoard(GlobalLeaderboardBoard left, GlobalLeaderboardBoard right) {
      if (left == right) {
         return true;
      }
      if (left == null || right == null) {
         return false;
      }
      if (!left.providerId().equals(right.providerId()) || !left.id().equals(right.id()) || left.order() != right.order() || !left.displayName().equals(right.displayName())) {
         return false;
      }
      if (left.columns().size() != right.columns().size()) {
         return false;
      }
      for (int i = 0; i < left.columns().size(); i++) {
         GlobalLeaderboardColumn a = left.columns().get(i);
         GlobalLeaderboardColumn b = right.columns().get(i);
         if (!a.id().equals(b.id()) || !a.displayName().equals(b.displayName())) {
            return false;
         }
      }
      return true;
   }
}
