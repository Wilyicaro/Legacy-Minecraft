package wily.legacy.client;

import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.network.chat.Component;

public interface BackupConfirmScreenAccessor {
    static BackupConfirmScreenAccessor of(BackupConfirmScreen screen){
        return (BackupConfirmScreenAccessor) screen;
    }

    void proceed(boolean bl, boolean bl2);

    boolean hasCacheErase();

    Component getDescription();

    void cancel();
}
