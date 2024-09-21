package wily.legacy.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public record TopMessage(Component message, int baseColor) {
    public static TopMessage small;
    public static TopMessage medium;

    public TopMessage(Component message) {
        this(message,0xFFFFFF);
    }

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

    public record Packet(SendType type, TopMessage message) implements CommonNetwork.Packet {
        public Packet(SendType type, Component component){
            this(type,new TopMessage(component));
        }
        public static Packet decode(FriendlyByteBuf buf){
            SendType type = buf.readEnum(SendType.class);
            return new Packet(type,type.clear() ? null : new TopMessage(buf.readComponent(),buf.readVarInt()));
        }
        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeEnum(type);
            if (type().clear()) return;
            buf.writeComponent(message.message());
            buf.writeInt(message.baseColor());
        }

        @Override
        public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> player) {
            if (type().isSmall()) small = message;
            if (type().isMedium()) medium = message;
        }
    }
}
