package wily.legacy.Skins.client.changeskin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import wily.legacy.Skins.client.preview.PlayerSkinWidget;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import java.util.*;
public final class ChangeSkinPackList {
    private static final String FESTIVE_MASHUP_PACK_ID = "festivemashup";
    private static final String FESTIVE_PACK_ID = "festive";
    private static final Component NO_PACKS = Component.translatable("consoleskins.pack.none");
    private final Runnable focusSound;
    private final Runnable pressSound;
    private final List<String> basePackIds = new ArrayList<>();
    private final List<String> packIds = new ArrayList<>();
    private int buttonHeight = 20;
    private int focusedPackIndex;
    private boolean queuedChangePack;
    private boolean reorderMode;
    public ChangeSkinPackList(Runnable focusSound, Runnable pressSound) {
        this.focusSound = focusSound;
        this.pressSound = pressSound;
    }
    public void initFromLoader() {
        SkinPackLoader.ensureLoaded();
        setBasePackIds(collectPackIds(), null);
        String preferred = resolvePreferredDefaultPackId(packIds);
        focusedPackIndex = preferred == null ? 0 : Math.max(0, packIds.indexOf(preferred));
        queuedChangePack = false;
    }
    public void applyResolvedButtonHeight(int resolvedHeight) { buttonHeight = Math.max(10, resolvedHeight); }
    public int getButtonHeight() { return buttonHeight; }
    public void setReorderMode(boolean reorderMode) {
        this.reorderMode = reorderMode;
        if (reorderMode) queuedChangePack = false;
    }
    public void refreshPackIdsIfNeeded() {
        SkinPackLoader.ensureLoaded();
        List<String> currentBase = collectPackIds();
        if (currentBase.equals(basePackIds)) return;
        setBasePackIds(currentBase, getFocusedPackId());
        queuedChangePack = true;
    }
    public boolean consumeQueuedChangePack() {
        boolean queued = queuedChangePack;
        queuedChangePack = false;
        return queued;
    }
    public int getFocusedPackIndex() { return focusedPackIndex; }
    public String getFocusedPackId() { return packIds.isEmpty() ? null : packIds.get(wrapIndex(focusedPackIndex)); }
    public int getPackCount() { return packIds.size(); }
    public Component getLabelForIndex(int index) { return getLabel(index, false); }
    public Component getWrappedLabelForIndex(int index) { return getLabel(index, true); }
    public SkinPack getFocusedPack() { return pack(SkinPackLoader.getPacks(), getFocusedPackId()); }
    public void setFocusedPackIndex(int index, boolean playSound) {
        if (packIds.isEmpty()) {
            focusedPackIndex = 0;
            return;
        }
        int wrapped = wrapIndex(index);
        if (wrapped == focusedPackIndex) return;
        focusedPackIndex = wrapped;
        if (!reorderMode) queuedChangePack = true;
        if (playSound) focusSound.run();
    }
    public void pressPackIndex(int index) {
        if (packIds.isEmpty()) {
            focusedPackIndex = 0;
            return;
        }
        focusedPackIndex = wrapIndex(index);
        if (!reorderMode) queuedChangePack = true;
        pressSound.run();
    }
    public void focusPackId(String packId, boolean playSound) {
        if (packId == null || packIds.isEmpty()) return;
        int index = packIds.indexOf(packId);
        if (index >= 0) setFocusedPackIndex(index, playSound);
    }
    private int wrapIndex(int index) {
        return packIds.isEmpty() ? 0 : Math.floorMod(index, packIds.size());
    }
    private Component getLabel(int index, boolean wrap) {
        if (packIds.isEmpty()) return wrap ? NO_PACKS : Component.empty();
        int resolved = wrap ? wrapIndex(index) : index;
        return resolved < 0 || resolved >= packIds.size() ? Component.empty() : labelForPackId(packIds.get(resolved));
    }
    public List<String> orderedPackIds() {
        ArrayList<String> ids = new ArrayList<>(packIds.size());
        for (String packId : packIds) {
            if (SkinIdUtil.PACK_DEFAULT.equals(packId) || SkinIdUtil.PACK_FAVOURITES.equals(packId)) continue;
            ids.add(packId);
        }
        return List.copyOf(ids);
    }
    public boolean moveFocusedPack(int delta) { return delta != 0 && moveFocusedPackTo(wrapIndex(focusedPackIndex) + delta); }
    public boolean moveFocusedPackTo(int targetIndex) {
        if (packIds.isEmpty()) return false;
        int minIndex = firstMovableIndex();
        int index = wrapIndex(focusedPackIndex);
        if (index < minIndex) return false;
        int target = Math.max(minIndex, Math.min(packIds.size() - 1, targetIndex));
        if (target == index) return false;
        String packId = packIds.remove(index);
        packIds.add(target, packId);
        focusedPackIndex = target;
        return true;
    }
    private void setBasePackIds(List<String> ids, String preserveFocusedId) {
        basePackIds.clear();
        basePackIds.addAll(ids);
        rebuildDisplayOrder(preserveFocusedId);
        if (focusedPackIndex >= packIds.size()) focusedPackIndex = 0;
    }
    private void rebuildDisplayOrder(String preserveFocusedId) {
        packIds.clear();
        packIds.addAll(basePackIds);
        normalizeSpecialPackOrder(packIds);
        String lastUsedCustomPackId = SkinPackLoader.getLastUsedCustomPackId();
        String preferredDefaultPackId = resolvePreferredDefaultPackId(packIds);
        SkinPack lastUsedPack = pack(SkinPackLoader.getPacks(), lastUsedCustomPackId);
        if (lastUsedCustomPackId != null && packIds.contains(lastUsedCustomPackId) && !lastUsedCustomPackId.equals(preferredDefaultPackId) && (lastUsedPack == null || !lastUsedPack.editable())) {
            packIds.remove(lastUsedCustomPackId);
            int insertAt = Math.max(1, packIds.indexOf(SkinIdUtil.PACK_FAVOURITES) + 1);
            packIds.add(Math.min(insertAt, packIds.size()), lastUsedCustomPackId);
        }
        if (preserveFocusedId == null) {
            if (focusedPackIndex >= packIds.size()) focusedPackIndex = 0;
            return;
        }
        int preservedIndex = packIds.indexOf(preserveFocusedId);
        focusedPackIndex = preservedIndex >= 0 ? preservedIndex : 0;
    }
    private Component labelForPackId(String packId) {
        SkinPack pack = pack(SkinPackLoader.getPacks(), packId);
        return pack == null ? Component.literal(String.valueOf(packId)) : Component.literal(SkinPackLoader.nameString(pack.name(), packId));
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
        if (preferredDefaultPackId != null && !SkinIdUtil.PACK_DEFAULT.equals(preferredDefaultPackId)) ids.removeIf(preferredDefaultPackId::equals);
        int insertAt = 0;
        if (preferredDefaultPackId != null && SkinPackLoader.getPacks().containsKey(preferredDefaultPackId)) ids.add(insertAt++, preferredDefaultPackId);
        if (SkinPackLoader.getPacks().containsKey(SkinIdUtil.PACK_FAVOURITES)) ids.add(insertAt, SkinIdUtil.PACK_FAVOURITES);
    }
    private String resolvePreferredDefaultPackId(List<String> ids) {
        String preferred = SkinPackLoader.getPreferredDefaultPackId();
        if (preferred != null && ids != null && ids.contains(preferred)) return preferred;
        if (ids == null) return null;
        for (String packId : ids) {
            if (packId == null || packId.isBlank() || SkinIdUtil.PACK_FAVOURITES.equals(packId)) continue;
            return packId;
        }
        return null;
    }
    private void restoreCuratedFestivePack(List<String> ids) {
        if (ids == null || ids.contains(FESTIVE_PACK_ID) || !SkinPackLoader.getPacks().containsKey(FESTIVE_PACK_ID)) return;
        int mashupIndex = ids.indexOf(FESTIVE_MASHUP_PACK_ID);
        ids.add(mashupIndex < 0 ? ids.size() : mashupIndex + 1, FESTIVE_PACK_ID);
    }
    private static SkinPack pack(Map<String, SkinPack> packs, String packId) {
        return packId == null || packId.isBlank() || packs == null ? null : packs.get(packId);
    }
    private int firstMovableIndex() {
        int index = 0;
        while (index < packIds.size()) {
            String packId = packIds.get(index);
            if (!SkinIdUtil.PACK_DEFAULT.equals(packId) && !SkinIdUtil.PACK_FAVOURITES.equals(packId)) break;
            index++;
        }
        return index;
    }
    public static final class PackButton extends Button {
        private static final int DIM_OVERLAY = 0x66303030;
        private final ChangeSkinPackList owner;
        private final int packIndex;
        public PackButton(ChangeSkinPackList owner, int packIndex, Component message, int height) {
            super(0, 0, 0, height, message, button -> {
                if (packIndex >= 0) owner.pressPackIndex(packIndex);
            }, DEFAULT_NARRATION);
            this.owner = owner;
            this.packIndex = packIndex;
            this.active = packIndex >= 0;
        }
        public int getPackIndex() { return packIndex; }

