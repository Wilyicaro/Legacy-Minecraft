package wily.legacy.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4J;
import wily.legacy.util.ScreenUtil;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScreenshotToast {
    private static final Component SCREENSHOT_MESSAGE = Component.translatable("legacy.menu.screenshot");
    private static final int MAX_TOAST_TIME = 150;
    private static final int TOAST_WIDTH = 190;
    private static final int TOAST_HEIGHT = 40;
    private static final ResourceLocation DEFAULT_ICON = FactoryAPI.createVanillaLocation("textures/misc/unknown_pack.png");

    private static final List<ScreenshotToast> queuedScreenshots = new ArrayList<>();
    private static float toastTime;
    private static ResourceLocation screenshotImage;
    private static int screenshotWidth = 128;
    private static int screenshotHeight = 128;

    private final String screenshotName;

    public ScreenshotToast(String screenshotName) {
        this.screenshotName = screenshotName;
    }

    public static void newScreenshot(ScreenshotToast screenshotToast) {
        queuedScreenshots.add(screenshotToast);
    }

    public static void render(GuiGraphics graphics) {
        if (queuedScreenshots.isEmpty()) return;
        if (toastTime <= 0) toastTime = MAX_TOAST_TIME + TOAST_HEIGHT * 2;

        ScreenshotToast screenshotToast = queuedScreenshots.get(0);
        if (screenshotImage == null) screenshotImage = getScreenshot(screenshotToast.screenshotName);

        float deltaTime = /*? if >=1.21 {*/FactoryAPIClient.getDeltaTracker().getRealtimeDeltaTicks()/*?} else {*//*Minecraft.getInstance().getDeltaFrameTime()*//*?}*/;
        toastTime -= deltaTime * 3;

        int heightOffset = 0;
        if (toastTime > MAX_TOAST_TIME + TOAST_HEIGHT) heightOffset = (int) toastTime - (MAX_TOAST_TIME + TOAST_HEIGHT);
        if (toastTime < TOAST_HEIGHT) heightOffset = ((int) toastTime - TOAST_HEIGHT) * -1;

        int toastX = graphics.guiWidth() - TOAST_WIDTH;
        int toastY = graphics.guiHeight() - TOAST_HEIGHT + heightOffset;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);
        ScreenUtil.renderPointerPanel(graphics, toastX, toastY, TOAST_WIDTH, TOAST_HEIGHT);
        FactoryGuiGraphics.of(graphics).blit(screenshotImage, toastX + 4, /*? if >=1.21.2 {*/toastX + 36, /*?}*/toastY + 4, /*? if >=1.21.2 {*/toastY + 36, /*?} else {*//*32, 32, *//*?}*/(screenshotWidth - screenshotHeight) / 2f, 0.0f, screenshotHeight, screenshotHeight, screenshotWidth, screenshotHeight);
        graphics.drawString(Minecraft.getInstance().font, SCREENSHOT_MESSAGE, toastX + 40, toastY + 8, 0xFFFFFFFF);
        graphics.drawString(Minecraft.getInstance().font, screenshotToast.screenshotName, toastX + 40, toastY + 24, 0xFFFFFFFF);
        graphics.pose().popPose();

        if (toastTime <= 0) {
            queuedScreenshots.remove(0);
            screenshotImage = null;
        }
    }

    private static ResourceLocation getScreenshot(String screenshot) {
        try {
            screenshotWidth = 128;
            screenshotHeight = 128;

            Path screenshotPath = Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots").resolve(screenshot);
            try (InputStream inputStream = Files.newInputStream(screenshotPath)) {
                NativeImage nativeImage = NativeImage.read(inputStream);
                ResourceLocation location = FactoryAPI.createVanillaLocation("screenshot/toast/" + screenshot);

                screenshotWidth = nativeImage.getWidth();
                screenshotHeight = nativeImage.getHeight();

                Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(/*? if >=1.21.5 {*//*location::toString, *//*?}*/nativeImage));
                return location;
            }
        } catch (Exception exception) {
            Legacy4J.LOGGER.warn("Failed to load screenshot", exception);
            return DEFAULT_ICON;
        }
    }
}
