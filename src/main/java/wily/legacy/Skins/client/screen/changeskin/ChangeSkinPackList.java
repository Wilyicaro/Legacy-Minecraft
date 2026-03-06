package wily.legacy.Skins.client.screen.changeskin;

import wily.legacy.Skins.skin.SkinIds;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.skin.SkinPackLoader;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.CommonValue;
import wily.legacy.client.ControlType;
import wily.legacy.client.screen.RenderableVList;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

public final class ChangeSkinPackList {

    private final Runnable clickSound;
    private final FocusChange onFocusChange;

    private final List<String> basePackIds = new ArrayList<>();

    private final List<String> packIds = new ArrayList<>();
    private final List<PackButton> packButtons = new ArrayList<>();

    private int buttonHeight = 20;
    private float textScale = 1f;

    private int focusedPackIndex;
    private boolean queuedChangePack;

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
        float s = uiScale * 1.15f;
        if (s < 0.90f) s = 0.90f;
        if (s > 1.30f) s = 1.30f;
        textScale = s;
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
        if (pack == null) return Component.literal(id);
        return SkinIdUtil.isFavouritesPack(pack.id())
                ? SkinPackLoader.nameComponent(pack.name(), "Favorites")
                : SkinPackLoader.nameComponent(pack.name(), id);
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
            if (!this.visible) return;

            boolean selected = packIndex >= 0 && packIndex == focusedPackIndex;
            boolean hot = selected || this.isHoveredOrFocused();
            ResourceLocation sprite = hot ? LegacySprites.BUTTON_HIGHLIGHTED : LegacySprites.BUTTON;
            int bw = Math.max(1, width);
            int bh = Math.max(1, height);

            this.alpha = active ? 1f : 0.8f;
            FactoryScreenUtil.enableBlend();
            FactoryGuiGraphics.of(graphics).blitSprite(sprite, getX(), getY(), bw, bh);
            FactoryScreenUtil.disableBlend();

            var font = Minecraft.getInstance().font;
            String label = getMessage() == null ? "" : getMessage().getString();

            float s = ChangeSkinPackList.this.textScale;
            int maxPx = Math.max(1, bw - 10);
            int maxUnscaled = (int) (maxPx / s);

            boolean shadow = CommonValue.WIDGET_TEXT_SHADOW.get();
            int color;
            if (!active) color = wily.legacy.client.CommonColor.GRAY.get();
            else if (hot) color = LegacyRenderUtil.getDefaultTextColor(false);
            else color = LegacyRenderUtil.getDefaultTextColor(true);

            var pose = graphics.pose();

            if (font.width(label) <= maxUnscaled) {
                int cx = getX() + bw / 2;
                int cy = getY() + (bh - Math.round(font.lineHeight * s)) / 2;
                pose.pushMatrix();
                pose.translate((float) cx, (float) cy);
                pose.scale(s, s);
                int w = font.width(label);
                graphics.drawString(font, label, -w / 2, 0, color, shadow);
                pose.popMatrix();
            } else {
                String ell = "...";
                float sEll = s * 1.2f;
                if (sEll > s * 1.35f) sEll = s * 1.35f;

                int ellWUn = font.width(ell);
                float availPx = maxPx - ellWUn * sEll;
                if (availPx < 0) availPx = 0;

                int baseMaxUn = (int) (availPx / s);
                String base = font.plainSubstrByWidth(label, Math.max(0, baseMaxUn));

                while (!base.isEmpty() && Character.isWhitespace(base.charAt(base.length() - 1))) {
                    base = base.substring(0, base.length() - 1);
                }

int baseWUn = font.width(base);
                float totalPx = baseWUn * s + ellWUn * sEll;

                float startX = getX() + (bw - totalPx) / 2f;
                float yBase = getY() + (bh - font.lineHeight * s) / 2f;
                float yEll = getY() + (bh - font.lineHeight * sEll) / 2f;

                pose.pushMatrix();
                pose.translate(startX, yBase);
                pose.scale(s, s);
                graphics.drawString(font, base, 0, 0, color, shadow);
                pose.popMatrix();

                pose.pushMatrix();
                pose.translate(startX + baseWUn * s, yEll);
                pose.scale(sEll, sEll);
                graphics.drawString(font, ell, 0, 0, color, shadow);
                pose.popMatrix();
            }
            if (!ControlType.getActiveType().isKbm() && this.isFocused() && packIndex >= 0 && focusedPackIndex != packIndex) {
                setFocusedPackIndex(packIndex, false);
            }
        }
    }
}
