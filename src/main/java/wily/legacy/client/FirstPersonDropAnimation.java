package wily.legacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import wily.factoryapi.FactoryAPIClient;

import java.lang.ref.WeakReference;

public class FirstPersonDropAnimation {
    private static final float DURATION = 9.0F;
    private static float startTime = -DURATION;
    private static WeakReference<LocalPlayer> player = new WeakReference<>(null);

    public static void start() {
        player = new WeakReference<>(Minecraft.getInstance().player);
        startTime = time(FactoryAPIClient.getGamePartialTick(false));
    }

    public static boolean isActive(float partialTick) {
        float elapsed = time(partialTick) - startTime;
        return Minecraft.getInstance().player == player.get() && elapsed >= 0.0F && elapsed < DURATION;
    }

    public static float progress(float partialTick) {
        return Mth.clamp((time(partialTick) - startTime) / DURATION, 0.0f, 1.0f);
    }

    private static float time(float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0.0F : player.tickCount + partialTick;
    }
}
