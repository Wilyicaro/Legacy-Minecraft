package wily.legacy.CustomModelSkins.cpm.shared.model.render;

import wily.legacy.CustomModelSkins.cpl.math.*;
import wily.legacy.CustomModelSkins.cpl.render.ListBuffer;
import wily.legacy.CustomModelSkins.cpl.render.VertexBuffer;
import wily.legacy.CustomModelSkins.cpl.util.Direction;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.PerFaceUV.Face;

import java.util.ArrayList;
import java.util.List;

public class BoxRender {
    private static class Vertex {
        public final Vec3f position;
        public final float textureU;
        public final float textureV;

        public Vertex(float x, float y, float z, float texU, float texV) {
            this(new Vec3f(x, y, z), texU, texV);
        }

        public Vertex setTextureUV(float texU, float texV) {
            return new Vertex(this.position, texU, texV);
        }

        public Vertex(Vec3f posIn, float texU, float texV) {
            this.position = posIn;
            this.textureU = texU;
            this.textureV = texV;
        }
    }

    private static class QuadMesh implements Mesh {
        private final Quad[] quadList;
        private final RenderMode mode;

        public QuadMesh(Quad[] quadList, RenderMode mode) {
            this.quadList = quadList;
            this.mode = mode;
        }

        @Override
        public void draw(MatrixStack matrixStackIn, VertexBuffer bufferIn, float red, float green, float blue, float alpha) {
            Mat4f matrix4f = matrixStackIn.getLast().getMatrix();
            Mat3f matrix3f = matrixStackIn.getLast().getNormal();
            for (Quad quad : quadList) {
                Vec3f vector3f = quad.normal.copy();
                vector3f.transform(matrix3f);
                float f = vector3f.x;
                float f1 = vector3f.y;
                float f2 = vector3f.z;
                for (int i = 0; i < 4; ++i) {
                    Vertex vertex = quad.vertexPositions[i];
                    float f3 = vertex.position.x / 16.0F;
                    float f4 = vertex.position.y / 16.0F;
                    float f5 = vertex.position.z / 16.0F;
                    Vec4f vector4f = new Vec4f(f3, f4, f5, 1.0F);
                    vector4f.transform(matrix4f);
                    bufferIn.addVertex(vector4f.x, vector4f.y, vector4f.z, red, green, blue, alpha, vertex.textureU, vertex.textureV, f, f1, f2);
                }
            }
        }

        @Override
        public RenderMode getLayer() {
            return mode;
        }

        @Override
        public void free() {
        }
    }

    private static class Quad {
        public final Vertex[] vertexPositions;
        public final Vec3f normal;

        public Quad(Vertex[] posIn, Face f, float texWidth, float texHeight, Vec3f directionIn) {
            this.vertexPositions = posIn;
            for (int i = 0; i < posIn.length; i++) {
                posIn[i] = posIn[i].setTextureUV(f.getVertexU(i) / texWidth, f.getVertexV(i) / texHeight);
            }
            this.normal = new Vec3f(directionIn);
        }

        public Quad(Vertex[] posIn, float u1, float v1, float u2, float v2, float texWidth, float texHeight, boolean mirrorIn, Vec3f directionIn) {
            this.vertexPositions = posIn;
            posIn[0] = posIn[0].setTextureUV(u2 / texWidth, v1 / texHeight);
            posIn[1] = posIn[1].setTextureUV(u1 / texWidth, v1 / texHeight);
            posIn[2] = posIn[2].setTextureUV(u1 / texWidth, v2 / texHeight);
            posIn[3] = posIn[3].setTextureUV(u2 / texWidth, v2 / texHeight);
            if (mirrorIn) {
                int i = posIn.length;
                for (int j = 0; j < i / 2; ++j) {
                    Vertex vertex = posIn[j];
                    posIn[j] = posIn[i - 1 - j];
                    posIn[i - 1 - j] = vertex;
                }
            }
            this.normal = new Vec3f(directionIn);
            if (mirrorIn) {
                this.normal.mul(-1.0F, 1.0F, 1.0F);
            }
        }
    }

