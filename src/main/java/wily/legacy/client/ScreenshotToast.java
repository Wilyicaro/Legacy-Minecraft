package wily.legacy.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.Legacy4J;
import wily.legacy.util.client.LegacyRenderUtil;

import java.io.InputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScreenshotToast {
    private static final Component SCREENSHOT_MESSAGE = Component.translatable("legacy.menu.screenshot");
    private static final int MAX_TOAST_TIME = 150;
    private static final int TOAST_WIDTH = 190;
    private static final int TOAST_HEIGHT = 40;
    private static final ResourceLocation DEFAULT_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");
    private static final String SCREENSHOTS_FOLDER = Minecraft.getInstance().gameDirectory + "/screenshots/";

    private static List<ScreenshotToast> queuedScreenshots = new ArrayList<>();
    private static float toastTime = 0f;
    private static ResourceLocation screenshotImage = null;
    private static int screenshotWidth = 128;
    private static int screenshotHeight = 128;

    private String screenshotName;

    public ScreenshotToast(String screenshotName) {
        this.screenshotName = screenshotName;
    }

    public static void newScreenshot(ScreenshotToast screenshotToast) {
        queuedScreenshots.add(screenshotToast);
    }

    public static void render(GuiGraphics graphics) {
        if (toastTime <= 0) toastTime = (float) (MAX_TOAST_TIME + (TOAST_HEIGHT * 2));
        if (!queuedScreenshots.isEmpty()) {
            ResourceLocation screenshotLocation = DEFAULT_ICON;
            ScreenshotToast screenshotToast = queuedScreenshots.getFirst();
            if (screenshotImage == null) screenshotImage = getScreenshot(screenshotToast.screenshotName);
            screenshotLocation = screenshotImage;
            int screenWidth = graphics.guiWidth();
            int screenHeight = graphics.guiHeight();


            float deltaTime = (Minecraft.getInstance().getDeltaTracker()).getRealtimeDeltaTicks();

            toastTime -= deltaTime * 3;

            int heightOffset = 0;
            if (toastTime > MAX_TOAST_TIME + TOAST_HEIGHT)
                heightOffset = ((int) toastTime - (MAX_TOAST_TIME + TOAST_HEIGHT));
            if (toastTime < TOAST_HEIGHT) heightOffset = ((int) toastTime - TOAST_HEIGHT) * -1;

            int toastX = screenWidth - TOAST_WIDTH;
            int toastY = (screenHeight - TOAST_HEIGHT) + heightOffset;

            LegacyRenderUtil.renderPointerPanel(graphics, toastX, toastY, TOAST_WIDTH, TOAST_HEIGHT);
            graphics.blit(RenderPipelines.GUI_TEXTURED, screenshotLocation, toastX + 4, toastY + 4, (float)((screenshotWidth/2)-(screenshotHeight/2)), 0, 32, 32, screenshotHeight, screenshotHeight, screenshotWidth, screenshotHeight);
            graphics.drawString(Minecraft.getInstance().font, SCREENSHOT_MESSAGE, toastX + 40, toastY + 8, 0xFFFFFFFF);
            graphics.drawString(Minecraft.getInstance().font, screenshotToast.screenshotName, toastX + 40, toastY + 24, 0xFFFFFFFF);

            if (toastTime <= 0) {
                queuedScreenshots.removeFirst();
                screenshotImage = null;
            }
        }
    }

    private static ResourceLocation getScreenshot(String screenshot) {
        try {
            screenshotWidth = 128;
            screenshotHeight = 128;
            ResourceLocation screenshotLocation = DEFAULT_ICON;
            Path screenshotPath = Paths.get(SCREENSHOTS_FOLDER + screenshot);
            InputStream screenshotInputStream = screenshotPath.getFileSystem().provider().newInputStream(screenshotPath, new OpenOption[] {StandardOpenOption.READ});

            ResourceLocation screenshotResourceLocation = ResourceLocation.withDefaultNamespace("screenshot/toast/" + screenshot);
            NativeImage nativeImage = NativeImage.read(screenshotInputStream);

            screenshotWidth = nativeImage.getWidth();
            screenshotHeight = nativeImage.getHeight();

            Objects.requireNonNull(screenshotResourceLocation);
            Minecraft.getInstance().getTextureManager().register(screenshotResourceLocation, new DynamicTexture(screenshotResourceLocation::toString, nativeImage));
            screenshotLocation = screenshotResourceLocation;

            return screenshotLocation;
        } catch (Exception exception) {
            Legacy4J.LOGGER.warn("Failed to load screenshot", exception);
            return DEFAULT_ICON;
        }
    }
}
