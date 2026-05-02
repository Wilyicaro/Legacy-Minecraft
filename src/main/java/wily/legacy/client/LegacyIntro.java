package wily.legacy.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4J;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.IOUtil;

import java.util.Collections;
import java.util.List;

public record LegacyIntro(List<Identifier> brands, Identifier background, float brandDuration, float fadeIn,
                          float fadeOut, boolean crossFade) {
    public static final Identifier DEFAULT_BACKGROUND = Legacy4J.createModLocation("textures/gui/intro/background.png");
    public static final LegacyIntro EMPTY = new LegacyIntro(Collections.emptyList());
    public static final Codec<LegacyIntro> COMPLETE_CODEC = RecordCodecBuilder.create(i -> i.group(Identifier.CODEC.listOf().fieldOf("brands").forGetter(LegacyIntro::brands), Identifier.CODEC.fieldOf("background").orElse(DEFAULT_BACKGROUND).forGetter(LegacyIntro::background), Codec.FLOAT.fieldOf("brandDuration").forGetter(LegacyIntro::brandDuration), Codec.FLOAT.fieldOf("fadeIn").orElse(0.4f).forGetter(LegacyIntro::fadeIn), Codec.FLOAT.fieldOf("fadeOut").orElse(0.4f).forGetter(LegacyIntro::fadeOut), Codec.BOOL.fieldOf("crossFade").orElse(false).forGetter(LegacyIntro::crossFade)).apply(i, LegacyIntro::new));
    public static final Codec<LegacyIntro> CODEC = IOUtil.createFallbackCodec(COMPLETE_CODEC, Identifier.CODEC.listOf().xmap(LegacyIntro::new, LegacyIntro::brands));
    public LegacyIntro(List<Identifier> brands) {
        this(brands, DEFAULT_BACKGROUND, 3200, 0.4f, 0.4f, false);
    }

    public static float getTimer(long initTime, LegacyIntro intro) {
        return (Util.getMillis() - initTime) / intro.brandDuration();
    }

    public static boolean canSkip(float timer, LegacyIntro intro) {
        return timer >= intro.brands().size() || LegacyOptions.skipIntro.get() || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), InputConstants.KEY_RETURN) || GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS || ControllerBinding.DOWN_BUTTON.state().pressed;
    }

    public static void render(GuiGraphicsExtractor GuiGraphicsExtractor, LegacyIntro intro, float timer) {
        GuiGraphicsExtractor.fill(0, 0, GuiGraphicsExtractor.guiWidth(), GuiGraphicsExtractor.guiHeight(), 0xFFFFFFFF);
        if (intro.brands.isEmpty()) return;
        int actual = (int) (timer % intro.brands().size());
        float last = (float) Math.ceil(timer) - timer;
        if (intro.crossFade() && last <= intro.fadeOut() && actual + 1 < intro.brands().size()) {
            FactoryGuiGraphics.of(GuiGraphicsExtractor).blit(intro.brands().get(actual + 1), (GuiGraphicsExtractor.guiWidth() - GuiGraphicsExtractor.guiHeight() * 320 / 180) / 2, 0, 0, 0, GuiGraphicsExtractor.guiHeight() * 320 / 180, GuiGraphicsExtractor.guiHeight(), GuiGraphicsExtractor.guiHeight() * 320 / 180, GuiGraphicsExtractor.guiHeight());
        } else
            FactoryGuiGraphics.of(GuiGraphicsExtractor).blit(intro.background(), 0, 0, 0, 0, GuiGraphicsExtractor.guiWidth(), GuiGraphicsExtractor.guiHeight(), GuiGraphicsExtractor.guiWidth(), GuiGraphicsExtractor.guiHeight());
        float alpha = last <= intro.fadeOut() ? last / intro.fadeOut() : last > 1 - intro.fadeIn() && !intro.crossFade() ? (1 - last) / intro.fadeIn() : 1.0f;
        FactoryGuiGraphics.of(GuiGraphicsExtractor).setBlitColor(1.0f, 1.0f, 1.0f, alpha);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blit(intro.brands().get(actual), (GuiGraphicsExtractor.guiWidth() - GuiGraphicsExtractor.guiHeight() * 320 / 180) / 2, 0, 0, 0, GuiGraphicsExtractor.guiHeight() * 320 / 180, GuiGraphicsExtractor.guiHeight(), GuiGraphicsExtractor.guiHeight() * 320 / 180, GuiGraphicsExtractor.guiHeight());
        FactoryGuiGraphics.of(GuiGraphicsExtractor).clearBlitColor();
    }
}