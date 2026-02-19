package wily.legacy.CustomModelSkins.cpl.util;

import net.minecraft.network.chat.Component;

import java.io.IOException;

public class LocalizedIOException extends IOException implements LocalizedException {
    private static final long serialVersionUID = -6332369839511402034L;
    private Component loc;

    public LocalizedIOException(String msg, Component loc) {
        super(msg);
        this.loc = loc;
    }

    @Override
    public Component getLocalizedText() {
        return loc;
    }
}
