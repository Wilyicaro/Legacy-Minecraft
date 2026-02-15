package wily.legacy.CustomModelSkins.cpm.shared.parts;

import wily.legacy.CustomModelSkins.cpm.shared.io.IOHelper;

import java.io.IOException;

public interface PartCollection {
    void writeBlocks(IOHelper dout) throws IOException;
}
