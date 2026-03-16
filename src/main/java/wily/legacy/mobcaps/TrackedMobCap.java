package wily.legacy.mobcaps;

enum TrackedMobCap {
    GENERAL_ANIMALS(50, 70, 90),
    CHICKENS(8, 16, 26),
    WOLVES(8, 16, 26),
    MOOSHROOMS(2, 22, 30),
    MONSTERS(50, 0, 70),
    AMBIENT(20, 0, 28),
    SQUIDS(5, 0, 13),
    VILLAGERS(0, 35, 50),
    SNOW_GOLEMS(0, 0, 16),
    IRON_GOLEMS(0, 0, 16),
    BOSSES(0, 0, 1),
    ARMOR_STANDS(0, 0, 24),
    BOATS(0, 0, 40),
    HANGING(0, 0, 400);

    private final int naturalLimit;
    private final int breedingLimit;
    private final int manualLimit;

    TrackedMobCap(int naturalLimit, int breedingLimit, int manualLimit) {
        this.naturalLimit = naturalLimit;
        this.breedingLimit = breedingLimit;
        this.manualLimit = manualLimit;
    }

    int naturalLimit() {
        return naturalLimit;
    }

    int breedingLimit() {
        return breedingLimit;
    }

    int manualLimit() {
        return manualLimit;
    }
}
