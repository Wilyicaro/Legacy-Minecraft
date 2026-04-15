package wily.legacy.Skins.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.api.ui.LegacySkinUi;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class SkinUiSource implements ChangeSkinScreenSource {
    private final LegacySkinUi.Adapter adapter;
    private final LinkedHashMap<String, SkinPack> packs = new LinkedHashMap<>();
    private final LinkedHashMap<String, SkinEntry> skins = new LinkedHashMap<>();
    private final LinkedHashMap<String, Component> packSubtitles = new LinkedHashMap<>();
    private final Set<String> favoriteSkinIds = new LinkedHashSet<>();
    private int seenAdapterVersion = Integer.MIN_VALUE;
    private int localVersion;
    private String lastUsedPackId;
    private String requestedFocusPackId;
    private String requestedFocusSkinId;
    private String appliedSkinId;

    SkinUiSource(LegacySkinUi.Adapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public Map<String, SkinPack> packs() {
        refresh();
        return packs;
    }

    @Override
    public @Nullable SkinEntry skin(String id) {
        return id == null ? null : skins.get(id);
    }

    @Override
    public @Nullable String currentAppliedSkinId() {
        return appliedSkinId;
    }

    @Override
    public boolean supportsFavorites() {
        return adapter.supportsFavorites();
    }

    @Override
    public boolean isFavorite(String skinId) {
        return skinId != null && favoriteSkinIds.contains(skinId);
    }

    @Override
    public void toggleFavorite(String skinId) {
        if (skinId == null || !adapter.supportsFavorites() || !skins.containsKey(skinId)) return;
        adapter.toggleFavorite(skinId);
        if (!favoriteSkinIds.add(skinId)) favoriteSkinIds.remove(skinId);
        localVersion++;
    }

    @Override
    public void selectSkin(@Nullable String packId, String skinId) {
        String resolvedPackId = SkinIdUtil.trimToNull(packId);
        if (resolvedPackId == null) resolvedPackId = findPackId(skinId);
        if (resolvedPackId == null || !skins.containsKey(skinId)) return;
        adapter.selectSkin(skinId);
        lastUsedPackId = resolvedPackId;
        requestedFocusPackId = resolvedPackId;
        requestedFocusSkinId = skinId;
        appliedSkinId = SkinIdUtil.isAutoSelect(skinId) ? "" : skinId;
        localVersion++;
    }

    @Override
    public void prewarmPreview(String skinId) {
        if (skinId != null && skins.containsKey(skinId)) adapter.prewarmPreview(skinId);
    }

    @Override
    public boolean renderPreview(GuiGraphics graphics, String skinId, float yawOffset, boolean crouchPose, float attackTime, float partialTick, int left, int top, int right, int bottom) {
        if (!skins.containsKey(skinId)) return false;
        adapter.renderPreview(new LegacySkinUi.PreviewContext(
                graphics,
                skinId,
                left,
                top,
                right,
                bottom,
                yawOffset,
                crouchPose,
                attackTime,
                partialTick
        ));
        return true;
    }

    @Override
    public @Nullable Component packSubtitle(SkinPack pack) {
        return pack == null ? null : packSubtitles.get(pack.id());
    }

    @Override
    public int version() {
        return adapter.version() * 31 + localVersion;
    }

    @Override
    public @Nullable String initialPackId(@Nullable String selectedSkinId) {
        String packId = requestedFocusPackId;
        requestedFocusPackId = null;
        if (packId != null) return packId;
        if (selectedSkinId != null && !selectedSkinId.isBlank()) {
            packId = findPackId(selectedSkinId);
            if (packId != null) return packId;
        }
        return lastUsedPackId != null ? lastUsedPackId : preferredDefaultPackId();
    }

    @Override
    public @Nullable String preferredDefaultPackId() {
        return packs.isEmpty() ? null : packs.keySet().iterator().next();
    }

    @Override
    public @Nullable String lastUsedPackId() {
        return lastUsedPackId;
    }

    @Override
    public void requestFocus(@Nullable String packId, @Nullable String skinId) {
        requestedFocusSkinId = SkinIdUtil.trimToNull(skinId);
        requestedFocusPackId = SkinIdUtil.trimToNull(packId);
        if (requestedFocusPackId == null && requestedFocusSkinId != null) requestedFocusPackId = findPackId(requestedFocusSkinId);
        if (requestedFocusPackId != null) lastUsedPackId = requestedFocusPackId;
    }

    @Override
    public @Nullable String consumeRequestedFocusSkinId() {
        String skinId = requestedFocusSkinId;
        requestedFocusSkinId = null;
        return skinId;
    }

    @Override
    public boolean supportsAdvancedOptions() {
        return true;
    }

    private void refresh() {
        int adapterVersion = adapter.version();
        if (adapterVersion == seenAdapterVersion) return;
        seenAdapterVersion = adapterVersion;
        packs.clear();
        skins.clear();
        packSubtitles.clear();
        favoriteSkinIds.clear();
        String selectedSkinId = SkinIdUtil.trimToNull(adapter.selectedSkinId());
        appliedSkinId = selectedSkinId == null ? null : SkinIdUtil.isAutoSelect(selectedSkinId) ? "" : selectedSkinId;
        boolean favorites = adapter.supportsFavorites();
        int packOrder = 0;
        var adapterPacks = adapter.packs();
        if (adapterPacks == null) return;
        for (LegacySkinUi.Pack apiPack : adapterPacks) {
            String packId = SkinIdUtil.trimToNull(apiPack == null ? null : apiPack.id());
            if (packId == null || packs.containsKey(packId)) continue;
            packSubtitles.put(packId, apiPack.subtitle());
            LinkedHashMap<String, SkinEntry> packSkins = new LinkedHashMap<>();
            int skinOrder = 0;
            for (LegacySkinUi.Skin apiSkin : apiPack.skins()) {
                String skinId = SkinIdUtil.trimToNull(apiSkin == null ? null : apiSkin.id());
                if (skinId == null || packSkins.containsKey(skinId)) continue;
                int order = ++skinOrder;
                SkinEntry skin = skins.get(skinId);
                if (skin == null) {
                    skin = new SkinEntry(skinId, skinId, apiSkin.title().getString(), null, null, null, false, order, false);
                    skins.put(skinId, skin);
                }
                if (favorites && adapter.isFavorite(skinId)) favoriteSkinIds.add(skinId);
                packSkins.put(skinId, skin.order() == order ? skin : orderedSkin(skin, order));
            }
            packs.put(packId, new SkinPack(
                    packId,
                    apiPack.title().getString(),
                    null,
                    null,
                    apiPack.icon(),
                    java.util.List.copyOf(packSkins.values()),
                    false,
                    packOrder++,
                    true,
                    0
            ));
        }
    }

    private static SkinEntry orderedSkin(SkinEntry skin, int order) {
        return new SkinEntry(skin.id(), skin.sourceId(), skin.name(), skin.texture(), skin.modelId(), skin.cape(), skin.slimArms(), order, skin.fair());
    }

    private @Nullable String findPackId(String skinId) {
        if (skinId == null || skinId.isBlank()) return null;
        String packId = findPackId(skinId, false);
        return packId != null ? packId : findPackId(skinId, true);
    }

    private @Nullable String findPackId(String skinId, boolean favoritesOnly) {
        for (Map.Entry<String, SkinPack> entry : packs.entrySet()) {
            String packId = entry.getKey();
            if (favoritesOnly != SkinIdUtil.isFavouritesPack(packId)) continue;
            for (SkinEntry skin : entry.getValue().skins()) {
                if (skin != null && skinId.equals(skin.id())) return packId;
            }
        }
        return null;
    }
}
