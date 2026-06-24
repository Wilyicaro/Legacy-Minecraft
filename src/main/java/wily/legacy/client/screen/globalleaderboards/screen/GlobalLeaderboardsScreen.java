package wily.legacy.client.screen.globalleaderboards.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4JClient;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardBoard;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardColumn;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardDifficulty;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardRow;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardViewMode;
import wily.legacy.api.client.leaderboards.LegacyLeaderboards;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.LeaderboardsScreen;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.client.screen.PanelVListScreen;
import wily.legacy.client.screen.globalleaderboards.GlobalLeaderboardsFeature;
import wily.legacy.client.screen.globalleaderboards.board.GlobalLeaderboardBoardRegistry;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardBoardSnapshot;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GlobalLeaderboardsScreen extends PanelVListScreen {
    private static final Component VIEW_AROUND_ME = Component.translatable("legacy.menu.leaderboard.view.around_me");
    private static final Component VIEW_TOP = Component.translatable("legacy.menu.leaderboard.view.top");
    private static final Component TOGGLE_VIEW = Component.translatable("legacy.menu.leaderboard.toggle_view");
    private final Map<String, List<SimpleLayoutRenderable>> columnRenderables = new HashMap<>();
    private int selectedStatBoard;
    private int statsInScreen;
    private int page;
    private int seenCacheVersion = -1;
    private int seenBoardsVersion = -1;
    private GlobalLeaderboardViewMode viewMode = GlobalLeaderboardViewMode.TOP;
    private GlobalLeaderboardDifficulty difficulty = GlobalLeaderboardDifficulty.NORMAL;
    private List<GlobalLeaderboardRow> rows = List.of();

    public GlobalLeaderboardsScreen(Screen parent) {
        super(parent, 568, 275, CommonComponents.EMPTY);
        GlobalLeaderboardsFeature.ensureStarted(Minecraft.getInstance());
        refreshBoardDefinitions();
        selectFirstNonEmptyBoard();
        rebuildRenderableVList(Minecraft.getInstance());
        renderableVList.layoutSpacing(layout -> 1);
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon(), () -> TOGGLE_VIEW);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_O) {
            cycleView();
            rebuildRenderableVList(minecraft);
            repositionElements();
            return true;
        }
        if (i == InputConstants.KEY_D || i == InputConstants.KEY_LBRACKET || i == InputConstants.KEY_RBRACKET) {
            cycleDifficulty(i == InputConstants.KEY_LBRACKET);
            rebuildRenderableVList(minecraft);
            repositionElements();
            return true;
        }
        if (i == InputConstants.KEY_LEFT || i == InputConstants.KEY_RIGHT) {
            changeStatBoard(i == InputConstants.KEY_LEFT);
            return true;
        }
        if (renderableVList.keyPressed(i)) {
            return true;
        }
        return super.keyPressed(i, j, k);
    }

    public void changeStatBoard(boolean left) {
        List<GlobalLeaderboardBoard> boards = GlobalLeaderboardsFeature.boards();
        if (boards.isEmpty()) {
            return;
        }
        int initialSelectedStatBoard = selectedStatBoard;
        while (selectedStatBoard != (selectedStatBoard = Stocker.cyclic(0, selectedStatBoard + (left ? -1 : 1), boards.size())) && selectedStatBoard != initialSelectedStatBoard) {
            if (!boards.get(selectedStatBoard).columns().isEmpty()) {
                if (!GlobalLeaderboardBoardRegistry.supportsDifficulty(boards.get(selectedStatBoard).id(), difficulty)) {
                    difficulty = GlobalLeaderboardDifficulty.EASY;
                }
                resetPageAndScroll();
                rebuildRenderableVList(minecraft);
                repositionElements();
                return;
            }
        }
    }

    public void rebuildRenderableVList(Minecraft minecraft) {
        renderableVList.renderables.clear();
        GlobalLeaderboardBoard board = selectedGlobalBoard();
        if (board == null || board.columns().isEmpty()) {
            rows = List.of();
            return;
        }

        rows = resolveRows(board);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            GlobalLeaderboardRow row = rows.get(rowIndex);
            String rank = Integer.toString(row.rank() > 0 ? row.rank() : rowIndex + 1);
            renderableVList.renderables.add(new AbstractWidget(0, 0, accessor.getInteger("renderableVList.buttonWidth", 551), accessor.getInteger("buttonsHeight", 20), Component.literal(row.playerName())) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
                    int y = getY() + (getHeight() - font.lineHeight) / 2 + 1;
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(isHoveredOrFocused() ? LegacySprites.LEADERBOARD_BUTTON_HIGHLIGHTED : LegacySprites.LEADERBOARD_BUTTON, getX(), getY(), getWidth(), getHeight());
                    guiGraphics.drawString(font, rank, getX() + accessor.getInteger("renderableVList.buttonRank.x", 40) - font.width(rank) / 2, y, ScreenUtil.getDefaultTextColor(!isHoveredOrFocused()));
                    guiGraphics.drawString(font, getMessage(), getX() + accessor.getInteger("renderableVList.buttonUsername.x", 120) - font.width(getMessage()) / 2, y, ScreenUtil.getDefaultTextColor(!isHoveredOrFocused()));

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
                        guiGraphics.drawString(font, value, renderable.getX() + (renderable.getWidth() - w) / 2, y, ScreenUtil.getDefaultTextColor(!isHoveredOrFocused()), true);
                        added++;
                    }
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
        refreshBoardDefinitions();
        int cacheVersion = GlobalLeaderboardsFeature.cacheVersion();
        int boardsVersion = GlobalLeaderboardsFeature.boards().hashCode();
        if (cacheVersion != seenCacheVersion || boardsVersion != seenBoardsVersion) {
            seenCacheVersion = cacheVersion;
            seenBoardsVersion = boardsVersion;
            rebuildRenderableVList(minecraft);
            repositionElements();
        }
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        addRenderableOnly((graphics, mouseX, mouseY, delta) -> {
            int topTooltipHeight = accessor.getInteger("topTooltip.height", 18);
            int topTooltipY = panel.y + accessor.getInteger("topTooltip.y", -topTooltipHeight);
            int filterTooltipWidth = accessor.getInteger("filterTooltip.width", 166);
            int filterTooltipX = panel.x + accessor.getInteger("filterTooltip.x", 8);
            int boardTooltipWidth = accessor.getInteger("boardTooltip.width", 211);
            int boardTooltipX = panel.x + accessor.getInteger("boardTooltip.x", (panel.width - boardTooltipWidth) / 2);
            int entriesTooltipWidth = accessor.getInteger("entriesTooltip.width", 166);
            int entriesTooltipX = panel.x + panel.width - entriesTooltipWidth + accessor.getInteger("entriesTooltip.x", -8);
            ScreenUtil.renderPointerPanel(graphics, filterTooltipX, topTooltipY, filterTooltipWidth, topTooltipHeight);
            ScreenUtil.renderPointerPanel(graphics, boardTooltipX, topTooltipY, boardTooltipWidth, topTooltipHeight);
            ScreenUtil.renderPointerPanel(graphics, entriesTooltipX, topTooltipY, entriesTooltipWidth, topTooltipHeight);

            GlobalLeaderboardBoard board = selectedGlobalBoard();
            if (board == null) {
                return;
            }

            Legacy4JClient.applyFontOverrideIf(ScreenUtil.is720p(), LegacyIconHolder.MOJANGLES_11_FONT, fontOverride -> {
                float topTooltipScale = accessor.getFloat("topTooltip.scale", ScreenUtil.is720p() ? 2 / 3f : 1.0f);
                int topTextColor = CommonColor.WHITE.get();
                graphics.pose().pushPose();
                Component filter = filterText();
                graphics.pose().translate(filterTooltipX + (filterTooltipWidth - font.width(filter) * topTooltipScale) / 2, topTooltipY + accessor.getInteger("filterText.y", 6), 0);
                if (!fontOverride) {
                    graphics.pose().scale(topTooltipScale, topTooltipScale, topTooltipScale);
                }
                graphics.drawString(font, filter, 0, 0, topTextColor, false);
                graphics.pose().popPose();

                graphics.pose().pushPose();
                Component boardTitle = boardTitle(board);
                graphics.pose().translate(boardTooltipX + (boardTooltipWidth - font.width(boardTitle) * topTooltipScale) / 2, topTooltipY + accessor.getInteger("boardText.y", 6), 0);
                if (!fontOverride) {
                    graphics.pose().scale(topTooltipScale, topTooltipScale, topTooltipScale);
                }
                graphics.drawString(font, boardTitle, 0, 0, topTextColor, false);
                graphics.pose().popPose();

                graphics.pose().pushPose();
                Component entries = Component.translatable("legacy.menu.leaderboard.entries", rows.size());
                graphics.pose().translate(entriesTooltipX + (entriesTooltipWidth - font.width(entries) * topTooltipScale) / 2, topTooltipY + accessor.getInteger("entriesText.y", 6), 0);
                if (!fontOverride) {
                    graphics.pose().scale(topTooltipScale, topTooltipScale, topTooltipScale);
                }
                graphics.drawString(font, entries, 0, 0, topTextColor, false);
                graphics.pose().popPose();
            });

            if (board.columns().isEmpty()) {
                renderNoResults(graphics);
                return;
            }

            graphics.drawString(font, LeaderboardsScreen.RANK, panel.x + accessor.getInteger("rankText.x", 40), panel.y + accessor.getInteger("rankText.y", 20), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            graphics.drawString(font, LeaderboardsScreen.USERNAME, panel.x + accessor.getInteger("usernameText.x", 108), panel.y + accessor.getInteger("usernameText.y", 20), CommonColor.INVENTORY_GRAY_TEXT.get(), false);

            int statsBoardX = accessor.getInteger("statsBoard.x", 182);
            int statsBoardY = accessor.getInteger("statsBoard.y", 22);
            int totalStatsWidth = accessor.getInteger("statsBoard.width", 351);
            List<SimpleLayoutRenderable> renderables = columnRenderables(board);
            int totalWidth = 0;
            statsInScreen = 0;
            for (int index = page; index < renderables.size(); index++) {
                int newWidth = totalWidth + renderables.get(index).getWidth();
                if (newWidth > totalStatsWidth) {
                    break;
                }
                statsInScreen++;
                totalWidth = newWidth;
            }

            FactoryScreenUtil.enableBlend();
            graphics.pose().pushPose();
            graphics.pose().translate(boardTooltipX + accessor.getInteger("boardControlTooltip.x", 2), topTooltipY + accessor.getInteger("boardControlTooltip.y", 6), 0);
            graphics.pose().scale(ScreenUtil.is720p() ? 1.0f : 0.5f, ScreenUtil.is720p() ? 1.0f : 0.5f, 1.0f);
            (ControlType.getActiveType().isKbm() ? ControlTooltip.ComponentIcon.compoundOf(ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)) : ControllerBinding.LEFT_STICK.getIcon()).render(graphics, 4, 0, false, false);
            graphics.pose().popPose();

            ControlTooltip.Icon difficultyControl = ControlTooltip.ComponentIcon.compoundOf(ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.getIcon(), ControlTooltip.SPACE_ICON, ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.getIcon());
            graphics.pose().pushPose();
            graphics.pose().translate(boardTooltipX + boardTooltipWidth + accessor.getInteger("boardPageTooltip.x", -4), topTooltipY + accessor.getInteger("boardControlTooltip.y", 6), 0);
            if (!ScreenUtil.is720p()) {
                graphics.pose().scale(0.5f, 0.5f, 0.5f);
            }
            difficultyControl.render(graphics, -difficultyControl.render(graphics, 0, 0, false, true), 0, false, false);
            graphics.pose().popPose();
            FactoryScreenUtil.disableBlend();

            if (statsInScreen == 0) {
                return;
            }

            int x = (totalStatsWidth - totalWidth) / (statsInScreen + 1);
            Integer hovered = null;
            for (int index = page; index < page + statsInScreen; index++) {
                SimpleLayoutRenderable renderable = renderables.get(index);
                renderable.setPosition(panel.x + statsBoardX + x, panel.y + statsBoardY - renderable.height / 2);
                renderable.render(graphics, mouseX, mouseY, delta);
                if (renderable.isHovered(mouseX, mouseY)) {
                    hovered = index;
                }
                x += renderable.getWidth() + (totalStatsWidth - totalWidth) / statsInScreen;
            }
            if (hovered != null) {
                graphics.renderTooltip(font, board.columns().get(hovered).displayName(), mouseX, mouseY);
            }
        });
    }

    @Override
    public void renderableVListInit() {
        renderableVList.init(panel.x + 9, panel.y + 39, 551, 226);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.EMPTY;
    }

    private void refreshBoardDefinitions() {
        GlobalLeaderboardsFeature.refreshBoards(minecraft);
    }

    private void selectFirstNonEmptyBoard() {
        List<GlobalLeaderboardBoard> boards = GlobalLeaderboardsFeature.boards();
        for (int i = 0; i < boards.size(); i++) {
            GlobalLeaderboardBoard board = boards.get(i);
            if (GlobalLeaderboardBoardRegistry.defaultBoardId().equals(board.id()) && !board.columns().isEmpty()) {
                selectedStatBoard = i;
                return;
            }
        }

        for (int i = 0; i < boards.size(); i++) {
            if (!boards.get(i).columns().isEmpty()) {
                selectedStatBoard = i;
                return;
            }
        }
        selectedStatBoard = 0;
    }

    private void cycleView() {
        viewMode = viewMode == GlobalLeaderboardViewMode.AROUND_ME ? GlobalLeaderboardViewMode.TOP : GlobalLeaderboardViewMode.AROUND_ME;
        resetPageAndScroll();
    }

    private void cycleDifficulty(boolean left) {
        GlobalLeaderboardDifficulty[] values = GlobalLeaderboardDifficulty.values();
        GlobalLeaderboardBoard board = selectedGlobalBoard();
        int index = difficulty.ordinal();
        int initial = index;
        do {
            index = Stocker.cyclic(0, index + (left ? -1 : 1), values.length);
            difficulty = values[index];
        } while (board != null && !GlobalLeaderboardBoardRegistry.supportsDifficulty(board.id(), difficulty) && index != initial);
        resetPageAndScroll();
    }

    private void resetPageAndScroll() {
        page = 0;
        renderableVList.resetScroll();
    }

    private Component filterText() {
        return Component.translatable("legacy.menu.leaderboard.filter", viewMode == GlobalLeaderboardViewMode.AROUND_ME ? VIEW_AROUND_ME : VIEW_TOP);
    }

    private GlobalLeaderboardBoard selectedGlobalBoard() {
        return GlobalLeaderboardsFeature.board(selectedStatBoard);
    }

    private List<GlobalLeaderboardRow> resolveRows(GlobalLeaderboardBoard board) {
        List<GlobalLeaderboardRow> cachedEntries = GlobalLeaderboardsFeature.entries(board, viewMode, difficulty);
        if (!cachedEntries.isEmpty()) {
            requestBoard(board);
            List<GlobalLeaderboardRow> entries = new ArrayList<>(cachedEntries);
            applyLocalPlayerRow(board, entries);
            entries.sort(Comparator.comparingInt(GlobalLeaderboardRow::rank));
            return entries;
        }

        requestBoard(board);
        GlobalLeaderboardBoardSnapshot snapshot = GlobalLeaderboardsFeature.boardSnapshot(board, difficulty);
        if (snapshot == null || snapshot.totalScore() <= 0) {
            return List.of();
        }

        return List.of(localRow(1, snapshot));
    }

    private void requestBoard(GlobalLeaderboardBoard board) {
        GlobalLeaderboardsFeature.requestBoard(board, viewMode, difficulty);
    }

    private void applyLocalPlayerRow(GlobalLeaderboardBoard board, List<GlobalLeaderboardRow> entries) {
        GlobalLeaderboardBoardSnapshot snapshot = GlobalLeaderboardsFeature.boardSnapshot(board, difficulty);
        if (snapshot == null || snapshot.totalScore() <= 0) {
            return;
        }

        String localName = GlobalLeaderboardsFeature.playerName().isBlank() ? minecraft.getUser().getName() : GlobalLeaderboardsFeature.playerName();
        String localUuid = GlobalLeaderboardsFeature.playerUuid();
        for (int i = 0; i < entries.size(); i++) {
            GlobalLeaderboardRow entry = entries.get(i);
            if (isLocalPlayer(entry, localUuid, localName)) {
                entries.set(i, localRow(entry.rank(), snapshot));
                return;
            }
        }

        if (viewMode == GlobalLeaderboardViewMode.AROUND_ME) {
            int nextRank = entries.stream().mapToInt(GlobalLeaderboardRow::rank).max().orElse(0) + 1;
            entries.add(localRow(nextRank, snapshot));
        }
    }

    private GlobalLeaderboardRow localRow(int rank, GlobalLeaderboardBoardSnapshot snapshot) {
        String localName = GlobalLeaderboardsFeature.playerName().isBlank() ? minecraft.getUser().getName() : GlobalLeaderboardsFeature.playerName();
        return GlobalLeaderboardRow.fromNumbers(rank, GlobalLeaderboardsFeature.playerUuid(), localName, snapshot.totalScore(), snapshot.statValues());
    }

    private Component boardTitle(GlobalLeaderboardBoard board) {
        return LegacyLeaderboards.LEGACY_PROVIDER.equals(board.providerId()) ? Component.translatable("legacy.menu.leaderboard.board_difficulty", board.displayName(), difficulty.displayName()) : board.displayName();
    }

    private boolean isLocalPlayer(GlobalLeaderboardRow entry, String localUuid, String localName) {
        if (!localUuid.isBlank() && entry.playerUuid().equals(localUuid)) {
            return true;
        }
        return entry.playerUuid().isBlank() && entry.playerName().equals(localName);
    }

    private List<SimpleLayoutRenderable> columnRenderables(GlobalLeaderboardBoard board) {
        return columnRenderables.computeIfAbsent(board.key(), key -> board.columns().stream().map(column -> column.icon().create(24, 24)).toList());
    }

    private void renderNoResults(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(panel.x + (panel.width - font.width(LeaderboardsScreen.NO_RESULTS) * 1.5f) / 2f, panel.y + (panel.height - 13.5f) / 2f, 0);
        graphics.pose().scale(1.5f, 1.5f, 1.5f);
        graphics.drawString(font, LeaderboardsScreen.NO_RESULTS, 0, 0, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        graphics.pose().popPose();
    }
}
