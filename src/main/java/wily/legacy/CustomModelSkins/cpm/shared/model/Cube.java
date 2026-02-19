package wily.legacy.CustomModelSkins.cpm.shared.model;

import wily.legacy.CustomModelSkins.cpl.math.Vec3f;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cube {
    public static final int HAS_MESH = 1 << 0, HAS_TEXTURE = 1 << 1, HIDDEN = 1 << 2, MESH_SCALED = 1 << 3, UV_SCALED = 1 << 4, MC_SCALED = 1 << 5, SCALED = 1 << 6;
    public Vec3f offset, rotation, pos, size, scale, meshScale;
    public int parentId, id, rgb, u, v, texSize;
    public float mcScale;
    public boolean hidden;

    @Deprecated
    public static Cube loadDefinitionCube(IOHelper din) throws IOException {
        Cube c = new Cube();
        c.size = din.readVec3ub();
        c.pos = din.readVec6b();
        c.offset = din.readVec6b();
        c.rotation = din.readAngle();
        c.meshScale = new Vec3f(1, 1, 1);
        c.scale = new Vec3f(1, 1, 1);
        c.parentId = din.readVarInt();
        int tex = din.readByte();
        if (tex == 0) {
            int ch2 = din.read(), ch3 = din.read(), ch4 = din.read();
            if ((ch2 | ch3 | ch4) < 0) throw new EOFException();
            c.rgb = ((0xff << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
        } else {
            c.texSize = tex;
            c.u = din.read();
            c.v = din.read();
        }
        return c;
    }

    public static Cube loadDefinitionCubeV2(IOHelper din) throws IOException {
        Cube c = new Cube();
        byte flags = din.readByte();
        c.parentId = din.readVarInt();
        c.pos = din.readVarVec3();
        c.rotation = din.readAngle();
        c.hidden = (flags & HIDDEN) != 0;
        if ((flags & HAS_MESH) != 0) {
            c.size = din.readVarVec3();
            c.offset = din.readVarVec3();
            if ((flags & HAS_TEXTURE) != 0) {
                c.texSize = (flags & UV_SCALED) != 0 ? din.readByte() : 1;
                c.u = din.readVarInt();
                c.v = din.readVarInt();
            } else {
                int ch2 = din.read(), ch3 = din.read(), ch4 = din.read();
                if ((ch2 | ch3 | ch4) < 0) throw new EOFException();
                c.rgb = ((0xff << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
            }
            if ((flags & MC_SCALED) != 0) c.mcScale = din.readFloat2();
        } else {
            c.size = new Vec3f();
            c.offset = new Vec3f();
        }
        c.meshScale = (flags & MESH_SCALED) != 0 ? din.readVarVec3() : new Vec3f(1, 1, 1);
        c.scale = (flags & SCALED) != 0 ? din.readVarVec3() : new Vec3f(1, 1, 1);
        return c;
    }

    @Deprecated
    public static void saveDefinitionCube(IOHelper dout, Cube cube) throws IOException {
        dout.writeVec3ub(cube.size);
        dout.writeVec6b(cube.pos);
        dout.writeVec6b(cube.offset);
        Vec3f rot = new Vec3f(cube.rotation);
        limitRotation(rot);
        dout.writeAngle(rot);
        dout.writeVarInt(cube.parentId);
        dout.writeByte(cube.texSize);
        if (cube.texSize == 0) {
            dout.write((cube.rgb >>> 16) & 0xFF);
            dout.write((cube.rgb >>> 8) & 0xFF);
            dout.write((cube.rgb >>> 0) & 0xFF);
        } else {
            dout.write(cube.u);
            dout.write(cube.v);
        }
    }

    public static void saveDefinitionCubeV2(IOHelper dout, Cube cube) throws IOException {
        boolean hasMesh = !cube.size.epsilon(0.001f), texture = hasMesh && cube.texSize != 0, hidden = cube.hidden, mesh_scaled = cube.meshScale != null && !cube.meshScale.sub(1).epsilon(0.001f), scaled = cube.scale != null && !cube.scale.sub(1).epsilon(0.001f), uv_scaled = texture && cube.texSize != 1, mc_scaled = hasMesh && Math.abs(cube.mcScale) > 0.0001f;
        int flags = 0;
        if (hasMesh) flags |= HAS_MESH;
        if (texture) flags |= HAS_TEXTURE;
        if (hidden) flags |= HIDDEN;
        if (mesh_scaled) flags |= MESH_SCALED;
        if (uv_scaled) flags |= UV_SCALED;
        if (mc_scaled) flags |= MC_SCALED;
        if (scaled) flags |= SCALED;
        dout.writeByte(flags);
        dout.writeVarInt(cube.parentId);
        dout.writeVarVec3(cube.pos);
        Vec3f rot = new Vec3f(cube.rotation);
        limitRotation(rot);
        dout.writeAngle(rot);
        if (hasMesh) {
            dout.writeVarVec3(cube.size);
            dout.writeVarVec3(cube.offset);
            if (texture) {
                if (uv_scaled) dout.writeByte(cube.texSize);
                dout.writeVarInt(cube.u);
                dout.writeVarInt(cube.v);
            } else {
                dout.write((cube.rgb >>> 16) & 0xFF);
                dout.write((cube.rgb >>> 8) & 0xFF);
                dout.write((cube.rgb >>> 0) & 0xFF);
            }
            if (mc_scaled) dout.writeFloat2(cube.mcScale);
        }
        if (mesh_scaled) dout.writeVarVec3(cube.meshScale);
        if (scaled) dout.writeVarVec3(cube.scale);
    }

    public static List<RenderedCube> resolveCubesV2(List<Cube> cubes) {
        return resolveCubes0(cubes, false);
    }

    public static List<RenderedCube> resolveCubes(List<Cube> cubes) {
        return resolveCubes0(cubes, true);
    }

    private static List<RenderedCube> resolveCubes0(List<Cube> cubes, boolean preserveHidden) {
        Map<Integer, RenderedCube> r = new HashMap<>();
        for (Cube cube : cubes) r.put(cube.id, makeRendered(cube, preserveHidden));
        for (Cube c : cubes) {
            if (c.parentId < 10) continue;
            RenderedCube child = r.get(c.id), parent = r.get(c.parentId);
            child.setParent(parent);
            parent.addChild(child);
        }
        return new ArrayList<>(r.values());
    }

    private static RenderedCube makeRendered(Cube cube, boolean preserveHidden) {
        if (!preserveHidden) return new RenderedCube(cube);
        boolean h = cube.hidden;
        cube.hidden = false;
        RenderedCube rc = new RenderedCube(cube);
        rc.getCube().hidden = h;
        return rc;
    }

    private static void limitRotation(Vec3f rot) {
        rot.x = wrapDeg(rot.x);
        rot.y = wrapDeg(rot.y);
        rot.z = wrapDeg(rot.z);
    }

    private static float wrapDeg(float v) {
        v = v % 360.0f;
        if (v < 0.0f) v += 360.0f;
        return v;
    }
}