    public static Mesh createTexturedSingle(Vec3f pos, Vec3f size, Vec3f sc, float delta, int texU, int texV, int texSize, int sheetSizeX, int sheetSizeY) {
        float x = pos.x;
        float y = pos.y;
        float z = pos.z;
        float w = size.x;
        float h = size.y;
        float d = size.z;
        int ts = Math.abs(texSize);
        int dx = MathHelper.ceil(w * ts);
        int dy = MathHelper.ceil(h * ts);
        int dz = MathHelper.ceil(d * ts);
        float ex = x + w * sc.x;
        float ey = y + h * sc.y;
        float ez = z + d * sc.z;
        x = x - delta;
        y = y - delta;
        z = z - delta;
        ex = ex + delta;
        ey = ey + delta;
        ez = ez + delta;
        texU *= ts;
        texV *= ts;
        if (texSize < 0) {
            float s = ex;
            ex = x;
            x = s;
        }
        if (ex == x || ey == y || ez == z) {
            Quad[] quadList = new Quad[2];
            if (ex == x) {
                ex += 0.001f;
                Vertex vertex7 = new Vertex(x, y, z, 0.0F, 0.0F);
                Vertex vertex = new Vertex(ex, y, z, 0.0F, 8.0F);
                Vertex vertex1 = new Vertex(ex, ey, z, 8.0F, 8.0F);
                Vertex vertex2 = new Vertex(x, ey, z, 8.0F, 0.0F);
                Vertex vertex3 = new Vertex(x, y, ez, 0.0F, 0.0F);
                Vertex vertex4 = new Vertex(ex, y, ez, 0.0F, 8.0F);
                Vertex vertex5 = new Vertex(ex, ey, ez, 8.0F, 8.0F);
                Vertex vertex6 = new Vertex(x, ey, ez, 8.0F, 0.0F);
                float tu = texU + dz;
                float tv = texV + dy;
                quadList[1] = new Quad(new Vertex[]{vertex7, vertex3, vertex6, vertex2}, texU, texV, tu, tv, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.NEGATIVE_X);
                quadList[0] = new Quad(new Vertex[]{vertex4, vertex, vertex1, vertex5}, tu, texV, texU, tv, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.POSITIVE_X);
            } else if (ey == y) {
                ey += 0.001f;
                Vertex vertex7 = new Vertex(x, y, z, 0.0F, 0.0F);
                Vertex vertex = new Vertex(ex, y, z, 0.0F, 8.0F);
                Vertex vertex1 = new Vertex(ex, ey, z, 8.0F, 8.0F);
                Vertex vertex2 = new Vertex(x, ey, z, 8.0F, 0.0F);
                Vertex vertex3 = new Vertex(x, y, ez, 0.0F, 0.0F);
                Vertex vertex4 = new Vertex(ex, y, ez, 0.0F, 8.0F);
                Vertex vertex5 = new Vertex(ex, ey, ez, 8.0F, 8.0F);
                Vertex vertex6 = new Vertex(x, ey, ez, 8.0F, 0.0F);
                float tu = texU + dx;
                float tv = texV + dz;
                quadList[0] = new Quad(new Vertex[]{vertex4, vertex3, vertex7, vertex}, texU, texV, tu, tv, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.NEGATIVE_Y);
                quadList[1] = new Quad(new Vertex[]{vertex1, vertex2, vertex6, vertex5}, tu, texV, texU, tv, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.POSITIVE_Y);
            } else if (ez == z) {
                ez += 0.001f;
                Vertex vertex7 = new Vertex(x, y, z, 0.0F, 0.0F);
                Vertex vertex = new Vertex(ex, y, z, 0.0F, 8.0F);
                Vertex vertex1 = new Vertex(ex, ey, z, 8.0F, 8.0F);
                Vertex vertex2 = new Vertex(x, ey, z, 8.0F, 0.0F);
                Vertex vertex3 = new Vertex(x, y, ez, 0.0F, 0.0F);
                Vertex vertex4 = new Vertex(ex, y, ez, 0.0F, 8.0F);
                Vertex vertex5 = new Vertex(ex, ey, ez, 8.0F, 8.0F);
                Vertex vertex6 = new Vertex(x, ey, ez, 8.0F, 0.0F);
                float tu = texU + dx;
                float tv = texV + dy;
                quadList[0] = new Quad(new Vertex[]{vertex, vertex7, vertex2, vertex1}, texU, texV, tu, tv, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.NEGATIVE_Z);
                quadList[1] = new Quad(new Vertex[]{vertex3, vertex4, vertex5, vertex6}, tu, texV, texU, tv, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.POSITIVE_Z);
            }
            if (quadList[0] != null) return new QuadMesh(quadList, RenderMode.NORMAL);
        }
        Quad[] quadList = new Quad[6];
        Vertex vertex7 = new Vertex(x, y, z, 0.0F, 0.0F);
        Vertex vertex = new Vertex(ex, y, z, 0.0F, 8.0F);
        Vertex vertex1 = new Vertex(ex, ey, z, 8.0F, 8.0F);
        Vertex vertex2 = new Vertex(x, ey, z, 8.0F, 0.0F);
        Vertex vertex3 = new Vertex(x, y, ez, 0.0F, 0.0F);
        Vertex vertex4 = new Vertex(ex, y, ez, 0.0F, 8.0F);
        Vertex vertex5 = new Vertex(ex, ey, ez, 8.0F, 8.0F);
        Vertex vertex6 = new Vertex(x, ey, ez, 8.0F, 0.0F);
        int txS = Math.max(dx, Math.max(dy, dz));
        float tu = texU + txS;
        float tv = texV + txS;
        quadList[2] = new Quad(new Vertex[]{vertex4, vertex3, vertex7, vertex}, texU, texV, tu, tv, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.NEGATIVE_Y);
        quadList[3] = new Quad(new Vertex[]{vertex1, vertex2, vertex6, vertex5}
                , texU, texV, tu, tv, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.POSITIVE_Y);
        quadList[1] = new Quad(new Vertex[]{vertex7, vertex3, vertex6, vertex2}, texU, texV, tu, tv, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.NEGATIVE_X);
        quadList[4] = new Quad(new Vertex[]{vertex, vertex7, vertex2, vertex1}, texU, texV, tu, tv, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.NEGATIVE_Z);
        quadList[0] = new Quad(new Vertex[]{vertex4, vertex, vertex1, vertex5}, texU, texV, tu, tv, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.POSITIVE_X);
        quadList[5] = new Quad(new Vertex[]{vertex3, vertex4, vertex5, vertex6}, texU, texV, tu, tv, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.POSITIVE_Z);
        return new QuadMesh(quadList, RenderMode.NORMAL);
    }

