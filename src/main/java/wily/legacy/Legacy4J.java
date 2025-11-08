package wily.legacy;

import net.minecraft.core.Holder;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import wily.factoryapi.base.network.CommonRecipeManager;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.block.LegacyBlockBehaviors;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.config.LegacyMixinToggles;
import wily.legacy.config.LegacyWorldOptions;
import wily.legacy.init.*;
import wily.legacy.network.*;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.util.ArmorStandPose;

//? if fabric {
//?} else if forge {
/*import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
*///?} else if neoforge {
/*import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
*///?}

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

//? if forge || neoforge
/*@Mod(Legacy4J.MOD_ID)*/
public class Legacy4J {

    public static final String MOD_ID = "legacy";
    public static final Supplier<String> VERSION = () -> FactoryAPIPlatform.getModInfo(MOD_ID).getVersion();
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final FactoryConfig.StorageHandler MIXIN_CONFIGS_STORAGE = FactoryConfig.StorageHandler.fromMixin(LegacyMixinToggles.COMMON_STORAGE, true);

    private static Collection<CommonNetwork.Payload> playerInitialPayloads = Collections.emptySet();

    public Legacy4J() {
        init();
        //? if forge || neoforge {
        /*if (FactoryAPI.isClient())
            Legacy4JClient.init();
        *///?}
    }

    public static List<Integer> getParsedVersion(String version) {
        List<Integer> parsedVersion = new ArrayList<>();
        String[] versions = version.split("[.\\-]");
        for (String s : versions) {
            int value;
            try {
                value = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                value = 0;
            }
            parsedVersion.add(value);
        }
        return parsedVersion;
    }

    public static boolean isNewerVersion(String actualVersion, String previous) {
        return isNewerVersion(actualVersion, previous, 2);
    }

    public static boolean isNewerVersion(String actualVersion, String previous, int limitCount) {
        List<Integer> v = getParsedVersion(actualVersion);
        List<Integer> v1 = getParsedVersion(previous);
        int size = limitCount <= 0 ? v.size() : Math.min(limitCount, v.size());
        for (int i = 0; i < size; i++) {
            if (v.get(i) > (v1.size() <= i ? 0 : v1.get(i))) return true;
        }
        return false;
    }

    public static void init() {
        FactoryConfig.registerCommonStorage(createModLocation("common"), LegacyCommonOptions.COMMON_STORAGE);
        FactoryConfig.registerCommonStorage(createModLocation("mixin_common"), MIXIN_CONFIGS_STORAGE);
        LegacyRegistries.register();
        LegacyGameRules.init();
        FactoryEvent.registerPayload(r -> {
            r.register(false, ClientAdvancementsPayload.ID);
            r.register(false, ClientAnimalInLoveSyncPayload.ID);
            r.register(false, ClientEffectActivationPayload.ID);
            r.register(true, ClientMerchantTradingPayload.ID_C2S);
            r.register(false, ClientMerchantTradingPayload.ID_S2C);
            r.register(true, PlayerInfoSync.ID);
            r.register(true, PlayerInfoSync.All.ID_C2S);
            r.register(false, PlayerInfoSync.All.ID_S2C);
            r.register(true, ServerMenuCraftPayload.ID);
            r.register(true, ServerOpenClientMenuPayload.ID);
            r.register(true, ServerPlayerMissHitPayload.ID);
            r.register(false, TipCommand.Payload.ID);
            r.register(false, TipCommand.EntityPayload.ID);
            r.register(false, TopMessage.Payload.ID);
        });
        ArmorStandPose.init();
        FactoryEvent.setItemComponent(Items.CAKE, DataComponents.MAX_STACK_SIZE, 64);
        FactoryEvent.registerCommands(TipCommand::register);
        FactoryEvent.setup(Legacy4J::setup);
        FactoryEvent.tagsLoaded(Legacy4J::tagsLoaded);
        FactoryEvent.serverStarted(Legacy4J::onServerStart);
        FactoryEvent.PlayerEvent.JOIN_EVENT.register(Legacy4J::onServerPlayerJoin);
        FactoryEvent.PlayerEvent.RELOAD_RESOURCES_EVENT.register(Legacy4J::onResourcesReload);
    }

    public static ResourceLocation createModLocation(String path) {
        return FactoryAPI.createLocation(MOD_ID, path);
    }

