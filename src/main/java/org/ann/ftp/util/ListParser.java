package org.ann.ftp.util;

import org.ann.ftp.client.FTPFile;

public class ListParser {

    public static FTPFile parse(String rawLine) {
        if (rawLine == null || rawLine.trim().isEmpty()) {
            return null;
        }

        // note: unix style  has 9 cols separated by space
        // [0:perms] [1:links] [2:owner] [3:group] [4:size] [5:Mmonth] [6:day] [7:time/year] [8:name]
        // -> regex "\\s+" to split by any amount of whitespace up to a max of 9
        // (so spaces inside filenames don't get split)
        String[] parts = rawLine.trim().split("\\s+", 9);

        try {
            if (parts.length >= 9 && (parts[0].startsWith("d") || parts[0].startsWith("-"))) {
                String permissions = parts[0];
                boolean isDir = permissions.startsWith("d");
                long size = Long.parseLong(parts[4]);


                String date = parts[5] + " " + parts[6] + " " + parts[7]; //month day time into 1 str
                String name = parts[8];

                return new FTPFile(name, size, date, permissions, isDir, rawLine);
            }
        } catch (Exception e) {
        }

        // fallback for weird formats or the ".." entry
        return new FTPFile(rawLine, 0, "", "", false, rawLine);
    }
}