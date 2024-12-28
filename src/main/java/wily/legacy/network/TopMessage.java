package wily.legacy.network;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;

import java.util.function.Supplier;

public record TopMessage(Component message, int baseColor) {
    public static TopMessage small;
    public static TopMessage medium;

    public enum SendType{
        SMALL,MEDIUM,CLEAR_SMALL,CLEAR_MEDIUM,CLEAR_ALL;
        public boolean isSmall(){
            return this == SMALL || this == CLEAR_SMALL || this == CLEAR_ALL;
        }
        public boolean isMedium(){
            return this == MEDIUM || this == CLEAR_MEDIUM || this == CLEAR_ALL;
        }
        public boolean clear(){
            return this.ordinal() > 1;
        }
    }

    public record Payload(SendType sendType, TopMessage topMessage) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<Payload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("send_top_message"), Payload::decode);

        public static Payload decode(CommonNetwork.PlayBuf buf){
            SendType type = buf.get().readEnum(SendType.class);
            return new Payload(type,type.clear() ? null : new TopMessage(CommonNetwork.decodeComponent(buf), buf.get().readVarInt()));
        }
        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            buf.get().writeEnum(sendType);
            if (sendType().clear()) return;
            CommonNetwork.encodeComponent(buf, topMessage.message);
            buf.get().writeVarInt(topMessage.baseColor);
        }

        @Override
        public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> player) {
            if (sendType().isSmall()) small = topMessage;
            if (sendType().isMedium()) medium = topMessage;
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }
}
