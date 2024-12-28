package wily.legacy.util;

import net.minecraft.core.Rotations;
import net.minecraft.world.entity.decoration.ArmorStand;
import wily.factoryapi.base.Stocker;

import java.util.ArrayList;
import java.util.List;

public record ArmorStandPose(Rotations headPose, Rotations bodyPose, Rotations leftArmPose, Rotations rightArmPose, Rotations leftLegPose, Rotations rightLegPose) {
    public static final Rotations ZERO_POSE = new Rotations(0.0F, 0.0F, 0.0F);
    public static final Rotations DEFAULT_LEFT_ARM_POSE = new Rotations(-10.0F, 0.0F, -10.0F);
    public static final Rotations DEFAULT_RIGHT_ARM_POSE = new Rotations(-15.0F, 0.0F, 10.0F);
    public static final Rotations DEFAULT_LEFT_LEG_POSE = new Rotations(-1.0F, 0.0F, -1.0F);
    public static final Rotations DEFAULT_RIGHT_LEG_POSE = new Rotations(1.0F, 0.0F, 1.0F);
    public static ArmorStandPose DEFAULT = new ArmorStandPose(ZERO_POSE,ZERO_POSE,DEFAULT_LEFT_ARM_POSE,DEFAULT_RIGHT_ARM_POSE,DEFAULT_LEFT_LEG_POSE,DEFAULT_RIGHT_LEG_POSE);
    public static List<ArmorStandPose> LIST = new ArrayList<>(15);
    public ArmorStandPose withArmsAndLegs(Rotations leftArmPose, Rotations rightArmPose, Rotations leftLegPose, Rotations rightLegPose){
        return new ArmorStandPose(headPose,bodyPose,leftArmPose,rightArmPose,leftLegPose,rightLegPose);
    }
    public ArmorStandPose withArms(Rotations leftArmPose, Rotations rightArmPose){
        return new ArmorStandPose(headPose,bodyPose,leftArmPose,rightArmPose,leftLegPose,rightLegPose);
    }
    public void applyPose(ArmorStand armorStand){
        if (headPose != null) armorStand.setHeadPose(headPose);
        if (bodyPose != null) armorStand.setBodyPose(bodyPose);
        if (leftArmPose != null) armorStand.setLeftArmPose(leftArmPose);
        if (rightArmPose != null) armorStand.setRightArmPose(rightArmPose);
        if (leftLegPose != null) armorStand.setLeftLegPose(leftLegPose);
        if (rightLegPose != null) armorStand.setRightLegPose(rightLegPose);
    }
    public boolean isApplied(ArmorStand armorStand){
        return armorStand.getHeadPose().equals(headPose) && armorStand.getBodyPose().equals(bodyPose) && armorStand.getLeftArmPose().equals(leftArmPose) && armorStand.getRightArmPose().equals(rightArmPose) && armorStand.getLeftLegPose().equals(leftLegPose) && armorStand.getRightLegPose().equals(rightLegPose);
    }
    public static ArmorStandPose getActualPose(int signal){
        return LIST.get(Math.min(signal - 1,LIST.size() - 1));
    }
    public static ArmorStandPose getActualPose(ArmorStand armorStand){
        for (ArmorStandPose pose : LIST) {
            if (pose.isApplied(armorStand)) return pose;
        }
        return null;
    }
    public static ArmorStandPose getNextPose(ArmorStand armorStand){
        ArmorStandPose pose = getActualPose(armorStand);
        return pose == null ? DEFAULT : LIST.get(Stocker.cyclic(0, LIST.indexOf(pose) + 1,LIST.size() - 1));
    }
    public static void init(){
        ArmorStandPose.LIST.add(ArmorStandPose.DEFAULT);
        ArmorStandPose.LIST.add(ArmorStandPose.DEFAULT.withArmsAndLegs(ArmorStandPose.ZERO_POSE,ArmorStandPose.ZERO_POSE,ArmorStandPose.ZERO_POSE,ArmorStandPose.ZERO_POSE));
        ArmorStandPose.LIST.add(new ArmorStandPose(new Rotations(16,0,0),ArmorStandPose.ZERO_POSE,new Rotations(303,18,0),new Rotations(303,338,0),ArmorStandPose.DEFAULT_LEFT_LEG_POSE,ArmorStandPose.DEFAULT_RIGHT_LEG_POSE));
        ArmorStandPose.LIST.add(ArmorStandPose.DEFAULT.withArms(new Rotations(16,18,0),new Rotations(303,11,12)));
        ArmorStandPose.LIST.add(new ArmorStandPose(new Rotations(357,0,0),ArmorStandPose.ZERO_POSE,new Rotations(16,18,0),new Rotations(241,31,12),new Rotations(351,11,0),new Rotations(10,352,0)));
        ArmorStandPose.LIST.add(new ArmorStandPose(new Rotations(357,0,0),ArmorStandPose.ZERO_POSE,new Rotations(241,38,0),new Rotations(241,318,12),new Rotations(10,352,0),new Rotations(351,11,0)));
        ArmorStandPose.LIST.add(new ArmorStandPose(new Rotations(357,0,0),ArmorStandPose.ZERO_POSE,new Rotations(241,318,0),new Rotations(241,38,12),new Rotations(10,352,0),new Rotations(351,11,0)));
        ArmorStandPose.LIST.add(ArmorStandPose.DEFAULT.withArms(new Rotations(16,0,0),new Rotations(296,318,0)));
        ArmorStandPose.LIST.add(new ArmorStandPose(new Rotations(16,11,0),ArmorStandPose.ZERO_POSE,new Rotations(180,86,60),new Rotations(282,331,0),new Rotations(0,38,353),new Rotations(10,11,6)));
        ArmorStandPose.LIST.add(new ArmorStandPose(new Rotations(351,345,0),ArmorStandPose.ZERO_POSE,new Rotations(255,4,60),new Rotations(255,352,0),new Rotations(16,0,0),new Rotations(330,0,0)));
        ArmorStandPose.LIST.add(new ArmorStandPose(new Rotations(357,106,12),new Rotations(0,4,6),new Rotations(173,52,53),new Rotations(201,113,319),new Rotations(255,35,0),new Rotations(3,31,0)));
        ArmorStandPose.LIST.add(new ArmorStandPose(new Rotations(357,79,0),new Rotations(0,0,353),new Rotations(160,120,53),new Rotations(201,0,299),new Rotations(351,31,6),new Rotations(228,331,46)));
        ArmorStandPose.LIST.add(ArmorStandPose.DEFAULT.withArmsAndLegs(new Rotations(16,0,0),new Rotations(248,38,0),new Rotations(0,38,353),new Rotations(3,243,0)));
    }
}
