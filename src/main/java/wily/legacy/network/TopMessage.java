package wily.legacy.network;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;

import java.util.function.Supplier;

public record TopMessage(Component message, int baseColor, int ticksOnScreen, boolean shadow, boolean fade,
                         boolean pulse) {
    public static TopMessage small;
    public static TopMessage medium;
    public static int smallTicks;
    public static int mediumTicks;

    public TopMessage(Component message, int baseColor, boolean shadow, boolean fade, boolean pulse) {
        this(message, baseColor, 40, shadow, fade, pulse);
    }

    public TopMessage(Component message, int baseColor, boolean shadow, boolean fade) {
        this(message, baseColor, shadow, fade, false);
    }

    public TopMessage(Component message, int baseColor, boolean shadow) {
        this(message, baseColor, shadow, false);
    }

    public TopMessage(Component message, int baseColor) {
        this(message, baseColor, true);
    }

    public static void tick() {
        if (small != null) {
            if (smallTicks < small.ticksOnScreen())
                smallTicks++;
            else {
                setSmall(null);
            }
        }

        if (medium != null) {
            if (mediumTicks < medium.ticksOnScreen())
                mediumTicks++;
            else {
                setMedium(null);
            }
        }
    }

    public static void setMedium(TopMessage topMessage) {
        medium = topMessage;
        mediumTicks = 0;
    }

    public static void setSmall(TopMessage topMessage) {
        small = topMessage;
        smallTicks = 0;
    }

    public enum SendType {
        SMALL, MEDIUM, CLEAR_SMALL, CLEAR_MEDIUM, CLEAR_ALL;

        public boolean isSmall() {
            return this == SMALL || this == CLEAR_SMALL || this == CLEAR_ALL;
        }

        public boolean isMedium() {
            return this == MEDIUM || this == CLEAR_MEDIUM || this == CLEAR_ALL;
        }

        public boolean clear() {
            return this.ordinal() > 1;
        }
    }

    public record Payload(SendType sendType, TopMessage topMessage) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<Payload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("send_top_message"), Payload::decode);

        public static Payload decode(CommonNetwork.PlayBuf buf) {
            SendType type = buf.get().readEnum(SendType.class);
            return new Payload(type, type.clear() ? null : new TopMessage(CommonNetwork.decodeComponent(buf), buf.get().readVarInt(), buf.get().readVarInt(), buf.get().readBoolean(), buf.get().readBoolean(), buf.get().readBoolean()));
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            buf.get().writeEnum(sendType);
            if (sendType().clear()) return;
            CommonNetwork.encodeComponent(buf, topMessage.message);
            buf.get().writeVarInt(topMessage.baseColor);
            buf.get().writeVarInt(topMessage.ticksOnScreen);
            buf.get().writeBoolean(topMessage.shadow);
            buf.get().writeBoolean(topMessage.fade);
            buf.get().writeBoolean(topMessage.pulse);
        }

        @Override
        public void apply(Context context) {
            if (sendType().isSmall()) setSmall(topMessage);
            if (sendType().isMedium()) setMedium(topMessage);
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }
}
