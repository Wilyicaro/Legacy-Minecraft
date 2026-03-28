package wily.legacy.Skins.skin;

public enum SkinPackSourceKind {
    BOX_MODEL("Box Models"),
    LEGACY_SKINS("Legacy Skins"),
    BEDROCK_SKINS("Bedrock Skins"),
    SPECIAL("Special");

    private final String label;

    SkinPackSourceKind(String label) {
        this.label = label;
    }

    
    public String label() {
        return label;
    }
}
