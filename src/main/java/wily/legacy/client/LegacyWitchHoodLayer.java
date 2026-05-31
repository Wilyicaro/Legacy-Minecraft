package wily.legacy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.WitchRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

public class LegacyWitchHoodLayer extends RenderLayer<WitchRenderState, WitchModel> {
    private static final Identifier WITCH_TEXTURE = Identifier.withDefaultNamespace("textures/entity/witch.png");
    private final ModelPart hood = createHood();

    public LegacyWitchHoodLayer(RenderLayerParent<WitchRenderState, WitchModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, WitchRenderState state, float partialTick, float ageInTicks) {
        if (state.isInvisible || !Minecraft.getInstance().getResourcePackRepository().getSelectedIds().contains("legacy:console_aspects")) return;
        WitchModel model = getParentModel();
        ModelPart root = snapshotPart(model.root());
        ModelPart head = snapshotPart(model.getHead());
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(WITCH_TEXTURE), (pose, vc) -> {
            PoseStack ps = new PoseStack();
            ps.last().set(pose);
            root.translateAndRotate(ps);
            head.translateAndRotate(ps);
            hood.render(ps, vc, packedLight, OverlayTexture.NO_OVERLAY);
        });
    }

    private static ModelPart createHood() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("hood", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.51F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 128).bakeRoot().getChild("hood");
    }

    private static ModelPart snapshotPart(ModelPart part) {
        ModelPart snapshot = new ModelPart(List.of(), Map.of());
        snapshot.loadPose(new PartPose(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.xScale, part.yScale, part.zScale));
        return snapshot;
    }
}
