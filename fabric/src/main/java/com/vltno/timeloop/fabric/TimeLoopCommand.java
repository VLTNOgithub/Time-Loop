package com.vltno.timeloop.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.vltno.timeloop.commands.BaseCommands;
import com.vltno.timeloop.commands.SettingsCommands;
import com.vltno.timeloop.commands.TogglesCommands;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class TimeLoopCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> commandBuilder = CommandManager.literal("loop");
        
        commandBuilder.then(BaseCommands.getArgumentBuilder(commandBuilder));
        commandBuilder.then(SettingsCommands.getArgumentBuilder()).requires(source -> source.hasPermissionLevel(2));
        commandBuilder.then(TogglesCommands.getArgumentBuilder()).requires(source -> source.hasPermissionLevel(2));
        
        dispatcher.register(commandBuilder);
    }
}