    public static Mesh createTextured(Vec3f pos, Vec3f size, Vec3f sc, float delta, int texU, int texV, int texSize, int sheetSizeX, int sheetSizeY) {
        Quad[] quadList = new Quad[6];
        {
            float x = pos.x;
            float y = pos.y;
            float z = pos.z;
            float w = size.x;
            float h = size.y;
            float d = size.z;
            int ts = Math.abs(texSize);
            int dx = MathHelper.ceil(w * ts);
            int dy = MathHelper.ceil(h * ts);
            int dz = MathHelper.ceil(d * ts);
            float ex = x + w * sc.x;
            float ey = y + h * sc.y;
            float ez = z + d * sc.z;
            x = x - delta;
            y = y - delta;
            z = z - delta;
            ex = ex + delta;
            ey = ey + delta;
            ez = ez + delta;
            texU *= ts;
            texV *= ts;
            if (texSize < 0) {
                float s = ex;
                ex = x;
                x = s;
            }
            Vertex vertex7 = new Vertex(x, y, z, 0.0F, 0.0F);
            Vertex vertex = new Vertex(ex, y, z, 0.0F, 8.0F);
            Vertex vertex1 = new Vertex(ex, ey, z, 8.0F, 8.0F);
            Vertex vertex2 = new Vertex(x, ey, z, 8.0F, 0.0F);
            Vertex vertex3 = new Vertex(x, y, ez, 0.0F, 0.0F);
            Vertex vertex4 = new Vertex(ex, y, ez, 0.0F, 8.0F);
            Vertex vertex5 = new Vertex(ex, ey, ez, 8.0F, 8.0F);
            Vertex vertex6 = new Vertex(x, ey, ez, 8.0F, 0.0F);
            float f4 = texU;
            float f5 = (float) texU + dz;
            float f6 = (float) texU + dz + dx;
            float f7 = (float) texU + dz + dx + dx;
            float f8 = (float) texU + dz + dx + dz;
            float f9 = (float) texU + dz + dx + dz + dx;
            float f10 = texV;
            float f11 = (float) texV + dz;
            float f12 = (float) texV + dz + dy;
            quadList[2] = new Quad(new Vertex[]{vertex4, vertex3, vertex7, vertex}, f5, f10, f6, f11, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.NEGATIVE_Y);
            quadList[3] = new Quad(new Vertex[]{vertex1, vertex2, vertex6, vertex5}, f6, f11, f7, f10, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.POSITIVE_Y);
            quadList[1] = new Quad(new Vertex[]{vertex7, vertex3, vertex6, vertex2}, f4, f11, f5, f12, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.NEGATIVE_X);
            quadList[4] = new Quad(new Vertex[]{vertex, vertex7, vertex2, vertex1}, f5, f11, f6, f12, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.NEGATIVE_Z);
            quadList[0] = new Quad(new Vertex[]{vertex4, vertex, vertex1, vertex5}, f6, f11, f8, f12, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.POSITIVE_X);
            quadList[5] = new Quad(new Vertex[]{vertex3, vertex4, vertex5, vertex6}, f8, f11, f9, f12, sheetSizeX, sheetSizeY, texSize < 0, Vec3f.POSITIVE_Z);
        }
        return new QuadMesh(quadList, RenderMode.NORMAL);
    }

