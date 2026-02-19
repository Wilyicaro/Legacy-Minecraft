package wily.legacy.CustomModelSkins.cpm.client;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Unit;
import wily.legacy.CustomModelSkins.cpl.render.RecordBuffer;

import java.util.Collections;

public class CustomModelLayer extends Model<Unit> {
    public CustomModelLayer(RecordBuffer buffer) {
        super(new ModelPart(Collections.emptyList(), Collections.emptyMap()), null);
        ModelPartHooks.registerRecordBuffer(root(), buffer);
    }
}
