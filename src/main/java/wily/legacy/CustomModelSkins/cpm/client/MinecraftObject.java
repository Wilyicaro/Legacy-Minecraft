package wily.legacy.CustomModelSkins.cpm.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.CustomModelSkins.cpl.render.RenderTypeBuilder;
import wily.legacy.CustomModelSkins.cpl.util.DynamicTexture.ITexture;
import wily.legacy.CustomModelSkins.cpl.util.Image;
import wily.legacy.CustomModelSkins.cpl.util.ImageIO.IImageIO;
import wily.legacy.CustomModelSkins.cpm.shared.MinecraftClientAccess;
import wily.legacy.CustomModelSkins.cpm.shared.MinecraftObjectHolder;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinitionLoader;
import wily.legacy.CustomModelSkins.cpm.shared.model.SkinType;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.RenderMode;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MinecraftObject implements MinecraftClientAccess {
    private final ModelDefinitionLoader<GameProfile> loader;
    private final PlayerRenderManager prm;
    public RenderTypeBuilder<ResourceLocation, RenderType> renderBuilder;

    public MinecraftObject() {
        MinecraftObjectHolder.setClientObject(this);
        loader = new ModelDefinitionLoader<>(PlayerProfile::new, GameProfile::id, GameProfile::name);
        prm = new PlayerRenderManager();
        renderBuilder = new RenderTypeBuilder<>();
        renderBuilder.register(RenderMode.DEFAULT, RenderType::entityTranslucent, 0);
    }

    @Override
    public PlayerRenderManager getPlayerRenderManager() {
        return prm;
    }

    @Override
    public ITexture createTexture() {
        return new DynTexture();
    }

    public static class DynTexture implements ITexture {
        private static int ID = 0;
        private DynamicTexture dynTex;
        private ResourceLocation loc;
        private static ResourceLocation bound_loc;
        private Minecraft mc = Minecraft.getInstance();

        @Override
        public void bind() {
            if (loc == null) return;
            bound_loc = loc;
            if (mc.getTextureManager().getTexture(loc) == null) mc.getTextureManager().register(loc, dynTex);
        }

        @Override
        public void load(Image texture) {
            if (loc == null || dynTex.getTexture().getWidth(0) != texture.getWidth() || dynTex.getTexture().getHeight(0) != texture.getHeight()) {
                if (loc != null) {
                    mc.getTextureManager().release(loc);
                }
                int id = ID++;
                dynTex = new DynamicTexture("CPM Dynamic Texture #" + id, texture.getWidth(), texture.getHeight(), true);
                loc = ResourceLocation.fromNamespaceAndPath("cpm", "dyn_" + id);
                mc.getTextureManager().register(loc, dynTex);
            }
            NativeImage ni = NativeImageIO.createFromBufferedImage(texture);
            dynTex.setPixels(ni);
            dynTex.upload();
        }

        public static ResourceLocation getBoundLoc() {
            return bound_loc;
        }

        @Override
        public void free() {
            if (loc != null) mc.getTextureManager().release(loc);
        }
    }

    @Override
    public void executeOnGameThread(Runnable r) {
        Minecraft.getInstance().execute(r);
    }

    public void executeNextFrame(Runnable r) {
        Minecraft.getInstance().schedule(r);
    }

    @Override
    public ModelDefinitionLoader<GameProfile> getDefinitionLoader() {
        return loader;
    }

    @Override
    public SkinType getSkinType() {
        return SkinType.get(DefaultPlayerSkin.get(Minecraft.getInstance().getUser().getProfileId()).model().getSerializedName());
    }

    @Override
    public boolean isInGame() {
        return Minecraft.getInstance().player != null;
    }

    @Override
    public Object getPlayerIDObject() {
        return Minecraft.getInstance().getGameProfile();
    }

    @Override
    public Object getCurrentPlayerIDObject() {
        var mc = Minecraft.getInstance();
        return PlayerProfile.getPlayerProfile(mc.player);
    }

    @Override
    public File getGameDir() {
        return Minecraft.getInstance().gameDirectory;
    }

    @Override
    public IImageIO getImageIO() {
        return new NativeImageIO();
    }

    @Override
    public void clearSkinCache() {
    }

    @Override
    public String getConnectedServer() {
        if (Minecraft.getInstance().getConnection() == null) return null;
        SocketAddress sa = Platform.getChannel(Minecraft.getInstance().getConnection().getConnection()).remoteAddress();
        if (sa instanceof InetSocketAddress) return ((InetSocketAddress) sa).getHostString();
        return null;
    }

    @Override
    public List<Object> getPlayers() {
        if (Minecraft.getInstance().getConnection() == null) return Collections.emptyList();
        return Minecraft.getInstance().getConnection().getOnlinePlayers().stream().map(PlayerInfo::getProfile).collect(Collectors.toList());
    }

    @Override
    public Proxy getProxy() {
        return Minecraft.getInstance().getProxy();
    }

    @Override
    public RenderTypeBuilder<?, ?> getRenderBuilder() {
        return renderBuilder;
    }
}
