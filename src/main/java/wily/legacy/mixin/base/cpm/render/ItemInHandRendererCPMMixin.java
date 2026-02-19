package wily.legacy.mixin.base.cpm.render;


/**
 * Mixin: console skins / CPM rendering glue.
 */

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.CustomModelSkins.cpm.client.CPMOrderedSubmitNodeCollector.CPMSubmitNodeCollector;
import wily.legacy.CustomModelSkins.cpm.client.CustomPlayerModelsClient;
import wily.legacy.CustomModelSkins.cpm.client.PlayerProfile;
import wily.legacy.mixin.base.cpm.access.LivingEntityRendererAccessor;
import wily.legacy.CustomModelSkins.cpm.shared.animation.AnimationEngine.AnimationMode;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinitionLoader;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererCPMMixin {
    @Shadow
    @Final
    private Minecraft minecraft;


    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private SubmitNodeCollector cpm$wrapCollector(SubmitNodeCollector snc) {
        return snc instanceof CPMSubmitNodeCollector ? snc : new CPMSubmitNodeCollector(snc);
    }

    @Unique
    private static PlayerModel cpm$getPlayerModel(Minecraft mc, AbstractClientPlayer player) {
        try {
            if (mc == null || player == null) return null;
            EntityRenderer<?, ?> er = mc.getEntityRenderDispatcher().getRenderer(player);
            if (!(er instanceof LivingEntityRenderer<?, ?, ?> ler)) return null;
            EntityModel<?> m = ((LivingEntityRendererAccessor) ler).cpm$invokeGetModel();
            return (m instanceof PlayerModel pm) ? pm : null;
        } catch (Throwable t) {
            return null;
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), require = 0)
    private void cpm$bindFirstPersonHand(AbstractClientPlayer player, float partialTicks, float pitch,
                                         net.minecraft.world.InteractionHand hand, float swingProgress, net.minecraft.world.item.ItemStack stack,
                                         float equipProgress, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                         net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, int packedLight, CallbackInfo ci) {
        try {
            if (CustomPlayerModelsClient.INSTANCE == null) return;

            if (minecraft != null && minecraft.player == player && CustomPlayerModelsClient.INSTANCE.manager != null) {
                GameProfile gp = PlayerProfile.getPlayerProfile(player);
                if (gp != null) {
                    CustomPlayerModelsClient.INSTANCE.manager.loadPlayerState(gp, player, ModelDefinitionLoader.PLAYER_UNIQUE, AnimationMode.PLAYER);
                }
            }
            PlayerModel pm = cpm$getPlayerModel(minecraft, player);
            if (pm == null) return;

            PlayerProfile.inFirstPerson = () -> true;
            CustomPlayerModelsClient.INSTANCE.renderHand(player, pm);
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("TAIL"), require = 0)
    private void cpm$unbindFirstPersonHand(AbstractClientPlayer player, float partialTicks, float pitch,
                                           net.minecraft.world.InteractionHand hand, float swingProgress, net.minecraft.world.item.ItemStack stack,
                                           float equipProgress, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                           net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, int packedLight, CallbackInfo ci) {
        try {
            if (CustomPlayerModelsClient.INSTANCE == null) return;
            PlayerModel pm = cpm$getPlayerModel(minecraft, player);
            if (pm == null) return;
            CustomPlayerModelsClient.INSTANCE.renderHandPost(pm);
        } catch (Throwable ignored) {
        } finally {

            PlayerProfile.inFirstPerson = () -> false;
        }
    }
}
