package wily.legacy.mixin.base;

import net.minecraft.client.Screenshot;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.ScreenshotToast;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.ScreenUtil;

import java.util.function.Consumer;

@Mixin(Screenshot.class)
public abstract class ScreenshotMixin {
    @ModifyVariable(method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V", at = @At("HEAD"), argsOnly = true)
    private static Consumer<Component> legacy$screenshot(Consumer<Component> original) {
        return component -> {
            if (LegacyOptions.screenshotToasts.get()) {
                String fileName = ((Component)((TranslatableContents)component.getContents()).getArgs()[0]).getString();
                Minecraft.getInstance().execute(() -> {
                    Legacy4J.LOGGER.info("Saved screenshot as " + fileName);
                    ScreenUtil.playSimpleUISound(LegacyRegistries.SCREENSHOT.get(), 1.0f);
                    ScreenshotToast.newScreenshot(new ScreenshotToast(fileName));
                });
            } else {
                original.accept(component);
            }
        };
    }
}
