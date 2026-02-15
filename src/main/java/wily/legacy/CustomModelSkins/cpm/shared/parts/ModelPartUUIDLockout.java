package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.MinecraftClientAccess;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.definition.SafetyException;
import wily.legacy.CustomModelSkins.cpm.shared.definition.SafetyException.BlockReason;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;
import java.util.UUID;

public class ModelPartUUIDLockout implements IModelPart, IResolvedModelPart {
    private UUID lockID;

    public ModelPartUUIDLockout(IOHelper in, ModelDefinition def) throws IOException {
        lockID = in.readUUID();
    }

    @Override
    public IResolvedModelPart resolve() throws IOException {
        return this;
    }

    @Override
    public void write(IOHelper dout) throws IOException {
        dout.writeUUID(lockID);
    }

    @Override
    public ModelPartType getType() {
        return ModelPartType.UUID_LOCK;
    }

    @Override
    public void apply(ModelDefinition def) {
        UUID uuid = def.getPlayerObj().getUUID();
        if (!lockID.equals(uuid) && !MinecraftClientAccess.get().getClientPlayer().getUUID().equals(lockID))
            throw new SafetyException(BlockReason.UUID_LOCK);
    }
}
