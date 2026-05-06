package wily.legacy.skins.client.render.boxloader;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BoxModelBaker {
    private BoxModelBaker() {
    }

    static BuiltBoxModel bake(int texW, int texH, float texelScale, List<BoneDef> bones, EnumSet<AttachSlot> hide) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        Bounds bounds = new Bounds();
        EnumMap<AttachSlot, Bounds> coreSlotBounds = new EnumMap<>(AttachSlot.class);
        Map<BoneKey, BoneBuild> builds = collectBuilds(bones);
        for (BoneBuild build : builds.values()) {
            addCubes(build, texelScale, bounds, coreSlotBounds);
        }
        for (BoneBuild build : builds.values()) {
            addPart(root, builds, build, texelScale, new LinkedHashSet<>());
        }
        ModelPart bakedRoot = LayerDefinition.create(mesh, texW, texH).bakeRoot();
        EnumMap<AttachSlot, List<ModelPart>> parts = new EnumMap<>(AttachSlot.class);
        IdentityHashMap<ModelPart, Integer> armorMasks = new IdentityHashMap<>();
        for (BoneBuild build : builds.values()) {
            if (!build.root) continue;
            ModelPart child = getChild(bakedRoot, build.childName);
            if (child == null) continue;
            parts.computeIfAbsent(build.key.slot(), ignored -> new ArrayList<>()).add(child);
            armorMasks.put(child, build.key.armorMask());
        }
        parts.replaceAll((slot, list) -> List.copyOf(list));
        return new BuiltBoxModel(texW, texH, 1.0F / texelScale, bounds.height(), bounds.width(), parts, hide == null ? EnumSet.noneOf(AttachSlot.class) : hide, armorMasks, slotSizes(coreSlotBounds));
    }

    private static Map<BoneKey, BoneBuild> collectBuilds(List<BoneDef> bones) {
        Map<BoneKey, BoneBuild> builds = new LinkedHashMap<>();
        if (bones == null) return builds;
        int index = 0;
        for (BoneDef bone : bones) {
            if (bone == null || bone.attach() == null || Boolean.FALSE.equals(bone.visible())) continue;
            String name = boneName(bone, index);
            Set<Integer> masks = armorMasks(bone);
            for (int mask : masks) {
                BoneKey key = new BoneKey(name, bone.attach(), mask);
                builds.putIfAbsent(key, new BoneBuild(key, bone, "consoleskins$bone_" + index + "_" + mask));
            }
            index++;
        }
        return builds;
    }

    private static Set<Integer> armorMasks(BoneDef bone) {
        Set<Integer> masks = new LinkedHashSet<>();
        if (bone.cubes() != null) {
            for (CubeDef cube : bone.cubes()) {
                if (validCube(cube)) masks.add(Math.max(0, cube.armorMask()));
            }
        }
        if (bone.planes() != null) {
            for (PlaneDef plane : bone.planes()) {
                if (validPlane(plane)) masks.add(Math.max(0, plane.armorMask()));
            }
        }
        if (masks.isEmpty()) masks.add(0);
        return masks;
    }

    private static void addCubes(BoneBuild build, float texelScale, Bounds bounds,
                                 EnumMap<AttachSlot, Bounds> coreSlotBounds) {
        float[] pivot = vec3(build.bone.pivot());
        CubeListBuilder builder = build.builder;
        if (build.bone.cubes() != null) {
            for (CubeDef cube : build.bone.cubes()) {
                if (!validCube(cube) || Math.max(0, cube.armorMask()) != build.key.armorMask()) continue;
                float[] origin = cube.origin();
                float[] size = cube.size();
                int[] uv = uv(cube.uv());
                builder = builder.texOffs(uv[0], uv[1]);
                builder = cube.mirror() ? builder.mirror() : builder.mirror(false);
                builder = builder.addBox(
                        (origin[0] - pivot[0]) * texelScale,
                        (origin[1] - pivot[1]) * texelScale,
                        (origin[2] - pivot[2]) * texelScale,
                        size[0] * texelScale,
                        size[1] * texelScale,
                        size[2] * texelScale,
                        new CubeDeformation(cube.inflate() * texelScale)
                );
                bounds.add(origin, size);
                coreSlotBounds.merge(build.key.slot(), Bounds.of(origin, size), Bounds::larger);
            }
        }
        if (build.bone.planes() != null) {
            for (PlaneDef plane : build.bone.planes()) {
                if (!validPlane(plane) || Math.max(0, plane.armorMask()) != build.key.armorMask()) continue;
                float[] origin = plane.origin();
                float[] size = plane.size();
                int[] uv = uv(plane.uv());
                Direction face = face(plane.face());
                builder = builder.texOffs(uv[0], uv[1]);
                builder = plane.mirror() ? builder.mirror() : builder.mirror(false);
                builder = builder.addBox(
                        (origin[0] - pivot[0]) * texelScale,
                        (origin[1] - pivot[1]) * texelScale,
                        (origin[2] - pivot[2]) * texelScale,
                        size[0] * texelScale,
                        size[1] * texelScale,
                        size[2] * texelScale,
                        EnumSet.of(face)
                );
                bounds.add(origin, size);
                coreSlotBounds.merge(build.key.slot(), Bounds.of(origin, size), Bounds::larger);
            }
        }
        build.builder = builder;
    }

    private static EnumMap<AttachSlot, float[]> slotSizes(EnumMap<AttachSlot, Bounds> bounds) {
        if (bounds == null || bounds.isEmpty()) return new EnumMap<>(AttachSlot.class);
        EnumMap<AttachSlot, float[]> sizes = new EnumMap<>(AttachSlot.class);
        bounds.forEach((slot, value) -> {
            if (value.hasBounds()) sizes.put(slot, value.size());
        });
        return sizes;
    }

    private static void addPart(PartDefinition root, Map<BoneKey, BoneBuild> builds, BoneBuild build, float texelScale, Set<BoneKey> stack) {
        if (build.added) return;
        BoneBuild parent = parent(builds, build);
        if (parent != null && stack.add(build.key)) {
            addPart(root, builds, parent, texelScale, stack);
            stack.remove(build.key);
        } else {
            parent = null;
        }
        PartDefinition owner = parent == null ? root : parent.part;
        build.root = parent == null;
        build.part = owner.addOrReplaceChild(build.childName, build.builder, pose(build, parent, texelScale));
        build.added = true;
    }

    private static BoneBuild parent(Map<BoneKey, BoneBuild> builds, BoneBuild build) {
        String parent = trimToNull(build.bone.parent());
        if (parent == null || parent.equals(build.key.name())) return null;
        return builds.get(new BoneKey(parent, build.key.slot(), build.key.armorMask()));
    }

    private static PartPose pose(BoneBuild build, BoneBuild parent, float texelScale) {
        float[] pivot = vec3(build.bone.pivot());
        if (parent != null) {
            float[] parentPivot = vec3(parent.bone.pivot());
            pivot = new float[]{pivot[0] - parentPivot[0], pivot[1] - parentPivot[1], pivot[2] - parentPivot[2]};
        }
        float[] rotation = vec3(build.bone.rotation());
        return PartPose.offsetAndRotation(
                pivot[0] * texelScale,
                pivot[1] * texelScale,
                pivot[2] * texelScale,
                (float) Math.toRadians(rotation[0]),
                (float) Math.toRadians(rotation[1]),
                (float) Math.toRadians(rotation[2])
        );
    }

    private static boolean validCube(CubeDef cube) {
        if (cube == null || Boolean.FALSE.equals(cube.visible())) return false;
        float[] origin = cube.origin();
        float[] size = cube.size();
        return origin != null && size != null && origin.length >= 3 && size.length >= 3;
    }

    private static boolean validPlane(PlaneDef plane) {
        if (plane == null || Boolean.FALSE.equals(plane.visible())) return false;
        float[] origin = plane.origin();
        float[] size = plane.size();
        return origin != null && size != null && origin.length >= 3 && size.length >= 3;
    }

    private static int[] uv(int[] value) {
        return value == null || value.length < 2 ? new int[]{0, 0} : value;
    }

    private static Direction face(String value) {
        if (value == null || value.isBlank()) return Direction.NORTH;
        try {
            return Direction.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Direction.NORTH;
        }
    }

    private static float[] vec3(float[] value) {
        if (value == null) return new float[]{0.0F, 0.0F, 0.0F};
        float x = value.length > 0 ? value[0] : 0.0F;
        float y = value.length > 1 ? value[1] : 0.0F;
        float z = value.length > 2 ? value[2] : 0.0F;
        return new float[]{x, y, z};
    }

    private static String boneName(BoneDef bone, int index) {
        String name = trimToNull(bone.name());
        return name == null ? "bone_" + index : name;
    }

    private static String trimToNull(String value) {
        return value == null || (value = value.trim()).isEmpty() ? null : value;
    }

    private static ModelPart getChild(ModelPart root, String name) {
        try {
            return root.getChild(name);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static final class BoneBuild {
        final BoneKey key;
        final BoneDef bone;
        final String childName;
        CubeListBuilder builder = CubeListBuilder.create();
        PartDefinition part;
        boolean added;
        boolean root;

        BoneBuild(BoneKey key, BoneDef bone, String childName) {
            this.key = key;
            this.bone = bone;
            this.childName = childName;
        }
    }

    private static final class Bounds {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        void add(float[] origin, float[] size) {
            minX = Math.min(minX, origin[0]);
            minY = Math.min(minY, origin[1]);
            minZ = Math.min(minZ, origin[2]);
            maxX = Math.max(maxX, origin[0] + size[0]);
            maxY = Math.max(maxY, origin[1] + size[1]);
            maxZ = Math.max(maxZ, origin[2] + size[2]);
        }

        static Bounds of(float[] origin, float[] size) {
            Bounds bounds = new Bounds();
            bounds.add(origin, size);
            return bounds;
        }

        Bounds larger(Bounds other) {
            if (other == null || !other.hasBounds()) return this;
            if (!hasBounds()) return other;
            return other.volume() > volume() ? other : this;
        }

        float height() {
            if (!hasBounds()) return 1.8F;
            float h = Math.max(0.0F, maxY - minY) / 16.0F;
            return h > 0.01F ? Math.max(1.8F, h) : 1.8F;
        }

        float width() {
            if (!hasBounds()) return 0.6F;
            float w = Math.max(Math.max(0.0F, maxX - minX), Math.max(0.0F, maxZ - minZ)) / 16.0F;
            return w > 0.01F ? Math.max(0.6F, w) : 0.6F;
        }

        boolean hasBounds() {
            return minX != Float.POSITIVE_INFINITY && minY != Float.POSITIVE_INFINITY && minZ != Float.POSITIVE_INFINITY
                    && maxX != Float.NEGATIVE_INFINITY && maxY != Float.NEGATIVE_INFINITY && maxZ != Float.NEGATIVE_INFINITY;
        }

        float[] size() {
            if (!hasBounds()) return null;
            return new float[]{Math.max(0.0F, maxX - minX), Math.max(0.0F, maxY - minY), Math.max(0.0F, maxZ - minZ)};
        }

        float volume() {
            float[] size = size();
            return size == null ? 0.0F : size[0] * size[1] * size[2];
        }
    }

    private record BoneKey(String name, AttachSlot slot, int armorMask) {
    }
}
