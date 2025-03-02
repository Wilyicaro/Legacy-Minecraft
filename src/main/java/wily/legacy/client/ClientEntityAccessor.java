package wily.legacy.client;

import net.minecraft.world.entity.Entity;

public interface ClientEntityAccessor {
    static ClientEntityAccessor of(Entity entity){
        return (ClientEntityAccessor) entity;
    }

    void setAllowDisplayFireAnimation(boolean displayFireAnimation);

}
