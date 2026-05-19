package wily.legacy.client;

import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LegacyTorchModel implements BlockStateModelPart {
    private static final Map<Key, BlockStateModel> CACHE = new HashMap<>();

    private final Material.Baked material;
    private final List<BakedQuad> quads;
    private final int flags;

    private LegacyTorchModel(Material.Baked material, Direction facing) {
        this.material = material;
        quads = bake(material, facing);
        flags = quads.stream().mapToInt(q -> q.materialInfo().flags()).reduce(0, (a, b) -> a | b);
    }

    public static BlockStateModel get(BlockState state, BlockStateModel model) {
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return model;
        if (!(state.getBlock() instanceof WallTorchBlock) && !(state.getBlock() instanceof RedstoneWallTorchBlock)) return model;

        Key key = new Key(model.particleMaterial(), state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(key, k -> new SingleVariant(new LegacyTorchModel(k.material, k.facing)));
        }
    }

    public List<BakedQuad> getQuads(Direction direction) {
        return direction == null ? quads : Collections.emptyList();
    }

    public boolean useAmbientOcclusion() {
        return false;
    }

    public Material.Baked particleMaterial() {
        return material;
    }

    public int materialFlags() {
        return flags;
    }

    private static List<BakedQuad> bake(Material.Baked material, Direction facing) {
        double x = 0.5;
        double z = 0.5;
        double xo = 0.0;
        double zo = 0.0;

        switch (facing) {
            case EAST -> {
                x = 0.4;
                xo = -0.4;
            }
            case WEST -> {
                x = 0.6;
                xo = 0.4;
            }
            case SOUTH -> {
                z = 0.4;
                zo = -0.4;
            }
            case NORTH -> {
                z = 0.6;
                zo = 0.4;
            }
        }

        double y = 0.2;
        double s = 0.0625;
        double top = 0.625;
        double minX = x - 0.5;
        double maxX = x + 0.5;
        double minZ = z - 0.5;
        double maxZ = z + 0.5;
        double topX = x + xo * (1.0 - top);
        double topZ = z + zo * (1.0 - top);

        List<BakedQuad> result = new ArrayList<>(6);
        add(result, material,
                p(topX - s, y + top, topZ - s, 7, 6),
                p(topX - s, y + top, topZ + s, 7, 8),
                p(topX + s, y + top, topZ + s, 9, 8),
                p(topX + s, y + top, topZ - s, 9, 6));
        add(result, material,
                p(x + s + xo, y, z - s + zo, 9, 13),
                p(x + s + xo, y, z + s + zo, 9, 15),
                p(x - s + xo, y, z + s + zo, 7, 15),
                p(x - s + xo, y, z - s + zo, 7, 13));
        add(result, material,
                p(x - s, y + 1.0, minZ, 0, 0),
                p(x - s + xo, y, minZ + zo, 0, 16),
                p(x - s + xo, y, maxZ + zo, 16, 16),
                p(x - s, y + 1.0, maxZ, 16, 0));
        add(result, material,
                p(x + s, y + 1.0, maxZ, 0, 0),
                p(x + s + xo, y, maxZ + zo, 0, 16),
                p(x + s + xo, y, minZ + zo, 16, 16),
                p(x + s, y + 1.0, minZ, 16, 0));
        add(result, material,
                p(minX, y + 1.0, z + s, 0, 0),
                p(minX + xo, y, z + s + zo, 0, 16),
                p(maxX + xo, y, z + s + zo, 16, 16),
                p(maxX, y + 1.0, z + s, 16, 0));
        add(result, material,
                p(maxX, y + 1.0, z - s, 0, 0),
                p(maxX + xo, y, z - s + zo, 0, 16),
                p(minX + xo, y, z - s + zo, 16, 16),
                p(minX, y + 1.0, z - s, 16, 0));
        return List.copyOf(result);
    }

    private static void add(List<BakedQuad> quads, Material.Baked material, Vertex v0, Vertex v1, Vertex v2, Vertex v3) {
        BakedQuad.MaterialInfo info = materialInfo(material, v0, v1, v2, v3);
        quads.add(new BakedQuad(v0.pos, v1.pos, v2.pos, v3.pos, uv(material, v0), uv(material, v1), uv(material, v2), uv(material, v3), direction(v0.pos, v1.pos, v2.pos), info));
    }

    private static BakedQuad.MaterialInfo materialInfo(Material.Baked material, Vertex v0, Vertex v1, Vertex v2, Vertex v3) {
        float minU = Math.min(Math.min(v0.u, v1.u), Math.min(v2.u, v3.u)) / 16.0f;
        float minV = Math.min(Math.min(v0.v, v1.v), Math.min(v2.v, v3.v)) / 16.0f;
        float maxU = Math.max(Math.max(v0.u, v1.u), Math.max(v2.u, v3.u)) / 16.0f;
        float maxV = Math.max(Math.max(v0.v, v1.v), Math.max(v2.v, v3.v)) / 16.0f;
        Transparency transparency = material.forceTranslucent() ? Transparency.TRANSLUCENT : material.sprite().contents().computeTransparency(minU, minV, maxU, maxV);
        return BakedQuad.MaterialInfo.of(material, transparency, -1, false, 0);
    }

    private static long uv(Material.Baked material, Vertex vertex) {
        TextureAtlasSprite sprite = material.sprite();
        return UVPair.pack(sprite.getU(vertex.u / 16.0f), sprite.getV(vertex.v / 16.0f));
    }

    private static Vertex p(double x, double y, double z, float u, float v) {
        return new Vertex(new Vector3f((float) x, (float) y, (float) z), u, v);
    }

    private static Direction direction(Vector3fc p0, Vector3fc p1, Vector3fc p2) {
        Vector3f normal = new Vector3f(p1).sub(p0).cross(new Vector3f(p2).sub(p0));
        if (!normal.isFinite() || normal.lengthSquared() <= 1.0E-7f) return Direction.UP;
        return Direction.getApproximateNearest(normal.x, normal.y, normal.z);
    }

    private record Key(Material.Baked material, Direction facing) {
    }

    private record Vertex(Vector3fc pos, float u, float v) {
    }
}
