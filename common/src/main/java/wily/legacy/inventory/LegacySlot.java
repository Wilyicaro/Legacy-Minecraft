package wily.legacy.inventory;

public interface LegacySlot {
    LegacySlotDisplay getDisplay();
    void setLegacySlot(LegacySlotDisplay slot);
    void setX(int x);
    void setY(int y);
}
