package wily.legacy.CustomModelSkins.cpm.client;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import wily.legacy.CustomModelSkins.cpm.shared.animation.AnimationEngine.AnimationMode;
import wily.legacy.CustomModelSkins.cpm.shared.config.Player;
import wily.legacy.CustomModelSkins.cpm.shared.model.RenderManager;
import wily.legacy.CustomModelSkins.cpm.shared.util.Log;

import java.lang.reflect.Method;

public abstract class ClientBase {
    public static MinecraftObject mc;
    public RenderManager<GameProfile, Avatar, Model, Void> manager;

    public void init0() {
        mc = new MinecraftObject();
    }

    public void init1() {
        if (mc == null) {
            try {
                init0();
            } catch (Throwable t) {
            }
        }
        try {
            manager = new RenderManager<>(mc.getPlayerRenderManager(), mc.getDefinitionLoader(), PlayerProfile::getPlayerProfile);
            try {
                manager.setGPGetters(ClientBase::getProfilePropertiesSafe, ClientBase::getPropertyValueSafe);
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            manager = null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Multimap<String, Property> getProfilePropertiesSafe(GameProfile profile) {
        if (profile == null) return (Multimap) ImmutableMultimap.of();
        for (String mn : new String[]{"getProperties", "properties"}) {
            try {
                Method m = GameProfile.class.getMethod(mn);
                Object pm = m.invoke(profile);
                if (pm instanceof Multimap mm) {
                    return (Multimap) mm;
                }
            } catch (Throwable ignored) {
            }
        }
        return (Multimap) ImmutableMultimap.of();
    }

    private static String getPropertyValueSafe(Property p) {
        if (p == null) return null;
        try {
            Method m = Property.class.getMethod("value");
            Object v = m.invoke(p);
            return v != null ? String.valueOf(v) : null;
        } catch (Throwable ignored) {
        }
        try {
            Method m = Property.class.getMethod("getValue");
            Object v = m.invoke(p);
            return v != null ? String.valueOf(v) : null;
        } catch (Throwable ignored) {
        }
        return null;
    }

    public void playerRenderPre(PlayerRenderStateAccess sa, PlayerModel model, AvatarRenderState renderState) {
        if (mc == null) {
            try {
                init0();
            } catch (Throwable t) {
            }
            if (mc == null) return;
        }
        Player<Avatar> pl = sa.cpm$getPlayer();
        if (pl != null) {
            Log.info("[CPM_DEBUG] Binding model for render: " + pl.getName());
            mc.getPlayerRenderManager().bindModel(model, null, pl.getModelDefinition(), pl, AnimationMode.PLAYER);
        } else {
            mc.getPlayerRenderManager().unbindModel(model);
        }
        model.setupAnim(renderState);
        mc.getPlayerRenderManager().setModelPose(model);
    }

    public void playerRenderPost(PlayerModel model) {
        if (mc == null) {
            try {
                init0();
            } catch (Throwable t) {
            }
            if (mc == null) return;
        }
        mc.getPlayerRenderManager().unbindModel(model);
    }

    public void renderHand(PlayerModel model) {
        try {
            var mi = Minecraft.getInstance();
            if (mi == null || mi.player == null) return;
            renderHand(mi.player, model);
        } catch (Throwable ignored) {
        }
    }

    public void renderHand(AbstractClientPlayer pl, PlayerModel model) {
        if (mc == null) {
            try {
                init0();
            } catch (Throwable t) {
            }
            if (mc == null) return;
        }
        if (manager == null) {
            try {
                init1();
            } catch (Throwable t) {
            }
        }
        try {
            if (manager != null) manager.bindHand(pl, null, model);
        } catch (Throwable ignored) {
        }
    }

    public void renderHandPost(HumanoidModel model) {
        if (mc == null) return;
        if (manager == null) {
            try {
                init1();
            } catch (Throwable t) {
            }
        }
        try {
            if (manager != null) manager.unbindClear(model);
            else if (model instanceof PlayerModel pm) mc.getPlayerRenderManager().unbindModel(pm);
        } catch (Throwable ignored) {
        }
    }
}
