package wily.legacy.client;

//? if >=1.21.5 {
/*import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
*///?} else {
import net.minecraft.client.resources.model.BakedModel;
//? if <1.21.3 {
import net.minecraft.client.renderer.block.model.ItemOverrides;
//?}
import net.minecraft.client.renderer.block.model.ItemTransforms;
//?}
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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

public class LegacyTorchModel implements /*? if >=1.21.5 {*//*BlockModelPart*//*?} else {*/BakedModel/*?}*/ {
    private static final Map<Key, /*? if >=1.21.5 {*//*BlockStateModel*//*?} else {*/BakedModel/*?}*/> CACHE = new HashMap<>();

    //? if <1.21.5 {
    private final BakedModel source;
    //?}
    private final TextureAtlasSprite particle;
    private final List<BakedQuad> quads;

    private LegacyTorchModel(/*? if <1.21.5 {*/BakedModel source, /*?}*/TextureAtlasSprite particle, Direction facing) {
        //? if <1.21.5 {
        this.source = source;
        //?}
        this.particle = particle;
        this.quads = bake(particle, facing);
    }

    public static /*? if >=1.21.5 {*//*BlockStateModel*//*?} else {*/BakedModel/*?}*/ get(BlockState state, /*? if >=1.21.5 {*//*BlockStateModel*//*?} else {*/BakedModel/*?}*/ model) {
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return model;
        if (!(state.getBlock() instanceof WallTorchBlock) && !(state.getBlock() instanceof RedstoneWallTorchBlock)) return model;

        TextureAtlasSprite particle = /*? if >=1.21.5 {*//*model.particleIcon()*//*?} else {*/model.getParticleIcon()/*?}*/;
        Key key = new Key(particle, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(key, k -> /*? if >=1.21.5 {*//*new SingleVariant(*//*?}*/new LegacyTorchModel(/*? if <1.21.5 {*/model, /*?}*/k.particle, k.facing)/*? if >=1.21.5 {*//*)*//*?}*/);
        }
    }

    //? if >=1.21.5 {
    /*@Override
    public TextureAtlasSprite particleIcon() {
        return particle;
    }
    *///?} else {
    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction direction, RandomSource randomSource) {
        return getQuads(direction);
    }

    @Override
    public boolean isGui3d() {
        return source.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return source.usesBlockLight();
    }

    //? if <1.21.4 {
    @Override
    public boolean isCustomRenderer() {
        return source.isCustomRenderer();
    }
    //?}

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return particle;
    }

    @Override
    public ItemTransforms getTransforms() {
        return source.getTransforms();
    }

    //? if <1.21.3 {
    @Override
    public ItemOverrides getOverrides() {
        return source.getOverrides();
    }
    //?}
    //?}

    public List<BakedQuad> getQuads(Direction direction) {
        return direction == null ? quads : Collections.emptyList();
    }

    public boolean useAmbientOcclusion() {
        return false;
    }

    private static List<BakedQuad> bake(TextureAtlasSprite particle, Direction facing) {
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
        add(result, particle,
                p(topX - s, y + top, topZ - s, 7, 6),
                p(topX - s, y + top, topZ + s, 7, 8),
                p(topX + s, y + top, topZ + s, 9, 8),
                p(topX + s, y + top, topZ - s, 9, 6));
        add(result, particle,
                p(x + s + xo, y, z - s + zo, 9, 13),
                p(x + s + xo, y, z + s + zo, 9, 15),
                p(x - s + xo, y, z + s + zo, 7, 15),
                p(x - s + xo, y, z - s + zo, 7, 13));
        add(result, particle,
                p(x - s, y + 1.0, minZ, 0, 0),
                p(x - s + xo, y, minZ + zo, 0, 16),
                p(x - s + xo, y, maxZ + zo, 16, 16),
                p(x - s, y + 1.0, maxZ, 16, 0));
        add(result, particle,
                p(x + s, y + 1.0, maxZ, 0, 0),
                p(x + s + xo, y, maxZ + zo, 0, 16),
                p(x + s + xo, y, minZ + zo, 16, 16),
                p(x + s, y + 1.0, minZ, 16, 0));
        add(result, particle,
                p(minX, y + 1.0, z + s, 0, 0),
                p(minX + xo, y, z + s + zo, 0, 16),
                p(maxX + xo, y, z + s + zo, 16, 16),
                p(maxX, y + 1.0, z + s, 16, 0));
        add(result, particle,
                p(maxX, y + 1.0, z - s, 0, 0),
                p(maxX + xo, y, z - s + zo, 0, 16),
                p(minX + xo, y, z - s + zo, 16, 16),
                p(minX, y + 1.0, z - s, 16, 0));
        return List.copyOf(result);
    }

    private static void add(List<BakedQuad> quads, TextureAtlasSprite particle, Vertex v0, Vertex v1, Vertex v2, Vertex v3) {
        int[] vertices = new int[32];
        fill(vertices, 0, particle, v0);
        fill(vertices, 1, particle, v1);
        fill(vertices, 2, particle, v2);
        fill(vertices, 3, particle, v3);
        quads.add(new BakedQuad(vertices, -1, direction(v0.pos, v1.pos, v2.pos), particle, false/*? if >=1.21.3 {*//*, 0*//*?}*/));
    }

    private static void fill(int[] vertices, int index, TextureAtlasSprite particle, Vertex vertex) {
        int offset = index * 8;
        vertices[offset] = Float.floatToRawIntBits(vertex.pos.x());
        vertices[offset + 1] = Float.floatToRawIntBits(vertex.pos.y());
        vertices[offset + 2] = Float.floatToRawIntBits(vertex.pos.z());
        vertices[offset + 3] = -1;
        vertices[offset + 4] = Float.floatToRawIntBits(particle.getU(/*? if >=1.21.3 {*//*vertex.u / 16.0F*//*?} else {*/vertex.u/*?}*/));
        vertices[offset + 5] = Float.floatToRawIntBits(particle.getV(/*? if >=1.21.3 {*//*vertex.v / 16.0F*//*?} else {*/vertex.v/*?}*/));
    }

    private static Vertex p(double x, double y, double z, float u, float v) {
        return new Vertex(new Vector3f((float) x, (float) y, (float) z), u, v);
    }

    private static Direction direction(Vector3fc p0, Vector3fc p1, Vector3fc p2) {
        Vector3f normal = new Vector3f(p1).sub(p0).cross(new Vector3f(p2).sub(p0));
        if (!normal.isFinite() || normal.lengthSquared() <= 1.0E-7f) return Direction.UP;
        return Direction./*? if >=1.21.3 {*//*getApproximateNearest*//*?} else {*/getNearest/*?}*/(normal.x, normal.y, normal.z);
    }

    private record Key(TextureAtlasSprite particle, Direction facing) {
    }

    private record Vertex(Vector3fc pos, float u, float v) {
    }
}
