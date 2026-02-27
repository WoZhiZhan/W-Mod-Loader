package com.wzz.w_loader.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public class WCommandBuilder {
    private final LiteralArgumentBuilder<CommandSourceStack> root;

    private WCommandBuilder(String name) {
        this.root = Commands.literal(name);
    }

    public static WCommandBuilder create(String name) {
        return new WCommandBuilder(name);
    }

    public WCommandBuilder literal(String name, Consumer<WCommandBuilder> sub) {
        WCommandBuilder subBuilder = new WCommandBuilder(name);
        sub.accept(subBuilder);
        root.then(subBuilder.build());
        return this;
    }

    public <T> WCommandBuilder argument(String name, ArgumentType<T> type, Command<CommandSourceStack> action) {
        root.then(Commands.argument(name, type).executes(action));
        return this;
    }

    public WCommandBuilder executes(Command<CommandSourceStack> action) {
        root.executes(action);
        return this;
    }

    public WCommandBuilder requires(int level) {
        root.requires(source -> {
            try {
                Field field = CommandSourceStack.class.getDeclaredField("source");
                field.setAccessible(true);
                CommandSource source1 = (CommandSource) field.get(source);
                if (source1 == source.getServer()) {
                    return true;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                boolean isOp = source.getServer().getPlayerList().isOp(player.nameAndId());
                if (isOp) return true;
                return level <= 0;
            }
            return false;
        });
        return this;
    }
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return root;
    }
}