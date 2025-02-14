package com.vltno.timeloop;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.StringIdentifiable;

public class LoopTypesArgumentType extends EnumArgumentType<LoopTypes> {
    protected LoopTypesArgumentType() {
        super(StringIdentifiable.createCodec(LoopTypes::values), LoopTypes::values);
    }

    @Override
    public LoopTypes parse(StringReader reader) throws CommandSyntaxException {
        // Example parsing for a custom `LoopType` object
        String argument = reader.readString();
        return LoopTypes.fromString(argument);  // Map argument to your custom type
    }

    public static LoopTypesArgumentType loopType() {
        return new LoopTypesArgumentType();
    }

    public static LoopTypes getLoopType(CommandContext<ServerCommandSource> context, String name) {
        return context.getArgument(name, LoopTypes.class);
    }
}