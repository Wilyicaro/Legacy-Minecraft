package wily.legacy.skins.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import wily.legacy.client.LegacyOptions;
import wily.legacy.skins.client.gui.GuiDollRender;
import wily.legacy.skins.client.gui.GuiSessionSkin;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.ClientSkinCache;
import wily.legacy.skins.skin.SkinDataStore;
import wily.legacy.skins.skin.DownloadedSkinPackStore;
import wily.legacy.skins.skin.SkinEntry;
import wily.legacy.skins.skin.SkinIdUtil;
import wily.legacy.skins.skin.SkinPack;
import wily.legacy.skins.skin.SkinPackLoader;
import wily.legacy.skins.skin.SkinSyncClient;
import wily.legacy.skins.api.ui.LegacySkinUi;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public interface ChangeSkinScreenSource {
    static ChangeSkinScreenSource from(LegacySkinUi.Adapter adapter) {
        return new SkinUiSource(adapter);
    }

    default Screen create(Screen parent) {
        return LegacyOptions.tu3ChangeSkinScreen.get()
                ? new TU3ChangeSkinScreen(parent, this)
                : new ChangeSkinScreen(parent, this);
    }

    Map<String, SkinPack> packs();

    default String packName(SkinPack pack) {
        return pack == null ? "" : pack.name();
    }

    @Nullable SkinEntry skin(String id);

    default String skinName(SkinEntry skin) {
        return skin == null ? "" : skin.name();
    }

    @Nullable String currentAppliedSkinId();

    boolean supportsFavorites();

    boolean isFavorite(String skinId);

    void toggleFavorite(String skinId);

    void selectSkin(@Nullable String packId, String skinId);

    void prewarmPreview(String skinId);

    boolean renderPreview(GuiGraphics graphics, String skinId, float yawOffset, boolean crouchPose, float attackTime, float partialTick, int left, int top, int right, int bottom);

    @Nullable Component packSubtitle(SkinPack pack);

    default Component noPacksLabel() {
        return Component.translatable("consoleskins.pack.none");
    }

    int version();

    @Nullable String initialPackId(@Nullable String selectedSkinId);

    @Nullable String preferredDefaultPackId();

    @Nullable String lastUsedPackId();

    void requestFocus(@Nullable String packId, @Nullable String skinId);

    @Nullable String consumeRequestedFocusSkinId();

    default boolean supportsCustomPackOptions() {
        return false;
    }

    boolean supportsAdvancedOptions();

    final class Default implements ChangeSkinScreenSource {
        public static final Default INSTANCE = new Default();

        private Default() {
        }

        @Override
        public Map<String, SkinPack> packs() {
            return SkinPackLoader.getPacks();
        }

        @Override
        public String packName(SkinPack pack) {
            return pack == null ? "" : SkinPackLoader.nameString(pack.name(), pack.id());
        }

        @Override
        public SkinEntry skin(String id) {
            return SkinPackLoader.getSkin(id);
        }

        @Override
        public String skinName(SkinEntry skin) {
            return skin == null ? "" : SkinPackLoader.nameString(skin.name(), skin.id());
        }

        @Override
        public String currentAppliedSkinId() {
            Minecraft minecraft = Minecraft.getInstance();
            UUID self = selfId(minecraft);
            if (self == null) return null;
            String applied = ClientSkinCache.get(self);
            if (SkinIdUtil.hasSkin(applied)) return applied;
            applied = SkinDataStore.getSelectedSkin(self);
            return SkinIdUtil.hasSkin(applied) ? applied : "";
        }

        @Override
        public boolean supportsFavorites() {
            return true;
        }

        @Override
        public boolean isFavorite(String skinId) {
            return skinId != null && SkinDataStore.isFavorite(skinId);
        }

        @Override
        public void toggleFavorite(String skinId) {
            SkinDataStore.toggleFavorite(skinId);
            SkinPackLoader.rebuildFavouritesPack();
        }

        @Override
        public void selectSkin(String packId, String skinId) {
            String requestedId = SkinIdUtil.isAutoSelect(skinId) ? "" : skinId;
            String sourcePackId = packId;
            if (sourcePackId != null && SkinIdUtil.isFavouritesPack(sourcePackId)) sourcePackId = SkinPackLoader.getSourcePackId(skinId);
            SkinPackLoader.setLastUsedCustomPackId(sourcePackId);
            SkinSyncClient.requestSetSkin(Minecraft.getInstance(), sourcePackId, requestedId);
        }

        @Override
        public void prewarmPreview(String skinId) {
            if (skinId == null || skinId.isBlank()) return;
            if (SkinIdUtil.isAutoSelect(skinId)) {
                GuiSessionSkin.prewarm();
                return;
            }
            ClientSkinAssets.enqueuePreviewWarmup(skinId);
        }

        @Override
        public boolean renderPreview(GuiGraphics graphics, String skinId, float yawOffset, boolean crouchPose, float attackTime, float partialTick, int left, int top, int right, int bottom) {
            if (skinId == null || skinId.isBlank()) return false;
            if (SkinIdUtil.isAutoSelect(skinId)) {
                var playerSkin = GuiSessionSkin.getSessionPlayerSkin();
                if (playerSkin == null) return false;
                GuiDollRender.renderDollInRect(graphics, skinId, playerSkin, yawOffset, crouchPose, attackTime, partialTick, left, top, right, bottom, 165);
                return true;
            }
            SkinEntry entry = skin(skinId);
            if (entry == null || entry.texture() == null) return false;
            GuiDollRender.renderDollInRect(graphics, skinId, entry.texture(), yawOffset, crouchPose, attackTime, partialTick, left, top, right, bottom, 165);
            return true;
        }

        @Override
        public Component packSubtitle(SkinPack pack) {
            if (pack == null) return null;
            Minecraft minecraft = Minecraft.getInstance();
            boolean downloaded = minecraft != null && DownloadedSkinPackStore.isDownloadedPack(minecraft, pack.id());
            if (pack.type() == null || pack.type().isBlank()) return downloaded ? Component.translatable("legacy.skinpack.type.community") : null;
            String key = pack.type().toLowerCase(Locale.ROOT);
            if (key.equals("skin")) return Component.translatable("legacy.skinpack.type.skin");
            if (key.equals("mashup")) return Component.translatable("legacy.skinpack.type.mashup");
            if (key.equals("community")) return Component.translatable("legacy.skinpack.type.community");
            if (key.equals("author") || key.equals("accredited")) {
                if (pack.author() != null && !pack.author().isBlank()) return Component.translatable("legacy.skinpack.type.author", pack.author());
                return Component.translatable("legacy.skinpack.type.skin");
            }
            return null;
        }

        @Override
        public int version() {
            return SkinPackLoader.getReloadVersion();
        }

        @Override
        public String initialPackId(String selectedSkinId) {
            String packId = SkinPackLoader.consumeRequestedFocusPackId();
            if (packId != null) return packId;
            UUID self = selfId(Minecraft.getInstance());
            if (self != null) {
                packId = SkinDataStore.getSelectedPack(self);
                if (packContainsSkin(packId, selectedSkinId)) return packId;
            }
            if (selectedSkinId != null && !selectedSkinId.isBlank()) {
                packId = SkinPackLoader.getSourcePackId(selectedSkinId);
                if (packId != null) return packId;
            }
            packId = SkinPackLoader.getLastUsedCustomPackId();
            return packId != null ? packId : SkinPackLoader.getPreferredDefaultPackId();
        }

        @Override
        public String preferredDefaultPackId() {
            return SkinPackLoader.getPreferredDefaultPackId();
        }

        @Override
        public String lastUsedPackId() {
            return SkinPackLoader.getLastUsedCustomPackId();
        }

        @Override
        public void requestFocus(String packId, String skinId) {
            if (packId != null && !packId.isBlank()) {
                SkinPackLoader.setLastUsedCustomPackId(packId);
                SkinPackLoader.requestFocusPack(packId);
            }
            if (skinId != null && !skinId.isBlank()) SkinPackLoader.requestFocusSkin(skinId);
        }

        @Override
        public String consumeRequestedFocusSkinId() {
            return SkinPackLoader.consumeRequestedFocusSkinId();
        }

        @Override
        public boolean supportsCustomPackOptions() {
            return true;
        }

        @Override
        public boolean supportsAdvancedOptions() {
            return true;
        }

        private static UUID selfId(Minecraft minecraft) {
            if (minecraft == null) return null;
            if (minecraft.player != null) return minecraft.player.getUUID();
            return minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
        }

        private static boolean packContainsSkin(String packId, String skinId) {
            if (packId == null || packId.isBlank() || skinId == null || skinId.isBlank()) return false;
            SkinPack pack = SkinPackLoader.getPacks().get(packId);
            if (pack == null || pack.skins() == null) return false;
            for (SkinEntry entry : pack.skins()) {
                if (entry == null) continue;
                if (skinId.equals(entry.id()) || skinId.equals(entry.sourceId())) return true;
            }
            return false;
        }
    }
}
