package wily.legacy;

import com.google.common.base.Suppliers;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.RegistrarManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wily.legacy.init.*;
import wily.legacy.network.*;
import wily.legacy.player.LegacyPlayerInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class Legacy4J
{
    public static LegacyServerProperties serverProperties;

    public static final String MOD_ID = "legacy";
    public static final Supplier<String> VERSION =  ()-> Platform.getMod(MOD_ID).getVersion();
    public static final Supplier<RegistrarManager> REGISTRIES = Suppliers.memoize(() -> RegistrarManager.get(MOD_ID));

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void init(){
        LegacySoundEvents.register();
        LegacyMenuTypes.register();
        LegacyRegistries.register();
        LegacyGameRules.init();
        CommonNetworkManager.init();
        CommonNetworkManager.register(PlayerInfoSync.class, PlayerInfoSync::new);
        CommonNetworkManager.register(PlayerInfoSync.HostOptions.class, PlayerInfoSync.HostOptions::new);
        CommonNetworkManager.register(ServerOpenClientMenuPacket.class, ServerOpenClientMenuPacket::new);
        CommonNetworkManager.register(ServerInventoryCraftPacket.class, ServerInventoryCraftPacket::new);
        CommonNetworkManager.register(TipCommand.Packet.class, TipCommand.Packet::decode);
        CommonNetworkManager.register(TipCommand.EntityPacket.class, TipCommand.EntityPacket::new);
        CommonNetworkManager.register(ClientAdvancementsPacket.class, ClientAdvancementsPacket::new);


        PlayerEvent.PLAYER_JOIN.register(Legacy4J::onServerPlayerJoin);
        CommandRegistrationEvent.EVENT.register((s,c,e)->{
            TipCommand.register(s,c);
        });
    }
    public static boolean canRepair(ItemStack repairItem, ItemStack ingredient){
        return repairItem.is(ingredient.getItem()) && repairItem.getCount() == 1 && ingredient.getCount() == 1 && repairItem.getItem().components().has(DataComponents.DAMAGE) && !repairItem.isEnchanted() && !ingredient.isEnchanted();
    }
    public static ItemStack dyeItem(ItemStack itemStack, int color) {
        List<Integer> colors = new ArrayList<>();
        DyedItemColor dyedItemColor = itemStack.get(DataComponents.DYED_COLOR);
        boolean bl = dyedItemColor == null || dyedItemColor.showInTooltip();
        if (dyedItemColor != null) colors.add(color);
        colors.add(color);
        itemStack.set(DataComponents.DYED_COLOR, new DyedItemColor(mixColors(colors.iterator()), bl));
        return itemStack;
    }
    public static int mixColors(Iterator<Integer> colors){
        int n;
        float h;
        int[] is = new int[3];
        int i = 0;
        int j = 0;

        for (Iterator<Integer> it = colors; it.hasNext(); ) {
            Integer color = it.next();
            float f = (float)(color >> 16 & 0xFF) / 255.0f;
            float g = (float)(color >> 8 & 0xFF) / 255.0f;
            h = (float)(color & 0xFF) / 255.0f;
            i += (int)(Math.max(f, Math.max(g, h)) * 255.0f);
            is[0] = is[0] + (int)(f * 255.0f);
            is[1] = is[1] + (int)(g * 255.0f);
            is[2] = is[2] + (int)(h * 255.0f);
            ++j;
        }
        int k = is[0] / j;
        int o = is[1] / j;
        int p = is[2] / j;
        h = (float)i / (float)j;
        float q = Math.max(k, Math.max(o, p));
        k = (int)((float)k * h / q);
        o = (int)((float)o * h / q);
        p = (int)((float)p * h / q);
        n = k;
        n = (n << 8) + o;
        n = (n << 8) + p;
        return n;
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
    public static void onServerPlayerJoin(ServerPlayer p){
        if (p.getServer() == null) return;
        CommonNetworkManager.sendToPlayer(p, new ClientAdvancementsPacket(List.copyOf(p.getServer().getAdvancements().getAllAdvancements())));
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
        ((LegacyPlayerInfo)p).setPosition(pos);

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
