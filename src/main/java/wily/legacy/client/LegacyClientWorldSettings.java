package wily.legacy.client;

import wily.legacy.world.LegacyWorldSettings;

public interface LegacyClientWorldSettings extends LegacyWorldSettings {
    static LegacyClientWorldSettings of(Object object) {
        return (LegacyClientWorldSettings) object;
    }

    long getDisplaySeed();

    void setDisplaySeed(long s);

    boolean isDifficultyLocked();

    void setDifficultyLocked(boolean locked);

    void setAllowCommands(boolean allow);

    PackAlbum getSelectedResourceAlbum();

    void setSelectedResourceAlbum(PackAlbum album);
}
