package wily.legacy.Skins.client.render.boxloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import wily.legacy.Skins.client.render.SkinPoseRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BoxModelJsonSupport {
    private BoxModelJsonSupport() {
    }

    static EnumSet<SkinPoseRegistry.PoseTag> parsePoseTags(JsonObject root) {
        EnumSet<SkinPoseRegistry.PoseTag> out = EnumSet.noneOf(SkinPoseRegistry.PoseTag.class);
        if (root == null) return out;

        JsonElement poses = null;
        if (root.has("poses")) poses = root.get("poses");
        else if (root.has("animations")) poses = root.get("animations");
        collectPoseTags(out, poses);

        JsonElement hide = root.has("hide") ? root.get("hide") : null;
        collectPoseTags(out, hide);

        return out;
    }

    static void collectPoseTags(EnumSet<SkinPoseRegistry.PoseTag> out, JsonElement el) {
        if (out == null || el == null || el.isJsonNull()) return;

        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isString()) {
                SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(p.getAsString());
                if (tag != null) out.add(tag);
            }
            return;
        }

        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                collectPoseTags(out, arr.get(i));
            }
            return;
        }

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(e.getKey());
                if (tag != null) {
                    JsonElement v = e.getValue();
                    if (v != null && v.isJsonPrimitive()) {
                        JsonPrimitive pv = v.getAsJsonPrimitive();
                        if (pv.isBoolean() && !pv.getAsBoolean()) {
                            collectPoseTags(out, v);
                            continue;
                        }
                    }
                    out.add(tag);
                }
                collectPoseTags(out, e.getValue());
            }
        }
    }

    static void putPoseTags(Map<String, EnumSet<SkinPoseRegistry.PoseTag>> map, String key, EnumSet<SkinPoseRegistry.PoseTag> tags) {
        if (map == null || key == null || key.isBlank() || tags == null || tags.isEmpty()) return;
        String k = key.trim();
        if (k.isEmpty()) return;
        EnumSet<SkinPoseRegistry.PoseTag> existing = map.get(k);
        if (existing == null || existing.isEmpty()) {
            map.put(k, EnumSet.copyOf(tags));
            return;
        }
        EnumSet<SkinPoseRegistry.PoseTag> merged = EnumSet.copyOf(existing);
        merged.addAll(tags);
        map.put(k, merged);
    }

    static EnumSet<AttachSlot> parseHideSlots(JsonElement el) {
        EnumSet<AttachSlot> out = EnumSet.noneOf(AttachSlot.class);
        if (el == null || el.isJsonNull()) return out;

        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isString()) {
                addHideToken(out, p.getAsString());
                return out;
            }
            if (p.isBoolean()) {
                if (p.getAsBoolean()) out.addAll(EnumSet.allOf(AttachSlot.class));
                return out;
            }
            return out;
        }

        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement e = arr.get(i);
                if (e == null || !e.isJsonPrimitive()) continue;
                JsonPrimitive p = e.getAsJsonPrimitive();
                if (!p.isString()) continue;
                addHideToken(out, p.getAsString());
            }
            return out;
        }

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("all") && obj.get("all").isJsonPrimitive() && obj.get("all").getAsJsonPrimitive().isBoolean() && obj.get("all").getAsBoolean()) {
                out.addAll(EnumSet.allOf(AttachSlot.class));
            }
            if (obj.has("parts")) {
                out.addAll(parseHideSlots(obj.get("parts")));
            }
            for (AttachSlot s : AttachSlot.values()) {
                String k1 = s.name();
                String k2 = s.name().toLowerCase(java.util.Locale.ROOT);
                if (obj.has(k1)) addHideToken(out, k1 + (obj.get(k1).getAsBoolean() ? "" : ""));
                if (obj.has(k2) && obj.get(k2).isJsonPrimitive() && obj.get(k2).getAsJsonPrimitive().isBoolean() && obj.get(k2).getAsBoolean()) out.add(s);
            }
            return out;
        }

        return out;
    }

    static EnumMap<AttachSlot, float[]> parseOffsets(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        EnumMap<AttachSlot, float[]> out = new EnumMap<>(AttachSlot.class);
        try {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    AttachSlot slot = AttachSlot.fromString(e.getKey());
                    if (slot == null) continue;
                    float[] v = parseVec3(e.getValue());
                    if (v != null) out.put(slot, v);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    static EnumMap<ArmorSlot, float[]> parseArmorOffsets(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        EnumMap<ArmorSlot, float[]> out = new EnumMap<>(ArmorSlot.class);
        try {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    ArmorSlot slot = ArmorSlot.fromString(e.getKey());
                    if (slot == null) continue;
                    float[] v = parseVec3(e.getValue());
                    if (v != null) out.put(slot, v);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    static EnumSet<ArmorSlot> parseArmorHideSlots(JsonElement el) {
        EnumSet<ArmorSlot> out = EnumSet.noneOf(ArmorSlot.class);
        if (el == null || el.isJsonNull()) return out;

        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isString()) {
                addArmorHideToken(out, p.getAsString());
                return out;
            }
            if (p.isBoolean()) {
                if (p.getAsBoolean()) out.addAll(EnumSet.allOf(ArmorSlot.class));
                return out;
            }
            return out;
        }

        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement e = arr.get(i);
                if (e == null || !e.isJsonPrimitive()) continue;
                JsonPrimitive p = e.getAsJsonPrimitive();
                if (!p.isString()) continue;
                addArmorHideToken(out, p.getAsString());
            }
            return out;
        }

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("all") && obj.get("all").isJsonPrimitive() && obj.get("all").getAsJsonPrimitive().isBoolean() && obj.get("all").getAsBoolean()) {
                out.addAll(EnumSet.allOf(ArmorSlot.class));
            }
            if (obj.has("parts")) {
                out.addAll(parseArmorHideSlots(obj.get("parts")));
            }
            for (ArmorSlot s : ArmorSlot.values()) {
                String k1 = s.name();
                String k2 = s.name().toLowerCase(java.util.Locale.ROOT);
                if (obj.has(k2) && obj.get(k2).isJsonPrimitive() && obj.get(k2).getAsJsonPrimitive().isBoolean() && obj.get(k2).getAsBoolean()) out.add(s);
                if (obj.has(k1) && obj.get(k1).isJsonPrimitive() && obj.get(k1).getAsJsonPrimitive().isBoolean() && obj.get(k1).getAsBoolean()) out.add(s);
            }
            return out;
        }

        return out;
    }

    static void addArmorHideToken(EnumSet<ArmorSlot> out, String token) {
        if (token == null) return;
        String t = token.trim();
        if (t.isEmpty()) return;
        String u = t.toUpperCase(java.util.Locale.ROOT);

        if ("ALL".equals(u)) {
            out.addAll(EnumSet.allOf(ArmorSlot.class));
            return;
        }
        if ("NONE".equals(u)) {
            out.clear();
            return;
        }

        ArmorSlot slot = ArmorSlot.fromString(u);
        if (slot != null) out.add(slot);
    }

    static float[] parseVec3(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        try {
            if (el.isJsonArray()) {
                JsonArray a = el.getAsJsonArray();
                float x = a.size() > 0 ? a.get(0).getAsFloat() : 0f;
                float y = a.size() > 1 ? a.get(1).getAsFloat() : 0f;
                float z = a.size() > 2 ? a.get(2).getAsFloat() : 0f;
                return new float[]{x, y, z};
            }
            if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                float x = o.has("x") ? o.get("x").getAsFloat() : (o.has("X") ? o.get("X").getAsFloat() : 0f);
                float y = o.has("y") ? o.get("y").getAsFloat() : (o.has("Y") ? o.get("Y").getAsFloat() : 0f);
                float z = o.has("z") ? o.get("z").getAsFloat() : (o.has("Z") ? o.get("Z").getAsFloat() : 0f);
                return new float[]{x, y, z};
            }
            if (el.isJsonPrimitive()) {
                JsonPrimitive p = el.getAsJsonPrimitive();
                if (p.isNumber()) {
                    return new float[]{0f, p.getAsFloat(), 0f};
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    static void addHideToken(EnumSet<AttachSlot> out, String token) {
        if (token == null) return;
        String t = token.trim();
        if (t.isEmpty()) return;
        String u = t.toUpperCase(java.util.Locale.ROOT);

        if ("ALL".equals(u)) {
            out.addAll(EnumSet.allOf(AttachSlot.class));
            return;
        }
        if ("NONE".equals(u)) {
            out.clear();
            return;
        }
        if ("LIMBS".equals(u)) {
            out.add(AttachSlot.LEFT_ARM);
            out.add(AttachSlot.RIGHT_ARM);
            out.add(AttachSlot.LEFT_LEG);
            out.add(AttachSlot.RIGHT_LEG);
            return;
        }
        if ("ARMS".equals(u)) {
            out.add(AttachSlot.LEFT_ARM);
            out.add(AttachSlot.RIGHT_ARM);
            return;
        }
        if ("LEGS".equals(u)) {
            out.add(AttachSlot.LEFT_LEG);
            out.add(AttachSlot.RIGHT_LEG);
            return;
        }
        if ("HEAD_AND_HAT".equals(u) || "HEAD+HAT".equals(u)) {
            out.add(AttachSlot.HEAD);
            out.add(AttachSlot.HAT);
            return;
        }

        try {
            out.add(AttachSlot.valueOf(u));
        } catch (Throwable ignored) {
        }
    }

    static java.util.List<BoneDef> expandMirrors(JsonObject root, BoneDef[] bonesArr) {
        java.util.List<BoneDef> bones = new java.util.ArrayList<>();
        java.util.Collections.addAll(bones, bonesArr);

        if (root == null) return bones;

        boolean mirrorArm = false;
        boolean mirrorLeg = false;

        JsonElement el = root.has("mirror_limbs") ? root.get("mirror_limbs") : (root.has("mirrorLimbs") ? root.get("mirrorLimbs") : null);
        if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean() && el.getAsBoolean()) {
            mirrorArm = true;
            mirrorLeg = true;
        }

        JsonElement el2 = root.has("mirror_right_arm") ? root.get("mirror_right_arm") : (root.has("mirrorRightArm") ? root.get("mirrorRightArm") : null);
        if (el2 != null && el2.isJsonPrimitive() && el2.getAsJsonPrimitive().isBoolean() && el2.getAsBoolean()) mirrorArm = true;
        JsonElement el3 = root.has("mirror_right_leg") ? root.get("mirror_right_leg") : (root.has("mirrorRightLeg") ? root.get("mirrorRightLeg") : null);
        if (el3 != null && el3.isJsonPrimitive() && el3.getAsJsonPrimitive().isBoolean() && el3.getAsBoolean()) mirrorLeg = true;

        JsonObject mirrorObj = root.has("mirror") && root.get("mirror").isJsonObject() ? root.getAsJsonObject("mirror") : null;
        if (mirrorObj != null) {
            JsonElement a = mirrorObj.has("rightArmFromLeft") ? mirrorObj.get("rightArmFromLeft") : (mirrorObj.has("right_arm_from_left") ? mirrorObj.get("right_arm_from_left") : null);
            if (a != null && a.isJsonPrimitive() && a.getAsJsonPrimitive().isBoolean() && a.getAsBoolean()) mirrorArm = true;
            JsonElement l = mirrorObj.has("rightLegFromLeft") ? mirrorObj.get("rightLegFromLeft") : (mirrorObj.has("right_leg_from_left") ? mirrorObj.get("right_leg_from_left") : null);
            if (l != null && l.isJsonPrimitive() && l.getAsJsonPrimitive().isBoolean() && l.getAsBoolean()) mirrorLeg = true;
        }

        if (!mirrorArm && !mirrorLeg) return bones;

        java.util.HashSet<String> names = new java.util.HashSet<>();
        for (BoneDef b : bones) if (b != null && b.name() != null) names.add(b.name());

        if (mirrorArm) {
            boolean hasLeftArm = false;
            boolean hasLeftSleeve = false;
            for (BoneDef b : bonesArr) {
                if (b == null || b.attach() == null) continue;
                if (b.attach() == AttachSlot.LEFT_ARM) hasLeftArm = true;
                if (b.attach() == AttachSlot.LEFT_SLEEVE) hasLeftSleeve = true;
            }
            if (hasLeftArm) {
                bones.removeIf(b -> b != null && b.attach() == AttachSlot.RIGHT_ARM);
                for (BoneDef b : bonesArr) {
                    if (b == null || b.attach() != AttachSlot.LEFT_ARM) continue;
                    bones.add(mirrorBone(b, AttachSlot.RIGHT_ARM, names));
                }
            }
            if (hasLeftSleeve) {
                bones.removeIf(b -> b != null && b.attach() == AttachSlot.RIGHT_SLEEVE);
                for (BoneDef b : bonesArr) {
                    if (b == null || b.attach() != AttachSlot.LEFT_SLEEVE) continue;
                    bones.add(mirrorBone(b, AttachSlot.RIGHT_SLEEVE, names));
                }
            } else if (hasLeftArm) {
                bones.removeIf(b -> b != null && b.attach() == AttachSlot.RIGHT_SLEEVE);
                for (BoneDef b : bonesArr) {
                    if (b == null || b.attach() != AttachSlot.LEFT_ARM) continue;
                    BoneDef mb = mirrorBoneFiltered(b, AttachSlot.RIGHT_SLEEVE, names, true);
                    if (mb != null) bones.add(mb);
                }
            }
        }

        if (mirrorLeg) {
            boolean hasLeftLeg = false;
            boolean hasLeftPants = false;
            for (BoneDef b : bonesArr) {
                if (b == null || b.attach() == null) continue;
                if (b.attach() == AttachSlot.LEFT_LEG) hasLeftLeg = true;
                if (b.attach() == AttachSlot.LEFT_PANTS) hasLeftPants = true;
            }
            if (hasLeftLeg) {
                bones.removeIf(b -> b != null && b.attach() == AttachSlot.RIGHT_LEG);
                for (BoneDef b : bonesArr) {
                    if (b == null || b.attach() != AttachSlot.LEFT_LEG) continue;
                    bones.add(mirrorBone(b, AttachSlot.RIGHT_LEG, names));
                }
            }
            if (hasLeftPants) {
                bones.removeIf(b -> b != null && b.attach() == AttachSlot.RIGHT_PANTS);
                for (BoneDef b : bonesArr) {
                    if (b == null || b.attach() != AttachSlot.LEFT_PANTS) continue;
                    bones.add(mirrorBone(b, AttachSlot.RIGHT_PANTS, names));
                }
            } else if (hasLeftLeg) {
                bones.removeIf(b -> b != null && b.attach() == AttachSlot.RIGHT_PANTS);
                for (BoneDef b : bonesArr) {
                    if (b == null || b.attach() != AttachSlot.LEFT_LEG) continue;
                    BoneDef mb = mirrorBoneFiltered(b, AttachSlot.RIGHT_PANTS, names, true);
                    if (mb != null) bones.add(mb);
                }
            }
        }

        return bones;
    }

    static BoneDef mirrorBone(BoneDef src, AttachSlot dstAttach, java.util.Set<String> usedNames) {
        return mirrorBoneFiltered(src, dstAttach, usedNames, false);
    }

    static BoneDef mirrorBoneFiltered(BoneDef src, AttachSlot dstAttach, java.util.Set<String> usedNames, boolean onlyInflated) {
        String base = src.name() == null ? "bone" : src.name();
        String n = base;
        if (usedNames != null) {
            int i = 0;
            while (usedNames.contains(n)) {
                i++;
                n = base + "_m" + i;
            }
            usedNames.add(n);
        }

        java.util.List<CubeDef> outCubes = new java.util.ArrayList<>();
        if (src.cubes() != null) {
            for (CubeDef c : src.cubes()) {
                if (c == null) continue;
                if (onlyInflated && !(c.inflate() > 0.0001F)) continue;
                float[] o = c.origin();
                float[] s = c.size();
                if (o == null || s == null || o.length < 3 || s.length < 3) continue;
                float[] no = new float[]{-(o[0] + s[0]), o[1], o[2]};
                int[] uv = c.uv() == null ? new int[]{0, 0} : new int[]{c.uv()[0], c.uv()[1]};
                outCubes.add(new CubeDef(uv, no, new float[]{s[0], s[1], s[2]}, c.inflate(), true));
            }
        }

        if (outCubes.isEmpty()) return null;

        return new BoneDef(n, dstAttach, outCubes);
    }

}
