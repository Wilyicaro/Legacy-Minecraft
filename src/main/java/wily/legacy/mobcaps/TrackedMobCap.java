package wily.legacy.mobcaps;

enum TrackedMobCap {
    GENERAL_ANIMALS(50, 70, 90),
    CHICKENS(8, 16, 26),
    WOLVES(8, 16, 26),
    MOOSHROOMS(2, 12, 20),
    MONSTERS(50, 50, 70),
    SLIMES(25, 25, 25),
    GUARDIANS(35, 35, 35),
    AMBIENT(20, 20, 28),
    WATER_ANIMALS(20, 20, 28),
    SQUIDS(8, 8, 14),
    DOLPHINS(15, 20, 25),
    PHANTOMS(5, 5, 5),
    VILLAGERS(35, 35, 50),
    SNOW_GOLEMS(16, 16, 16),
    IRON_GOLEMS(16, 16, 16),
    WITHERS(5, 5, 5),
    ARMOR_STANDS(16, 16, 16),
    END_CRYSTALS(64, 64, 64),
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
