package wily.legacy.CustomModelSkins.cpm.shared.config;

import wily.legacy.CustomModelSkins.cpm.shared.definition.Link;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;

import java.io.IOException;
import java.net.URISyntaxException;

public interface ResourceLoader {
    enum ResourceEncoding {NO_ENCODING, BASE64}

    @FunctionalInterface
    interface Validator {
        Link test(String url) throws URISyntaxException;
    }

    default Validator getValidator() {
        return null;
    }

    byte[] loadResource(String path, ResourceEncoding enc, ModelDefinition def) throws IOException;

    default byte[] loadResource(Link link, ResourceEncoding enc, ModelDefinition def) throws IOException {
        return loadResource(link.getPath(), enc, def);
    }
}
