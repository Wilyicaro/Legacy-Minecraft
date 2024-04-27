package wily.legacy;

import com.google.common.base.Suppliers;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkChannel;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.RegistrarManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wily.legacy.init.*;
import wily.legacy.network.*;
import wily.legacy.player.LegacyPlayerInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class Legacy4J
{
    public static LegacyServerProperties serverProperties;

    public static final String MOD_ID = "legacy";
    public static final Supplier<String> VERSION =  Platform.getMod(MOD_ID)::getVersion;
    public static final NetworkChannel NETWORK = NetworkChannel.create(new ResourceLocation(MOD_ID, "main"));
    public static final Supplier<RegistrarManager> REGISTRIES = Suppliers.memoize(() -> RegistrarManager.get(MOD_ID));

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void init(){
        LegacySoundEvents.register();
        LegacyMenuTypes.register();
        LegacyBlockItems.register();
        LegacyGameRules.init();
        registerCommonPacket(PlayerInfoSync.class, PlayerInfoSync::new);
        registerCommonPacket(PlayerInfoSync.HostOptions.class, PlayerInfoSync.HostOptions::new);
        registerCommonPacket(ServerOpenClientMenu.class,ServerOpenClientMenu::new);
        registerCommonPacket(ServerInventoryCraftPacket.class, ServerInventoryCraftPacket::new);
        registerCommonPacket(TipCommand.Packet.class, TipCommand.Packet::decode);
        registerCommonPacket(TipCommand.EntityPacket.class, TipCommand.EntityPacket::new);
        PlayerEvent.PLAYER_JOIN.register(Legacy4J::updatePlayerPosition);
        CommandRegistrationEvent.EVENT.register((s,c,e)->{
            TipCommand.register(s,c);
        });
    }
    public static boolean canRepair(ItemStack repairItem, ItemStack ingredient){
        return repairItem.is(ingredient.getItem()) && repairItem.getCount() == 1 && ingredient.getCount() == 1 && repairItem.getItem().canBeDepleted() && !repairItem.isEnchanted() && !ingredient.isEnchanted();
    }
    @FunctionalInterface
    public interface PackRegistry {
        void register(String path, String name, Component translation, Pack.Position position, boolean enabledByDefault);
        default void register(String path, String name, boolean enabledByDefault){
            register(path,name,Component.translatable(MOD_ID + ".builtin." + name), Pack.Position.TOP,enabledByDefault);
        }
        default void register(String pathName, boolean enabledByDefault){
            register("resourcepacks/"+pathName,pathName,enabledByDefault);
        }
    }
    public static void registerBuiltInPacks(PackRegistry registry){
        registry.register("legacy_waters",true);
        registry.register("console_aspects",false);
        if (Platform.isForgeLike()) registry.register("programmer_art","programmer_art", Component.translatable("legacy.builtin.console_programmer"), Pack.Position.TOP,false);
    }
    public static void updatePlayerPosition(ServerPlayer p){
        if (p.getServer() == null) return;
        int pos = 0;
        boolean b = true;
        main : while (b) {
            b = false;
            for (ServerPlayer player : p.server.getPlayerList().getPlayers())
                if (player != p && ((LegacyPlayerInfo)player).getPosition() == pos){
                    pos++;
                    b = true;
                    continue main;
                }
        }
        Legacy4J.LOGGER.warn(pos);
        ((LegacyPlayerInfo)p).setPosition(pos);

    }
    public static <T extends CommonPacket> void  registerCommonPacket(Class<T> packet, Function<FriendlyByteBuf,T> decode){
        NETWORK.register(packet,CommonPacket::encode,decode,CommonPacket::apply);
    }
    public static void copySaveToDirectory(InputStream stream, File directory){
        try (ZipInputStream inputStream = new ZipInputStream(stream))
        {
            ZipEntry zipEntry = inputStream.getNextEntry();
            byte[] buffer = new byte[1024];
            while (zipEntry != null)
            {
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
                zipEntry = inputStream.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
