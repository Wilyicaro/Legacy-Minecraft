package wily.legacy.CustomModelSkins.cpm.shared;

import wily.legacy.CustomModelSkins.cpl.render.RenderTypeBuilder;
import wily.legacy.CustomModelSkins.cpl.util.DynamicTexture.ITexture;
import wily.legacy.CustomModelSkins.cpl.util.ImageIO.IImageIO;
import wily.legacy.CustomModelSkins.cpm.shared.config.Player;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinitionLoader;
import wily.legacy.CustomModelSkins.cpm.shared.model.SkinType;

import java.io.File;
import java.net.Proxy;
import java.util.List;

public interface MinecraftClientAccess {
    IPlayerRenderManager getPlayerRenderManager();

    ModelDefinitionLoader getDefinitionLoader();

    ITexture createTexture();

    void executeOnGameThread(Runnable r);

    static MinecraftClientAccess get() {
        return MinecraftObjectHolder.clientObject;
    }

    default Player<?> getClientPlayer() {
        return getDefinitionLoader().loadPlayer(getPlayerIDObject(), ModelDefinitionLoader.PLAYER_UNIQUE);
    }

    Object getPlayerIDObject();

    Object getCurrentPlayerIDObject();

    SkinType getSkinType();

    boolean isInGame();

    File getGameDir();

    IImageIO getImageIO();

    void clearSkinCache();

    String getConnectedServer();

    List<Object> getPlayers();

    Proxy getProxy();

    RenderTypeBuilder<?, ?> getRenderBuilder();

    default void onLogOut() {
        getDefinitionLoader().clearServerData();
    }

    @Deprecated
    default void executeLater(Runnable r) {
        executeOnGameThread(r);
    }
}