    public static Mesh createTextured(Vec3f pos, Vec3f size, Vec3f sc, float delta, PerFaceUV uv, int texSize, int sheetSizeX, int sheetSizeY) {
        List<Quad> quadList = new ArrayList<>();
        {
            float x = pos.x;
            float y = pos.y;
            float z = pos.z;
            float w = size.x;
            float h = size.y;
            float d = size.z;
            float ex = x + w * sc.x;
            float ey = y + h * sc.y;
            float ez = z + d * sc.z;
            x = x - delta;
            y = y - delta;
            z = z - delta;
            ex = ex + delta;
            ey = ey + delta;
            ez = ez + delta;
            Vertex ptv7 = new Vertex(x, y, z, 0.0F, 0.0F);
            Vertex ptv = new Vertex(ex, y, z, 0.0F, 8.0F);
            Vertex ptv1 = new Vertex(ex, ey, z, 8.0F, 8.0F);
            Vertex ptv2 = new Vertex(x, ey, z, 8.0F, 0.0F);
            Vertex ptv3 = new Vertex(x, y, ez, 0.0F, 0.0F);
            Vertex ptv4 = new Vertex(ex, y, ez, 0.0F, 8.0F);
            Vertex ptv5 = new Vertex(ex, ey, ez, 8.0F, 8.0F);
            Vertex ptv6 = new Vertex(x, ey, ez, 8.0F, 0.0F);
            if (uv.contains(Direction.EAST))
                quadList.add(new Quad(new Vertex[]{ptv4, ptv, ptv1, ptv5}, uv.get(Direction.EAST), sheetSizeX, sheetSizeY, Vec3f.POSITIVE_X));
            if (uv.contains(Direction.WEST))
                quadList.add(new Quad(new Vertex[]{ptv7, ptv3, ptv6, ptv2}, uv.get(Direction.WEST), sheetSizeX, sheetSizeY, Vec3f.NEGATIVE_X));
            if (uv.contains(Direction.UP))
                quadList.add(new Quad(new Vertex[]{ptv4, ptv3, ptv7, ptv}, uv.get(Direction.UP), sheetSizeX, sheetSizeY, Vec3f.NEGATIVE_Y));
            if (uv.contains(Direction.DOWN))
                quadList.add(new Quad(new Vertex[]{ptv1, ptv2, ptv6, ptv5}, uv.get(Direction.DOWN), sheetSizeX, sheetSizeY, Vec3f.POSITIVE_Y));
            if (uv.contains(Direction.NORTH))
                quadList.add(new Quad(new Vertex[]{ptv, ptv7, ptv2, ptv1}, uv.get(Direction.NORTH), sheetSizeX, sheetSizeY, Vec3f.NEGATIVE_Z));
            if (uv.contains(Direction.SOUTH))
                quadList.add(new Quad(new Vertex[]{ptv3, ptv4, ptv5, ptv6}, uv.get(Direction.SOUTH), sheetSizeX, sheetSizeY, Vec3f.POSITIVE_Z));
        }
        return new QuadMesh(quadList.toArray(new Quad[0]), RenderMode.NORMAL);
    }

