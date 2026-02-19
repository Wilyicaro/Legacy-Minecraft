package wily.legacy.CustomModelSkins.cpm;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import wily.legacy.CustomModelSkins.cpm.shared.MinecraftCommonAccess;

public abstract class CommonBase implements MinecraftCommonAccess {
    @Override
    public String getMCVersion() {
        return SharedConstants.getCurrentVersion().name();
    }

    protected static MutableComponent t(String k) {
        return Component.translatable(k);
    }

    protected static MutableComponent t(String k, Object... args) {
        return Component.translatable(k, args);
    }

    protected static Style st() {
        return Style.EMPTY;
    }
}
