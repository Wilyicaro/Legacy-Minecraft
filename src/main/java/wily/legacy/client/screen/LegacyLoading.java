package wily.legacy.client.screen;

import net.minecraft.network.chat.Component;

public interface LegacyLoading {
    default LegacyLoadingRenderer getLoadingRenderer() {
        return LegacyLoadingRenderer.getInstance();
    }

    default float getProgress() {
        return getLoadingRenderer().progress;
    }

    default void setProgress(float progress) {
        getLoadingRenderer().progress = progress;
    }

    default Component getLoadingHeader() {
        return getLoadingRenderer().loadingHeader;
    }

    default void setLoadingHeader(Component loadingHeader) {
        getLoadingRenderer().loadingHeader = loadingHeader;
    }

    default Component getLoadingStage() {
        return getLoadingRenderer().loadingStage;
    }

    default void setLoadingStage(Component loadingStage) {
        getLoadingRenderer().loadingStage = loadingStage;
    }

    default boolean isGenericLoading() {
        return getLoadingRenderer().genericLoading;
    }

    default void setGenericLoading(boolean genericLoading) {
        getLoadingRenderer().genericLoading = genericLoading;
    }
}
