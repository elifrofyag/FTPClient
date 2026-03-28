package org.ann.ftp.util;

import org.ann.ftp.client.FTPFile;

public class ListParser {

    public static FTPFile parse(String rawLine) {
        if (rawLine == null || rawLine.trim().isEmpty()) {
            return null;
        }

        // Unix style usually has 9 columns separated by whitespace:
        // [0:Perms] [1:Links] [2:Owner] [3:Group] [4:Size] [5:Month] [6:Day] [7:Time/Year] [8:Name]
        // We use regex "\\s+" to split by ANY amount of whitespace, up to a maximum of 9 chunks
        // (so spaces inside filenames don't get split!)
        String[] parts = rawLine.trim().split("\\s+", 9);

        try {
            if (parts.length >= 9 && (parts[0].startsWith("d") || parts[0].startsWith("-"))) {
                String permissions = parts[0];
                boolean isDir = permissions.startsWith("d");
                long size = Long.parseLong(parts[4]);

                // Combine Month, Day, and Time into one clean string
                String date = parts[5] + " " + parts[6] + " " + parts[7];
                String name = parts[8];

                return new FTPFile(name, size, date, permissions, isDir, rawLine);
            }
        } catch (Exception e) {
            // If parsing fails, we fall back to a safe default so the app doesn't crash
        }

        // Fallback for weird formats (like Windows DOS style) or the ".." directory
        return new FTPFile(rawLine, 0, "", "", false, rawLine);
    }
}