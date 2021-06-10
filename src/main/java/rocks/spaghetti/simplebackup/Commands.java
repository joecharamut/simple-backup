/*
 * Copyright (C) 2021 Joseph Charamut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package rocks.spaghetti.simplebackup;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.world.GameRules;

import static net.minecraft.server.command.CommandManager.*;

public class Commands {
    private Commands() {
        throw new IllegalStateException("Utility Class");
    }

    public static void broadcastToOps(MinecraftServer server, String message) {
        Text text = new LiteralText(String.format("[simplebackup] %s", message)).formatted(Formatting.GRAY, Formatting.ITALIC);
        if (server.getGameRules().getBoolean(GameRules.SEND_COMMAND_FEEDBACK)) {
            server.getPlayerManager().getPlayerList().forEach(player -> {
                if (server.getPlayerManager().isOperator(player.getGameProfile())) {
                    player.sendSystemMessage(text, Util.NIL_UUID);
                }
            });
        }

        if (server.getGameRules().getBoolean(GameRules.LOG_ADMIN_COMMANDS)) {
            server.sendSystemMessage(text, Util.NIL_UUID);
        }
    }

    public static void broadcastMessage(MinecraftServer server, String message) {
        server.getPlayerManager().getPlayerList().forEach(pl -> pl.sendMessage(
                new LiteralText(message).formatted(Formatting.GOLD),
                false));
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicatedServer) {
        // /backup
        dispatcher.register(literal("backup")
                .requires(context -> {
                    try {
                        return
                                   context.hasPermissionLevel(4)
                                || context.getMinecraftServer().isSinglePlayer();
                    } catch (Exception e) {
                        return true;
                    }
                })

                // /backup start
                .then(
                        literal("start")
                        .executes(ctx -> {
                            if (BackupManager.INSTANCE.isBackupRunning()) {
                                throw new CommandException(new LiteralText("A backup is already in progress"));
                            }

                            BackupManager.INSTANCE.requestBackup();
                            return Command.SINGLE_SUCCESS;
                        })
                )

                // /backup reload
                .then(
                        literal("reload")
                        .executes(ctx -> {
                            ModConfig.reloadConfig();

                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
    }
}
