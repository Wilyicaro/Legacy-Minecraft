package wily.legacy;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import wily.legacy.util.RegisterListing;
import wily.legacy.util.ModInfo;

import java.nio.file.Path;
import java.util.Collection;

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
    @ExpectPlatform
    public static boolean isForgeLike() {
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
}

