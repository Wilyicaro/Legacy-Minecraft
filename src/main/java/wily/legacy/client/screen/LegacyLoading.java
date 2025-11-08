package wily.legacy.client.screen;

import net.minecraft.network.chat.Component;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.LegacyTipManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public interface LegacyLoading {

    float getProgress();

    void setProgress(float progress);

    Component getLoadingHeader();

    void setLoadingHeader(Component loadingHeader);

    Component getLoadingStage();

    void setLoadingStage(Component loadingStage);

    boolean isGenericLoading();

    void setGenericLoading(boolean genericLoading);
}
