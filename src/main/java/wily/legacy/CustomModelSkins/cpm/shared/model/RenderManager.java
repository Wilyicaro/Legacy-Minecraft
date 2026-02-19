package wily.legacy.CustomModelSkins.cpm.shared.model;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import wily.legacy.CustomModelSkins.cpm.shared.animation.AnimationEngine.AnimationMode;
import wily.legacy.CustomModelSkins.cpm.shared.config.Player;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinitionLoader;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.ModelRenderManager;
import wily.legacy.CustomModelSkins.cpm.shared.util.Msg;

import java.util.function.Function;

public class RenderManager<G, P, M, D> {
    private Player<P> profile;
    private final ModelRenderManager<D, ?, ?, M> renderManager;
    private final ModelDefinitionLoader<G> loader;
    private final Function<P, G> getProfile;
    private Function<G, String> getSkullModel, getTexture;

    public RenderManager(ModelRenderManager<D, ?, ?, M> renderManager, ModelDefinitionLoader<G> loader, Function<P, G> getProfile) {
        this.renderManager = renderManager;
        this.loader = loader;
        this.getProfile = getProfile;
    }

    @SuppressWarnings("unchecked")
    public boolean tryBindModel(G gprofile, P player, D buffer, M toBind, String arg, String unique, AnimationMode mode) {
        if (gprofile == null) gprofile = getProfile.apply(player);
        Player<P> profile = (Player<P>) loader.loadPlayer(gprofile, unique);
        if (profile == null) {
            renderManager.unbindModel(toBind);
            return false;
        }
        if (!profile.isModel && getTexture != null) {
            String texture = getTexture.apply(gprofile), ptexture = getTexture.apply((G) profile.getGameProfile());
            if (texture != null && !Objects.equal(texture, ptexture))
                profile = (Player<P>) loader.reloadPlayer(gprofile, unique);
            if (profile == null) {
                renderManager.unbindModel(toBind);
                return false;
            }
        }
        ModelDefinition def = profile.getModelDefinition();
        if (def != null) {
            this.profile = profile;
            profile.animState.animationMode = mode;
            if (player != null) profile.updatePlayer(player);
            renderManager.bindModel(toBind, arg, buffer, def, profile, profile.animState.animationMode);
            renderManager.getAnimationEngine().prepareAnimations(profile, profile.animState.animationMode, def);
            return true;
        }
        renderManager.unbindModel(toBind);
        return false;
    }

    @SuppressWarnings("unchecked")
    public Player<P> loadPlayerState(G gprofile, P player, String unique, AnimationMode mode) {
        if (gprofile == null) gprofile = getProfile.apply(player);
        Player<P> profile = (Player<P>) loader.loadPlayer(gprofile, unique);
        if (profile == null) return null;
        ModelDefinition def = profile.getModelDefinition();
        if (def != null) {
            this.profile = profile;
            profile.animState.animationMode = mode;
            if (player != null) profile.updatePlayer(player);
            return profile;
        }
        return null;
    }

    public void unbindClear(M model) {
        unbindFlush(model);
        clearBoundPlayer();
    }

    public void unbind(M model) {
        renderManager.unbindModel(model);
    }

    public void unbindFlush(M model) {
        renderManager.flushBatch(model, null);
        unbind(model);
    }

    public void bindArmor(M playerModel, M armorModel, int slot) {
        renderManager.bindSubModel(playerModel, armorModel, "armor" + slot);
    }

    public void bindHand(P player, D buffer, M model) {
        tryBindModel(null, player, buffer, model, null, ModelDefinitionLoader.PLAYER_UNIQUE, AnimationMode.HAND);
    }

    public void bindSkin(M model, TextureSheetType tex) {
        renderManager.bindSkin(model, null, tex);
    }

    public void clearBoundPlayer() {
        profile = null;
    }

    public void setGetSkullModel(Function<G, String> getSkullModel) {
        this.getSkullModel = getSkullModel;
    }

    public void setGetTexture(Function<G, String> getTexture) {
        this.getTexture = getTexture;
    }

    public <PR> void setGPGetters(Function<G, Multimap<String, PR>> getMap, Function<PR, String> getValue) {
        setGetSkullModel(profile -> {
                    PR p = Iterables.getFirst(getMap.apply(profile).get("cpm:model"), null);
                    return p != null ? getValue.apply(p) : null;
                }
        );
        setGetTexture(profile -> {
            PR p = Iterables.getFirst(getMap.apply(profile).get("textures"), null);
            return p != null ? getValue.apply(p) : null;
        });
    }

    @SuppressWarnings("unchecked")
    public Msg getStatus(G gprofile, String unique) {
        Player<P> profile = (Player<P>) loader.loadPlayer(gprofile, unique);
        if (profile == null) return null;
        ModelDefinition def = profile.getModelDefinition0();
        return def != null ? def.getStatus() : null;
    }
}
