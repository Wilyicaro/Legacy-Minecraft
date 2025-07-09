package wily.legacy.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.Legacy4J;
import wily.legacy.util.client.LegacyRenderUtil;

public class AnimatedCharacterRenderer {
    public static long time;
    public static long remainingTime;

    public static void render(GuiGraphics guiGraphics){
        if (!LegacyOptions.animatedCharacter.get()) return;
        if (Minecraft.getInstance().getCameraEntity() instanceof LivingEntity character) {
            boolean hasRemainingTime = character.isSprinting() || character.isCrouching() || character.isFallFlying() || character.isVisuallySwimming() || !(character instanceof Player);
            if ((hasRemainingTime || character instanceof Player p && p.getAbilities().flying) && !character.isSleeping()) {
                updateTime(450);
            }
            if (Util.getMillis() - time <= remainingTime) {
                float xRot = character.getXRot();
                float xRotO = character.xRotO;
                if (!character.isFallFlying()) character.setXRot(character.xRotO = -2.5f);
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(32f , character.isFallFlying() ? 44 : 18);
                float f = LegacyOptions.smoothAnimatedCharacter.get() ? FactoryAPIClient.getPartialTick() : 0;
                ClientEntityAccessor.of(character).setAllowDisplayFireAnimation(false);
                guiGraphics.pose().translate(10 * (3f / LegacyRenderUtil.getHUDScale()), (character.isFallFlying() ? -character.getViewXRot(f) / 180 * 40 : 36) * (3f / LegacyRenderUtil.getHUDScale()));
                LegacyRenderUtil.renderEntity(guiGraphics, -guiGraphics.guiWidth(), -guiGraphics.guiHeight(), guiGraphics.guiWidth(), guiGraphics.guiHeight(), Math.round(36 / LegacyRenderUtil.getHUDScale()), new Vector3f(), new Quaternionf().rotationXYZ(-5* Mth.PI/180f, (165 -Mth.lerp(f, character.yBodyRotO, character.yBodyRot)) * Mth.PI/180f, Mth.PI), null, character);
                ClientEntityAccessor.of(character).setAllowDisplayFireAnimation(true);
                guiGraphics.pose().popMatrix();
                character.setXRot(xRot);
                character.xRotO = xRotO;
            }
        }
    }

    public static void updateTime(long remainingTime){
        time = Util.getMillis();
        AnimatedCharacterRenderer.remainingTime = remainingTime;
    }
}
