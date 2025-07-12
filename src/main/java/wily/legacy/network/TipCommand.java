package wily.legacy.network;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.config.LegacyWorldOptions;
import wily.legacy.util.LegacyTipBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TipCommand {

    public static final SuggestionProvider<CommandSourceStack> TIP_PROVIDER = (c,builder)-> {
        LegacyWorldOptions.customTips.get().keySet().forEach(builder::suggest);
        return builder.buildFuture();
    };
    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
        commandDispatcher.register(Commands.literal("legacyTip").requires(commandSourceStack -> commandSourceStack.hasPermission(2)).
                then(Commands.literal("item").then(Commands.argument("item", ItemArgument.item(commandBuildContext)).then(Commands.literal("display").then(Commands.argument("targets", EntityArgument.players()).executes(commandContext -> sendTip(commandContext, new Payload(new LegacyTipBuilder().itemIcon(ItemArgument.getItem(commandContext,"item").createItemStack(1,true))))).then(Commands.argument("force",BoolArgumentType.bool()).executes(commandContext -> sendTip(commandContext, new Payload(new LegacyTipBuilder().itemIcon(ItemArgument.getItem(commandContext,"item").createItemStack(1,true)),BoolArgumentType.getBool(commandContext,"force"))))))))).
                then(Commands.literal("entity").then(Commands.argument("entity", ResourceArgument.resource(commandBuildContext, Registries.ENTITY_TYPE)).suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES)).then(Commands.literal("display").then(Commands.argument("targets", EntityArgument.players()).executes(commandContext -> sendTip(commandContext,new EntityPayload(ResourceArgument.getSummonableEntityType(commandContext, "entity").value(),false))).then(Commands.argument("force", BoolArgumentType.bool()).executes(commandContext -> sendTip(commandContext,new EntityPayload(ResourceArgument.getSummonableEntityType(commandContext, "entity").value(),BoolArgumentType.getBool(commandContext,"force"))))))))).
                then(Commands.literal("custom").then(Commands.argument("custom_tip",StringArgumentType.string()).suggests(TIP_PROVIDER).then(Commands.literal("add").executes(TipCommand::addCustomTip)).then(Commands.literal("remove").executes(c-> handleCustomTipPresence(c, LegacyWorldOptions.customTips.get()::remove,"legacy.commands.legacyTip.success.remove"))).then(Commands.literal("reset").executes(c-> handleCustomTipPresence(c, s-> LegacyWorldOptions.customTips.get().put(s,new LegacyTipBuilder()),"legacy.commands.legacyTip.success.reset"))).then(Commands.literal("modify").then(Commands.literal("item").then(Commands.argument("item",ItemArgument.item(commandBuildContext)).executes(c-> modifyCustomTip(c, t->t.itemIcon(ItemArgument.getItem(c,"item").createItemStack(1,false)))))).then(Commands.literal("tip").then(Commands.argument("tip", ComponentArgument.textComponent(/*? if >=1.20.5 {*/commandBuildContext/*?}*/)).executes(c-> modifyCustomTip(c, t->t.tip(ComponentArgument./*? if <1.21.5 {*//*getComponent*//*?} else {*/getRawComponent/*?}*/(c,"tip")))))).then(Commands.literal("title").then(Commands.argument("title", ComponentArgument.textComponent(/*? if >=1.20.5 {*/commandBuildContext/*?}*/)).executes(c-> modifyCustomTip(c, t->t.title(ComponentArgument./*? if <1.21.5 {*//*getComponent*//*?} else {*/getRawComponent/*?}*/(c,"title")))))).then(Commands.literal("time").then(Commands.argument("seconds", IntegerArgumentType.integer(1)).executes(c-> modifyCustomTip(c, t->t.disappearTime(IntegerArgumentType.getInteger(c,"seconds"))))))).then(Commands.literal("display").then(Commands.argument("targets", EntityArgument.players()).executes(commandContext -> handleCustomTipPresence(commandContext, s-> sendTip(commandContext,new Payload(LegacyWorldOptions.customTips.get().get(s))))).then(Commands.argument("force",BoolArgumentType.bool()).executes(commandContext -> handleCustomTipPresence(commandContext, s-> sendTip(commandContext,new Payload(LegacyWorldOptions.customTips.get().get(s), BoolArgumentType.getBool(commandContext,"force")))))))))));
    }
    private static <T extends CommonNetwork.Payload> int sendTip(CommandContext<CommandSourceStack> context, T packet) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getOptionalPlayers(context, "targets");
        CommonNetwork.sendToPlayers(players, packet);
        return players.size();
    }
    private static int addCustomTip(CommandContext<CommandSourceStack> context) {
        return handleCustomTip(context, s-> context.getSource().sendFailure(Component.translatable("legacy.commands.legacyTip.invalidName", s)), s-> {
            LegacyWorldOptions.customTips.get().put(s,new LegacyTipBuilder());
            LegacyWorldOptions.customTips.save();
            context.getSource().sendSuccess(()->Component.translatable("legacy.commands.legacyTip.success.add", s),true);
        });
    }
    private static int handleCustomTipPresence(CommandContext<CommandSourceStack> context, CommandConsumer<String> consumer) {
        return handleCustomTip(context, consumer, s-> context.getSource().sendFailure(Component.translatable("legacy.commands.legacyTip.incorrectName", s)));
    }

    private static int handleCustomTipPresence(CommandContext<CommandSourceStack> context, CommandConsumer<String> modifier, String successKey) {
        return handleCustomTipPresence(context, s->{
            modifier.accept(s);
            LegacyWorldOptions.customTips.save();
            context.getSource().sendSuccess(() -> Component.translatable(successKey, s), true);
        });
    }
    interface CommandConsumer<T> extends Consumer<T> {
        @Override
        default void accept(T t) {
            try {
                acceptExceptionally(t);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        void acceptExceptionally(T t) throws CommandSyntaxException;
    }
    private static int modifyCustomTip(CommandContext<CommandSourceStack> context, CommandConsumer<LegacyTipBuilder> modifier){
        return handleCustomTip(context, s-> {
            modifier.accept(LegacyWorldOptions.customTips.get().get(s));
            LegacyWorldOptions.customTips.save();
        }, s-> context.getSource().sendFailure(Component.translatable("legacy.commands.legacyTip.incorrectName", s)));
    }
    private static int handleCustomTip(CommandContext<CommandSourceStack> context, CommandConsumer<String> presence, CommandConsumer<String> absence) {
        String tip = StringArgumentType.getString(context,"custom_tip");
        if (LegacyWorldOptions.customTips.get().containsKey(tip)) presence.accept(tip);
        else absence.accept(tip);
        return 1;
    }

    public record Payload(LegacyTipBuilder builder, boolean force) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<Payload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("send_tip"),Payload::decode);

        public Payload(LegacyTipBuilder builder) {
            this(builder, false);
        }

        public static Payload decode(CommonNetwork.PlayBuf buf){
            return new Payload(LegacyTipBuilder.decode(buf), buf.get().readBoolean());
        }
        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            builder.encode(buf);
            buf.get().writeBoolean(force);
        }

        @Override
        public void apply(Context context) {
            if (context.player().level().isClientSide) {
                if (force) LegacyTipManager.setTip(LegacyTipManager.getTip(builder.getItem(), builder));
                else LegacyTipManager.addTip(LegacyTipManager.getTip(builder.getItem(), builder));
            }
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }
    public record EntityPayload(EntityType<?> entityType, boolean force) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<EntityPayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("send_entity_tip"),EntityPayload::new);

        public EntityPayload(CommonNetwork.PlayBuf buf){
            this(FactoryAPIPlatform.getRegistryValue(buf.get().readResourceLocation(),BuiltInRegistries.ENTITY_TYPE), buf.get().readBoolean());
        }
        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            buf.get().writeResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
            buf.get().writeBoolean(force);
        }
        @Override
        public void apply(Context context) {
            if (context.player().level().isClientSide) {
                if (force) LegacyTipManager.setTip(LegacyTipManager.getTip(entityType));
                else LegacyTipManager.addTip(entityType);
            }
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }
    }
}
