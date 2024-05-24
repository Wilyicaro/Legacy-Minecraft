package wily.legacy.mixin;

import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.ScreenUtil;

@Mixin(PersistentEntitySectionManager.class)
public class ClientPersistentEntitySectionManagerMixin {
    @Inject(method = "processUnloads", at = @At("HEAD"), cancellable = true)
    private void processUnloads(CallbackInfo ci){
        if (!ScreenUtil.getLegacyOptions().autoSave().get()) ci.cancel();
    }
}
