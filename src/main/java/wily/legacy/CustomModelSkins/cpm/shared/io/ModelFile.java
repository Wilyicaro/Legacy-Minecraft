package wily.legacy.CustomModelSkins.cpm.shared.io;

import wily.legacy.CustomModelSkins.cpm.shared.definition.Link;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinitionLoader;
import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper.ImageBlock;

import java.io.IOException;
import java.io.InputStream;

public class ModelFile {
    private String name, desc, fname;
    private Link link;
    private byte[] dataBlock, overflowLocal;
    private ImageBlock icon;

    private ModelFile() {
    }

    public static ModelFile load(String name, InputStream fin) throws IOException {
        if (fin.read() != ModelDefinitionLoader.HEADER) throw new IOException("Magic number mismatch");
        ChecksumInputStream cis = new ChecksumInputStream(fin);
        IOHelper h = new IOHelper(cis);
        ModelFile mf = new ModelFile();
        mf.fname = name;
        mf.name = h.readUTF();
        mf.desc = h.readUTF();
        mf.dataBlock = h.readByteArray();
        byte[] ovf = h.readByteArray();
        if (ovf.length != 0) {
            mf.overflowLocal = ovf;
            mf.link = new Link(h);
        }
        ImageBlock block = h.readImage();
        if (block.getWidth() > 256 || block.getHeight() > 256) throw new IOException("Texture size too large");
        mf.icon = block;
        cis.checkSum();
        return mf;
    }

    public byte[] getDataBlock() {
        return dataBlock;
    }

    public void registerLocalCache(ModelDefinitionLoader loader) {
        if (overflowLocal != null) {
            loader.putLocalResource(link, overflowLocal);
        }
    }
}
