package wily.legacy.Skins.client.render;

import net.minecraft.client.Minecraft;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ZombieArmsConfig {
    private static final String FILE_NAME = "consoleskins_zombie_arms.txt";
    private static final Set<String> DEFAULT_IDS;
    private static volatile long lastLoadMs;
    private static volatile Set<String> ids = Collections.emptySet();

    static {
        Set<String> d = new HashSet<>();

        d.add("cpm:legacy_skinpacks:skinpacks/sp1/zombie.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/sp3/zombieherobrine.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/sp3/zombiepigman.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/summer_of_arcade/skinnyzombie.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/summer_of_arcade/zombief.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/sp5/femalezombie.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/sp5/malezombie.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/sp6/villagerzombie.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/birthday_2/partyzombievillager.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/birthday_2/partywitherskeleton.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/birthday_1/cakezombie.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/mgm/zombieglider.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/mgm/zombiebattler.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/mgm/zombietumbler.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/festive_pack/marleysghost.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/festive_pack/zombieturkey.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/festive_pack/zombiepudding.cpmmodel");

        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/captainzombietwistedpixelgames.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/happyzombieclerictoylogic.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/happyzombiemagetoylogic.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/happyzombiewarriortoylogic.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/zippyzombierare.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/zombiebusinessmanclimaxstudios.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/zombieclimaxstudios.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/zombiejacklumberowlchemylabs.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/zombielonestarronimogames.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_mashup/zippyzombie.cpmmodel");

        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/ancientmummyrare.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/mummy4jstudios.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/mummyarmyoftrolls.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/ghost4jstudios.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/evilrobotclimaxstudios.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/evilrobotmojang.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_mashup/ancientmummy.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_mashup/mummy.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_mashup/ghost.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_mashup/evilrobot.cpmmodel");
        DEFAULT_IDS = Collections.unmodifiableSet(d);
    }

    private ZombieArmsConfig() {
    }

    public static boolean isZombieArmsSkin(String skinId) {
        if (skinId == null || skinId.isBlank()) return false;
        reloadIfNeeded();
        return ids.contains(skinId) || SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.ZOMBIE_ARMS, skinId);
    }

    public static void reloadNow() {
        ids = loadIds();
        lastLoadMs = System.currentTimeMillis();
    }

    private static void reloadIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastLoadMs < 2000) return;
        reloadNow();
    }

    private static Set<String> loadIds() {
        try {
            File dir = Minecraft.getInstance().gameDirectory;
            File cfg = new File(dir, "config");
            File f = new File(cfg, FILE_NAME);
            Set<String> out = new HashSet<>(DEFAULT_IDS);
            if (!f.exists()) return out;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    out.add(line);
                }
            }
            return out;
        } catch (Throwable ignored) {
            return DEFAULT_IDS;
        }
    }
}
