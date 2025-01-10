package wily.legacy.network;

import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.config.LegacyConfig;

import java.util.Map;
import java.util.function.Supplier;

public record CommonConfigSyncPayload(CompoundTag configTag) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<CommonConfigSyncPayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("common_config_sync_s2c"),CommonConfigSyncPayload::new);

    public static CommonConfigSyncPayload of(LegacyConfig.StorageHandler handler){
        return new CommonConfigSyncPayload((CompoundTag)handler.encodeConfigs(NbtOps.INSTANCE));
    }
    public static CommonConfigSyncPayload of(LegacyConfig<?> config){
        return new CommonConfigSyncPayload((CompoundTag)LegacyConfig.encodeConfigs(Map.of(config.getKey(), config), NbtOps.INSTANCE));
    }

    public CommonConfigSyncPayload(CommonNetwork.PlayBuf playBuf){
        this(playBuf.get().readNbt());
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor secureExecutor, Supplier<Player> supplier) {
        LegacyConfig.COMMON_STORAGE.decodeConfigs(new Dynamic<>(NbtOps.INSTANCE, configTag), c->{});
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }

    @Override
    public void encode(CommonNetwork.PlayBuf playBuf) {
        playBuf.get().writeNbt(configTag);
    }

}
