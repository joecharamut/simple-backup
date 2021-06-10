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

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import rocks.spaghetti.simplebackup.mixins.MinecraftServerSessionAccessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static rocks.spaghetti.simplebackup.Commands.broadcastMessage;
import static rocks.spaghetti.simplebackup.Commands.broadcastToOps;

public class BackupManager {
    public static final BackupManager INSTANCE = new BackupManager();

    private boolean backupInProgress;
    private boolean backupRequested;
    private long lastBackupTime;

    private BackupManager() {
        backupInProgress = false;
        backupRequested = false;
        lastBackupTime = System.currentTimeMillis();
    }

    public void tick(MinecraftServer server) {
        if (!ModConfig.automaticBackupsEnabled()) return;
        long currentTime = System.currentTimeMillis();

        if ((currentTime - lastBackupTime) > ModConfig.millisBetweenBackups()) {
            requestBackup();
        }

        if (backupInProgress) {
            backupRequested = false;
        }

        if (backupRequested) {
            invokeBackup(server);
        }
    }

    public void requestBackup() {
        backupRequested = true;
    }

    private void invokeBackup(MinecraftServer server) {
        if (backupInProgress) {
            throw new IllegalStateException("Backup already in progress");
        }
        backupInProgress = true;

        new BackupThread(server, ex -> {
            lastBackupTime = System.currentTimeMillis();
            backupInProgress = false;
            if (ex != null) {
                ex.printStackTrace();
            }
        }).start();
    }

    public boolean isBackupRunning() {
        return backupInProgress;
    }

    private static class BackupThread extends Thread {
        private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
        private final Consumer<Exception> callback;
        private final MinecraftServer server;

        public BackupThread(MinecraftServer server, Consumer<Exception> callback) {
            this.server = server;
            this.callback = callback;
            setName("Server Backup Thread");
        }

        @Override
        public void run() {
            try {
                doBackup();
            } catch (Exception e) {
                callback.accept(e);
            }
            callback.accept(null);
        }

        private void doBackup() throws IOException {
            long startTime = System.currentTimeMillis();
            broadcastMessage(server, "Beginning server backup, watch out for lag!");

            broadcastToOps(server, "Automatic saving is now disabled");
            server.getWorlds().forEach(world -> world.savingDisabled = true);

            broadcastToOps(server, "Saving the game (this may take a moment!)");
            server.getPlayerManager().saveAllPlayerData();
            server.save(false, true, true);
            broadcastToOps(server, "Saved the game");

            broadcastToOps(server, "Compressing backup");
            File worldDir = ((MinecraftServerSessionAccessor) server).getSession().getDirectory(WorldSavePath.ROOT).getParent().toFile();
            File backupDir = new File(server.getRunDirectory(), "backup");
            if (!backupDir.isDirectory() && !backupDir.mkdir()) {
                throw new IOException("Could not create backup directory");
            }

            File backupFile = new File(backupDir, String.format("%s_%s.zip",
                    worldDir.getName().replaceAll("\\s", "-"),
                    DATE_FORMAT.format(new Date())));

            ZipParameters params = new ZipParameters();
            params.setCompressionMethod(CompressionMethod.DEFLATE);
            params.setCompressionLevel(CompressionLevel.NORMAL);

            ZipFile zip = new ZipFile(backupFile);
            zip.addFolder(worldDir, params);

            broadcastToOps(server, "Done: " + backupFile.toString());

            int toKeep = ModConfig.getBackupsToKeep();
            if (toKeep > 0) {
                File[] files = backupDir.listFiles();
                int toDelete = files.length - toKeep;
                if (toDelete > 0) {
                    try (Stream<Path> stream = Files.walk(backupDir.toPath(), 1)) {
                        stream.filter(Files::isRegularFile)
                            .sorted(Comparator.comparingLong(path -> path.toFile().lastModified()))
                            .limit(toDelete)
                            .forEach(path -> {
                                try {
                                    broadcastToOps(server, "Deleting old backup: " + path.toString());
                                    Files.delete(path);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                    }
                }
            }

            broadcastToOps(server, "Automatic saving is now enabled");
            server.getWorlds().forEach(world -> world.savingDisabled = false);

            long endTime = System.currentTimeMillis();
            broadcastMessage(server, String.format("Backup finished in %s seconds", (endTime - startTime) / 1000));
        }
    }
}
