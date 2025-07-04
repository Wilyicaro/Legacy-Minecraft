package wily.legacy.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.util.client.LegacyRenderUtil;

public class AnimatedCharacterRenderer {
    public static long animatedCharacterTime;
    public static long remainingAnimatedCharacterTime;

    public static void render(GuiGraphics guiGraphics){
        if (!LegacyOptions.animatedCharacter.get()) return;
        if (Minecraft.getInstance().getCameraEntity() instanceof LivingEntity character) {
            boolean hasRemainingTime = character.isSprinting() || character.isCrouching() || character.isFallFlying() || character.isVisuallySwimming() || !(character instanceof Player);
            if ((hasRemainingTime || character instanceof Player p && p.getAbilities().flying) && !character.isSleeping()) {
                LegacyRenderUtil.updateAnimatedCharacterTime(450);
            }
            if (Util.getMillis() - animatedCharacterTime <= remainingAnimatedCharacterTime) {
                float xRot = character.getXRot();
                float xRotO = character.xRotO;
                if (!character.isFallFlying()) character.setXRot(character.xRotO = -2.5f);
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(32f, character.isFallFlying() ? 44 : 18);
                float f = LegacyOptions.smoothAnimatedCharacter.get() ? FactoryAPIClient.getPartialTick() : 0;
                ClientEntityAccessor.of(character).setAllowDisplayFireAnimation(false);
                if (character.isFallFlying()) guiGraphics.pose().translate(0, -character.getViewXRot(f) / 180 * 4);
                LegacyRenderUtil.renderEntity(guiGraphics, 10, 36, 30, 120, Math.round(36 / LegacyRenderUtil.getHUDScale()), new Vector3f(), new Quaternionf().rotationXYZ(-5* Mth.PI/180f, (165 -Mth.lerp(f, character.yBodyRotO, character.yBodyRot)) * Mth.PI/180f, Mth.PI), null, character);
                ClientEntityAccessor.of(character).setAllowDisplayFireAnimation(true);
                guiGraphics.pose().popMatrix();
                character.setXRot(xRot);
                character.xRotO = xRotO;
            }
        }
    }
}
