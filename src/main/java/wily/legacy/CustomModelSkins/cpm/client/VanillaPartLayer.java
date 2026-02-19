package wily.legacy.CustomModelSkins.cpm.client;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class VanillaPartLayer extends Model<PartPose> {
    public VanillaPartLayer(ModelPart p_368583_, Function<ResourceLocation, RenderType> p_103110_) {
        super(p_368583_, p_103110_);
    }

    @Override
    public void setupAnim(PartPose p_435637_) {
        super.setupAnim(p_435637_);
        root().loadPose(p_435637_);
    }
}