        private boolean isMovingPack() {
            return owner.reorderMode && packIndex >= 0 && owner.focusedPackIndex == packIndex;
        }
        private boolean isDimmedPack() {
            return owner.reorderMode && packIndex >= 0 && owner.focusedPackIndex != packIndex;
        }
        @Override
        public void setFocused(boolean focused) {
            boolean wasFocused = isFocused();
            super.setFocused(focused);
            if (!owner.reorderMode && !wasFocused && focused && packIndex >= 0 && owner.focusedPackIndex != packIndex) owner.setFocusedPackIndex(packIndex, false);
        }
        @Override
        public boolean isHoveredOrFocused() { return packIndex >= 0 && owner.focusedPackIndex == packIndex; }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            if (isDimmedPack()) graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), DIM_OVERLAY);
            if (!owner.reorderMode && !ControlType.getActiveType().isKbm() && isFocused() && packIndex >= 0 && owner.focusedPackIndex != packIndex) owner.setFocusedPackIndex(packIndex, false);
        }
        @Override public void playDownSound(SoundManager soundManager) { }
        @Override
        public void renderString(GuiGraphics graphics, Font font, int color) {
            String visibleText = PlayerSkinWidget.clipText(font, getMessage() == null ? "" : getMessage().getString(), Math.max(0, getWidth() - TEXT_MARGIN * 2));
            float textScale = height < 20 && LegacyOptions.getUIMode().isSD() ? 0.84f : 1.0f;
            int centerX = getX() + getWidth() / 2;
            float textY = getY() + (getHeight() - font.lineHeight * textScale) / 2.0f;
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
