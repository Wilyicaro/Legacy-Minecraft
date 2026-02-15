package wily.legacy.CustomModelSkins.cpm.mixin;


/**
 * Mixin: console skins / CPM rendering glue.
 */

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.CustomModelSkins.cpm.client.PlayerRenderStateAccess;
import wily.legacy.CustomModelSkins.cpm.shared.config.Player;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements PlayerRenderStateAccess {
    private @Unique Player<Avatar> cpm$player;
    private @Unique Component cpm$modelStatus;
    private @Unique PartPose cpm$head;
    private @Unique PartPose cpm$body;
    private @Unique PartPose cpm$rightArm;
    private @Unique PartPose cpm$leftArm;
    private @Unique PartPose cpm$rightLeg;
    private @Unique PartPose cpm$leftLeg;

    @Override
    public void cpm$setPlayer(Player<Avatar> player) {
        this.cpm$player = player;
    }

    @Override
    public Player<Avatar> cpm$getPlayer() {
        return cpm$player;
    }

    @Override
    public void cpm$setModelStatus(Component status) {
        cpm$modelStatus = status;
    }

    @Override
    public Component cpm$getModelStatus() {
        return cpm$modelStatus;
    }

    @Override
    public void cpm$storeState(PlayerModel model) {
        cpm$head = model.head.storePose();
        cpm$body = model.body.storePose();
        cpm$rightArm = model.rightArm.storePose();
        cpm$leftArm = model.leftArm.storePose();
        cpm$rightLeg = model.rightLeg.storePose();
        cpm$leftLeg = model.leftLeg.storePose();
    }

    @Override
    public void cpm$loadState(PlayerModel model) {
        if (cpm$head == null) return;
        model.head.loadPose(cpm$head);
        model.body.loadPose(cpm$body);
        model.rightArm.loadPose(cpm$rightArm);
        model.leftArm.loadPose(cpm$leftArm);
        model.rightLeg.loadPose(cpm$rightLeg);
        model.leftLeg.loadPose(cpm$leftLeg);
    }
}
