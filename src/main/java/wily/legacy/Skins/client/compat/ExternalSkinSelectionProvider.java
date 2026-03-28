package wily.legacy.Skins.client.compat;

public interface ExternalSkinSelectionProvider extends ExternalSkinProvider {

    String getCurrentSelectedSkinId();

    boolean applySelectedSkin(String skinId);

    void clearSelectedSkin();

    void importCurrentSelectionIfAbsent();

    default boolean canResolveSelection(String skinId) {
        return skinId != null && !skinId.isBlank() && ownsSkinId(skinId);
    }

    default boolean hasSelectedSkin() {
        String selectedId = getCurrentSelectedSkinId();
        return selectedId != null && !selectedId.isBlank();
    }
}
