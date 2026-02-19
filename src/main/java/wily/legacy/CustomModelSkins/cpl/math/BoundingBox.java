package wily.legacy.CustomModelSkins.cpl.math;

import wily.legacy.CustomModelSkins.cpl.util.Direction;

public class BoundingBox {
    public float minX;
    public float minY;
    public float minZ;
    public float maxX;
    public float maxY;
    public float maxZ;

    public BoundingBox(float x1, float y1, float z1, float x2, float y2, float z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public static BoundingBox create(float x, float y, float z, float w, float h, float d) {
        return new BoundingBox(x, y, z, x + w, y + h, z + d);
    }

    public BoundingBox mul(float v) {
        return new BoundingBox(minX * v, minY * v, minZ * v, maxX * v, maxY * v, maxZ * v);
    }

    public BoundingBox offset(float x, float y, float z) {
        return new BoundingBox(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z);
    }

    public BoundingBox getFaceOnly(Direction d) {
        float x = minX;
        float y = minY;
        float z = minZ;
        switch (d) {
            case DOWN:
                y = maxY;
            case UP:
                return new BoundingBox(minX, y, minZ, maxX, y, maxZ);
            case SOUTH:
                z = maxZ;
            case NORTH:
                return new BoundingBox(minX, minY, z, maxX, maxY, z);
            case EAST:
                x = maxX;
            case WEST:
                return new BoundingBox(x, minY, minZ, x, maxY, maxZ);
            default:
                throw new IllegalArgumentException("Unknown direction");
        }
    }
}