    public static void setup() {
        LegacyCommonOptions.COMMON_STORAGE.load();
        CommonRecipeManager.addRecipeTypeToSync(RecipeType.CRAFTING);
        CommonRecipeManager.addRecipeTypeToSync(RecipeType.STONECUTTING);

        LegacyBlockBehaviors.setup();
    }

    public static boolean isChunkPosVisibleInSquare(int centerX, int centerZ, int viewDistance, int x, int z, boolean offset) {
        int n = Math.max(0, Math.abs(x - centerX) - 1);
        int o = Math.max(0, Math.abs(z - centerZ) - 1);
        long p = Math.max(0, Math.max(n, o) - (offset ? 1 : 0));
        long q = Math.min(n, o);
        return Math.max(p, q) < viewDistance;
    }

    public static void tagsLoaded() {
        LegacyBlockBehaviors.registerDyedWaterCauldronInteraction(CauldronInteraction.WATER.map());
    }

    public static Vec3 getRelativeMovement(LivingEntity entity, float f, Vec3 vec3, int relRot) {
        vec3 = getNormal(vec3, Math.toRadians(relRot));
        double d = vec3.lengthSqr();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        } else {
            Vec3 vec32 = (d > 1.0 ? vec3.normalize() : vec3).scale(f);
            double angle = Math.toRadians(relRot == 0 ? entity.getYRot() : Math.round(entity.getYRot() / relRot) * relRot);
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            return new Vec3(vec32.x * cos - vec32.z * sin, vec32.y, vec32.z * cos + vec32.x * sin);
        }
    }

    public static Vec3 getNormal(Vec3 vec3, double relRot) {
        if (relRot == 0) return vec3;
        double angleRad = Math.atan2(vec3.z, vec3.x);
        double quantizedAngle = Math.round(angleRad / relRot) * relRot;
        double length = vec3.length();
        return new Vec3(length * Math.cos(quantizedAngle), vec3.y, length * Math.sin(quantizedAngle));
    }

    public static void onServerPlayerJoin(ServerPlayer p) {
        MinecraftServer server = FactoryAPIPlatform.getEntityServer(p);
        if (server == null) return;
        int pos = 0;
        boolean b = true;
        main:
        while (b) {
            b = false;
            for (ServerPlayer player : server.getPlayerList().getPlayers())
                if (player != p && ((LegacyPlayerInfo) player).getIdentifierIndex() == pos) {
                    pos++;
                    b = true;
                    continue main;
                }
        }
        ((LegacyPlayerInfo) p).setIdentifierIndex(pos);
        CommonNetwork.sendToPlayers(server.getPlayerList().getPlayers().stream().filter(sp -> sp != p).collect(Collectors.toSet()), new PlayerInfoSync.All(Map.of(p.getUUID(), (LegacyPlayerInfo) p), Collections.emptyMap(), server.getDefaultGameType(), PlayerInfoSync.All.ID_S2C));

        CommonNetwork.sendToPlayer(p, PlayerInfoSync.All.fromPlayerList(server), true);
        playerInitialPayloads.forEach(payload -> CommonNetwork.sendToPlayer(p, payload, true));

        if (!FactoryAPIPlatform.getEntityServer(p).isDedicatedServer()) Legacy4JClient.serverPlayerJoin(p);
    }

    public static void onServerStart(MinecraftServer server) {
        playerInitialPayloads = createPlayerInitialPayloads(server);
        LegacyWorldOptions.WORLD_STORAGE.withServerFile(server, "legacy_data.json").resetAndLoad();
    }

    public static void onResourcesReload(PlayerList playerList) {
        onServerStart(playerList.getServer());
        playerInitialPayloads.forEach(payload -> CommonNetwork.sendToPlayers(playerList.getPlayers(), payload));
    }

    public static Collection<CommonNetwork.Payload> createPlayerInitialPayloads(MinecraftServer server) {
        HashSet<CommonNetwork.Payload> payloads = new HashSet<>();
        payloads.add(new ClientAdvancementsPayload(List.copyOf(server.getAdvancements().getAllAdvancements())));
        return payloads;
    }

    public static void copySaveToDirectory(InputStream stream, File directory) {
        if (directory.exists()) FileUtils.deleteQuietly(directory);
        try (ZipInputStream inputStream = new ZipInputStream(stream)) {
            ZipEntry zipEntry;
            byte[] buffer = new byte[1024];
            while ((zipEntry = inputStream.getNextEntry()) != null) {
                File newFile = new File(directory, zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = inputStream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
