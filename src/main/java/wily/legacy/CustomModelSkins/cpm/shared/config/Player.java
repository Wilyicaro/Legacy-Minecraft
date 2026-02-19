package wily.legacy.CustomModelSkins.cpm.shared.config;

import wily.legacy.CustomModelSkins.cpm.shared.animation.AnimationState;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition.ModelLoadingState;
import wily.legacy.CustomModelSkins.cpm.shared.model.SkinType;
import wily.legacy.CustomModelSkins.cpm.shared.skin.PlayerTextureLoader;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Player<P> {
    private static boolean enableRendering = true;
    private static final Set<UUID> clientPlayerOverrides = ConcurrentHashMap.newKeySet();

    public static void addClientPlayerOverride(UUID uuid) {
        if (uuid != null) clientPlayerOverrides.add(uuid);
    }

    public static void removeClientPlayerOverride(UUID uuid) {
        if (uuid != null) clientPlayerOverrides.remove(uuid);
    }

    public static boolean isClientPlayerOverride(UUID uuid) {
        return uuid != null && clientPlayerOverrides.contains(uuid);
    }

    private CompletableFuture<ModelDefinition> definition;
    private PlayerTextureLoader textures;
    public final AnimationState animState = new AnimationState();
    public boolean forcedSkin, isModel;
    public String unique;

    public PlayerTextureLoader getTextures() {
        if (textures == null) {
            textures = initTextures();
            textures.load();
        }
        return textures;
    }

    public abstract SkinType getSkinType();

    protected abstract PlayerTextureLoader initTextures();

    public abstract String getName();

    public abstract UUID getUUID();

    public abstract void updateFromPlayer(P player);

    public abstract Object getGameProfile();

    public abstract void updateFromModel(Object model);

    public void updatePlayer(P player) {
        updateFromPlayer(player);
    }

    public void setModelDefinition(CompletableFuture<ModelDefinition> definition, boolean isModel) {
        this.definition = definition.exceptionally(e -> new ModelDefinition(e, this));
        this.isModel = isModel;
    }

    public ModelDefinition getModelDefinition0() {
        try {
            return enableRendering && definition != null ? definition.getNow(null) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public ModelDefinition getModelDefinition() {
        ModelDefinition def = getModelDefinition0();
        if (def != null) {
            ModelLoadingState st = def.getResolveState();
            if (st == ModelLoadingState.NEW) def.startResolve();
            else if (st == ModelLoadingState.LOADED && def.doRender()) return def;
        }
        return null;
    }

    public static boolean isEnableNames() {
        return true;
    }

    public static boolean isEnableLoadingInfo() {
        return false;
    }

    public void cleanup() {
        if (textures != null) {
            textures.cleanup();
            textures = null;
        }
        definition = null;
    }

    public boolean isClientPlayer() {
        return isClientPlayerOverride(getUUID());
    }
}
