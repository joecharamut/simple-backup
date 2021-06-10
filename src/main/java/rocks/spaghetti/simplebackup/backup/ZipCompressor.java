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

package rocks.spaghetti.simplebackup.backup;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;

import java.io.File;
import java.io.IOException;

public class ZipCompressor {
    private ZipCompressor() {
        throw new IllegalStateException("Utility Class");
    }

    public static void compress(File src, File dest) throws IOException {
        ZipParameters params = new ZipParameters();
        params.setCompressionMethod(CompressionMethod.DEFLATE);
        params.setCompressionLevel(CompressionLevel.NORMAL);

        ZipFile zip = new ZipFile(dest);
        if (src.isFile()) {
            zip.addFile(src, params);
        } else if (src.isDirectory()) {
            zip.addFolder(src, params);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
