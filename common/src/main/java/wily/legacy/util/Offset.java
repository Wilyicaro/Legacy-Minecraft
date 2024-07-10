package wily.legacy.util;

import com.mojang.blaze3d.vertex.PoseStack;

public record Offset(double x, double y, double z) {
    public static final Offset ZERO = new Offset(0,0,0);

    public void apply(PoseStack pose){
        pose.translate(x,y,z);
    }
    public Offset copy(){
        return new Offset(x,y,z);
    }
}
