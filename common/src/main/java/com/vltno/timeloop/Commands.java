package com.vltno.timeloop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.vltno.timeloop.commands.BaseCommands;
import com.vltno.timeloop.commands.SettingsCommands;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Commands {
    public static final Logger LOOP_COMMANDS_LOGGER = LoggerFactory.getLogger("LoopCommands");
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> commandBuilder = CommandManager.literal("loop");
        
        BaseCommands.register(commandBuilder);
        SettingsCommands.register(commandBuilder);
        
        dispatcher.register(commandBuilder);
    }

    public static int returnText(CommandContext<ServerCommandSource> context, String text) {
        context.getSource().sendMessage(Text.literal(text));
        return 1;
    }
}
