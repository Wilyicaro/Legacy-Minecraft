package wily.legacy;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import wily.legacy.util.RegisterListing;
import wily.legacy.util.ModInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Legacy4JPlatform {
    @ExpectPlatform
    public static Path getConfigDirectory() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static TagKey<Item> getCommonItemTag(String tag) {
        throw new AssertionError();
    }
    @ExpectPlatform
    public static Collection<ModInfo> getMods() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isLoadingMod(String modId) {
        throw new AssertionError();
    }
    @ExpectPlatform
    public static boolean isModLoaded(String modId) {
        throw new AssertionError();
    }
    @ExpectPlatform
    public static ModInfo getModInfo(String modId) {
        throw new AssertionError();
    }
    @ExpectPlatform
    public static boolean isPackHidden(Pack pack) {
        throw new AssertionError();
    }
    @ExpectPlatform
    public static <T> RegisterListing<T> createLegacyRegister(String namespace, Registry<T> registry) {
        throw new AssertionError();
    }
    public enum Loader {
        FABRIC,FORGE,NEOFORGE;
        public boolean isForgeLike(){
            return this == FORGE || this == NEOFORGE;
        }
    }
    @ExpectPlatform
    public static Loader getLoader() {
        throw new AssertionError();
    }
    @ExpectPlatform
    public static boolean isClient() {
        throw new AssertionError();
    }
    @ExpectPlatform
    public static Fluid getBucketFluid(BucketItem item) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static<T extends CustomPacketPayload> void sendToPlayer(ServerPlayer serverPlayer, T packetHandler) {
        throw new AssertionError();
    }
    @ExpectPlatform
    public static<T extends CustomPacketPayload> void sendToServer(T packetHandler) {
        throw new AssertionError();
    }
    public static List<String> getMinecraftResourceAssort(){
        return new ArrayList<>(List.of("minecraft",getLoader().isForgeLike() ? "mod_resources" : "fabric","legacy:legacy_waters"));
    }
    public static List<String> getMinecraftClassicResourceAssort(){
        List<String> assort = getMinecraftResourceAssort();
        assort.add(assort.size() - 1,"programmer_art");
        if (getLoader().isForgeLike())  assort.add(assort.size() - 1,"legacy:programmer_art");
        return assort;
    }
}

