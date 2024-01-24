package wily.legacy;

import com.google.common.base.Suppliers;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkChannel;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.RegistrarManager;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.network.CommonPacket;
import wily.legacy.network.ServerDisplayInfoSync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class LegacyMinecraft
{

    public static final String MOD_ID = "legacy";
    public static final Supplier<String> VERSION =  Platform.getMod(MOD_ID)::getVersion;
    public static final String MC_VERSION = "1.20.4";
    public static final NetworkChannel NETWORK = NetworkChannel.create(new ResourceLocation(MOD_ID, "main"));
    public static final Supplier<RegistrarManager> REGISTRIES = Suppliers.memoize(() -> RegistrarManager.get(MOD_ID));

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final Object2IntMap<String> playerVisualIds = new Object2IntArrayMap<>();
    public static void init(){
        LegacySoundEvents.register();
        LegacyGameRules.init();
        registerCommonPacket(ServerDisplayInfoSync.class,ServerDisplayInfoSync::new);
        registerCommonPacket(ServerDisplayInfoSync.HostOptions.class,ServerDisplayInfoSync.HostOptions::new);
        LifecycleEvent.SERVER_STARTED.register(l-> playerVisualIds.clear());
        PlayerEvent.PLAYER_JOIN.register(p->{
            updateDisplayPlayersMap(p,true);
        });
        PlayerEvent.PLAYER_QUIT.register(p->{
            updateDisplayPlayersMap(p,false);
        });
        LifecycleEvent.SERVER_STOPPED.register(l-> playerVisualIds.clear());
    }
    public static void updateDisplayPlayersMap(ServerPlayer p, boolean addRemove){
        if (p.getServer() == null) return;
        if (addRemove) {
            int i = 0;
            while (playerVisualIds.containsValue(i))
                i++;
            playerVisualIds.put(p.getGameProfile().getName(),i);
        } else playerVisualIds.removeInt(p.getGameProfile().getName());
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
