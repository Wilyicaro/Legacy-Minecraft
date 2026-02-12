package wily.legacy.client;

import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.resources.*;

public interface AdvancementToastAccessor {
    static AdvancementToastAccessor of(AdvancementToast toast) {
        return (AdvancementToastAccessor) toast;
    }

    /*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ getAdvancementId();
}
