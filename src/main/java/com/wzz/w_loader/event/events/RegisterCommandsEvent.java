package com.wzz.w_loader.event.events;


import com.mojang.brigadier.CommandDispatcher;
import com.wzz.w_loader.event.Event;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class RegisterCommandsEvent extends Event {
    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final Commands.CommandSelection environment;
    private final CommandBuildContext context;

    public RegisterCommandsEvent(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection environment, CommandBuildContext context) {
        this.dispatcher = dispatcher;
        this.environment = environment;
        this.context = context;
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return this.dispatcher;
    }

    public Commands.CommandSelection getCommandSelection() {
        return this.environment;
    }

    public CommandBuildContext getBuildContext() {
        return this.context;
    }
}