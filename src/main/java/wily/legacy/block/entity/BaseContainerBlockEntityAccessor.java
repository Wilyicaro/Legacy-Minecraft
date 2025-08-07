package wily.legacy.block.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

public interface BaseContainerBlockEntityAccessor {

    static BaseContainerBlockEntityAccessor of(BaseContainerBlockEntity be) {
        return (BaseContainerBlockEntityAccessor) be;
    }

    void setTempName(Component component);

    Component getTempName();

}
