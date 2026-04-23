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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class SkinUiSource implements ChangeSkinScreenSource {
    private final LegacySkinUi.Adapter adapter;
    private final LinkedHashMap<String, SkinPack> packs = new LinkedHashMap<>();
    private final LinkedHashMap<String, SkinEntry> skins = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> packSubtitles = new LinkedHashMap<>();
    private final Set<String> favoriteSkinIds = new LinkedHashSet<>();
    private int stateHash = Integer.MIN_VALUE;
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
        refresh();
        return id == null ? null : skins.get(id);
    }

    @Override
    public @Nullable String currentAppliedSkinId() {
        refresh();
        return appliedSkinId;
    }

    @Override
    public boolean supportsFavorites() {
        return adapter.supportsFavorites();
    }

    @Override
    public boolean isFavorite(String skinId) {
        refresh();
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
        refresh();
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
        refresh();
        if (pack == null) return null;
        String subtitle = packSubtitles.get(pack.id());
        return subtitle == null || subtitle.isBlank() ? null : Component.literal(subtitle);
    }

    @Override
    public int version() {
        refresh();
        return stateHash * 31 + localVersion;
    }

    @Override
    public @Nullable String initialPackId(@Nullable String selectedSkinId) {
        String packId = requestedFocusPackId;
        requestedFocusPackId = null;
        if (packId != null) return packId;
        if (matchesPackSkin(lastUsedPackId, selectedSkinId)) return lastUsedPackId;
        if (selectedSkinId != null && !selectedSkinId.isBlank()) {
            packId = findPackId(selectedSkinId);
            if (packId != null) return packId;
        }
        return lastUsedPackId != null ? lastUsedPackId : preferredDefaultPackId();
    }

    @Override
    public @Nullable String preferredDefaultPackId() {
        refresh();
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
        return false;
    }

    private void refresh() {
        int nextStateHash = adapter.version();
        String selectedSkinId = SkinIdUtil.trimToNull(adapter.selectedSkinId());
        nextStateHash = 31 * nextStateHash + Objects.hashCode(selectedSkinId);
        boolean favorites = adapter.supportsFavorites();
        nextStateHash = 31 * nextStateHash + Boolean.hashCode(favorites);
        List<LegacySkinUi.Pack> adapterPacks = adapter.packs();
        if (adapterPacks != null) {
            for (LegacySkinUi.Pack apiPack : adapterPacks) {
                if (apiPack == null) {
                    nextStateHash = 31 * nextStateHash;
                    continue;
                }
                nextStateHash = 31 * nextStateHash + Objects.hash(
                        SkinIdUtil.trimToNull(apiPack.id()),
                        apiPack.title(),
                        apiPack.subtitle(),
                        apiPack.icon()
                );
                for (LegacySkinUi.Skin apiSkin : apiPack.skins()) {
                    if (apiSkin == null) {
                        nextStateHash = 31 * nextStateHash;
                        continue;
                    }
                    String skinId = SkinIdUtil.trimToNull(apiSkin.id());
                    boolean favorite = favorites && skinId != null && adapter.isFavorite(skinId);
                    nextStateHash = 31 * nextStateHash + Objects.hash(skinId, apiSkin.title(), favorite);
                }
            }
        }
        if (nextStateHash == stateHash) return;
        stateHash = nextStateHash;
        packs.clear();
        skins.clear();
        packSubtitles.clear();
        favoriteSkinIds.clear();
        appliedSkinId = selectedSkinId == null ? null : SkinIdUtil.isAutoSelect(selectedSkinId) ? "" : selectedSkinId;
        int packOrder = 0;
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
                    skin = new SkinEntry(skinId, skinId, apiSkin.title(), null, null, null, false, order, false);
                    skins.put(skinId, skin);
                }
                if (favorites && adapter.isFavorite(skinId)) favoriteSkinIds.add(skinId);
                packSkins.put(skinId, skin.order() == order ? skin : orderedSkin(skin, order));
            }
            packs.put(packId, new SkinPack(
                    packId,
                    apiPack.title(),
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

    private boolean matchesPackSkin(@Nullable String packId, @Nullable String skinId) {
        if (packId == null || skinId == null || skinId.isBlank()) return false;
        SkinPack pack = packs.get(packId);
        if (pack == null) return false;
        for (SkinEntry skin : pack.skins()) {
            if (skin == null) continue;
            if (skinId.equals(skin.id()) || skinId.equals(skin.sourceId())) return true;
        }
        return false;
    }
}
