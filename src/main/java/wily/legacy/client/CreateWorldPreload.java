package wily.legacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import org.jetbrains.annotations.Nullable;
import wily.legacy.Legacy4J;

import java.util.concurrent.CompletableFuture;

public class CreateWorldPreload {
    private static boolean capturing;
    private static CompletableFuture<WorldCreationContext> preload;

    public static boolean isEnabled(Minecraft minecraft) {
        return !minecraft.isDemo() && LegacyMixinOptions.legacyCreateWorldScreen.get();
    }

    public static void start(Minecraft minecraft) {
        if (!isEnabled(minecraft) || preload != null) return;
        capturing = true;
        try {
            CreateWorldScreen.openFresh(minecraft, () -> {});
        } catch (RuntimeException exception) {
            Legacy4J.LOGGER.warn("Couldn't preload world creation", exception);
        } finally {
            capturing = false;
        }
    }

    public static boolean isCapturing() {
        return capturing;
    }

    public static void capture(CompletableFuture<WorldCreationContext> future) {
        preload = future.whenComplete((context, throwable) -> {
            if (throwable != null) Legacy4J.LOGGER.warn("Couldn't preload world creation", throwable);
        });
    }

    public static CompletableFuture<?> whenSettled(Minecraft minecraft) {
        start(minecraft);
        return preload == null ? CompletableFuture.completedFuture(null) : preload.handle((context, throwable) -> null);
    }

    public static boolean isSettled(Minecraft minecraft) {
        return whenSettled(minecraft).isDone();
    }

    @Nullable
    public static WorldCreationContext getNow() {
        return preload != null && preload.isDone() && !preload.isCompletedExceptionally() ? preload.join() : null;
    }
}
