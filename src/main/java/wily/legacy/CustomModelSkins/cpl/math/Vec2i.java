package wily.legacy.CustomModelSkins.cpl.math;

public class Vec2i {
    public int x, y;

    public Vec2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Vec2i(float x, float y) {
        this.x = (int) x;
        this.y = (int) y;
    }

    public Vec2i(Vec2i v) {
        this.x = v.x;
        this.y = v.y;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Vec2i other = (Vec2i) obj;
        if (x != other.x) return false;
        if (y != other.y) return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("Vec2i[%s, %s]", x, y);
    }
}
