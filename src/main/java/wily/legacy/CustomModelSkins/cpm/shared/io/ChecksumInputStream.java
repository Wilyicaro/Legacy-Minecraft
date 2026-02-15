package wily.legacy.CustomModelSkins.cpm.shared.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class ChecksumInputStream extends InputStream {
    private InputStream is;
    private short sum;

    public ChecksumInputStream(InputStream is) {
        this.is = is;
    }

    @Override
    public int read() throws IOException {
        int v = is.read();
        if (v != -1) sum += v;
        return v;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int rd = is.read(b, off, len);
        for (int i = 0; i < rd; i++) {
            sum += Byte.toUnsignedInt(b[off + i]);
        }
        return rd;
    }

    public short getSum() {
        return sum;
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    public void checkSum() throws IOException {
        int ch1 = is.read();
        int ch2 = is.read();
        if ((ch1 | ch2) < 0) throw new EOFException();
        short sum = (short) ((ch1 << 8) + (ch2 << 0));
        if (getSum() != sum) {
            throw new IOException("Sum error: expected: " + sum + " actual: " + getSum());
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        is.reset();
        sum = 0;
    }
}