    public static Mesh createColored(float x, float y, float z, float w, float h, float d, float delta, int sheetSizeX, int sheetSizeY) {
        Quad[] quadList = new Quad[6];
        {
            float ex = x + w;
            float ey = y + h;
            float ez = z + d;
            x = x - delta;
            y = y - delta;
            z = z - delta;
            ex = ex + delta;
            ey = ey + delta;
            ez = ez + delta;
            Vertex vertex7 = new Vertex(x, y, z, 0.0F, 0.0F);
            Vertex vertex = new Vertex(ex, y, z, 0.0F, 8.0F);
            Vertex vertex1 = new Vertex(ex, ey, z, 8.0F, 8.0F);
            Vertex vertex2 = new Vertex(x, ey, z, 8.0F, 0.0F);
            Vertex vertex3 = new Vertex(x, y, ez, 0.0F, 0.0F);
            Vertex vertex4 = new Vertex(ex, y, ez, 0.0F, 8.0F);
            Vertex vertex5 = new Vertex(ex, ey, ez, 8.0F, 8.0F);
            Vertex vertex6 = new Vertex(x, ey, ez, 8.0F, 0.0F);
            int dx = MathHelper.ceil(w);
            int dy = MathHelper.ceil(h);
            int dz = MathHelper.ceil(d);
            float f4 = 0;
            float f5 = dz;
            float f6 = dz + dx;
            float f7 = dz + dx + dx;
            float f8 = dz + dx + dz;
            float f9 = dz + dx + dz + dx;
            float f10 = 0;
            float f11 = dz;
            float f12 = dz + dy;
            quadList[2] = new Quad(new Vertex[]{vertex4, vertex3, vertex7, vertex}, f5, f10, f6, f11, sheetSizeX, sheetSizeY, false, Vec3f.NEGATIVE_Y);
            quadList[3] = new Quad(new Vertex[]{vertex1, vertex2, vertex6, vertex5}, f6, f11, f7, f10, sheetSizeX, sheetSizeY, false, Vec3f.POSITIVE_Y);
            quadList[1] = new Quad(new Vertex[]{vertex7, vertex3, vertex6, vertex2}, f4, f11, f5, f12, sheetSizeX, sheetSizeY, false, Vec3f.NEGATIVE_X);
            quadList[4] = new Quad(new Vertex[]{vertex, vertex7, vertex2, vertex1}, f5, f11, f6, f12, sheetSizeX, sheetSizeY, false, Vec3f.NEGATIVE_Z);
            quadList[0] = new Quad(new Vertex[]{vertex4, vertex, vertex1, vertex5}, f6, f11, f8, f12, sheetSizeX, sheetSizeY, false, Vec3f.POSITIVE_X);
            quadList[5] = new Quad(new Vertex[]{vertex3, vertex4, vertex5, vertex6}, f8, f11, f9, f12, sheetSizeX, sheetSizeY, false, Vec3f.POSITIVE_Z);
        }
        return new QuadMesh(quadList, RenderMode.NORMAL);
    }

    private static class ExtrudedMesh implements Mesh {
        private ListBuffer buffer;

