package wily.legacy.Skins.client.screen.changeskin;

import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.client.compat.ExternalSkinProviders;
import wily.legacy.Skins.skin.SkinIds;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.Skins.skin.SkinPackSourceKind;
import wily.legacy.client.LegacyOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.client.ControlType;
import wily.legacy.client.screen.RenderableVList;

public final class ChangeSkinPackList {
    private static final String FESTIVE_MASHUP_PACK_ID = "festivemashup";
    private static final String FESTIVE_PACK_ID = "festive";

    private final Runnable clickSound;
    private final FocusChange onFocusChange;

    private final List<String> basePackIds = new ArrayList<>();

    private final List<String> packIds = new ArrayList<>();
    private final List<PackButton> packButtons = new ArrayList<>();
    private SkinPackViewFilter viewFilter = SkinPackViewFilter.DEFAULT;

    private int buttonHeight = 20;
    private int focusedPackIndex;
    private boolean queuedChangePack;

    public interface FocusChange {
        void onFocusedPackChanged();
    }

    public ChangeSkinPackList(Runnable clickSound, FocusChange onFocusChange) {
        this.clickSound = clickSound;
        this.onFocusChange = onFocusChange;
        this.viewFilter = resolveInitialViewFilter();
        syncExternalSelectionPreference();
    }

    public void initFromLoader() {
        SkinPackLoader.ensureLoaded();
        syncExternalSelectionPreference();

        refreshPackIds();
        String preferred = resolvePreferredDefaultPackId(viewFilter, packIds);
        int prefIdx = preferred == null ? -1 : packIds.indexOf(preferred);
        focusedPackIndex = prefIdx >= 0 ? prefIdx : 0;
        queuedChangePack = false;
        ensureButtons();
    }

    public void applyUiScale(float uiScale) {
        int h = LegacyOptions.getUIMode().isSD() ? 18 : Math.max(1, Math.round(20f * uiScale));
        applyResolvedButtonHeight(h);
    }

    public void applyResolvedButtonHeight(int resolvedHeight) {
        buttonHeight = Math.max(10, resolvedHeight);
        for (PackButton b : packButtons) b.applyHeight(buttonHeight);
    }

    public int getButtonHeight() {
        return buttonHeight;
    }

    public void refreshPackIdsIfNeeded() {
        SkinPackLoader.ensureLoaded();
        List<String> currentBase = collectPackIds(viewFilter);

        if (currentBase.equals(basePackIds)) {
            refreshButtonLabels();
            return;
        }

        String focusedId = getFocusedPackId();

        basePackIds.clear();
        basePackIds.addAll(currentBase);

        rebuildDisplayOrder(focusedId);
        queuedChangePack = true;
        ensureButtons();
    }

    public void bumpMostRecentCustomPack(String packId) {
        SkinPackLoader.setLastUsedCustomPackId(packId);
    }

    public void populateInto(RenderableVList vList) {
        vList.renderables.clear();
        if (packButtons.isEmpty()) ensureButtons();
        for (PackButton b : packButtons) vList.addRenderable(b);
    }

    public boolean consumeQueuedChangePack() {
        boolean q = queuedChangePack;
        queuedChangePack = false;
        return q;
    }

    public int getFocusedPackIndex() {
        return focusedPackIndex;
    }

    public int getPackCount() {
        return packIds.size();
    }

    public PackButton getButtonForIndex(int idx) {
        if (idx < 0 || idx >= packButtons.size()) return null;
        return packButtons.get(idx);
    }

    private int wrapIndex(int idx) {
        int n = packIds.size();
        if (n <= 0) return 0;
        int r = idx % n;
        if (r < 0) r += n;
        return r;
    }

    private String getFocusedPackId() {
        if (packIds.isEmpty()) return null;
        int idx = wrapIndex(focusedPackIndex);
        return packIds.get(idx);
    }

    public SkinPack getFocusedPack() {
        String id = getFocusedPackId();
        return id == null ? null : resolvePack(id);
    }

    public List<SkinPack> getPacks() {
        if (packIds.isEmpty()) return List.of();
        List<SkinPack> out = new ArrayList<>(packIds.size());
        for (String id : packIds) {
            SkinPack p = resolvePack(id);
            if (p != null) out.add(p);
        }
        return out;
    }

    public ResourceLocation getFocusedPackIcon() {
        SkinPack pack = getFocusedPack();
        return pack == null ? null : pack.icon();
    }

