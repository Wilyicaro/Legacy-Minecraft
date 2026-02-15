package wily.legacy.CustomModelSkins.cpm.shared.io;

import wily.legacy.CustomModelSkins.cpl.math.MathHelper;
import wily.legacy.CustomModelSkins.cpl.math.Vec2i;
import wily.legacy.CustomModelSkins.cpl.math.Vec3f;
import wily.legacy.CustomModelSkins.cpl.util.Image;
import wily.legacy.CustomModelSkins.cpl.util.ImageIO;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class IOHelper extends OutputStream implements AutoCloseable {
    public static final int MAX_BLOCK_SIZE = 16 * 1024 * 1024;
    private static final int DIV = Short.MAX_VALUE / Vec3f.MAX_POS;
    private DataInputStream din;
    private final DataOutputStream dout;
    private final ByteArrayOutputStream baos;
    private IOHelper parent;
    final byte[] dataIn;

    public IOHelper(InputStream in) {
        this(new DataInputStream(in), null, null, null);
    }

    public IOHelper(byte[] in) {
        byte[] b = in == null ? new byte[0] : in;
        this.din = new DataInputStream(new ByteArrayInputStream(b));
        this.dout = null;
        this.baos = null;
        this.parent = null;
        this.dataIn = b;
    }

    private IOHelper(ByteArrayOutputStream out) {
        this(null, new DataOutputStream(out), out, null);
    }

    private IOHelper(DataInputStream din, DataOutputStream dout, ByteArrayOutputStream baos, byte[] dataIn) {
        this.din = din;
        this.dout = dout;
        this.baos = baos;
        this.parent = null;
        this.dataIn = dataIn;
    }

    public DataOutputStream getDout() {
        return dout;
    }

    @Override
    public void write(int b) throws IOException {
        dout.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        dout.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        dout.write(b, off, len);
    }

    public void readFully(byte[] b) throws IOException {
        din.readFully(b);
    }

    public byte readByte() throws IOException {
        return din.readByte();
    }

    public boolean readBoolean() throws IOException {
        return din.readBoolean();
    }

    public long readLong() throws IOException {
        return din.readLong();
    }

    public short readShort() throws IOException {
        return din.readShort();
    }

    public int read() throws IOException {
        return din.read();
    }

    public void writeByte(int v) throws IOException {
        dout.writeByte(v);
    }

    public void writeBoolean(boolean v) throws IOException {
        dout.writeBoolean(v);
    }

    public void writeLong(long v) throws IOException {
        dout.writeLong(v);
    }

    public void writeShort(int v) throws IOException {
        dout.writeShort(v);
    }

    public String readUTF() throws IOException {
        int n = readVarInt();
        if (n < 0) throw new IOException();
        if (n == 0) return "";
        byte[] b = new byte[n];
        readFully(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    public void writeUTF(String s) throws IOException {
        if (s == null || s.isEmpty()) {
            writeVarInt(0);
            return;
        }
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(b.length);
        write(b);
    }

    public Vec2i read2s() throws IOException {
        return new Vec2i(readShort(), readShort());
    }

    public void write2s(Vec2i v) throws IOException {
        writeShort(v.x);
        writeShort(v.y);
    }

    public <T extends Enum<T>> void writeEnum(T val) throws IOException {
        writeByte(val.ordinal());
    }

    public <T extends Enum<T>> T readEnum(T[] values) throws IOException {
        int id = readByte();
        return id >= 0 && id < values.length ? values[id] : null;
    }

    public void writeUUID(UUID uuid) throws IOException {
        writeLong(uuid.getLeastSignificantBits());
        writeLong(uuid.getMostSignificantBits());
    }

    public UUID readUUID() throws IOException {
        long l = readLong(), m = readLong();
        return new UUID(m, l);
    }

    public void writeVarInt(int v) throws IOException {
        while ((v & ~0x7F) != 0) {
            dout.writeByte((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        dout.writeByte(v);
    }

    public int readVarInt() throws IOException {
        int out = 0, shift = 0;
        while (true) {
            byte b = din.readByte();
            out |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return out;
            shift += 7;
            if (shift > 35) throw new IOException("VarInt too big");
        }
    }

    public void writeSignedVarInt(int v) throws IOException {
        int sign = v < 0 ? 0x40 : 0;
        int a = Math.abs(v);
        int b = (a & 0x3F) | sign;
        a >>>= 6;
        while (a != 0) {
            dout.writeByte(b | 0x80);
            b = a & 0x7F;
            a >>>= 7;
        }
        dout.writeByte(b);
    }

    public int readSignedVarInt() throws IOException {
        byte b = din.readByte();
        int sign = (b & 0x40) != 0 ? -1 : 1;
        int out = b & 0x3F;
        int shift = 6;
        while ((b & 0x80) != 0) {
            b = din.readByte();
            out |= (b & 0x7F) << shift;
            shift += 7;
            if (shift > 34) throw new IOException("SignedVarInt too big");
        }
        return out * sign;
    }

    public float readVarFloat() throws IOException {
        return readSignedVarInt() / (float) DIV;
    }

    public void writeVarFloat(float f) throws IOException {
        writeSignedVarInt((int) (f * DIV));
    }

    public Vec3f readVec3ub() throws IOException {
        Vec3f v = new Vec3f();
        v.x = din.read() / 10f;
        v.y = din.read() / 10f;
        v.z = din.read() / 10f;
        return v;
    }

    public void writeVec3ub(Vec3f v) throws IOException {
        dout.write(MathHelper.clamp((int) (v.x * 10), 0, 255));
        dout.write(MathHelper.clamp((int) (v.y * 10), 0, 255));
        dout.write(MathHelper.clamp((int) (v.z * 10), 0, 255));
    }

    public Vec3f readAngle() throws IOException {
        Vec3f v = new Vec3f();
        v.x = (float) (din.readShort() / 65535f * 2 * Math.PI);
        v.y = (float) (din.readShort() / 65535f * 2 * Math.PI);
        v.z = (float) (din.readShort() / 65535f * 2 * Math.PI);
        return v;
    }

    public void writeAngle(Vec3f v) throws IOException {
        dout.writeShort(MathHelper.clamp((int) (v.x / 360f * 65535), 0, 65535));
        dout.writeShort(MathHelper.clamp((int) (v.y / 360f * 65535), 0, 65535));
        dout.writeShort(MathHelper.clamp((int) (v.z / 360f * 65535), 0, 65535));
    }

    public float readFloat2() throws IOException {
        return din.readShort() / (float) DIV;
    }

    public void writeFloat2(float f) throws IOException {
        dout.writeShort(MathHelper.clamp((int) (f * DIV), Short.MIN_VALUE, Short.MAX_VALUE));
    }

    public Vec3f readVec6b() throws IOException {
        Vec3f v = new Vec3f();
        v.x = readFloat2();
        v.y = readFloat2();
        v.z = readFloat2();
        return v;
    }

    public void writeVec6b(Vec3f v) throws IOException {
        writeFloat2(v.x);
        writeFloat2(v.y);
        writeFloat2(v.z);
    }

    public Vec3f readVarVec3() throws IOException {
        Vec3f v = new Vec3f();
        v.x = readVarFloat();
        v.y = readVarFloat();
        v.z = readVarFloat();
        return v;
    }

    public void writeVarVec3(Vec3f v) throws IOException {
        writeVarFloat(v.x);
        writeVarFloat(v.y);
        writeVarFloat(v.z);
    }

    public IOHelper readNextBlock() throws IOException {
        int size = readVarInt();
        if (size < 0 || size > MAX_BLOCK_SIZE) throw new IOException("Invalid block size: " + size);
        byte[] dt = new byte[size];
        readFully(dt);
        return new IOHelper(dt);
    }

    public IOHelper writeNextBlock() {
        IOHelper ch = new IOHelper(new ByteArrayOutputStream());
        ch.parent = this;
        return ch;
    }

    public byte[] readByteArray() throws IOException {
        int n = readVarInt();
        if (n < 0) throw new IOException();
        if (n == 0) return new byte[0];
        byte[] b = new byte[n];
        readFully(b);
        return b;
    }

    public int size() throws IOException {
        if (dataIn != null) return dataIn.length;
        if (baos != null) return baos.size();
        throw new IOException("Not a byte array backed IOHelper");
    }

    public void reset() throws IOException {
        if (dataIn != null) din = new DataInputStream(new ByteArrayInputStream(dataIn));
        else din.reset();
    }

    @FunctionalInterface
    public interface ObjectReader<T, R> {
        R read(T type, IOHelper block) throws IOException;
    }

    @FunctionalInterface
    public interface ObjectWriter<B> {
        void write(B data, IOHelper out) throws IOException;
    }

    public interface ObjectBlock<T extends Enum<T>> {
        void write(IOHelper h) throws IOException;

        T getType();
    }

    public <T extends Enum<T>, B> B readObjectBlock(T[] values, ObjectReader<T, B> reader) throws IOException {
        T v = readEnum(values);
        IOHelper b = readNextBlock();
        return v == null ? null : reader.read(v, b);
    }

    public <T extends Enum<T>, B extends ObjectBlock<T>> void writeObjectBlock(B val) throws IOException {
        writeEnum(val.getType());
        try (IOHelper h = writeNextBlock()) {
            val.write(h);
        }
    }

    public ImageBlock readImage() throws IOException {
        return new ImageBlock(this);
    }

    public void writeImage(Image img) throws IOException {
        try (IOHelper h = writeNextBlock()) {
            img.storeTo(h.getDout());
        }
    }

    public static final class ImageBlock {
        private final IOHelper buf;
        private final byte[] raw;
        private Image image;
        private int w, h;

        public ImageBlock(IOHelper io) throws IOException {
            buf = io.readNextBlock();
            raw = buf.dataIn == null ? new byte[0] : buf.dataIn;
            if (raw.length != 0 && ImageIO.isAvailable()) {
                Vec2i s = ImageIO.getSize(new ByteArrayInputStream(raw));
                w = s.x;
                h = s.y;
            }
        }

        public int getWidth() {
            return w;
        }

        public int getHeight() {
            return h;
        }

        public void doReadImage() throws IOException {
            if (raw.length != 0 && ImageIO.isAvailable()) {
                image = Image.loadFrom(new ByteArrayInputStream(raw));
                w = image.getWidth();
                h = image.getHeight();
            }
        }

        public Image getImage() {
            return image;
        }
    }

    @Override
    public void close() throws IOException {
        if (dout != null) dout.flush();
        if (parent != null) {
            byte[] b = baos.toByteArray();
            parent.writeVarInt(b.length);
            parent.write(b);
        }
    }
}
