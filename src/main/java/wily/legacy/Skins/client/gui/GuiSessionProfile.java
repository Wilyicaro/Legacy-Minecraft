package wily.legacy.Skins.client.gui;

import wily.legacy.Skins.client.util.SessionSkinStore;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class GuiSessionProfile {
    private GuiSessionProfile() {
    }

    private static void cacheSessionTexturesProperty(GameProfile profile) {
        try {
            String b64 = ProfilePropertyUtil.getTexturesB64(profile);
            if (b64 != null && !b64.isBlank()) {
                SessionSkinStore.saveTexturesB64(b64);
            }
        } catch (Throwable ignored) {
        }
    }
}