    public void setFocusedPackIndex(int idx, boolean playSound) {
        if (packIds.isEmpty()) {
            focusedPackIndex = 0;
            return;
        }
        int wrapped = wrapIndex(idx);
        if (wrapped == focusedPackIndex) return;
        focusedPackIndex = wrapped;
        queuedChangePack = true;
        if (playSound) clickSound.run();
        onFocusChange.onFocusedPackChanged();
    }

    public void focusPackId(String packId, boolean playSound) {
        if (packId == null || packIds.isEmpty()) return;
        int idx = packIds.indexOf(packId);
        if (idx >= 0) setFocusedPackIndex(idx, playSound);
    }

    public SkinPackViewFilter getViewFilter() {
        return viewFilter;
    }

    public boolean cycleViewFilter() {
        SkinPackViewFilter original = viewFilter;
        SkinPackViewFilter next = viewFilter;
        for (int i = 0; i < SkinPackViewFilter.values().length; i++) {
            next = next.next();
            if (hasAnyPacksForFilter(next)) {
                setViewFilter(next);
                return original != viewFilter;
            }
        }
        return false;
    }

    public void setViewFilter(SkinPackViewFilter nextFilter) {
        SkinPackViewFilter normalized = nextFilter == null ? SkinPackViewFilter.DEFAULT : nextFilter;
        if (!normalized.isAvailable()) normalized = SkinPackViewFilter.DEFAULT;
        if (normalized == viewFilter) return;

        viewFilter = normalized;
        syncExternalSelectionPreference();
        ConsoleSkinsClientSettings.setLastSkinPackViewFilter(viewFilter);
        basePackIds.clear();
        basePackIds.addAll(collectPackIds(viewFilter));
        rebuildDisplayOrder(null);
        focusedPackIndex = 0;
        queuedChangePack = true;
        ensureButtons();
    }

    private void refreshPackIds() {
        if (!viewFilter.isAvailable()) viewFilter = SkinPackViewFilter.DEFAULT;
        syncExternalSelectionPreference();
        basePackIds.clear();
        basePackIds.addAll(collectPackIds(viewFilter));
        rebuildDisplayOrder(null);
        if (focusedPackIndex >= packIds.size()) focusedPackIndex = 0;
    }

    private void syncExternalSelectionPreference() {
        ExternalSkinProviders.setPreferredSelectionSource(switch (viewFilter) {
            case LEGACY_SKINS -> SkinPackSourceKind.LEGACY_SKINS;
            case BEDROCK_SKINS -> SkinPackSourceKind.BEDROCK_SKINS;
            default -> null;
        });
    }

    private void rebuildDisplayOrder(String preserveFocusedId) {
        packIds.clear();
        packIds.addAll(basePackIds);
        normalizeSpecialPackOrder(viewFilter, packIds);

        String bump = SkinPackLoader.getLastUsedCustomPackId();
        String pinnedDefaultPackId = resolvePreferredDefaultPackId(viewFilter, packIds);
        if (bump != null && packIds.contains(bump) && !bump.equals(pinnedDefaultPackId)) {
            packIds.remove(bump);

            int insertAt = 1;
            int favIdx = packIds.indexOf(SkinIds.PACK_FAVOURITES);
            if (favIdx >= 0) insertAt = favIdx + 1;

            if (insertAt < 0) insertAt = 0;
            if (insertAt > packIds.size()) insertAt = packIds.size();
            packIds.add(insertAt, bump);
        }

        if (preserveFocusedId != null) {
            int idx = packIds.indexOf(preserveFocusedId);
            focusedPackIndex = idx >= 0 ? idx : 0;
        } else {
            if (focusedPackIndex >= packIds.size()) focusedPackIndex = 0;
        }
    }

    private void ensureButtons() {
        packButtons.clear();
        if (packIds.isEmpty()) {
            packButtons.add(new PackButton(-1, Component.translatable("consoleskins.pack.none")));
            return;
        }
        for (int i = 0; i < packIds.size(); i++) {
            packButtons.add(new PackButton(i, labelForPackIndex(i)));
        }
    }

    private void refreshButtonLabels() {
        if (packIds.isEmpty()) {
            if (packButtons.isEmpty()) ensureButtons();
            return;
        }
        if (packButtons.size() != packIds.size()) {
            ensureButtons();
            return;
        }
        for (int i = 0; i < packButtons.size(); i++) {
            packButtons.get(i).packIndex = i;
            packButtons.get(i).setMessage(labelForPackIndex(i));
        }
    }

