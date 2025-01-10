package wily.legacy.network;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Dynamic;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.nbt.NbtOps;
import wily.legacy.config.LegacyConfig;

public class Legacy4JCommand {

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandBuildContext) {
        commandDispatcher.register(Commands.literal("legacy4j").requires(commandSourceStack -> commandSourceStack.hasPermission(2)).then(Commands.literal("reload_config").then(Commands.literal("common").executes(c->{
            LegacyConfig.COMMON_STORAGE.load();
            return 1;
        }))).then(Commands.literal("set_config").then(Commands.literal("common").then(Commands.argument("value", CompoundTagArgument.compoundTag()).executes(c->{
            LegacyConfig.COMMON_STORAGE.decodeConfigs(new Dynamic<>(NbtOps.INSTANCE, CompoundTagArgument.getCompoundTag(c, "value")), LegacyConfig::sync);
            return 1;
        })))).then(Commands.literal("save_config").then(Commands.literal("common").executes(c->{
            LegacyConfig.COMMON_STORAGE.save();
            return 1;
        }))));

    }
}
