package wily.legacy.skins.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.ConfirmationScreen;
import wily.legacy.skins.SkinsClientBootstrap;
import wily.legacy.skins.client.changeskin.ChangeSkinPackList;
import wily.legacy.skins.client.preview.PlayerSkinWidget;
import wily.legacy.skins.client.render.boxloader.BoxModelManager;
import wily.legacy.skins.skin.*;
import wily.legacy.util.LegacyComponents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class CustomSkinPackFlow {
    private final AbstractChangeSkinScreen screen;
    private String editingPackId, reorderingPackId;
    private boolean pendingRefresh;
    private String pendingPackId, pendingSkinId;

    CustomSkinPackFlow(AbstractChangeSkinScreen screen) {
        this.screen = screen;
    }

    void openOptionsScreen() {
        if (screen.minecraft == null) return;
        Screen rootParent = screen.rootParentScreen();
        String customPackId = isReordering() ? reorderingPackId : focusedCustomPackId();
        String downloadedPackId = customPackId == null ? focusedDownloadedPackId() : null;
        screen.minecraft.setScreen(new ConfirmationScreen(screen, ConfirmationScreen::getPanelWidth,
                () -> optionsHeight(customPackId, downloadedPackId),
                LegacyComponents.CUSTOM_SKIN_PACK_OPTIONS,
                Component.translatable("legacy.menu.custom_skin_pack_options_message"),
                s -> {
                }) {
            @Override
            protected void addButtons() {
                if (downloadedPackId != null && customPackId == null) {
                    renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.remove_pack"), button -> openPackRemoveScreen(rootParent, customPackId, downloadedPackId)).build());
                }
                if (!isEditing() && !isReordering()) {
                    renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.add_custom_skin_pack"), button -> openCreateScreen(rootParent)).build());
                }
                if (customPackId != null) {
                    renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.edit_custom_skin_pack"), button -> openEditScreen(rootParent, customPackId)).build());
                    renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.remove_custom_skin_pack"), button -> openPackRemoveScreen(rootParent, customPackId, downloadedPackId)).build());
                }
                if (isReordering()) {
                    renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.done_reordering"), button -> finishReorderingPack()).build());
                } else if (customPackId != null || isEditing()) {
                    Component label = isEditing()
                            ? Component.translatable("legacy.menu.done_editing")
                            : Component.translatable("legacy.menu.edit_custom_skin_pack_skins");
                    renderableVList.addRenderable(Button.builder(label, button -> toggleEditMode(customPackId)).build());
                }
            }
        });
        screen.playPressSound();
    }

    boolean isEditing(String packId) {
        return packId != null && packId.equals(editingPackId);
    }

    boolean isEditing() {
        return editingPackId != null;
    }

    void setEditing(String packId) {
        editingPackId = packId == null || packId.isBlank() ? null : packId;
        if (editingPackId != null) reorderingPackId = null;
    }

    boolean isReordering() {
        return reorderingPackId != null;
    }

    void setReordering(String packId) {
        reorderingPackId = packId == null || packId.isBlank() ? null : packId;
        if (reorderingPackId != null) editingPackId = null;
    }

    String reorderingPackId() {
        return reorderingPackId;
    }

    void queueRefresh(String packId, String skinId) {
        pendingRefresh = true;
        pendingPackId = packId;
        pendingSkinId = skinId;
        if (packId != null && !packId.isBlank()) editingPackId = packId;
    }

    void applyPendingRefresh() {
        if (!pendingRefresh || screen.minecraft == null || screen.minecraft.getResourceManager() == null) return;
        pendingRefresh = false;
        String packId = pendingPackId;
        String skinId = pendingSkinId;
        pendingPackId = null;
        pendingSkinId = null;
        reloadCustomPackResources(packId, skinId);
    }

    void handlePackReload() {
        if (editingPackId != null && !CustomSkinPackStore.isCustomPack(screen.minecraft, editingPackId))
            editingPackId = null;
        if (reorderingPackId != null && !CustomSkinPackStore.isCustomPack(screen.minecraft, reorderingPackId))
            reorderingPackId = null;
    }

    boolean moveReorderingPack(int delta) {
        return moveReorderingPack(delta, true);
    }

    boolean moveReorderingPack(int delta, boolean playSound) {
        if (screen.minecraft == null || delta == 0 || !isReordering()) return false;
        if (reorderingPackId != null) screen.packList.focusPackId(reorderingPackId, false);
        if (!screen.packList.moveFocusedPack(delta)) return false;
        reorderingPackId = screen.packList.getFocusedPackId();
        screen.rebuildScreenWidgets();
        syncReorderingPackList();
        if (playSound) screen.playPressSound();
        return true;
    }

    boolean moveReorderingPackTo(int targetIndex) {
        if (screen.minecraft == null || !isReordering()) return false;
        if (reorderingPackId != null) screen.packList.focusPackId(reorderingPackId, false);
        if (!screen.packList.moveFocusedPackTo(targetIndex)) return false;
        reorderingPackId = screen.packList.getFocusedPackId();
        screen.rebuildScreenWidgets();
        syncReorderingPackList();
        screen.playPressSound();
        return true;
    }

    void finishReorderingPack() {
        if (screen.minecraft == null || !isReordering()) return;
        String packId = reorderingPackId;
        try {
            SkinPackLoader.saveCustomPackOrder(screen.minecraft, screen.packList.orderedPackIds());
            reorderingPackId = null;
            SkinsClientBootstrap.reloadChangeSkinScreen(screen.minecraft, screen.rootParentScreen(), packId, null);
        } catch (IOException ex) {
            screen.showError(Component.translatable("legacy.menu.reorder_custom_skin_pack"), ex);
        }
    }

    private SkinPack focusedPack() {
        return screen.packList.getFocusedPack();
    }

    String focusedCustomPackId() {
        SkinPack focusedPack = focusedPack();
        return focusedPack != null && focusedPack.editable() ? focusedPack.id() : null;
    }

    String focusedDownloadedPackId() {
        if (screen.minecraft == null) return null;
        SkinPack focusedPack = focusedPack();
        if (focusedPack == null || focusedPack.editable()) return null;
        return DownloadedSkinPackStore.isDownloadedPack(screen.minecraft, focusedPack.id()) ? focusedPack.id() : null;
    }

    boolean isImportSkinSelection(String skinId) {
        String packId = focusedCustomPackId();
        return CustomSkinPackStore.isImportSkin(packId, skinId);
    }

    boolean isEditableSkinSelection(String skinId) {
        String packId = focusedCustomPackId();
        return packId != null && skinId != null && !skinId.isBlank() && !CustomSkinPackStore.isImportSkin(packId, skinId);
    }

    boolean isLockedSkinSelection(String skinId) {
        return isEditing() && skinId != null && !skinId.isBlank() && !isImportSkinSelection(skinId) && !isEditableSkinSelection(skinId);
    }

    private String selectedSkinId() {
        PlayerSkinWidget center = screen.getCenterWidget();
        return center == null ? null : center.skinId.get();
    }

    void editSelectedSkin() {
        if (screen.minecraft == null) return;
        String packId = focusedCustomPackId();
        String skinId = selectedSkinId();
        if (packId == null || skinId == null || skinId.isBlank() || isLockedSkinSelection(skinId)) return;
        if (CustomSkinPackStore.isImportSkin(packId, skinId)) {
            screen.openImportSkinScreen(packId, savedSkinId -> queueRefresh(packId, savedSkinId));
            return;
        }
        SkinEntry skin = SkinPackLoader.getSkin(skinId);
        if (skin == null) return;
        String theme = skin.modelId() == null ? null : BoxModelManager.getThemeText(skin.modelId());
        ArrayList<String> poses = new ArrayList<>(BoxModelManager.getPoseKeys(skinId));
        Boolean slim = skin.modelId() == null ? null : BoxModelManager.getSlimFlag(skin.modelId());
        if (slim == null && skin.slimArms()) slim = true;
        if (Boolean.TRUE.equals(slim) && !poses.contains("slim")) poses.add(0, "slim");
        screen.minecraft.setScreen(ImportCustomSkinScreen.edit(screen, screen.rootParentScreen(), packId, skin.id(), skin.name(), theme, poses, savedSkinId -> queueRefresh(packId, savedSkinId)));
        screen.playPressSound();
    }

    void moveSelectedSkin(int delta) {
        if (screen.minecraft == null || delta == 0) return;
        String packId = focusedCustomPackId();
        String skinId = selectedSkinId();
        if (packId == null || skinId == null || skinId.isBlank() || !isEditableSkinSelection(skinId)) return;
        try {
            CustomSkinPackStore.moveSkin(screen.minecraft, packId, skinId, delta);
            reloadCustomPackResources(packId, skinId);
            screen.packList.refreshPackIdsIfNeeded();
            screen.rebuildScreenWidgets();
            PlayerSkinWidget current = screen.getCenterWidget();
            if (current != null) current.hintMove(delta);
            screen.playPressSound();
        } catch (IOException ex) {
            screen.showError(Component.translatable("legacy.menu.edit_custom_skin_pack_skins"), ex);
        }
    }

    void openDeleteSelectedSkin() {
        String packId = focusedCustomPackId();
        String skinId = selectedSkinId();
        if (packId == null || skinId == null || skinId.isBlank() || !isEditableSkinSelection(skinId)) return;
        openRemoveScreen(Component.translatable("legacy.menu.delete_custom_skin_title"), Component.translatable("legacy.menu.delete_custom_skin_message"), () -> deleteSelectedSkin(packId, skinId));
        screen.playPressSound();
    }

    private int optionsHeight(String customPackId, String downloadedPackId) {
        boolean sd = LegacyOptions.getUIMode().isSD();
        int buttonStep = sd ? 19 : 22;
        int count = 0;
        if (downloadedPackId != null && customPackId == null) count++;
        if (!isEditing() && !isReordering()) count++;
        if (customPackId != null) count += 2;
        if (customPackId != null || isEditing() || isReordering()) count++;
        int height = sd ? 50 : 75;
        return height + Math.max(0, count - 1) * buttonStep;
    }

    private void openCreateScreen(Screen rootParent) {
        if (screen.minecraft != null) screen.minecraft.setScreen(CreateCustomSkinPackScreen.create(screen, rootParent));
    }

    private void openEditScreen(Screen rootParent, String packId) {
        if (screen.minecraft == null || packId == null) return;
        SkinPack pack = focusedPack();
        if (pack == null || !packId.equals(pack.id())) pack = SkinPackLoader.getPacks().get(packId);
        screen.minecraft.setScreen(CreateCustomSkinPackScreen.edit(screen, rootParent, packId, pack == null ? packId : pack.name()));
    }

    private void toggleEditMode(String packId) {
        if (screen.minecraft == null) return;
        setEditing(isEditing() ? null : packId);
        screen.minecraft.setScreen(screen);
    }

    private void openPackRemoveScreen(Screen rootParent, String customPackId, String downloadedPackId) {
        String packId = customPackId != null ? customPackId : downloadedPackId;
        if (packId == null) return;
        openRemoveScreen(Component.translatable("legacy.menu.delete_custom_skin_pack_title").withStyle(ChatFormatting.BOLD), Component.translatable("legacy.menu.delete_custom_skin_pack_message"), () -> removePack(rootParent, customPackId, downloadedPackId));
    }

    private void removePack(Screen rootParent, String customPackId, String downloadedPackId) {
        if (screen.minecraft == null) return;
        String packId = customPackId != null ? customPackId : downloadedPackId;
        if (packId == null) return;
        try {
            if (customPackId != null) {
                CustomSkinPackStore.deletePack(screen.minecraft, customPackId);
                if (customPackId.equals(SkinPackLoader.getLastUsedCustomPackId()))
                    SkinPackLoader.setLastUsedCustomPackId(null);
            } else {
                DownloadedSkinPackStore.deletePack(screen.minecraft, packId);
            }
            SkinPackLoader.requestFocusPack(SkinPackLoader.getPreferredDefaultPackId());
            SkinsClientBootstrap.reloadChangeSkinScreen(screen.minecraft, rootParent);
        } catch (IOException ex) {
            screen.showError(Component.translatable(customPackId != null ? "legacy.menu.remove_custom_skin_pack" : "legacy.menu.remove_pack"), ex);
        }
    }

    void syncReorderingPackList() {
        if (!isReordering() || reorderingPackId == null) return;
        screen.packList.focusPackId(reorderingPackId, false);
        ChangeSkinPackList.PackButton button = screen.findPackButton(screen.packList.getFocusedPackIndex());
        if (button == null) return;
        screen.getRenderableVList().focusRenderable(button);
    }

    private void reloadCustomPackResources(String packId, String skinId) {
        if (screen.minecraft == null || screen.minecraft.getResourceManager() == null) return;
        releasePackResources(focusedPack());
        ClientSkinAssets.clearPreviewCaches();
        ClientSkinAssets.clearPreviewWarmup();
        BoxModelManager.reload(screen.minecraft.getResourceManager());
        SkinPackLoader.loadPacks(screen.minecraft.getResourceManager());
        SkinSyncClient.onSkinAssetsReloaded(screen.minecraft);
        screen.seenPackReloadVersion = SkinPackLoader.getReloadVersion();
        if (packId != null && !packId.isBlank()) {
            screen.packList.refreshPackIdsIfNeeded();
            screen.packList.focusPackId(packId, false);
            editingPackId = packId;
        }
        SkinPackLoader.requestFocusSkin(skinId);
    }

    private void releasePackResources(SkinPack pack) {
        if (screen.minecraft == null || pack == null) return;
        if (pack.icon() != null) screen.minecraft.getTextureManager().release(pack.icon());
        if (pack.skins() == null) return;
        for (SkinEntry entry : pack.skins()) {
            if (entry == null) continue;
            if (entry.texture() != null) screen.minecraft.getTextureManager().release(entry.texture());
            if (entry.cape() != null) screen.minecraft.getTextureManager().release(entry.cape());
        }
    }

    private void deleteSelectedSkin(String packId, String skinId) {
        if (screen.minecraft == null) return;
        try {
            SkinPack pack = SkinPackLoader.getPacks().get(packId);
            List<SkinEntry> skins = pack == null || pack.skins() == null ? List.of() : pack.skins();
            String nextSkinId = skinId;
            for (int i = 0; i < skins.size(); i++) {
                SkinEntry skin = skins.get(i);
                if (skin == null || !skinId.equals(skin.id())) continue;
                if (i + 1 < skins.size() && skins.get(i + 1) != null) nextSkinId = skins.get(i + 1).id();
                else if (i > 0 && skins.get(i - 1) != null) nextSkinId = skins.get(i - 1).id();
                break;
            }
            CustomSkinPackStore.deleteSkin(screen.minecraft, packId, skinId);
            reloadCustomPackResources(packId, nextSkinId);
            screen.packList.refreshPackIdsIfNeeded();
            screen.rebuildScreenWidgets();
        } catch (IOException ex) {
            screen.showError(Component.translatable("legacy.menu.remove_custom_skin"), ex);
        }
    }

    private void openRemoveScreen(Component title, Component message, Runnable action) {
        if (screen.minecraft == null) return;
        screen.minecraft.setScreen(new ConfirmationScreen(screen, ConfirmationScreen::getPanelWidth, ConfirmationScreen::getBaseHeight, title, message, s -> action.run()) {
            @Override
            protected void addButtons() {
                renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), button -> onClose()).build());
                renderableVList.addRenderable(okButton = Button.builder(LegacyComponents.REMOVE, button -> okAction.accept(this)).build());
            }
        });
    }
}
