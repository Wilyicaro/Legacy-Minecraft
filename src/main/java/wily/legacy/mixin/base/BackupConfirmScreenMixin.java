package wily.legacy.mixin.base;

import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.client.BackupConfirmScreenAccessor;

@Mixin(BackupConfirmScreen.class)
public class BackupConfirmScreenMixin implements BackupConfirmScreenAccessor {
    @Shadow @Final protected BackupConfirmScreen.Listener /*? if >1.20.2 {*/onProceed/*?} else {*//*listener*//*?}*/;

    @Shadow @Final public boolean promptForCacheErase;

    @Shadow @Final public Component description;

    @Override
    public void proceed(boolean bl, boolean bl2) {
        /*? if >1.20.2 {*/onProceed/*?} else {*//*listener*//*?}*/.proceed(bl,bl2);
    }
    //? if >1.20.2 {
    @Shadow @Final protected Runnable onCancel;
    @Override
    public void cancel() {
        onCancel.run();
    }
    //?}
    @Override
    public boolean hasCacheErase() {
        return promptForCacheErase;
    }

    @Override
    public Component getDescription() {
        return description;
    }
}
