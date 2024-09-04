package wily.legacy.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public record ClientAnimalInLoveSyncPacket(int entityID, int inLove, int age) implements CommonNetwork.Packet {

    public static ClientAnimalInLoveSyncPacket of(Animal animal){
        return new ClientAnimalInLoveSyncPacket(animal.getId(),animal.getInLoveTime(),animal.getAge());
    }
    public static void sync(Animal entity){
        if (entity.level() instanceof ServerLevel l) {
            ClientAnimalInLoveSyncPacket packet = null;
            for (ServerPlayer player : l.getServer().getPlayerList().getPlayers()) {
                if (player.level() == entity.level() && player.distanceTo(entity) < l.getServer().getPlayerList().getViewDistance() * 16) CommonNetwork.sendToPlayer(player,packet == null ? (packet = ClientAnimalInLoveSyncPacket.of(entity)) : packet);
            }
        }
    }
    public ClientAnimalInLoveSyncPacket(RegistryFriendlyByteBuf buf){
        this(buf.readVarInt(),buf.readVarInt(),buf.readVarInt());
    }
    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entityID());
        buf.writeVarInt(inLove);
        buf.writeVarInt(age);
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        if (p.get().level().getEntity(entityID) instanceof Animal e) {
            e.setInLoveTime(inLove);
            e.setAge(age);
        }
    }
}
