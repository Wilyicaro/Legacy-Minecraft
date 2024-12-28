package wily.legacy.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;

import java.util.function.Supplier;

public record ClientAnimalInLoveSyncPayload(int entityID, int inLove, int age) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<ClientAnimalInLoveSyncPayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("client_in_love"), ClientAnimalInLoveSyncPayload::new);

    public static ClientAnimalInLoveSyncPayload of(Animal animal){
        return new ClientAnimalInLoveSyncPayload(animal.getId(),animal.getInLoveTime(),animal.getAge());
    }
    public static void sync(Animal entity){
        if (entity.level() instanceof ServerLevel l) {
            ClientAnimalInLoveSyncPayload packet = null;
            for (ServerPlayer player : l.getServer().getPlayerList().getPlayers()) {
                if (player.level() == entity.level() && player.distanceTo(entity) < l.getServer().getPlayerList().getViewDistance() * 16) CommonNetwork.sendToPlayer(player,packet == null ? (packet = ClientAnimalInLoveSyncPayload.of(entity)) : packet);
            }
        }
    }
    public ClientAnimalInLoveSyncPayload(CommonNetwork.PlayBuf buf){
        this(buf.get().readVarInt(),buf.get().readVarInt(),buf.get().readVarInt());
    }
    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeVarInt(entityID());
        buf.get().writeVarInt(inLove);
        buf.get().writeVarInt(age);
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        if (p.get().level().getEntity(entityID) instanceof Animal e) {
            e.setInLoveTime(inLove);
            e.setAge(age);
        }
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }
}