        public ExtrudedMesh(Mat4f matrix4f, Mat3f matrix3f, float minU, float minV, float maxU, float maxV, int texWidth, int texHeight) {
            buffer = new ListBuffer();
            buffer.pos(matrix4f, 0.0f, 0.0f, 0.0f).normal(matrix3f, 0.0F, 0.0F, 1.0F).tex(maxU, maxV).color(1, 1, 1, 1).endVertex();
            buffer.pos(matrix4f, 1.0f, 0.0f, 0.0f).normal(matrix3f, 0.0F, 0.0F, 1.0F).tex(minU, maxV).color(1, 1, 1, 1).endVertex();
            buffer.pos(matrix4f, 1.0f, 1.0f, 0.0f).normal(matrix3f, 0.0F, 0.0F, 1.0F).tex(minU, minV).color(1, 1, 1, 1).endVertex();
            buffer.pos(matrix4f, 0.0f, 1.0f, 0.0f).normal(matrix3f, 0.0F, 0.0F, 1.0F).tex(maxU, minV).color(1, 1, 1, 1).endVertex();
            buffer.pos(matrix4f, 0.0f, 1.0f, 0.0F - 1).normal(matrix3f, 0.0F, 0.0F, -1.0F).tex(maxU, minV).color(1, 1, 1, 1).endVertex();
            buffer.pos(matrix4f, 1.0f, 1.0f, 0.0F - 1).normal(matrix3f, 0.0F, 0.0F, -1.0F).tex(minU, minV).color(1, 1, 1, 1).endVertex();
            buffer.pos(matrix4f, 1.0f, 0.0f, 0.0F - 1).normal(matrix3f, 0.0F, 0.0F, -1.0F).tex(minU, maxV).color(1, 1, 1, 1).endVertex();
            buffer.pos(matrix4f, 0.0f, 0.0f, 0.0F - 1).normal(matrix3f, 0.0F, 0.0F, -1.0F).tex(maxU, maxV).color(1, 1, 1, 1).endVertex();
            float f5 = 0.5F * (maxU - minU) / texWidth;
            float f6 = 0.5F * (maxV - minV) / texHeight;
            int k;
            float f7;
            float f8;
            for (k = 0; k < texWidth; ++k) {
                f7 = (float) k / (float) texWidth;
                f8 = maxU + (minU - maxU) * f7 - f5;
                buffer.pos(matrix4f, f7, 0.0f, 0.0F - 1).normal(matrix3f, -1.0F, 0.0F, 0.0F).tex(f8, maxV).color(1, 1, 1, 1).endVertex();
                buffer.pos(matrix4f, f7, 0.0f, 0.0f).normal(matrix3f, -1.0F, 0.0F, 0.0F).tex(f8, maxV).color(1, 1, 1, 1).endVertex();
                buffer.pos(matrix4f, f7, 1.0f, 0.0f).normal(matrix3f, -1.0F, 0.0F, 0.0F).tex(f8, minV).color(1, 1, 1, 1).endVertex();
                buffer.pos(matrix4f, f7, 1.0f, 0.0F - 1).normal(matrix3f, -1.0F, 0.0F, 0.0F).tex(f8, minV).color(1, 1, 1, 1).endVertex();
            }
            float f9;
            for (k = 0; k < texWidth; ++k) {
                f7 = (float) k / (float) texWidth;
                f8 = maxU + (minU - maxU) * f7 - f5;
                f9 = f7 + 1.0F / texWidth;
                buffer.pos(matrix4f, f9, 1.0f, 0.0F - 1).normal(matrix3f, 1.0F, 0.0F, 0.0F).tex(f8, minV).color(1, 1, 1, 1).endVertex();
                buffer.pos(matrix4f, f9, 1.0f, 0.0f).normal(matrix3f, 1.0F, 0.0F, 0.0F).tex(f8, minV).color(1, 1, 1, 1).endVertex();
                buffer.pos(matrix4f, f9, 0.0f, 0.0f).normal(matrix3f, 1.0F, 0.0F, 0.0F).tex(f8, maxV).color(1, 1, 1, 1).endVertex();
                buffer.pos(matrix4f, f9, 0.0f, 0.0F - 1).normal(matrix3f, 1.0F, 0.0F, 0.0F).tex(f8, maxV).color(1, 1, 1, 1).endVertex();
            }
            for (k = 0; k < texHeight; ++k) {
                f7 = (float) k / (float) texHeight;
                f8 = maxV + (minV - maxV) * f7 - f6;
                f9 = f7 + 1.0F / texHeight;
                buffer.pos(matrix4f, 0.0f, f9, 0.0f).normal(matrix3f, 0.0F, 1.0F, 0.0F).tex(maxU, f8).color(1, 1, 1, 1).endVertex();
                buffer.pos(matrix4f, 1.0f, f9, 0.0f).normal(matrix3f, 0.0F, 1.0F, 0.0F).tex(minU, f8).color(1, 1, 1, 1).endVertex();
                buffer.pos(matrix4f, 1.0f, f9, (0.0F - 1)).normal(matrix3f, 0.0F, 1.0F, 0.0F).tex(minU, f8).color(1, 1, 1, 1).endVertex();
                buffer.pos(matrix4f, 0.0f, f9, (0.0F - 1)).normal(matrix3f, 0.0F, 1.0F, 0.0F).tex(maxU, f8).color(1, 1, 1, 1).endVertex();
            }
            for (k = 0; k < texHeight; ++k) {
                f7 = (float) k / (float) texHeight;
                f8 = maxV + (minV - maxV) * f7 - f6;
                buffer.pos(matrix4f, 1.0f, f7, 0.0f).normal(matrix3f, 0.0F, -1.0F, 0.0F).tex(minU, f8).color(1, 1, 1, 1).endVertex();
                buffer.pos(matrix4f, 0.0f, f7, 0.0f).normal(matrix3f, 0.0F, -1.0F, 0.0F).tex(maxU, f8).color(1, 1, 1, 1).endVertex();
                buffer.pos(matrix4f, 0.0f, f7, (0.0F - 1)).normal(matrix3f, 0.0F, -1.0F, 0.0F).tex(maxU, f8).color(1, 1, 1, 1).endVertex();
                buffer.pos(matrix4f, 1.0f, f7, (0.0F - 1)).normal(matrix3f, 0.0F, -1.0F, 0.0F).tex(minU, f8).color(1, 1, 1, 1).endVertex();
            }
        }

