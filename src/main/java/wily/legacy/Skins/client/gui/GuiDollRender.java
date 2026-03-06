package wily.legacy.Skins.client.gui;

import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class GuiDollRender {
    private static final int DOLL_RENDER_STATE_ID = DollRenderIds.MENU_DOLL_ID;
    private static final float PREVIEW_BRIGHTNESS_MUL = 1.2F;
    private static volatile GpuBufferSlice cachedGuiEntityLights;
    private static volatile boolean triedResolveGuiEntityLights;
    private static volatile Method cachedInventoryEntitySetup;
    private static volatile boolean triedResolveInventoryEntitySetup;
    private static volatile Method cachedGuiSetColor;
    private static volatile boolean triedResolveGuiSetColor;
    private static volatile Method cachedRsSetColor;
    private static volatile boolean triedResolveRsSetColor;

    private GuiDollRender() {
    }

    private static float normalizeYaw(float yaw) {
        while (yaw < 0.0F) yaw += 360.0F;
        return (yaw + 180.0F) % 360.0F - 180.0F;
    }

    public static void renderDollInRect(GuiGraphics gui, String selectionId, PlayerSkin skin, float yawOffset, boolean crouching, float attackTime, float partialTick, int left, int top, int right, int bottom, int sizeCap) {
        final float BASE_BBOX_H = 1.8F;
        final float BASE_BBOX_W = 0.6F;
        final float BASE_TRANSLATE_Y = BASE_BBOX_H / 2.0F;
        AvatarRenderState state = new AvatarRenderState();
        state.id = DOLL_RENDER_STATE_ID;
        state.entityType = EntityType.PLAYER;
        state.lightCoords = 15728880;
        state.boundingBoxHeight = BASE_BBOX_H;
        state.boundingBoxWidth = BASE_BBOX_W;
        float yaw = normalizeYaw(yawOffset + 180.0F);
        state.bodyRot = yaw;
        state.yRot = yaw;
        state.xRot = 0.0F;
        state.pose = Pose.STANDING;
        state.isBaby = false;
        state.scale = 1.0F;
        state.ageScale = 1.0F;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showCape = false;
        state.attackArm = HumanoidArm.RIGHT;
        state.attackTime = attackTime;
        state.isCrouching = crouching;
        state.skin = skin;
        int baseHeight = bottom - top;
        int size = Math.min((int) (baseHeight / 2.75F), sizeCap);
        size = Math.max(size, 20);
        float renderScale = (float) size;
        float crouchComp = crouching ? -0.125F : 0.0F;
        Vector3f translate = new Vector3f(0.0F, BASE_TRANSLATE_Y + crouchComp, 0.0F);
        Quaternionf quat = new Quaternionf().rotationZ((float) Math.toRadians(180.0F));
        Quaternionf quat2 = new Quaternionf().rotationX(0);
        quat.mul(quat2);
        quat2.conjugate();
        int expandedTop = top - baseHeight;
        int expandedBottom = bottom + baseHeight;

        applyPreviewLighting(gui, PREVIEW_BRIGHTNESS_MUL);
        gui.submitEntityRenderState(state, renderScale, translate, quat, quat2, left, expandedTop, right, expandedBottom);
        resetPreviewLighting(gui);
    }

    public static void renderDollInRect(GuiGraphics gui, String selectionId, ResourceLocation skinTexture, float yawOffset, boolean crouching, float attackTime, float partialTick, int left, int top, int right, int bottom, int sizeCap) {
        final float BASE_BBOX_H = 1.8F;
        final float BASE_BBOX_W = 0.6F;
        float bboxH = 1.8F;
        float bboxW = 0.6F;
        if (selectionId != null && skinTexture != null) {
            String p = skinTexture.getPath();
            int slash = p.lastIndexOf('/');
            if (slash != -1) p = p.substring(slash + 1);
            if (p.endsWith(".png")) p = p.substring(0, p.length() - 4);

            ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(skinTexture.getNamespace(), p);
            BuiltBoxModel built = BoxModelManager.get(modelId);
            if (built == null) {
                var mj = ClientSkinAssets.getModelJson(selectionId);
                if (mj != null) BoxModelManager.registerRuntime(modelId, mj);
                built = BoxModelManager.get(modelId);
            }
            if (built != null) {
                bboxH = Math.max(bboxH, built.bboxHeight());
                bboxW = Math.max(bboxW, built.bboxWidth());
            }
        }
        final float BASE_TRANSLATE_Y = BASE_BBOX_H / 2.0F;
        AvatarRenderState state = new AvatarRenderState();
        try {
            Minecraft.getInstance().getTextureManager().getTexture(skinTexture);
        } catch (Throwable ignored) {
        }
        if (state instanceof RenderStateSkinIdAccess a) a.consoleskins$setSkinId(selectionId);
        state.id = DOLL_RENDER_STATE_ID;
        state.entityType = EntityType.PLAYER;
        state.lightCoords = 15728880;
        state.boundingBoxHeight = bboxH;
        state.boundingBoxWidth = bboxW;
        float yaw = normalizeYaw(yawOffset + 180.0F);
        state.bodyRot = yaw;
        state.yRot = yaw;
        state.xRot = 0.0F;
        state.pose = Pose.STANDING;
        state.isBaby = false;
        state.scale = 1.0F;
        state.ageScale = 1.0F;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showCape = false;
        state.attackArm = HumanoidArm.RIGHT;
        state.attackTime = attackTime;
        state.isCrouching = crouching;

        ClientAsset.Texture body = new ClientAsset.ResourceTexture(skinTexture, skinTexture);

        ClientAsset.Texture cape = body;
        boolean showCape = false;
        try {
            SkinEntry entry = selectionId != null ? SkinPackLoader.getSkin(selectionId) : null;
            if (entry != null && entry.cape() != null) {
                ResourceLocation capePath = entry.cape();
                try {
                    Minecraft.getInstance().getTextureManager().getTexture(capePath);
                } catch (Throwable ignored) {
                }
                cape = new ClientAsset.ResourceTexture(capePath, capePath);
                showCape = true;
            }
        } catch (Throwable ignored) {
        }
        state.showCape = showCape;

        boolean slim = false;
        try {
            SkinEntry entry = selectionId != null ? SkinPackLoader.getSkin(selectionId) : null;
            if (entry != null) {
                slim = entry.slimArms();
            } else {
                Boolean b = ClientSkinAssets.getSlimFlag(selectionId);
                if (b != null) slim = b;
            }
        } catch (Throwable ignored) {
            Boolean b = ClientSkinAssets.getSlimFlag(selectionId);
            if (b != null) slim = b;
        }
        PlayerModelType model = slim ? PlayerModelType.SLIM : PlayerModelType.WIDE;
        PlayerSkin skin = PlayerSkin.insecure(body, cape, body, model);
        state.skin = skin;
        int baseHeight = bottom - top;
        int size = Math.min((int) (baseHeight / 2.75F), sizeCap);
        size = Math.max(size, 20);
        float renderScale = (float) size;
        float crouchComp = crouching ? -0.125F : 0.0F;
        Vector3f translate = new Vector3f(0.0F, BASE_TRANSLATE_Y + crouchComp, 0.0F);
        Quaternionf quat = new Quaternionf().rotationZ((float) Math.toRadians(180.0F));
        Quaternionf quat2 = new Quaternionf().rotationX(0);
        quat.mul(quat2);
        quat2.conjugate();
        int expandedTop = top - baseHeight;
        int expandedBottom = bottom + baseHeight;

        applyPreviewLighting(gui, PREVIEW_BRIGHTNESS_MUL);
        gui.submitEntityRenderState(state, renderScale, translate, quat, quat2, left, expandedTop, right, expandedBottom);
        resetPreviewLighting(gui);
    }


    private static void applyPreviewLighting(GuiGraphics gui, float brightnessMul) {
        setupInventoryEntityLighting();
        GpuBufferSlice lights = resolveGuiEntityLights();
        if (lights != null) {
            try {
                RenderSystem.setShaderLights(lights);
            } catch (Throwable ignored) {
            }
        }

        if (brightnessMul == 1.0F) return;
        if (invokeGuiSetColor(gui, brightnessMul, brightnessMul, brightnessMul, 1.0F)) return;
        invokeRsSetColor(brightnessMul, brightnessMul, brightnessMul, 1.0F);
    }

    private static void resetPreviewLighting(GuiGraphics gui) {
        if (invokeGuiSetColor(gui, 1.0F, 1.0F, 1.0F, 1.0F)) return;
        invokeRsSetColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static boolean invokeGuiSetColor(GuiGraphics gui, float r, float g, float b, float a) {
        if (!triedResolveGuiSetColor) {
            synchronized (GuiDollRender.class) {
                if (!triedResolveGuiSetColor) {
                    triedResolveGuiSetColor = true;
                    cachedGuiSetColor = null;
                    try {
                        Method m = gui.getClass().getMethod("setColor", float.class, float.class, float.class, float.class);
                        cachedGuiSetColor = m;
                    } catch (Throwable ignored) {
                        try {
                            Method m = gui.getClass().getDeclaredMethod("setColor", float.class, float.class, float.class, float.class);
                            m.setAccessible(true);
                            cachedGuiSetColor = m;
                        } catch (Throwable ignored2) {
                            try {
                                for (Method m : gui.getClass().getDeclaredMethods()) {
                                    if (m.getParameterCount() != 4) continue;
                                    Class<?>[] p = m.getParameterTypes();
                                    if (p[0] != float.class || p[1] != float.class || p[2] != float.class || p[3] != float.class) continue;
                                    String n = m.getName().toLowerCase(java.util.Locale.ROOT);
                                    if (!n.contains("color")) continue;
                                    m.setAccessible(true);
                                    cachedGuiSetColor = m;
                                    break;
                                }
                            } catch (Throwable ignored3) {
                            }
                        }
                    }
                }
            }
        }

        if (cachedGuiSetColor == null) return false;
        try {
            cachedGuiSetColor.invoke(gui, r, g, b, a);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void invokeRsSetColor(float r, float g, float b, float a) {
        if (!triedResolveRsSetColor) {
            synchronized (GuiDollRender.class) {
                if (!triedResolveRsSetColor) {
                    triedResolveRsSetColor = true;
                    cachedRsSetColor = null;
                    try {
                        for (Method m : RenderSystem.class.getMethods()) {
                            if (!Modifier.isStatic(m.getModifiers())) continue;
                            if (m.getParameterCount() != 4) continue;
                            Class<?>[] p = m.getParameterTypes();
                            if (p[0] != float.class || p[1] != float.class || p[2] != float.class || p[3] != float.class) continue;
                            String n = m.getName().toLowerCase(java.util.Locale.ROOT);
                            if (!n.contains("color")) continue;
                            cachedRsSetColor = m;
                            break;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        if (cachedRsSetColor == null) return;
        try {
            cachedRsSetColor.invoke(null, r, g, b, a);
        } catch (Throwable ignored) {
        }
    }

    private static void setupInventoryEntityLighting() {
        if (!triedResolveInventoryEntitySetup) {
            synchronized (GuiDollRender.class) {
                if (!triedResolveInventoryEntitySetup) {
                    triedResolveInventoryEntitySetup = true;
                    cachedInventoryEntitySetup = null;
                    try {
                        Class<?> lighting = Class.forName("com.mojang.blaze3d.platform.Lighting");
                        for (Method m : lighting.getDeclaredMethods()) {
                            if (!Modifier.isStatic(m.getModifiers())) continue;
                            String n = m.getName().toLowerCase(java.util.Locale.ROOT);
                            if (!n.contains("inventory")) continue;
                            if (!n.contains("entity") && !n.contains("entities")) continue;
                            m.setAccessible(true);
                            cachedInventoryEntitySetup = m;
                            break;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        if (cachedInventoryEntitySetup == null) return;
        try {
            if (cachedInventoryEntitySetup.getParameterCount() == 0) {
                cachedInventoryEntitySetup.invoke(null);
                return;
            }
            if (cachedInventoryEntitySetup.getParameterCount() == 1 && cachedInventoryEntitySetup.getParameterTypes()[0].getName().equals("org.joml.Quaternionf")) {
                cachedInventoryEntitySetup.invoke(null, new Quaternionf());
            }
        } catch (Throwable ignored) {
        }
    }

    private static GpuBufferSlice resolveGuiEntityLights() {
        if (triedResolveGuiEntityLights) return cachedGuiEntityLights;
        synchronized (GuiDollRender.class) {
            if (triedResolveGuiEntityLights) return cachedGuiEntityLights;
            triedResolveGuiEntityLights = true;
            cachedGuiEntityLights = null;
            try {
                Class<?> lighting = Class.forName("com.mojang.blaze3d.platform.Lighting");
                GpuBufferSlice best = null;
                int bestScore = Integer.MIN_VALUE;
                for (Field f : lighting.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) continue;
                    if (!GpuBufferSlice.class.isAssignableFrom(f.getType())) continue;
                    f.setAccessible(true);
                    Object v = f.get(null);
                    if (!(v instanceof GpuBufferSlice slice)) continue;
                    String name = f.getName().toLowerCase(java.util.Locale.ROOT);
                    int score = 0;
                    if (name.contains("entity")) score += 10;
                    if (name.contains("inventory")) score += 10;
                    if (name.contains("gui")) score += 6;
                    if (name.contains("item")) score += 3;
                    if (score > bestScore) {
                        bestScore = score;
                        best = slice;
                    }
                }
                if (best != null) {
                    cachedGuiEntityLights = best;
                    return cachedGuiEntityLights;
                }

                for (Method m : lighting.getDeclaredMethods()) {
                    if (!Modifier.isStatic(m.getModifiers())) continue;
                    if (!GpuBufferSlice.class.isAssignableFrom(m.getReturnType())) continue;
                    if (m.getParameterCount() != 0) continue;
                    m.setAccessible(true);
                    Object v = m.invoke(null);
                    if (v instanceof GpuBufferSlice slice) {
                        cachedGuiEntityLights = slice;
                        return cachedGuiEntityLights;
                    }
                }
            } catch (Throwable ignored) {
            }
            return cachedGuiEntityLights;
        }
    }
}
