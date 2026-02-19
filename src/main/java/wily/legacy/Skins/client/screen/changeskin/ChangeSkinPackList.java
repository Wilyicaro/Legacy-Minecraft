package wily.legacy.Skins.client.screen.changeskin;

import wily.legacy.Skins.skin.SkinIds;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.skin.SkinPackLoader;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.client.ControlType;
import wily.legacy.client.screen.RenderableVList;

public final class ChangeSkinPackList {

    private final Runnable clickSound;
    private final FocusChange onFocusChange;


    private final List<String> basePackIds = new ArrayList<>();

    private final List<String> packIds = new ArrayList<>();
    private final List<PackButton> packButtons = new ArrayList<>();

    private int buttonHeight = 20;

    private int focusedPackIndex;
    private boolean queuedChangePack;


    private String lastUsedCustomPackId;

    public interface FocusChange {
        void onFocusedPackChanged();
    }

    public ChangeSkinPackList(Runnable clickSound, FocusChange onFocusChange) {
        this.clickSound = clickSound;
        this.onFocusChange = onFocusChange;
    }

    public void initFromLoader() {
        SkinPackLoader.ensureLoaded();

        refreshPackIds();
        String preferred = SkinPackLoader.getPreferredDefaultPackId();
        int prefIdx = preferred == null ? -1 : packIds.indexOf(preferred);
        focusedPackIndex = prefIdx >= 0 ? prefIdx : 0;
        queuedChangePack = false;
        ensureButtons();
    }

    public void applyUiScale(float uiScale) {
        int h = Math.max(1, Math.round(20f * uiScale));
        buttonHeight = Math.max(10, h);
        for (PackButton b : packButtons) b.applyHeight(buttonHeight);
    }

    public void refreshPackIdsIfNeeded() {
        SkinPackLoader.ensureLoaded();
        List<String> currentBase = new ArrayList<>(SkinPackLoader.getPacks().keySet());

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
        return id == null ? null : SkinPackLoader.getPacks().get(id);
    }

    public List<SkinPack> getPacks() {
        if (packIds.isEmpty()) return List.of();
        List<SkinPack> out = new ArrayList<>(packIds.size());
        var all = SkinPackLoader.getPacks();
        for (String id : packIds) {
            SkinPack p = all.get(id);
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


    private void refreshPackIds() {
        basePackIds.clear();
        basePackIds.addAll(SkinPackLoader.getPacks().keySet());
        rebuildDisplayOrder(null);
        if (focusedPackIndex >= packIds.size()) focusedPackIndex = 0;
    }

    private void rebuildDisplayOrder(String preserveFocusedId) {
        packIds.clear();
        packIds.addAll(basePackIds);

        String bump = SkinPackLoader.getLastUsedCustomPackId();
        if (bump != null && packIds.contains(bump)) {

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
        SkinPack pack = SkinPackLoader.getPacks().get(id);
        return Component.literal(pack != null ? pack.name() : id);
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
    }
}
