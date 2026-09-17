package wily.legacy.network;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.effect.MobEffect;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;

import java.util.Collection;

public record TopMessage(Component message, int baseColor, int ticksOnScreen, boolean shadow, boolean fade,
                         boolean pulse) {
    public static TopMessage small;
    public static TopMessage medium;
    public static int smallTicks;
    public static int mediumTicks;

    public TopMessage(Component message, int baseColor, int ticksOnScreen, boolean shadow, boolean fade) {
        this(message, baseColor, ticksOnScreen, shadow, fade, false);
    }

    public TopMessage(Component message, int baseColor, int ticksOnScreen, boolean shadow) {
        this(message, baseColor, ticksOnScreen, shadow, false);
    }

    public TopMessage(Component message, int baseColor, int ticksOnScreen) {
        this(message, baseColor, ticksOnScreen, true, false);
    }

    public TopMessage(Component message, int baseColor) {
        this(message, baseColor, 40);
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

    public static void registerCommand(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext ctx, Commands.CommandSelection environment) {
        commandDispatcher.register(Commands.literal("displayTopMessage").requires(commandSourceStack -> commandSourceStack.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).then(Commands.argument("targets", EntityArgument.players()).
                then(setupTopMessageArgument(ctx, "small", SendType.SMALL)).
                then(setupTopMessageArgument(ctx, "medium", SendType.MEDIUM)).
                then(setupClearTopMessageArgument("clear_small", SendType.CLEAR_SMALL)).
                then(setupClearTopMessageArgument("clear_medium", SendType.CLEAR_MEDIUM)).
                then(setupClearTopMessageArgument("clear_all", SendType.CLEAR_ALL))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> setupTopMessageArgument(CommandBuildContext ctx, String id, SendType sendType) {
        return Commands.literal(id).
                then(Commands.argument("message", ComponentArgument.textComponent(ctx)).executes((c) -> sendTopMessage(EntityArgument.getPlayers(c, "targets"), sendType, new TopMessage(ComponentArgument.getRawComponent(c, "message"), 0xFFFFFFFF))).
                then(Commands.argument("ticksOnScreen", IntegerArgumentType.integer(1)).executes((c) -> sendTopMessage(EntityArgument.getPlayers(c, "targets"), sendType, new TopMessage(ComponentArgument.getRawComponent(c, "message"), 0xFFFFFFFF, IntegerArgumentType.getInteger(c, "ticksOnScreen")))).
                then(Commands.argument("shadow", BoolArgumentType.bool()).executes((c) -> sendTopMessage(EntityArgument.getPlayers(c, "targets"), sendType, new TopMessage(ComponentArgument.getRawComponent(c, "message"), 0xFFFFFFFF, IntegerArgumentType.getInteger(c, "ticksOnScreen"), BoolArgumentType.getBool(c, "shadow")))).
                then(Commands.argument("fade", BoolArgumentType.bool()).executes((c) -> sendTopMessage(EntityArgument.getPlayers(c, "targets"), sendType, new TopMessage(ComponentArgument.getRawComponent(c, "message"), 0xFFFFFFFF, IntegerArgumentType.getInteger(c, "ticksOnScreen"), BoolArgumentType.getBool(c, "shadow"), BoolArgumentType.getBool(c, "fade")))).
                then(Commands.argument("pulse", BoolArgumentType.bool()).executes((c) -> sendTopMessage(EntityArgument.getPlayers(c, "targets"), sendType, new TopMessage(ComponentArgument.getRawComponent(c, "message"), 0xFFFFFFFF, IntegerArgumentType.getInteger(c, "ticksOnScreen"), BoolArgumentType.getBool(c, "shadow"), BoolArgumentType.getBool(c, "fade"), BoolArgumentType.getBool(c, "pulse")))))))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> setupClearTopMessageArgument(String id, SendType sendType) {
        return Commands.literal(id).executes((c) -> sendTopMessage(EntityArgument.getPlayers(c, "targets"), sendType, null));
    }

    public static int sendTopMessage(Collection<ServerPlayer> targets, SendType sendType, TopMessage topMessage) {
        CommonNetwork.sendToPlayers(targets, new Payload(sendType, topMessage));
        return targets.size();
    }
}
