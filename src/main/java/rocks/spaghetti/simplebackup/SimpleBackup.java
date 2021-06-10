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

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class SimpleBackup implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);
        ModConfig.reloadConfig();

        CommandRegistrationCallback.EVENT.register(Commands::register);
        ServerTickEvents.END_SERVER_TICK.register(BackupManager.INSTANCE::tick);
    }
}
