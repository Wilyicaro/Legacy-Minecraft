package wily.legacy.CustomModelSkins.cpm.shared.definition;

import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Link {
    protected String loader;
    protected String path;

    public Link(IOHelper in) throws IOException {
        int pathLen = in.read();
        byte[] path = new byte[pathLen];
        in.readFully(path);
        String[] sp = new String(path).split(":");
        loader = sp[0];
        this.path = sp[1];
    }

    public void write(IOHelper dout) throws IOException {
        byte[] path = (loader + ":" + this.path).getBytes();
        dout.write(path.length);
        dout.write(path);
    }

    @Override
    public String toString() {
        return loader + ":" + this.path;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((loader == null) ? 0 : loader.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Link other = (Link) obj;
        if (loader == null) {
            if (other.loader != null) return false;
        } else if (!loader.equals(other.loader)) return false;
        if (path == null) {
            if (other.path != null) return false;
        } else if (!path.equals(other.path)) return false;
        return true;
    }

    public static class ResolvedLink {
        private byte[] data;
        private IOException error;

        public ResolvedLink(IOException error) {
            this.error = error;
        }

        public ResolvedLink(byte[] data) {
            this.data = data;
        }

        public InputStream getData() throws IOException {
            if (error != null) throw error;
            return new ByteArrayInputStream(data);
        }
    }

    public String getPath() {
        return path;
    }
}
