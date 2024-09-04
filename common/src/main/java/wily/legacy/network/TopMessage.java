package wily.legacy.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public class TopMessage {
    public static Component small;
    public static Component medium;

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

    public record Packet(SendType type, Component message) implements CommonNetwork.Packet {

        public static Packet create(RegistryFriendlyByteBuf buf){
            SendType type = buf.readEnum(SendType.class);
            return new Packet(type,type.clear() ? null : ComponentSerialization.STREAM_CODEC.decode(buf));
        }
        @Override
        public void encode(RegistryFriendlyByteBuf buf) {
            buf.writeEnum(type);
            if (type().clear()) return;
            ComponentSerialization.STREAM_CODEC.encode(buf,message);
        }

        @Override
        public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> player) {
            if (type().isSmall()) small = message;
            if (type().isMedium()) medium = message;
        }
    }
}
