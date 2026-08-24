package wily.legacy.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import wily.legacy.Legacy4J;

public class LegacyHurtFlash {
    private static final Identifier TEXTURE = Legacy4J.createModLocation("hurt_flash");
    private static final RenderType RENDER_TYPE = RenderType.create("legacy_hurt_flash", RenderSetup.builder(LegacyRenderPipelines.LEGACY_HURT_FLASH).withTexture("Sampler0", TEXTURE).useLightmap().sortOnUpload().createRenderSetup());
    private static boolean registered;

    public static RenderType renderType() {
        if (!registered) {
            NativeImage image = new NativeImage(1, 1, false);
            image.setPixel(0, 0, -1);
            Minecraft.getInstance().getTextureManager().register(TEXTURE, new DynamicTexture(TEXTURE::toString, image));
            registered = true;
        }
        return RENDER_TYPE;
    }
}
