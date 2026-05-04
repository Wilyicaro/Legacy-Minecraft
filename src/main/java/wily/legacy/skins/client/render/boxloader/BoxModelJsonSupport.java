package wily.legacy.skins.client.render.boxloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import wily.legacy.skins.pose.SkinPoseRegistry;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class BoxModelJsonSupport {
    private BoxModelJsonSupport() {
    }

    static EnumSet<SkinPoseRegistry.PoseTag> parsePoseTags(JsonObject root) {
        EnumSet<SkinPoseRegistry.PoseTag> out = EnumSet.noneOf(SkinPoseRegistry.PoseTag.class);
        if (root == null) return out;
        JsonElement poses = root.has("poses") ? root.get("poses") : root.get("animations");
        collectPoseTags(out, poses);
        collectPoseTags(out, root.get("hide"));
        return out;
    }

    static void collectPoseTags(EnumSet<SkinPoseRegistry.PoseTag> out, JsonElement el) {
        if (out == null || el == null || el.isJsonNull()) return;
        if (el.isJsonPrimitive()) {
            JsonPrimitive primitive = el.getAsJsonPrimitive();
            if (!primitive.isString()) return;
            SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(primitive.getAsString());
            if (tag != null) out.add(tag);
            return;
        }
        if (el.isJsonArray()) {
            JsonArray array = el.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                collectPoseTags(out, array.get(i));
            }
            return;
        }
        if (!el.isJsonObject()) return;
        JsonObject obj = el.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(entry.getKey());
            if (tag != null) {
                JsonElement value = entry.getValue();
                if (isFalse(value)) {
                    collectPoseTags(out, value);
                    continue;
                }
                out.add(tag);
            }
            collectPoseTags(out, entry.getValue());
        }
    }

    static void putPoseTags(Map<String, EnumSet<SkinPoseRegistry.PoseTag>> map, String key, EnumSet<SkinPoseRegistry.PoseTag> tags) {
        if (map == null || key == null || key.isBlank() || tags == null || tags.isEmpty()) return;
        String normalized = key.trim();
        if (normalized.isEmpty()) return;
        EnumSet<SkinPoseRegistry.PoseTag> existing = map.get(normalized);
        if (existing == null || existing.isEmpty()) {
            map.put(normalized, EnumSet.copyOf(tags));
            return;
        }
        EnumSet<SkinPoseRegistry.PoseTag> merged = EnumSet.copyOf(existing);
        merged.addAll(tags);
        map.put(normalized, merged);
    }

    static EnumSet<AttachSlot> parseHideSlots(JsonElement el) {
        return parseEnumFlags(el, AttachSlot.class, BoxModelJsonSupport::addHideToken);
    }

    static EnumMap<AttachSlot, float[]> parseOffsets(JsonElement el) {
        return parseMap(el, AttachSlot.class, AttachSlot::fromString, BoxModelJsonSupport::parseVec3);
    }

    static EnumMap<ToolSlot, float[]> parseToolOffsets(JsonElement el) {
        return parseMap(el, ToolSlot.class, ToolSlot::fromString, BoxModelJsonSupport::parseVec3);
    }

    static EnumMap<AttachSlot, float[]> parseScales(JsonElement el) {
        return parseMap(el, AttachSlot.class, AttachSlot::fromString, BoxModelJsonSupport::parseScale3);
    }

    static EnumMap<ArmorSlot, float[]> parseArmorOffsets(JsonElement el) {
        return parseMap(el, ArmorSlot.class, ArmorSlot::fromString, BoxModelJsonSupport::parseVec3);
    }

    static EnumSet<ArmorSlot> parseArmorHideSlots(JsonElement el) {
        return parseEnumFlags(el, ArmorSlot.class, BoxModelJsonSupport::addArmorHideToken);
    }

    static float[] parseVec3(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        try {
            if (el.isJsonArray()) {
                JsonArray array = el.getAsJsonArray();
                float x = !array.isEmpty() ? array.get(0).getAsFloat() : 0f;
                float y = array.size() > 1 ? array.get(1).getAsFloat() : 0f;
                float z = array.size() > 2 ? array.get(2).getAsFloat() : 0f;
                return new float[]{x, y, z};
            }
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                float x = readAxis(obj, "x", "X");
                float y = readAxis(obj, "y", "Y");
                float z = readAxis(obj, "z", "Z");
                return new float[]{x, y, z};
            }
            if (el.isJsonPrimitive()) {
                JsonPrimitive primitive = el.getAsJsonPrimitive();
                if (primitive.isNumber()) return new float[]{0f, primitive.getAsFloat(), 0f};
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    static void addHideToken(EnumSet<AttachSlot> out, String token) {
        if (token == null) return;
        String value = token.trim();
        if (value.isEmpty()) return;
        String upper = value.toUpperCase(Locale.ROOT);
        if ("ALL".equals(upper)) {
            out.addAll(EnumSet.allOf(AttachSlot.class));
            return;
        }
        if ("NONE".equals(upper)) {
            out.clear();
            return;
        }
        if ("LIMBS".equals(upper)) {
            out.add(AttachSlot.LEFT_ARM);
            out.add(AttachSlot.RIGHT_ARM);
            out.add(AttachSlot.LEFT_LEG);
            out.add(AttachSlot.RIGHT_LEG);
            return;
        }
        if ("ARMS".equals(upper)) {
            out.add(AttachSlot.LEFT_ARM);
            out.add(AttachSlot.RIGHT_ARM);
            return;
        }
        if ("LEGS".equals(upper)) {
            out.add(AttachSlot.LEFT_LEG);
            out.add(AttachSlot.RIGHT_LEG);
            return;
        }
        if ("HEAD_AND_HAT".equals(upper) || "HEAD+HAT".equals(upper)) {
            out.add(AttachSlot.HEAD);
            out.add(AttachSlot.HAT);
            return;
        }
        try {
            out.add(AttachSlot.valueOf(upper));
        } catch (IllegalArgumentException ignored) {
        }
    }

    static void addArmorHideToken(EnumSet<ArmorSlot> out, String token) {
        if (token == null) return;
        String value = token.trim();
        if (value.isEmpty()) return;
        String upper = value.toUpperCase(Locale.ROOT);
        if ("ALL".equals(upper)) {
            out.addAll(EnumSet.allOf(ArmorSlot.class));
            return;
        }
        if ("NONE".equals(upper)) {
            out.clear();
            return;
        }
        ArmorSlot slot = ArmorSlot.fromString(upper);
        if (slot != null) out.add(slot);
    }

    static List<BoneDef> expandMirrors(JsonObject root, BoneDef[] sourceBones) {
        List<BoneDef> bones = new ArrayList<>();
        Collections.addAll(bones, sourceBones);
        if (root == null) return bones;
        JsonObject mirror = root.has("mirror") && root.get("mirror").isJsonObject() ? root.getAsJsonObject("mirror") : null;
        boolean mirrorArm = readMirrorFlag(root, mirror, "mirror_limbs", "mirrorLimbs", "mirror_right_arm", "mirrorRightArm", "right_arm_from_left", "rightArmFromLeft");
        boolean mirrorLeg = readMirrorFlag(root, mirror, "mirror_limbs", "mirrorLimbs", "mirror_right_leg", "mirrorRightLeg", "right_leg_from_left", "rightLegFromLeft");
        if (!mirrorArm && !mirrorLeg) return bones;
        java.util.HashSet<String> names = new java.util.HashSet<>();
        for (BoneDef bone : bones) {
            if (bone != null && bone.name() != null) names.add(bone.name());
        }
        if (mirrorArm) {
            mirrorSide(bones, sourceBones, names, AttachSlot.LEFT_ARM, AttachSlot.RIGHT_ARM, AttachSlot.LEFT_SLEEVE, AttachSlot.RIGHT_SLEEVE);
        }
        if (mirrorLeg) {
            mirrorSide(bones, sourceBones, names, AttachSlot.LEFT_LEG, AttachSlot.RIGHT_LEG, AttachSlot.LEFT_PANTS, AttachSlot.RIGHT_PANTS);
        }
        return bones;
    }

    static BoneDef mirrorBone(BoneDef src, AttachSlot dstAttach, Set<String> usedNames) {
        return mirrorBoneFiltered(src, dstAttach, usedNames, false);
    }

    static BoneDef mirrorBoneFiltered(BoneDef src, AttachSlot dstAttach, Set<String> usedNames, boolean onlyInflated) {
        String base = src.name() == null ? "bone" : src.name();
        String name = base;
        if (usedNames != null) {
            int index = 0;
            while (usedNames.contains(name)) {
                index++;
                name = base + "_m" + index;
            }
            usedNames.add(name);
        }
        List<CubeDef> outCubes = new ArrayList<>();
        if (src.cubes() != null) {
            for (CubeDef cube : src.cubes()) {
                if (cube == null) continue;
                if (onlyInflated && cube.inflate() <= 0.0001F) continue;
                float[] origin = cube.origin();
                float[] size = cube.size();
                if (origin == null || size == null || origin.length < 3 || size.length < 3) continue;
                float[] mirroredOrigin = new float[]{-(origin[0] + size[0]), origin[1], origin[2]};
                int[] uv = cube.uv() == null ? new int[]{0, 0} : new int[]{cube.uv()[0], cube.uv()[1]};
                outCubes.add(new CubeDef(uv, mirroredOrigin, new float[]{size[0], size[1], size[2]}, cube.inflate(), cube.mirror(), cube.visible(), cube.armorMask()));
            }
        }
        if (outCubes.isEmpty()) return null;
        return new BoneDef(name, src.parent(), dstAttach, mirrorPivot(src.pivot()), mirrorRotation(src.rotation()), outCubes, src.visible());
    }

    private static float[] mirrorPivot(float[] pivot) {
        if (pivot == null || pivot.length < 3) return pivot;
        return new float[]{-pivot[0], pivot[1], pivot[2]};
    }

    private static float[] mirrorRotation(float[] rotation) {
        if (rotation == null || rotation.length < 3) return rotation;
        return new float[]{rotation[0], -rotation[1], -rotation[2]};
    }

    private static <E extends Enum<E>> EnumSet<E> parseEnumFlags(JsonElement el, Class<E> type, BiConsumer<EnumSet<E>, String> tokenReader) {
        EnumSet<E> out = EnumSet.noneOf(type);
        if (el == null || el.isJsonNull()) return out;
        if (el.isJsonPrimitive()) {
            JsonPrimitive primitive = el.getAsJsonPrimitive();
            if (primitive.isString()) tokenReader.accept(out, primitive.getAsString());
            else if (primitive.isBoolean() && primitive.getAsBoolean()) out.addAll(EnumSet.allOf(type));
            return out;
        }
        if (el.isJsonArray()) {
            JsonArray array = el.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonElement value = array.get(i);
                if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    tokenReader.accept(out, value.getAsString());
                }
            }
            return out;
        }
        if (!el.isJsonObject()) return out;
        JsonObject obj = el.getAsJsonObject();
        if (isTrue(obj, "all")) out.addAll(EnumSet.allOf(type));
        if (obj.has("parts")) out.addAll(parseEnumFlags(obj.get("parts"), type, tokenReader));
        addBooleanFlags(out, obj, type.getEnumConstants());
        return out;
    }

    private static <E extends Enum<E>> void addBooleanFlags(EnumSet<E> out, JsonObject obj, E[] values) {
        for (E value : values) {
            String upper = value.name();
            String lower = upper.toLowerCase(Locale.ROOT);
            if (isTrue(obj, upper) || isTrue(obj, lower)) out.add(value);
        }
    }

    private static <E extends Enum<E>> EnumMap<E, float[]> parseMap(JsonElement el, Class<E> type, Function<String, E> parser, Function<JsonElement, float[]> valueParser) {
        if (el == null || el.isJsonNull() || !el.isJsonObject()) return null;
        EnumMap<E, float[]> out = new EnumMap<>(type);
        try {
            for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
                E slot = parser.apply(entry.getKey());
                if (slot == null) continue;
                float[] value = valueParser.apply(entry.getValue());
                if (value != null) out.put(slot, value);
            }
        } catch (RuntimeException ignored) {
        }
        return out;
    }

    private static float[] parseScale3(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        try {
            if (el.isJsonPrimitive()) {
                JsonPrimitive primitive = el.getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    float scale = primitive.getAsFloat();
                    return new float[]{scale, scale, scale};
                }
                return null;
            }
            if (el.isJsonArray()) {
                JsonArray array = el.getAsJsonArray();
                if (array.isEmpty()) return null;
                float x = array.get(0).getAsFloat();
                float y = array.size() > 1 ? array.get(1).getAsFloat() : x;
                float z = array.size() > 2 ? array.get(2).getAsFloat() : x;
                return new float[]{x, y, z};
            }
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                float base = obj.has("value") ? obj.get("value").getAsFloat() : 1.0F;
                float x = obj.has("x") || obj.has("X") ? readAxis(obj, "x", "X") : base;
                float y = obj.has("y") || obj.has("Y") ? readAxis(obj, "y", "Y") : base;
                float z = obj.has("z") || obj.has("Z") ? readAxis(obj, "z", "Z") : base;
                return new float[]{x, y, z};
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private static void mirrorSide(List<BoneDef> bones,
                                   BoneDef[] sourceBones,
                                   Set<String> names,
                                   AttachSlot srcBase,
                                   AttachSlot dstBase,
                                   AttachSlot srcOuter,
                                   AttachSlot dstOuter) {
        boolean hasBase = hasAttach(sourceBones, srcBase);
        boolean hasOuter = hasAttach(sourceBones, srcOuter);
        if (hasBase) {
            if (!hasAttach(sourceBones, dstBase)) addMirroredBones(bones, sourceBones, names, srcBase, dstBase, false);
        }
        if (hasOuter) {
            if (!hasAttach(sourceBones, dstOuter))
                addMirroredBones(bones, sourceBones, names, srcOuter, dstOuter, false);
            return;
        }
        if (hasBase && !hasAttach(sourceBones, dstOuter))
            addMirroredBones(bones, sourceBones, names, srcBase, dstOuter, true);
    }

    private static void addMirroredBones(List<BoneDef> bones,
                                         BoneDef[] sourceBones,
                                         Set<String> names,
                                         AttachSlot srcAttach,
                                         AttachSlot dstAttach,
                                         boolean onlyInflated) {
        for (BoneDef bone : sourceBones) {
            if (bone == null || bone.attach() != srcAttach) continue;
            BoneDef mirrored = onlyInflated
                    ? mirrorBoneFiltered(bone, dstAttach, names, true)
                    : mirrorBone(bone, dstAttach, names);
            if (mirrored != null) bones.add(mirrored);
        }
    }

    private static boolean hasAttach(BoneDef[] bones, AttachSlot slot) {
        for (BoneDef bone : bones) {
            if (bone != null && bone.attach() == slot) return true;
        }
        return false;
    }

    private static boolean readMirrorFlag(JsonObject root,
                                          JsonObject mirror,
                                          String limbsSnake,
                                          String limbsCamel,
                                          String directSnake,
                                          String directCamel,
                                          String nestedSnake,
                                          String nestedCamel) {
        if (isTrue(root, limbsSnake) || isTrue(root, limbsCamel)) return true;
        if (isTrue(root, directSnake) || isTrue(root, directCamel)) return true;
        return mirror != null && (isTrue(mirror, nestedSnake) || isTrue(mirror, nestedCamel));
    }

    private static float readAxis(JsonObject obj, String lower, String upper) {
        if (obj.has(lower)) return obj.get(lower).getAsFloat();
        if (obj.has(upper)) return obj.get(upper).getAsFloat();
        return 0f;
    }

    private static boolean isFalse(JsonElement el) {
        if (el == null || !el.isJsonPrimitive()) return false;
        JsonPrimitive primitive = el.getAsJsonPrimitive();
        return primitive.isBoolean() && !primitive.getAsBoolean();
    }

    private static boolean isTrue(JsonObject obj, String key) {
        return obj.has(key)
                && obj.get(key).isJsonPrimitive()
                && obj.get(key).getAsJsonPrimitive().isBoolean()
                && obj.get(key).getAsBoolean();
    }
}
