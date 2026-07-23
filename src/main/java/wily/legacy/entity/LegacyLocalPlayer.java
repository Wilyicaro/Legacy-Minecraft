package wily.legacy.entity;

import net.minecraft.world.phys.Vec3;

public interface LegacyLocalPlayer {

    boolean canSprintController();

    boolean isLegacyElytraBoosting();

    boolean isLegacyElytraBoostBobbing();

    double getLegacyElytraBoostYBobMovement();

    float getLegacyUnderwaterVisionClarity();

    Vec3 getLegacyPreviousDeltaMovement();
}
