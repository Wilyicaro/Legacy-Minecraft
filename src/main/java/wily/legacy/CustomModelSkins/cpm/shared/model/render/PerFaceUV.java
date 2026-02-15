package wily.legacy.CustomModelSkins.cpm.shared.model.render;

import wily.legacy.CustomModelSkins.cpl.math.Vec4f;
import wily.legacy.CustomModelSkins.cpl.util.Direction;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PerFaceUV {
    public final Map<Direction, Face> faces = new HashMap<>();

    public static class Face {
        public int sx, sy, ex, ey;
        public Rot rotation = Rot.ROT_0;
        public boolean autoUV;

        public Face() {
        }

        public Face(Face f) {
            sx = f.sx;
            sy = f.sy;
            ex = f.ex;
            ey = f.ey;
            rotation = f.rotation;
            autoUV = f.autoUV;
        }

        private Face(Map<String, Object> m) {
            sx = ((Number) m.getOrDefault("sx", 0)).intValue();
            sy = ((Number) m.getOrDefault("sy", 0)).intValue();
            ex = ((Number) m.getOrDefault("ex", 0)).intValue();
            ey = ((Number) m.getOrDefault("ey", 0)).intValue();
            String rot = "ROT_" + String.valueOf(m.getOrDefault("rot", "0")).toUpperCase(Locale.ROOT);
            for (Rot r : Rot.VALUES)
                if (r.name().equals(rot)) {
                    rotation = r;
                    break;
                }
            autoUV = Boolean.TRUE.equals(m.get("autoUV"));
        }

        public static Face load(Map<String, Object> m) {
            return m == null ? null : new Face(m);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("sx", sx);
            m.put("sy", sy);
            m.put("ex", ex);
            m.put("ey", ey);
            m.put("rot", rotation.name().toLowerCase(Locale.ROOT).substring(4));
            m.put("autoUV", autoUV);
            return m;
        }

        public int getVertexU(int index) {
            int i = getVertexRotated(index);
            return i != 0 && i != 1 ? ex : sx;
        }

        public int getVertexV(int index) {
            int i = getVertexRotated(index);
            return i != 0 && i != 3 ? ey : sy;
        }

        private int getVertexRotated(int index) {
            return (index + rotation.ordinal() + 3) % 4;
        }

        public Vec4f getVec() {
            return new Vec4f(sx, sy, ex, ey);
        }
    }

    public void readFaces(IOHelper h) throws IOException {
        int hidden = h.read();
        for (Direction dir : Direction.VALUES)
            if ((hidden & (1 << dir.ordinal())) != 0) {
                Face f = new Face();
                f.sx = h.readVarInt();
                f.sy = h.readVarInt();
                f.ex = h.readVarInt();
                f.ey = h.readVarInt();
                f.rotation = h.readEnum(Rot.VALUES);
                faces.put(dir, f);
            }
    }

    public void writeFaces(IOHelper h) throws IOException {
        int hidden = 0;
        for (Direction dir : Direction.VALUES) if (faces.get(dir) != null) hidden |= (1 << dir.ordinal());
        h.write(hidden);
        for (Direction dir : Direction.VALUES) {
            Face face = faces.get(dir);
            if (face != null) {
                h.writeVarInt(face.sx);
                h.writeVarInt(face.sy);
                h.writeVarInt(face.ex);
                h.writeVarInt(face.ey);
                h.writeEnum(face.rotation);
            }
        }
    }

    public Map<String, Object> toMap() {
        return new HashMap<>();
    }

    public enum Rot {
        ROT_0, ROT_90, ROT_180, ROT_270;
        public static final Rot[] VALUES = values();
    }

    public boolean contains(Direction key) {
        return faces.containsKey(key);
    }

    public Face get(Direction key) {
        return faces.get(key);
    }

    public Vec4f getVec(Direction key) {
        Face f = faces.get(key);
        return f == null ? new Vec4f(0, 0, 0, 0) : f.getVec();
    }
}
