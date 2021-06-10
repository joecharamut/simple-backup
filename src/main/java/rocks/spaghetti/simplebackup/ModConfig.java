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
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "simplebackup")
public class ModConfig implements ConfigData {
    @ConfigEntry.Gui.Excluded
    private static ModConfig CONFIG;

    @Comment("(boolean) Whether automatic backups are enabled or not")
    public boolean automaticBackups = true;

    @Comment("(int) How many minutes between each automatic backup")
    public int minutesBetweenBackups = 60;

    @Comment("(int) Number of backups to keep, set to -1 to disable automatic deletion")
    public int backupsToKeep = -1;

    public static void reloadConfig() {
        CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }

    public static long millisBetweenBackups() {
        return CONFIG.minutesBetweenBackups * 60L * 1000L;
    }

    public static boolean automaticBackupsEnabled() {
        return CONFIG.automaticBackups;
    }

    public static int getBackupsToKeep() {
        return CONFIG.backupsToKeep;
    }
}
