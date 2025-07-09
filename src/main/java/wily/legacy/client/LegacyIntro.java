package wily.legacy.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4J;
import wily.legacy.client.controller.ControllerBinding;

import java.util.Collections;
import java.util.List;

public record LegacyIntro(List<ResourceLocation> brands, ResourceLocation background, float brandDuration, float fadeIn, float fadeOut, boolean crossFade) {
    public static final ResourceLocation DEFAULT_BACKGROUND = Legacy4J.createModLocation("textures/gui/intro/background.png");
    public LegacyIntro(List<ResourceLocation> brands){
        this(brands, DEFAULT_BACKGROUND, 3200, 0.4f, 0.4f, false);
    }
    public static final LegacyIntro EMPTY = new LegacyIntro(Collections.emptyList());
    public static final Codec<LegacyIntro> COMPLETE_CODEC = RecordCodecBuilder.create(i-> i.group(ResourceLocation.CODEC.listOf().fieldOf("brands").forGetter(LegacyIntro::brands), ResourceLocation.CODEC.fieldOf("background").orElse(DEFAULT_BACKGROUND).forGetter(LegacyIntro::background), Codec.FLOAT.fieldOf("brandDuration").forGetter(LegacyIntro::brandDuration), Codec.FLOAT.fieldOf("fadeIn").orElse(0.4f).forGetter(LegacyIntro::fadeIn), Codec.FLOAT.fieldOf("fadeOut").orElse(0.4f).forGetter(LegacyIntro::fadeOut), Codec.BOOL.fieldOf("crossFade").orElse(false).forGetter(LegacyIntro::crossFade)).apply(i, LegacyIntro::new));
    public static final Codec<LegacyIntro> CODEC = Codec.either(ResourceLocation.CODEC.listOf().xmap(LegacyIntro::new, LegacyIntro::brands), COMPLETE_CODEC).xmap(e-> e.right().orElseGet(e.left()::get), Either::right);

    public static float getTimer(long initTime, LegacyIntro intro){
        return (Util.getMillis() - initTime) / intro.brandDuration();
    }

    public static boolean canSkip(float timer, LegacyIntro intro){
        return timer >= intro.brands().size() || LegacyOptions.skipIntro.get() || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_RETURN) || GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS || ControllerBinding.DOWN_BUTTON.state().pressed;
    }

    public static void render(GuiGraphics guiGraphics, LegacyIntro intro, float timer){
        guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), 0xFFFFFFFF);
        int actual = (int) (timer % intro.brands().size());
        float last = (float) Math.ceil(timer) - timer;
        if (intro.crossFade() && last <= intro.fadeOut() && actual + 1 < intro.brands().size()){
            FactoryGuiGraphics.of(guiGraphics).blit(intro.brands().get(actual + 1), (guiGraphics.guiWidth() - guiGraphics.guiHeight() * 320 / 180) / 2, 0, 0, 0, guiGraphics.guiHeight() * 320 / 180, guiGraphics.guiHeight(), guiGraphics.guiHeight() * 320 / 180, guiGraphics.guiHeight());
        } else FactoryGuiGraphics.of(guiGraphics).blit(intro.background(), 0, 0, 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), guiGraphics.guiWidth(), guiGraphics.guiHeight());
        float alpha = last <= intro.fadeOut() ? last / intro.fadeOut() : last > 1 - intro.fadeIn() && !intro.crossFade() ? (1 - last) / intro.fadeIn() : 1.0f;
        FactoryGuiGraphics.of(guiGraphics).setBlitColor(1.0f, 1.0f, 1.0f, alpha);
        FactoryGuiGraphics.of(guiGraphics).blit(intro.brands().get(actual), (guiGraphics.guiWidth() - guiGraphics.guiHeight() * 320 / 180) / 2, 0, 0, 0, guiGraphics.guiHeight() * 320 / 180, guiGraphics.guiHeight(), guiGraphics.guiHeight() * 320 / 180, guiGraphics.guiHeight());
        FactoryGuiGraphics.of(guiGraphics).clearBlitColor();
    }
}