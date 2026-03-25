package org.ann.ftp.util;

import java.io.IOException;
public class FTPException extends IOException {
    private final int code;

    public FTPException(int code, String message) {
        super("FTP Error " + code + ": " + message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}