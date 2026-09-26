package wily.legacy.client;

import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.resources.Identifier;

public interface AdvancementToastAccessor {
    static AdvancementToastAccessor of(AdvancementToast toast) {
        return (AdvancementToastAccessor) toast;
    }

    Identifier getAdvancementId();
}
