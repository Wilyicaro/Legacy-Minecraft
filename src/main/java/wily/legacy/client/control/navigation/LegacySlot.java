package wily.legacy.client.control.navigation;

public interface LegacySlot {
    LegacySlotDisplay getDisplay();

    void setDisplay(LegacySlotDisplay slot);

    void setX(int x);

    void setY(int y);
}
