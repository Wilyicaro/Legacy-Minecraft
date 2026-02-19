package wily.legacy.CustomModelSkins.cpm.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import wily.legacy.CustomModelSkins.cpl.math.MathHelper;
import wily.legacy.CustomModelSkins.cpl.util.Hand;
import wily.legacy.CustomModelSkins.cpl.util.HandAnimation;
import wily.legacy.CustomModelSkins.cpm.shared.config.Player;
import wily.legacy.CustomModelSkins.cpm.shared.model.SkinType;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.PlayerModelSetup.ArmPose;
import wily.legacy.CustomModelSkins.cpm.shared.skin.PlayerTextureLoader;
import wily.legacy.CustomModelSkins.cpm.shared.skin.TextureType;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

public class PlayerProfile extends Player<Avatar> {
    public static boolean inGui;
    public static BooleanSupplier inFirstPerson;

    static {
        inFirstPerson = () -> false;
    }

    private final GameProfile profile;
    private String skinType;

    public static GameProfile getPlayerProfile(Avatar avatar) {
        if (avatar == null) return null;
        if (avatar instanceof AbstractClientPlayer player) {
            GameProfile profile = player.getGameProfile();
            try {
                boolean needsFill = profile != null && (profile.properties().isEmpty() || !profile.properties().containsKey("textures"));
                if (needsFill) {
                    var conn = Minecraft.getInstance().getConnection();
                    if (conn != null) {
                        Object info = null;
                        try {
                            if (profile.id() != null) info = conn.getPlayerInfo(profile.id());
                        } catch (Throwable ignored) {
                        }
                        if (info == null) {
                            String n = null;
                            try {
                                n = player.getScoreboardName();
                            } catch (Throwable ignored) {
                            }
                            if (n == null || n.isEmpty()) {
                                try {
                                    n = profile.name();
                                } catch (Throwable ignored) {
                                }
                            }
                            String match = consoleskins$normalizeName(n);
                            if (match != null) {
                                try {
                                    for (var pi : conn.getOnlinePlayers()) {
                                        if (pi == null) continue;
                                        var gp = pi.getProfile();
                                        if (gp == null) continue;
                                        String pn = gp.name();
                                        if (pn == null) continue;
                                        if (match.equalsIgnoreCase(consoleskins$normalizeName(pn))) {
                                            info = pi;
                                            break;
                                        }
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                        try {
                            if (info != null) {
                                var mGetProfile = info.getClass().getMethod("getProfile");
                                Object gp = mGetProfile.invoke(info);
                                if (gp instanceof GameProfile gpp) profile = gpp;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            return profile;
        }
        return new GameProfile(avatar.getUUID(), avatar.getScoreboardName());
    }

    private static String consoleskins$normalizeName(String n) {
        if (n == null) return null;
        n = n.trim();
        if (n.isEmpty()) return null;
        int i = n.lastIndexOf('(');
        if (i > 0 && n.endsWith(")")) {
            String inside = n.substring(i + 1, n.length() - 1);
            boolean digits = !inside.isEmpty();
            for (int j = 0; j < inside.length(); j++) {
                char c = inside.charAt(j);
                if (c < '0' || c > '9') {
                    digits = false;
                    break;
                }
            }
            if (digits) n = n.substring(0, i).trim();
        }
        return n;
    }

    public PlayerProfile(GameProfile profile) {
        this.profile = new GameProfile(profile.id(), profile.name(), profile.properties());
        if (profile.id() != null) this.skinType = DefaultPlayerSkin.get(profile.id()).model().getSerializedName();
    }

    @Override
    public SkinType getSkinType() {
        return SkinType.get(skinType);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((profile == null) ? 0 : profile.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PlayerProfile other = (PlayerProfile) obj;
        if (profile == null) {
            if (other.profile != null) return false;
        } else if (!profile.equals(other.profile)) return false;
        return true;
    }

    @Override
    public UUID getUUID() {
        return profile.id();
    }

    @Override
    public void updateFromPlayer(Avatar player) {
        Pose p = player.getPose();
        animState.resetPlayer();
        switch (p) {
            case FALL_FLYING:
                break;
            case SLEEPING:
                animState.sleeping = true;
                break;
            case SPIN_ATTACK:
                animState.tridentSpin = true;
                break;
            default:
                break;
        }
        animState.sneaking = player.isCrouching();
        animState.crawling = player.isVisuallyCrawling();
        animState.swimming = player.isVisuallySwimming();
        if (!player.isAlive()) animState.dying = true;
        if (Platform.isSitting(player)) animState.riding = true;
        if (player.isSprinting()) animState.sprinting = true;
        if (player.isUsingItem()) {
            animState.usingAnimation = HandAnimation.of(player.getUseItem().getUseAnimation());
        }
        if (player.isInWater()) animState.retroSwimming = true;
        animState.moveAmountX = (float) (player.getX() - player.xo);
        animState.moveAmountY = (float) (player.getY() - player.yo);
        animState.moveAmountZ = (float) (player.getZ() - player.zo);
        animState.yaw = player.getYRot();
        animState.pitch = player.getXRot();
        animState.bodyYaw = player.yBodyRot;
        if (player.isModelPartShown(PlayerModelPart.HAT)) animState.encodedState |= 1;
        if (player.isModelPartShown(PlayerModelPart.JACKET)) animState.encodedState |= 2;
        if (player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG)) animState.encodedState |= 4;
        if (player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG)) animState.encodedState |= 8;
        if (player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE)) animState.encodedState |= 16;
        if (player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE)) animState.encodedState |= 32;
        ItemStack is = player.getItemBySlot(EquipmentSlot.HEAD);
        animState.hasSkullOnHead = is.getItem() instanceof BlockItem && ((BlockItem) is.getItem()).getBlock() instanceof AbstractSkullBlock;
        animState.wearingHelm = !is.isEmpty();
        is = player.getItemBySlot(EquipmentSlot.CHEST);
        animState.wearingElytra = false;
        animState.wearingBody = !is.isEmpty();
        animState.wearingLegs = !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty();
        animState.wearingBoots = !player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
        animState.mainHand = Hand.of(player.getMainArm());
        animState.activeHand = Hand.of(animState.mainHand, player.getUsedItemHand());
        animState.swingingHand = Hand.of(animState.mainHand, player.swingingArm);
        animState.hurtTime = player.hurtTime;
        animState.isOnLadder = player.onClimbable();
        animState.isBurning = player.displayFireAnimation();
        animState.isFreezing = player.getTicksFrozen() > 0;
        animState.inGui = inGui;
        animState.firstPersonMod = inFirstPerson.getAsBoolean();
        if (player.getUseItem().getItem() instanceof CrossbowItem) {
            float f = CrossbowItem.getChargeDuration(player.getUseItem(), player);
            float f1 = MathHelper.clamp(player.getTicksUsingItem(), 0.0F, f);
            animState.crossbowPullback = f1 / f;
        }
        if (player.getUseItem().getItem() instanceof BowItem) {
            float f = 20F;
            float f1 = MathHelper.clamp(player.getTicksUsingItem(), 0.0F, f);
            animState.bowPullback = f1 / f;
        }
    }

    @Override
    public void updateFromModel(Object model) {
        if (model instanceof PlayerModel) {
            animState.vrState = null;
        }
    }

    public void updateFromState(AvatarRenderState state) {
        animState.resetModel();
        animState.attackTime = state.attackTime;
        animState.swimAmount = state.swimAmount;
        animState.leftArm = ArmPose.of(state.leftArmPose);
        animState.rightArm = ArmPose.of(state.rightArmPose);
        animState.parrotLeft = state.parrotOnLeftShoulder != null;
        animState.parrotRight = state.parrotOnRightShoulder != null;
    }

    @Override
    protected PlayerTextureLoader initTextures() {
        var cache = new File(Minecraft.getInstance().gameDirectory, "assets/skins");
        if (!cache.exists()) cache.mkdirs();
        return new PlayerTextureLoader(cache) {
            @Override
            protected CompletableFuture<Void> load0() {
                return CompletableFuture.supplyAsync(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    var mss = mc.services().sessionService();
                    var pt = mss.getPackedTextures(profile);
                    if (pt == null) return MinecraftProfileTextures.EMPTY;
                    var mpts = mss.unpackTextures(pt);
                    return mpts;
                }, Util.backgroundExecutor().forName("CPM:unpackSkinTextures")).thenAcceptAsync(mpts -> {
                    var skin = mpts.skin();
                    var cape = mpts.cape();
                    if (skin != null) {
                        skinType = skin.getMetadata("model");
                        defineTexture(TextureType.SKIN, skin.getUrl(), skin.getHash());
                    }
                    if (cape != null) {
                        defineTexture(TextureType.CAPE, cape.getUrl(), cape.getHash());
                    }
                }, t -> Minecraft.getInstance().schedule(t::run));
            }
        };
    }

    @Override
    public String getName() {
        return profile.name();
    }

    @Override
    public Object getGameProfile() {
        return profile;
    }
}