    private Component labelForPackIndex(int i) {
        if (i < 0 || i >= packIds.size()) return Component.empty();
        String id = packIds.get(i);
        SkinPack pack = resolvePack(id);
        if (pack == null) return Component.literal(id);
        return SkinPackLoader.nameComponent(pack.name(), id);
    }

    private SkinPack resolvePack(String packId) {
        if (packId == null || packId.isBlank()) return null;
        Map<String, SkinPack> source = packMapForFilter(viewFilter);
        SkinPack direct = source.get(packId);
        return direct != null ? direct : SkinPackLoader.getAllPacks().get(packId);
    }

    private List<String> collectPackIds(SkinPackViewFilter filter) {
        if (filter != null && !filter.isAvailable()) return List.of();
        Map<String, SkinPack> source = packMapForFilter(filter);
        ArrayList<String> ids = new ArrayList<>(source.size());
        for (String packId : source.keySet()) {
            if (packId == null || packId.isBlank()) continue;
            if (includesPack(filter, packId)) ids.add(packId);
        }
        if (filter != null && filter.usesNativeSourceOrder()) {
            ids.sort((left, right) -> {
                int leftOrder = SkinPackLoader.getPackSourceOrder(left);
                int rightOrder = SkinPackLoader.getPackSourceOrder(right);
                if (leftOrder != rightOrder) return Integer.compare(leftOrder, rightOrder);
                return left.compareToIgnoreCase(right);
            });
        }
        restoreCuratedFestivePack(ids, filter);
        normalizeSpecialPackOrder(filter, ids);
        return ids;
    }

    private SkinPackViewFilter resolveInitialViewFilter() {
        SkinPackViewFilter saved = ConsoleSkinsClientSettings.getLastSkinPackViewFilter();
        return saved.isAvailable() ? saved : SkinPackViewFilter.DEFAULT;
    }

    private void normalizeSpecialPackOrder(SkinPackViewFilter filter, List<String> ids) {
        if (ids == null) return;
        String preferredDefaultPackId = resolvePreferredDefaultPackId(filter, ids);
        ids.removeIf(SkinIds.PACK_FAVOURITES::equals);
        ids.removeIf(SkinIds.PACK_DEFAULT::equals);
        if (preferredDefaultPackId != null && !SkinIds.PACK_DEFAULT.equals(preferredDefaultPackId)) {
            ids.removeIf(preferredDefaultPackId::equals);
        }

        int insertAt = 0;
        if (preferredDefaultPackId != null && SkinPackLoader.getAllPacks().containsKey(preferredDefaultPackId)) {
            ids.add(insertAt++, preferredDefaultPackId);
        }
        if (SkinPackLoader.getAllPacks().containsKey(SkinIds.PACK_FAVOURITES)) {
            ids.add(insertAt, SkinIds.PACK_FAVOURITES);
        }
    }

    private String resolvePreferredDefaultPackId(SkinPackViewFilter filter, List<String> ids) {
        if (filter == SkinPackViewFilter.LEGACY_SKINS) {
            return firstPackForSource(ids, SkinPackSourceKind.LEGACY_SKINS);
        }
        if (filter == SkinPackViewFilter.BEDROCK_SKINS) {
            return firstPackForSource(ids, SkinPackSourceKind.BEDROCK_SKINS);
        }
        if (filter == SkinPackViewFilter.BOX_MODELS) {
            if (SkinPackLoader.getAllPacks().containsKey(SkinIds.PACK_DEFAULT)) return SkinIds.PACK_DEFAULT;
            return firstPackForSource(ids, SkinPackSourceKind.BOX_MODEL);
        }

        String preferred = SkinPackLoader.getPreferredDefaultPackId();
        if (preferred != null && ids != null && ids.contains(preferred)) return preferred;
        return firstNonSpecialPackId(ids);
    }

    private String firstPackForSource(List<String> ids, SkinPackSourceKind sourceKind) {
        if (ids == null || sourceKind == null) return null;
        for (String packId : ids) {
            if (packId == null || packId.isBlank()) continue;
            if (SkinIds.PACK_DEFAULT.equals(packId) || SkinIds.PACK_FAVOURITES.equals(packId)) continue;
            if (SkinPackLoader.getPackSourceKind(packId) == sourceKind) return packId;
        }
        return null;
    }

    private String firstNonSpecialPackId(List<String> ids) {
        if (ids == null) return null;
        for (String packId : ids) {
            if (packId == null || packId.isBlank()) continue;
            if (SkinIds.PACK_FAVOURITES.equals(packId)) continue;
            return packId;
        }
        return null;
    }

