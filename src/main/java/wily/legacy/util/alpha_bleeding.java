// Copyright (c) 2013, 2014, 2015, 2017 urraka
// Copyright (c) 2026 Jab125
// The original C++ is at https://github.com/urraka/alpha-bleeding/tree/ac89ab30006c91afc259c8f68152ad79eae1a08c under the MIT license
// This version is released under the GNU Lesser General Public License versions 3.0 or later
package wily.legacy.util;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

// C++ style name preserved
public class alpha_bleeding {
    private alpha_bleeding() {
        throw new AssertionError();
    }

    @SuppressWarnings({"PointlessArithmeticExpression", "UnusedAssignment", "MethodNameSameAsClassName"})
    public static void alpha_bleeding(MemorySegment image, int width, int height) {
        final int N = width * height;
        if (image.byteSize() < N * 4L) image = image.reinterpret(N * 4L);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment opaque = arena.allocate(JAVA_BYTE, N);
            MemorySegment loose = arena.allocate((N + 7) / 8);
            MemorySegment pending = arena.allocate(JAVA_LONG, N);
            int pendingSize = 0;
            MemorySegment pendingNext = arena.allocate(JAVA_LONG, N);
            int pendingNextSize = 0;

            int[][] offsets = {
                    {-1, -1},
                    {0, -1},
                    {1, -1},
                    {-1, 0},
                    {1, 0},
                    {-1, 1},
                    {0, 1},
                    {1, 1}
            };

            for (int i = 0, j = 3; i < N; i++, j += 4) {
                long byteIndex = i >>> 3;
                int bitIndex = i & 7;
                if (image.getAtIndex(JAVA_BYTE, j) == 0) {
                    boolean isLoose = true;

                    int x = i % width;
                    int y = i / width;

                    for (int k = 0; k < 8; k++) {
                        int s = offsets[k][0];
                        int t = offsets[k][1];

                        if (x + s >= 0 && x + s < width && y + t >= 0 && y + t < height) {
                            long index = j + 4 * (s + (long) t * width);

                            if (image.getAtIndex(JAVA_BYTE, index) != 0) {
                                isLoose = false;
                                break;
                            }
                        }
                    }

                    if (!isLoose) {
                        pending.setAtIndex(JAVA_LONG, pendingSize++, i);
                    } else {
                        byte value = loose.get(ValueLayout.JAVA_BYTE, byteIndex);
                        value |= (byte) (1 << bitIndex);
                        loose.set(ValueLayout.JAVA_BYTE, byteIndex, value);
                    }

                } else {
                    opaque.setAtIndex(JAVA_BYTE, i, (byte) -1);
                }
            }

            while (pendingSize > 0) {
                pendingNextSize = 0;

                for (long p = 0; p < pendingSize; p++) {
                    long i = pending.getAtIndex(JAVA_LONG, p) * 4;
                    long j = pending.getAtIndex(JAVA_LONG, p);

                    int x = (int) j % width;
                    int y = (int) j / width;

                    int r = 0;
                    int g = 0;
                    int b = 0;

                    int count = 0;

                    for (long k = 0; k < 8; k++) {
                        int s = offsets[(int)k][0];
                        int t = offsets[(int)k][1];

                        if (x + s >= 0 && x + s < width && y + t >= 0 && y + t < height) {
                            t *= width;

                            if ((opaque.getAtIndex(JAVA_BYTE, j + s + t) & 1) == 1) {
                                long index = i + 4L * (s + t);

                                r += Byte.toUnsignedInt(image.getAtIndex(JAVA_BYTE, index + 0));
                                g += Byte.toUnsignedInt(image.getAtIndex(JAVA_BYTE, index + 1));
                                b += Byte.toUnsignedInt(image.getAtIndex(JAVA_BYTE, index + 2));

                                count++;
                            }
                        }
                    }

                    if (count > 0) {
                        image.setAtIndex(JAVA_BYTE, i + 0, (byte) (r / count));
                        image.setAtIndex(JAVA_BYTE, i + 1, (byte) (g / count));
                        image.setAtIndex(JAVA_BYTE, i + 2, (byte) (b / count));

                        opaque.setAtIndex(JAVA_BYTE, j, (byte) 0xFE);

                        for (long k = 0; k < 8; k++) {
                            int s = offsets[(int)k][0];
                            int t = offsets[(int)k][1];

                            if (x + s >= 0 && x + s < width && y + t >= 0 && y + t < height) {
                                long index = j + s + (long) t * width;
                                long byteIndex = index >>> 3;
                                int bitIndex = (int) index & 7;
                                if ((loose.get(ValueLayout.JAVA_BYTE, byteIndex) & (1 << bitIndex)) != 0) {
                                    pendingNext.setAtIndex(JAVA_LONG, pendingNextSize++, index);
                                    byte value = loose.get(ValueLayout.JAVA_BYTE, byteIndex);
                                    value &= (byte) ~(1 << bitIndex);
                                    loose.set(ValueLayout.JAVA_BYTE, byteIndex, value);
                                }
                            }
                        }
                    } else {
                        pendingNext.setAtIndex(JAVA_LONG, pendingNextSize++, j);
                    }
                }

                if (pendingNextSize > 0) {
                    for (long p = 0; p < pendingSize; p++) {
                        long atIndex = opaque.getAtIndex(JAVA_BYTE, pending.getAtIndex(JAVA_LONG, p));
                        opaque.setAtIndex(JAVA_BYTE, pending.getAtIndex(JAVA_LONG, p), (byte) (atIndex >> 1));
                    }
                }
                {
                    int tempNextSize = pendingNextSize;
                    MemorySegment tempPendingNext = pendingNext;
                    pendingNext = pending;
                    pendingNextSize = pendingSize;
                    pending = tempPendingNext;
                    pendingSize = tempNextSize;
                }
            }
        }
    }

    public static void alpha_remove(MemorySegment image, int width, int height) {
        final long N = 4L * width * height;
        if (image.byteSize() < N) image = image.reinterpret(N);;
        for (long i = 3; i < N; i += 4) {
            image.setAtIndex(JAVA_BYTE, i, (byte) 0xFF);
        }
    }

    @SuppressWarnings({"JavaPrintToLogpoint", "DuplicateExpressions"})
    static void main(String[] args) throws IOException {
        int argc = args.length + 1;
        if (argc != 3) {
            IO.println("Usage: alpha-bleeding <input> <output>");
            System.exit(0);
        }
        String[] argv = new String[3];
        System.arraycopy(args, 0, argv, 1, 2);

        final String input = argv[1];
        final String output = argv[2];
        if (Files.exists(Path.of(output))) {
            IO.println("Output file already exists!");
            System.exit(0);
        }

        int w, h, c;

        NativeImage image = NativeImage.read(Files.newInputStream(Path.of(input)));
        MemorySegment data = MemorySegment.ofAddress(image.getPointer());
        w = image.getWidth();
        h = image.getHeight();
        c = image.format().components();

        //noinspection ConstantValue
        if (data == null) {
            IO.println("Error loading image. Must be PNG format.");
            System.exit(1);
        }

        if (c != 4) {
            IO.println("The image must be 32 bits (RGB with alpha channel).");
            image.close();
            System.exit(0);
        }

        alpha_bleeding(data, w, h);
        image.writeToFile(Path.of(output));
    }
}
