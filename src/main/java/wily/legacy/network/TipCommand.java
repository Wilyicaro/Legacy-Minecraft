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
import net.minecraft.world.item.ItemStack;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyTipManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TipCommand {
    public static final Map<String, Payload> CUSTOM_TIPS = new HashMap<>();

    public static final SuggestionProvider<CommandSourceStack> TIP_PROVIDER = (c,builder)-> {
        CUSTOM_TIPS.keySet().forEach(builder::suggest);
        return builder.buildFuture();
    };
    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
        commandDispatcher.register(Commands.literal("legacyTip").requires(commandSourceStack -> commandSourceStack.hasPermission(2)).
                then(Commands.literal("item").then(Commands.argument("item", ItemArgument.item(commandBuildContext)).then(Commands.literal("display").then(Commands.argument("targets", EntityArgument.players()).executes(commandContext -> sendTip(commandContext,new Payload().itemIcon(ItemArgument.getItem(commandContext,"item").createItemStack(1,true)))).then(Commands.argument("force",BoolArgumentType.bool()).executes(commandContext -> sendTip(commandContext,new Payload().itemIcon(ItemArgument.getItem(commandContext,"item").createItemStack(1,true)).force(BoolArgumentType.getBool(commandContext,"force"))))))))).
                then(Commands.literal("entity").then(Commands.argument("entity", ResourceArgument.resource(commandBuildContext, Registries.ENTITY_TYPE)).suggests(SuggestionProviders.SUMMONABLE_ENTITIES).then(Commands.literal("display").then(Commands.argument("targets", EntityArgument.players()).executes(commandContext -> sendTip(commandContext,new EntityPayload(ResourceArgument.getSummonableEntityType(commandContext, "entity").value(),false))).then(Commands.argument("force", BoolArgumentType.bool()).executes(commandContext -> sendTip(commandContext,new EntityPayload(ResourceArgument.getSummonableEntityType(commandContext, "entity").value(),BoolArgumentType.getBool(commandContext,"force"))))))))).
                then(Commands.literal("custom").then(Commands.argument("custom_tip",StringArgumentType.string()).suggests(TIP_PROVIDER).then(Commands.literal("add").executes(TipCommand::addCustomTip)).then(Commands.literal("remove").executes(c-> handleCustomTipPresence(c, CUSTOM_TIPS::remove,"legacy.commands.legacyTip.success.remove"))).then(Commands.literal("reset").executes(c-> handleCustomTipPresence(c, s-> CUSTOM_TIPS.put(s,new Payload()),"legacy.commands.legacyTip.success.reset"))).then(Commands.literal("modify").then(Commands.literal("item").then(Commands.argument("item",ItemArgument.item(commandBuildContext)).executes(c-> modifyCustomTip(c, t->t.itemIcon(ItemArgument.getItem(c,"item").createItemStack(1,false)))))).then(Commands.literal("tip").then(Commands.argument("tip", ComponentArgument.textComponent(/*? if >=1.20.5 {*/commandBuildContext/*?}*/)).executes(c-> modifyCustomTip(c, t->t.tip(ComponentArgument.getComponent(c,"tip")))))).then(Commands.literal("title").then(Commands.argument("title", ComponentArgument.textComponent(/*? if >=1.20.5 {*/commandBuildContext/*?}*/)).executes(c-> modifyCustomTip(c, t->t.title(ComponentArgument.getComponent(c,"title")))))).then(Commands.literal("time").then(Commands.argument("seconds", IntegerArgumentType.integer(1)).executes(c-> modifyCustomTip(c, t->t.disappearTime(IntegerArgumentType.getInteger(c,"seconds"))))))).then(Commands.literal("display").then(Commands.argument("targets", EntityArgument.players()).executes(commandContext -> sendTip(commandContext,CUSTOM_TIPS.get(StringArgumentType.getString(commandContext,"custom_tip")).force(false))).then(Commands.argument("force",BoolArgumentType.bool()).executes(commandContext -> sendTip(commandContext,CUSTOM_TIPS.get(StringArgumentType.getString(commandContext,"custom_tip")).force(BoolArgumentType.getBool(commandContext,"force"))))))))));
    }
    private static <T extends CommonNetwork.Payload> int sendTip(CommandContext<CommandSourceStack> context, T packet) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getOptionalPlayers(context, "targets");
        CommonNetwork.sendToPlayers(players, packet);
        return players.size();
    }
    private static int addCustomTip(CommandContext<CommandSourceStack> context) {
        return handleCustomTip(context, s-> context.getSource().sendFailure(Component.translatable("legacy.commands.legacyTip.invalidName", s)), s-> {
            CUSTOM_TIPS.put(s,new Payload());
            context.getSource().sendSuccess(()->Component.translatable("legacy.commands.legacyTip.success.add", s),true);
        });
    }
    private static int handleCustomTipPresence(CommandContext<CommandSourceStack> context,CommandConsumer<String> modifier, String successKey) {
        return handleCustomTip(context, s-> {
            modifier.accept(s);
            context.getSource().sendSuccess(() -> Component.translatable(successKey, s), true);
        }, s-> context.getSource().sendFailure(Component.translatable("legacy.commands.legacyTip.incorrectName", s)));
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
    private static int modifyCustomTip(CommandContext<CommandSourceStack> context, CommandConsumer<Payload> modifier){
        return handleCustomTip(context, s-> modifier.accept(CUSTOM_TIPS.get(s)), s-> context.getSource().sendFailure(Component.translatable("legacy.commands.legacyTip.incorrectName", s)));
    }
    private static int handleCustomTip(CommandContext<CommandSourceStack> context, CommandConsumer<String> presence, CommandConsumer<String> absence) {
        String tip = StringArgumentType.getString(context,"custom_tip");
        if (CUSTOM_TIPS.containsKey(tip)) presence.accept(tip);
        else absence.accept(tip);
        return 1;
    }

    public static class Payload implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<Payload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("send_tip"),Payload::decode);
        boolean force = false;
        Component title = Component.empty();
        Component tip = Component.empty();
        ItemStack itemIcon = ItemStack.EMPTY;
        int time = -1;
        public Payload title(Component title){
            this.title = title;
            return this;
        }
        public Payload tip(Component tip){
            this.tip = tip;
            return this;
        }
        public Payload itemIcon(ItemStack itemIcon){
            this.itemIcon = itemIcon;
            return this;
        }
        public Payload disappearTime(int time){
            this.time = time;
            return this;
        }
        public Payload force(boolean force){
            this.force = force;
            return this;
        }
        public static Payload decode(CommonNetwork.PlayBuf buf){
            return new Payload().title(CommonNetwork.decodeComponent(buf)).tip(CommonNetwork.decodeComponent(buf)).itemIcon(CommonNetwork.decodeItemStack(buf)).disappearTime(buf.get().readInt()).force(buf.get().readBoolean());
        }
        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            CommonNetwork.encodeComponent(buf,title);
            CommonNetwork.encodeComponent(buf,tip);
            CommonNetwork.encodeItemStack(buf,itemIcon);
            buf.get().writeInt(time);
            buf.get().writeBoolean(force);
        }

        @Override
        public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> player) {
            if (player.get().level().isClientSide) {
                if (force) LegacyTipManager.setActualTip(LegacyTipManager.getCustomTip(title, tip, itemIcon, time * 1000L));
                else LegacyTipManager.addTip(()->LegacyTipManager.getCustomTip(title, tip, itemIcon, time * 1000L));
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
        public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> player) {
            if (player.get().level().isClientSide) {
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
