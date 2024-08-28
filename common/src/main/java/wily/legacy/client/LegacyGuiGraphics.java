package wily.legacy.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public interface LegacyGuiGraphics {
    Map<TextureAtlasSprite, Map<String,ResourceLocation>> spriteTilesCache = new ConcurrentHashMap<>();

    static LegacyGuiGraphics of(PoseStack poseStack) {
        return (LegacyGuiGraphics) poseStack;
    }

    PoseStack self();
    default void blitSprite(ResourceLocation resourceLocation, int i, int j, int k, int l) {
        this.blitSprite(resourceLocation, i, j, 0, k, l);
    }

    default void blitSprite(ResourceLocation resourceLocation, int i, int j, int k, int l, int m) {
        TextureAtlasSprite textureAtlasSprite = Legacy4JClient.sprites.getSprite(resourceLocation);
        GuiSpriteScaling guiSpriteScaling = Legacy4JClient.sprites.getSpriteScaling(textureAtlasSprite);
        if (guiSpriteScaling instanceof GuiSpriteScaling.Stretch) {
            this.blitSprite(textureAtlasSprite, i, j, k, l, m);
        } else if (guiSpriteScaling instanceof GuiSpriteScaling.Tile tile) {
            this.blitTiledSprite(textureAtlasSprite, i, j, k, l, m, 0, 0, tile.width(), tile.height(), tile.width(), tile.height());
        } else if (guiSpriteScaling instanceof GuiSpriteScaling.NineSlice nineSlice) {
            this.blitNineSlicedSprite(textureAtlasSprite, nineSlice, i, j, k, l, m);
        }

    }
    default void blitNineSlicedSprite(TextureAtlasSprite textureAtlasSprite, GuiSpriteScaling.NineSlice nineSlice, int i, int j, int k, int l, int m) {
        GuiSpriteScaling.NineSlice.Border border = nineSlice.border();
        int n = Math.min(border.left(), l / 2);
        int o = Math.min(border.right(), l / 2);
        int p = Math.min(border.top(), m / 2);
        int q = Math.min(border.bottom(), m / 2);
        if (l == nineSlice.width() && m == nineSlice.height()) {
            this.blitSprite(textureAtlasSprite, nineSlice.width(), nineSlice.height(), 0, 0, i, j, k, l, m);
        } else if (m == nineSlice.height()) {
            this.blitSprite(textureAtlasSprite, nineSlice.width(), nineSlice.height(), 0, 0, i, j, k, n, m);
            this.blitTiledSprite(textureAtlasSprite, i + n, j, k, l - o - n, m, n, 0, nineSlice.width() - o - n, nineSlice.height(), nineSlice.width(), nineSlice.height());
            this.blitSprite(textureAtlasSprite, nineSlice.width(), nineSlice.height(), nineSlice.width() - o, 0, i + l - o, j, k, o, m);
        } else if (l == nineSlice.width()) {
            this.blitSprite(textureAtlasSprite, nineSlice.width(), nineSlice.height(), 0, 0, i, j, k, l, p);
            this.blitTiledSprite(textureAtlasSprite, i, j + p, k, l, m - q - p, 0, p, nineSlice.width(), nineSlice.height() - q - p, nineSlice.width(), nineSlice.height());
            this.blitSprite(textureAtlasSprite, nineSlice.width(), nineSlice.height(), 0, nineSlice.height() - q, i, j + m - q, k, l, q);
        } else {
            this.blitSprite(textureAtlasSprite, nineSlice.width(), nineSlice.height(), 0, 0, i, j, k, n, p);
            this.blitTiledSprite(textureAtlasSprite, i + n, j, k, l - o - n, p, n, 0, nineSlice.width() - o - n, p, nineSlice.width(), nineSlice.height());
            this.blitSprite(textureAtlasSprite, nineSlice.width(), nineSlice.height(), nineSlice.width() - o, 0, i + l - o, j, k, o, p);
            this.blitSprite(textureAtlasSprite, nineSlice.width(), nineSlice.height(), 0, nineSlice.height() - q, i, j + m - q, k, n, q);
            this.blitTiledSprite(textureAtlasSprite, i + n, j + m - q, k, l - o - n, q, n, nineSlice.height() - q, nineSlice.width() - o - n, q, nineSlice.width(), nineSlice.height());
            this.blitSprite(textureAtlasSprite, nineSlice.width(), nineSlice.height(), nineSlice.width() - o, nineSlice.height() - q, i + l - o, j + m - q, k, o, q);
            this.blitTiledSprite(textureAtlasSprite, i, j + p, k, n, m - q - p, 0, p, n, nineSlice.height() - q - p, nineSlice.width(), nineSlice.height());
            this.blitTiledSprite(textureAtlasSprite, i + n, j + p, k, l - o - n, m - q - p, n, p, nineSlice.width() - o - n, nineSlice.height() - q - p, nineSlice.width(), nineSlice.height());
            this.blitTiledSprite(textureAtlasSprite, i + l - o, j + p, k, n, m - q - p, nineSlice.width() - o, p, o, nineSlice.height() - q - p, nineSlice.width(), nineSlice.height());
        }
    }

    default void blitTiledSprite(TextureAtlasSprite textureAtlasSprite, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s) {
        Minecraft minecraft = Minecraft.getInstance();
        if (l <= 0 || m <= 0 ) {
            return;
        }
        if (p <= 0 || q <= 0) {
            throw new IllegalArgumentException("Tiled sprite texture size must be positive, got " + p + "x" + q);
        }

        ResourceLocation tile = spriteTilesCache.computeIfAbsent(textureAtlasSprite, sp-> new ConcurrentHashMap<>()).computeIfAbsent("tile_" + n + "x" + o + "_" + p + "x" + q,(string)->{
            try {
                TextureAtlas atlas = (TextureAtlas) minecraft.getTextureManager().getTexture(textureAtlasSprite.atlasLocation());
                Optional<ResourceLocation> opt = atlas.texturesByName.entrySet().stream().filter(e-> e.getValue() == textureAtlasSprite).findFirst().map(Map.Entry::getKey);
                if (opt.isPresent()) {
                    NativeImage image = NativeImage.read(minecraft.getResourceManager().getResourceOrThrow(opt.get().withPath("textures/gui/sprites/" +opt.get().getPath() + ".png")).open());
                    int width = (int)Math.ceil(p * image.getWidth() / (double) r);
                    int height = (int)Math.ceil(q * image.getHeight() / (double) s);
                    NativeImage tileImage = new NativeImage(width, height, false);
                    image.copyRect(tileImage,  n * image.getWidth() / r, o * image.getHeight() / s, 0, 0, width, height, false, false);
                    return minecraft.getTextureManager().register("tile", new DynamicTexture(tileImage));
                }
            } catch (IOException e) {
                Legacy4J.LOGGER.warn(e.getMessage());
            }
            return null;
        });
        self().blit(tile,i,j,Math.min(n,p),Math.min(o,q),l,m,p,q);
    }
    default void blitSprite(ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n, int o, int p) {
        this.blitSprite(resourceLocation, i, j, k, l, m, n, 0, o, p);
    }

    default void blitSprite(ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n, int o, int p, int q) {
        TextureAtlasSprite textureAtlasSprite = Legacy4JClient.sprites.getSprite(resourceLocation);
        GuiSpriteScaling guiSpriteScaling = Legacy4JClient.sprites.getSpriteScaling(textureAtlasSprite);
        if (guiSpriteScaling instanceof GuiSpriteScaling.Stretch) {
            this.blitSprite(textureAtlasSprite, i, j, k, l, m, n, o, p, q);
        } else {
            this.blitSprite(textureAtlasSprite, m, n, o, p, q);
        }

    }

    void blitSprite(TextureAtlasSprite textureAtlasSprite, int i, int j, int k, int l, int m, int n, int o, int p, int q);

    void blitSprite(TextureAtlasSprite textureAtlasSprite, int i, int j, int k, int l, int m);

}
