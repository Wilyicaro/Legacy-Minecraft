package wily.legacy.Skins.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import wily.legacy.Skins.client.gui.GuiDollRender;
import wily.legacy.Skins.client.gui.GuiSessionSkin;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinDataStore;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.Skins.skin.SkinSyncClient;
import wily.legacy.Skins.api.ui.LegacySkinUi;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public interface ChangeSkinScreenSource {
    static ChangeSkinScreenSource from(LegacySkinUi.Adapter adapter) {
        return new SkinUiSource(adapter);
    }

    default Screen create(Screen parent) {
        return ConsoleSkinsClientSettings.isTu3ChangeSkinScreen()
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
            UUID self = minecraft.player != null ? minecraft.player.getUUID() : minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
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
            SkinSyncClient.requestSetSkin(Minecraft.getInstance(), requestedId);
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
            if (pack == null || pack.type() == null || pack.type().isBlank()) return null;
            String key = pack.type().toLowerCase(Locale.ROOT);
            if (key.equals("skin")) return Component.translatable("legacy.skinpack.type.skin");
            if (key.equals("mashup")) return Component.translatable("legacy.skinpack.type.mashup");
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
    }
}
