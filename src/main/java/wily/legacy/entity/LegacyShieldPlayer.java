package wily.legacy.entity;

public interface LegacyShieldPlayer {
    int SHIELD_PAUSE_TICKS = 6;

    void pauseShield(int ticks);

    boolean isShieldPaused();
}