        @Override
        public void draw(MatrixStack matrixStackIn, VertexBuffer bufferIn, float red, float green, float blue, float alpha) {
            buffer.draw(matrixStackIn, bufferIn, red, green, blue, alpha);
        }

        @Override
        public RenderMode getLayer() {
            return RenderMode.NORMAL;
        }

        @Override
        public void free() {
        }
    }

    public static Mesh createTexturedExtruded(Vec3f pos, Vec3f size, Vec3f sc, float delta, int texU, int texV, int texSize, int sheetSizeX, int sheetSizeY) {
        float x = pos.x;
        float y = pos.y;
        float z = pos.z;
        float w = size.x;
        float h = size.y;
        float d = size.z;
        int ts = Math.abs(texSize);
        int dx = MathHelper.ceil(w * ts);
        int dy = MathHelper.ceil(h * ts);
        x = x - delta;
        y = y - delta;
        z = z - delta;
        w = w * sc.x + delta * 2;
        h = h * sc.y + delta * 2;
        d = d * sc.z + delta * 2;
        texU *= ts;
        texV *= ts;
        MatrixStack stack = new MatrixStack();
        if (texSize < 0) {
            stack.translate(x / 16, (y + h) / 16, z / 16);
            stack.scale(w / 16, -h / 16, -d / 16);
        } else {
            stack.translate((x + w) / 16, (y + h) / 16, (z + d) / 16);
            stack.scale(-w / 16, -h / 16, d / 16);
        }
        MatrixStack.Entry e = stack.getLast();
        float tu = texU + dx;
        float tv = texV + dy;
        return new ExtrudedMesh(e.getMatrix(), e.getNormal(), texU / (float) sheetSizeX, texV / (float) sheetSizeY, tu / sheetSizeX, tv / sheetSizeY, dx, dy);
    }

    public static int getExtrudeSize(Vec3f size, int texSize) {
        float w = size.x;
        float h = size.y;
        int ts = Math.abs(texSize);
        int dx = MathHelper.ceil(w * ts);
        int dy = MathHelper.ceil(h * ts);
        return (dx * dy * 3) / 4;
    }
}
