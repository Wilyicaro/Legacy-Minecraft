package wily.legacy.world;

public interface LegacyWorldSettings {
    static LegacyWorldSettings of(Object object) {
        return (LegacyWorldSettings) object;
    }

    boolean trustPlayers();

    void setTrustPlayers(boolean trust);
}
