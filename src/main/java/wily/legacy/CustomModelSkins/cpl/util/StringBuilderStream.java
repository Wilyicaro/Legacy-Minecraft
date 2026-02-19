package wily.legacy.CustomModelSkins.cpl.util;

import java.io.PrintStream;

public class StringBuilderStream extends PrintStream {
    private StringBuilder bb;
    private String sep;

    public StringBuilderStream(StringBuilder bb, String sep) {
        super(System.err);
        this.bb = bb;
        this.sep = sep;
    }

    @Override
    public void println(Object x) {
        bb.append(x);
        bb.append(sep);
    }

    @Override
    public void println(String x) {
        bb.append(x);
        bb.append(sep);
    }

    public static void stacktraceToString(Throwable t, StringBuilder sb, String sep) {
        t.printStackTrace(new StringBuilderStream(sb, sep));
    }
}