    private Map<String, SkinPack> packMapForFilter(SkinPackViewFilter filter) {
        SkinPackViewFilter normalized = filter == null ? SkinPackViewFilter.DEFAULT : filter;
        return normalized.usesAllPacks() ? SkinPackLoader.getAllPacks() : SkinPackLoader.getPacks();
    }

    private boolean includesPack(SkinPackViewFilter filter, String packId) {
        if (filter == null || !filter.isAvailable()) return false;
        if (SkinIds.PACK_FAVOURITES.equals(packId)) return true;
        if (SkinIds.PACK_DEFAULT.equals(packId)) {
            return filter == SkinPackViewFilter.DEFAULT
                    || filter == SkinPackViewFilter.ALL_PACKS
                    || filter == SkinPackViewFilter.BOX_MODELS;
        }
        SkinPackSourceKind sourceKind = SkinPackLoader.getPackSourceKind(packId);
        return filter.includesSource(sourceKind);
    }

    private void restoreCuratedFestivePack(List<String> ids, SkinPackViewFilter filter) {
        if (filter != SkinPackViewFilter.DEFAULT || ids == null || ids.contains(FESTIVE_PACK_ID)) return;
        if (!SkinPackLoader.getAllPacks().containsKey(FESTIVE_PACK_ID)) return;

        int mashupIndex = ids.indexOf(FESTIVE_MASHUP_PACK_ID);
        if (mashupIndex < 0) {
            ids.add(FESTIVE_PACK_ID);
            return;
        }
        ids.add(mashupIndex + 1, FESTIVE_PACK_ID);
    }

    private boolean hasAnyPacksForFilter(SkinPackViewFilter filter) {
        return !collectPackIds(filter).isEmpty();
    }

    public final class PackButton extends Button {
        private int packIndex;

        private PackButton(int packIndex, Component msg) {
            super(
                    0,
                    0,
                    0,
                    buttonHeight,
                    msg,
                    b -> {
                        if (!(b instanceof PackButton pb)) return;
                        if (pb.packIndex >= 0) setFocusedPackIndex(pb.packIndex, true);
                    },
                    DEFAULT_NARRATION
            );
            this.packIndex = packIndex;
            this.active = packIndex >= 0;
        }

        private void applyHeight(int h) {
            this.height = h;
        }

        public int getPackIndex() {
            return packIndex;
        }

        @Override
        public void setFocused(boolean focused) {
            boolean wasFocused = this.isFocused();
            super.setFocused(focused);
            if (!wasFocused && focused && packIndex >= 0 && focusedPackIndex != packIndex) {
                setFocusedPackIndex(packIndex, false);
            }
        }

        @Override
        public boolean isHoveredOrFocused() {
            return (packIndex >= 0 && packIndex == focusedPackIndex) || super.isHoveredOrFocused();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            if (!ControlType.getActiveType().isKbm() && this.isFocused() && packIndex >= 0 && focusedPackIndex != packIndex) {
                setFocusedPackIndex(packIndex, false);
            }
        }

        @Override
        public void renderString(GuiGraphics graphics, net.minecraft.client.gui.Font font, int color) {
            Component message = getMessage();
            if (message == null) return;

            int maxTextWidth = Math.max(0, getWidth() - TEXT_MARGIN * 2);
            String visibleText = message.getString();
            if (maxTextWidth > 0 && font.width(visibleText) > maxTextWidth) {
                int dotsWidth = font.width("...");
                int clippedWidth = Math.max(0, maxTextWidth - dotsWidth);
                visibleText = font.plainSubstrByWidth(visibleText, clippedWidth) + "...";
            }

            float textScale = buttonHeight < 20 && LegacyOptions.getUIMode().isSD() ? 0.92f : 1.0f;
            int centerX = getX() + getWidth() / 2;
            float scaledHeight = font.lineHeight * textScale;
            float textY = getY() + (getHeight() - scaledHeight) / 2.0f;
            if (textScale == 1.0f) {
                graphics.drawCenteredString(font, visibleText, centerX, Math.round(textY), color);
                return;
            }

            graphics.pose().pushMatrix();
            graphics.pose().translate(centerX, textY);
            graphics.pose().scale(textScale, textScale);
            graphics.drawCenteredString(font, visibleText, 0, 0, color);
            graphics.pose().popMatrix();
        }
    }
}
