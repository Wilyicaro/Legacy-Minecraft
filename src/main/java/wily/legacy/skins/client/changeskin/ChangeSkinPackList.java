package wily.legacy.Skins.client.changeskin;

import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.client.LegacyOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import wily.legacy.client.ControlType;
import wily.legacy.client.screen.RenderableVList;

public final class ChangeSkinPackList {
    private static final String FESTIVE_MASHUP_PACK_ID = "festivemashup";
    private static final String FESTIVE_PACK_ID = "festive";

    private final Runnable clickSound;

    private final List<String> basePackIds = new ArrayList<>();

    private final List<String> packIds = new ArrayList<>();
    private final List<PackButton> packButtons = new ArrayList<>();

    private int buttonHeight = 20;
    private int focusedPackIndex;
    private boolean queuedChangePack;

    public ChangeSkinPackList(Runnable clickSound) { this.clickSound = clickSound; }

    public void initFromLoader() {
        SkinPackLoader.ensureLoaded();
        setBasePackIds(collectPackIds(), null);
        String preferred = resolvePreferredDefaultPackId(packIds);
        int preferredIndex = preferred == null ? -1 : packIds.indexOf(preferred);
        focusedPackIndex = preferredIndex >= 0 ? preferredIndex : 0;
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

    public void refreshPackIdsIfNeeded() {
        SkinPackLoader.ensureLoaded();
        List<String> currentBase = collectPackIds();

        if (currentBase.equals(basePackIds)) {
            refreshButtonLabels();
            return;
        }

        setBasePackIds(currentBase, getFocusedPackId());
        queuedChangePack = true;
        ensureButtons();
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

    public int getFocusedPackIndex() { return focusedPackIndex; }

    public int getPackCount() { return packIds.size(); }

    public PackButton getButtonForIndex(int idx) {
        if (idx < 0 || idx >= packButtons.size()) return null;
        return packButtons.get(idx);
    }

    private int wrapIndex(int idx) {
        int n = packIds.size();
        return n <= 0 ? 0 : Math.floorMod(idx, n);
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
    }

    public void focusPackId(String packId, boolean playSound) {
        if (packId == null || packIds.isEmpty()) return;
        int idx = packIds.indexOf(packId);
        if (idx >= 0) setFocusedPackIndex(idx, playSound);
    }

    private void rebuildDisplayOrder(String preserveFocusedId) {
        packIds.clear();
        packIds.addAll(basePackIds);
        normalizeSpecialPackOrder(packIds);

        String bump = SkinPackLoader.getLastUsedCustomPackId();
        String pinnedDefaultPackId = resolvePreferredDefaultPackId(packIds);
        if (bump != null && packIds.contains(bump) && !bump.equals(pinnedDefaultPackId)) {
            packIds.remove(bump);

            int insertAt = 1;
        int favIdx = packIds.indexOf(SkinIdUtil.PACK_FAVOURITES);
            if (favIdx >= 0) insertAt = favIdx + 1;

            if (insertAt < 0) insertAt = 0;
            if (insertAt > packIds.size()) insertAt = packIds.size();
            packIds.add(insertAt, bump);
        }

        if (preserveFocusedId != null) {
            int idx = packIds.indexOf(preserveFocusedId);
            focusedPackIndex = idx >= 0 ? idx : 0;
        } else { if (focusedPackIndex >= packIds.size()) focusedPackIndex = 0; }
    }

    private void setBasePackIds(List<String> ids, String preserveFocusedId) {
        basePackIds.clear();
        basePackIds.addAll(ids);
        rebuildDisplayOrder(preserveFocusedId);
        if (focusedPackIndex >= packIds.size()) focusedPackIndex = 0;
    }

    private void ensureButtons() {
        packButtons.clear();
        if (packIds.isEmpty()) {
            packButtons.add(new PackButton(-1, Component.translatable("consoleskins.pack.none")));
            return;
        }
        for (int i = 0; i < packIds.size(); i++) { packButtons.add(new PackButton(i, labelForPackIndex(i))); }
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
        return Component.literal(SkinPackLoader.nameString(pack.name(), id));
    }

    private SkinPack resolvePack(String packId) {
        if (packId == null || packId.isBlank()) return null;
        return SkinPackLoader.getPacks().get(packId);
    }

    private List<String> collectPackIds() {
        Map<String, SkinPack> source = SkinPackLoader.getPacks();
        ArrayList<String> ids = new ArrayList<>(source.size());
        for (String packId : source.keySet()) {
            if (packId == null || packId.isBlank()) continue;
            ids.add(packId);
        }
        restoreCuratedFestivePack(ids);
        return ids;
    }

    private void normalizeSpecialPackOrder(List<String> ids) {
        if (ids == null) return;
        String preferredDefaultPackId = resolvePreferredDefaultPackId(ids);
        ids.removeIf(SkinIdUtil.PACK_FAVOURITES::equals);
        ids.removeIf(SkinIdUtil.PACK_DEFAULT::equals);
        if (preferredDefaultPackId != null && !SkinIdUtil.PACK_DEFAULT.equals(preferredDefaultPackId)) { ids.removeIf(preferredDefaultPackId::equals); }

        int insertAt = 0;
        if (preferredDefaultPackId != null && SkinPackLoader.getPacks().containsKey(preferredDefaultPackId)) { ids.add(insertAt++, preferredDefaultPackId); }
        if (SkinPackLoader.getPacks().containsKey(SkinIdUtil.PACK_FAVOURITES)) { ids.add(insertAt, SkinIdUtil.PACK_FAVOURITES); }
    }

    private String resolvePreferredDefaultPackId(List<String> ids) {
        String preferred = SkinPackLoader.getPreferredDefaultPackId();
        if (preferred != null && ids != null && ids.contains(preferred)) return preferred;
        if (ids == null) return null;
        for (String packId : ids) {
            if (packId == null || packId.isBlank()) continue;
            if (SkinIdUtil.PACK_FAVOURITES.equals(packId)) continue;
            return packId;
        }
        return null;
    }

    private void restoreCuratedFestivePack(List<String> ids) {
        if (ids == null || ids.contains(FESTIVE_PACK_ID)) return;
        if (!SkinPackLoader.getPacks().containsKey(FESTIVE_PACK_ID)) return;

        int mashupIndex = ids.indexOf(FESTIVE_MASHUP_PACK_ID);
        if (mashupIndex < 0) {
            ids.add(FESTIVE_PACK_ID);
            return;
        }
        ids.add(mashupIndex + 1, FESTIVE_PACK_ID);
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

        private void applyHeight(int h) { this.height = h; }

        public int getPackIndex() { return packIndex; }

        @Override
        public void setFocused(boolean focused) {
            boolean wasFocused = this.isFocused();
            super.setFocused(focused);
            if (!wasFocused && focused && packIndex >= 0 && focusedPackIndex != packIndex) { setFocusedPackIndex(packIndex, false); }
        }

        @Override
        public boolean isHoveredOrFocused() { return (packIndex >= 0 && packIndex == focusedPackIndex) || super.isHoveredOrFocused(); }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            if (!ControlType.getActiveType().isKbm() && this.isFocused() && packIndex >= 0 && focusedPackIndex != packIndex) { setFocusedPackIndex(packIndex, false); }
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
