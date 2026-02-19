package wily.legacy.CustomModelSkins.cpm.shared.model;

import wily.legacy.CustomModelSkins.cpl.util.Image;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinitionLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public enum SkinType {
    SLIM(0, "Slim"), DEFAULT(1, "Classic"), UNKNOWN(-1, "Classic"),
    ;
    private final int channel;
    private final String lowerName, apiName;
    public static final SkinType[] VALUES = values();

    private SkinType(int channel, String apiName) {
        this.channel = channel;
        lowerName = name().toLowerCase(Locale.ROOT);
        this.apiName = apiName;
    }

    public static SkinType get(String name) {
        if (name == null) return DEFAULT;
        for (SkinType pl : VALUES) {
            if (name.equals(pl.lowerName)) return pl;
        }
        return UNKNOWN;
    }

    public String getName() {
        return lowerName;
    }

    public Image getSkinTexture() {
        if (this == UNKNOWN) return DEFAULT.getSkinTexture();
        Image tex = new Image(64, 64);
        try (InputStream is = ModelDefinitionLoader.class.getResourceAsStream("/assets/cpm/textures/template/" + lowerName + ".png")) {
            tex = Image.loadFrom(is);
        } catch (IOException e) {
        }
        return tex;
    }
}
