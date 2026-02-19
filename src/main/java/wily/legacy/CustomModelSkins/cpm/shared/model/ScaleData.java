package wily.legacy.CustomModelSkins.cpm.shared.model;

import wily.legacy.CustomModelSkins.cpm.shared.util.ScalingOptions;

import java.util.EnumMap;
import java.util.Map;

public class ScaleData extends PartPosition {
    private Map<ScalingOptions, Float> scaling;

    @Deprecated
    public ScaleData(float scale) {
        scaling = new EnumMap<>(ScalingOptions.class);
        this.scaling.put(ScalingOptions.ENTITY, scale);
    }

    private ScaleData() {
        this(new EnumMap<>(ScalingOptions.class));
    }

    public ScaleData(Map<ScalingOptions, Float> scaling) {
        this.scaling = scaling;
    }

    @Deprecated
    public void setScale(float eyeHeight, float hitboxW, float hitboxH) {
        this.scaling.put(ScalingOptions.EYE_HEIGHT, eyeHeight);
        this.scaling.put(ScalingOptions.HITBOX_WIDTH, hitboxW);
        this.scaling.put(ScalingOptions.EYE_HEIGHT, hitboxH);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((rPos == null) ? 0 : rPos.hashCode());
        result = prime * result + ((rRotation == null) ? 0 : rRotation.hashCode());
        result = prime * result + ((rScale == null) ? 0 : rScale.hashCode());
        result = prime * result + ((scaling == null) ? 0 : scaling.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ScaleData other = (ScaleData) obj;
        if (rPos == null) {
            if (other.rPos != null) return false;
        } else if (!rPos.equals(other.rPos)) return false;
        if (rRotation == null) {
            if (other.rRotation != null) return false;
        } else if (!rRotation.equals(other.rRotation)) return false;
        if (rScale == null) {
            if (other.rScale != null) return false;
        } else if (!rScale.equals(other.rScale)) return false;
        if (scaling == null) {
            if (other.scaling != null) return false;
        } else if (!scaling.equals(other.scaling)) return false;
        return true;
    }
}
