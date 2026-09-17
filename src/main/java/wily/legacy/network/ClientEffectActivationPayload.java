package wily.legacy.network;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.effect.MobEffect;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyActivationAnim;
import wily.legacy.config.LegacyWorldOptions;
import wily.legacy.util.LegacyTipBuilder;

import java.util.Collection;

public record ClientEffectActivationPayload(/*? if <1.20.5 {*//*MobEffect*//*?} else {*/
                                            Holder<MobEffect>/*?}*/ effect) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<ClientEffectActivationPayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("client_effect_activation"), ClientEffectActivationPayload::new);

    public ClientEffectActivationPayload(CommonNetwork.PlayBuf buf) {
        this(/*? if <1.20.5 {*//*BuiltInRegistries.MOB_EFFECT.getHolder(buf.get().readVarInt()).get().value()*//*?} else {*/MobEffect.STREAM_CODEC.decode(buf.get())/*?}*/);
    }

    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        //? if <1.20.5 {
        /*buf.get().writeVarInt(BuiltInRegistries.MOB_EFFECT.getId(effect));
         *///?} else {
        MobEffect.STREAM_CODEC.encode(buf.get(), effect);
        //?}
    }

    @Override
    public void apply(Context context) {
        if (FactoryAPIPlatform.isClient()) LegacyActivationAnim.displayEffect(effect);
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }

    public static void registerCommand(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
        commandDispatcher.register(Commands.literal("displayEffectActivation").requires(commandSourceStack -> commandSourceStack.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("effect", ResourceArgument.resource(commandBuildContext, Registries.MOB_EFFECT)).executes((c) -> sendDisplayEffectActivation(EntityArgument.getPlayers(c, "targets"), ResourceArgument.getResource(c, "effect", Registries.MOB_EFFECT))))));
    }

    public static int sendDisplayEffectActivation(Collection<ServerPlayer> targets, Holder<MobEffect> holder) {
        CommonNetwork.sendToPlayers(targets, new ClientEffectActivationPayload(holder));
        return targets.size();
    }
}
