/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
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
 *
 * Website: https://fptn.org
 */

package org.fptn.vpn.utils;

import android.content.Context;

import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.flattener.PatternFlattener;
import com.elvishew.xlog.printer.AndroidPrinter;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.backup.FileSizeBackupStrategy2;
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy;
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator;

import org.fptn.vpn.enums.AppLogLevel;

import java.io.File;

/**
 * Builds the {@code XLog} configuration from the log level saved in {@link SharedPrefUtils}.
 * Called once from {@code App#onCreate} and again whenever the user changes the level in
 * Settings, so the new level takes effect immediately without an app restart.
 */
public final class XLogInitializer {
    private XLogInitializer() {
    }

    public static void init(Context context) {
        File logDir = new File(context.getFilesDir(), "logs2");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        String logPath = logDir.getAbsolutePath() + File.separator;

        PatternFlattener flattener = new PatternFlattener("{d yyyy-MM-dd HH:mm:ss}|{l}|{t}|{m}");

        AppLogLevel level = SharedPrefUtils.getLogLevel(context);
        LogConfiguration config = new LogConfiguration.Builder()
                .logLevel(level.getXLogLevel())
                .tag("FPTN")
                .build();

        FilePrinter filePrinter = new FilePrinter.Builder(logPath)
                .fileNameGenerator(new DateFileNameGenerator())
                .backupStrategy(new FileSizeBackupStrategy2(512 * 1024, 10))
                .cleanStrategy(new FileLastModifiedCleanStrategy(60 * 60 * 1000L))
                .flattener(flattener)
                .build();
        XLog.init(config, filePrinter, new AndroidPrinter());
    }

    /**
     * Deletes every file in the log directory, then re-inits XLog so it opens a fresh file
     * instead of continuing to write to the now-deleted (and thus invisible) one.
     */
    public static void clearLogs(Context context) {
        File logDir = new File(context.getFilesDir(), "logs2");
        File[] files = logDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        init(context);
    }
}
