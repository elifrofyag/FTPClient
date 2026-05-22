package org.ann.ftp.client;

public class FTPFile {
    private final String name;
    private final long size;
    private final String date;
    private final String permissions;
    private final boolean isDirectory;
    private final String rawResponse;

    public FTPFile(String name, long size, String date, String permissions, boolean isDirectory, String rawResponse) {
        this.name = name;
        this.size = size;
        this.date = date;
        this.permissions = permissions;
        this.isDirectory = isDirectory;
        this.rawResponse = rawResponse;
    }

    public String getName() { return name; }
    public String getSizeFormatted() {
        if (isDirectory) return "--";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return (size / 1024) + " KB";
        return (size / (1024 * 1024)) + " MB";
    }
    //getters but not used yet, but maybe in the future
    public String getDate() { return date; }
    public String getPermissions() { return permissions; }
    public boolean isDirectory() { return isDirectory; }
    public String getRawResponse() { return rawResponse; }
}