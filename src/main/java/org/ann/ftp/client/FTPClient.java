package org.ann.ftp.client;

import org.ann.ftp.protocol.FTPConnection;
import org.ann.ftp.protocol.FTPResponse;
import org.ann.ftp.util.FTPException;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple FTP client that can connect to an FTP server, log in, navigate directories, and disconnect.
 * for now, client is designed for demonstration purposes and does not implement all FTP features (like file transfers).
 *
 * @author Van An Nguyen
 */

public class FTPClient {
    private final FTPConnection connection;

    public FTPClient() {
        this.connection = new FTPConnection();
    }

    /**
     * Helper method to send a command and throw an exception if the response code isn't what was expected.
     */
    private FTPResponse executeCommand(String command, int... expectedCodes) throws IOException {
        connection.sendCommand(command);
        FTPResponse response = connection.readResponse();

        boolean isExpected = false;
        for (int code : expectedCodes) {
            if (response.getCode() == code) {
                isExpected = true;
                break;
            }
        }

        if (!isExpected) {
            throw new FTPException(response.getCode(), response.getMessage());
        }
        return response;
    }

    public void connect(String host, int port) throws IOException {
        connection.connect(host, port);
        FTPResponse welcome = connection.readResponse();
        if (welcome.getCode() != 220) {
            throw new FTPException(welcome.getCode(), "Server rejected connection: " + welcome.getMessage());
        }
    }

    public void login(String username, String password) throws IOException {
        // Send user, expect 331 (Need password) or 230 (Already logged in - no need for password)
        FTPResponse userResponse = executeCommand("USER " + username, 331, 230);

        if (userResponse.getCode() == 331) {
            executeCommand("PASS " + password, 230);
        }
    }

    public String pwd() throws IOException {
        FTPResponse response = executeCommand("PWD", 257);

        String msg = response.getMessage();
        int firstQuote = msg.indexOf('"');
        int secondQuote = msg.indexOf('"', firstQuote + 1);

        if (firstQuote != -1 && secondQuote != -1) {
            return msg.substring(firstQuote + 1, secondQuote);
        }
        return msg; // Fallback if the server format is weird
    }

    public void cd(String path) throws IOException {
        executeCommand("CWD " + path, 250);
    }

    public void mkdir(String directoryName) throws IOException {
        executeCommand("MKD " + directoryName, 257);
    }

    public void rmdir(String directoryName) throws IOException {
        executeCommand("RMD " + directoryName, 250);
    }

    public void delete(String filename) throws IOException {
        executeCommand("DELE " + filename, 250);
    }

    public void quit() throws IOException {
        try {
            executeCommand("QUIT", 221);
        } finally {
            connection.close();
        }
    }

    // ==============DATA TRANSFER===========
    public List<String> list (String path) throws IOException{
        List<String> fileList = new ArrayList<>();
        Socket dataSocket = connection.openPassiveDataConnection();

        // 150: File status okay; about to open data connection.
        // 125: Data connection already open; transfer starting.
        String cmd = (path == null || path.trim().isEmpty()) ? "LIST" : "LIST " + path;
        executeCommand(cmd, 150, 125);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream(),"UTF-8"))){
            String line;
            while((line = reader.readLine()) != null){
                fileList.add(line);
            }
        } finally {
            dataSocket.close();
        }

        FTPResponse finalResponse = connection.readResponse();
        if (finalResponse.getCode() != 226){
            throw new FTPException(finalResponse.getCode(), "LIST did not complete successfully");
        }

        return fileList;
    }

    public void get(String remoteFile, String localPath) throws IOException {
        executeCommand("TYPE I", 200); // Set to Binary/Image mode
        Socket dataSocket = connection.openPassiveDataConnection();

        executeCommand("RETR " + remoteFile, 150, 125);

        try (InputStream in = dataSocket.getInputStream();
             FileOutputStream out = new FileOutputStream(localPath)) {

            byte[] buffer = new byte[8192]; // 8KB chunks
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            dataSocket.close();
        }

        FTPResponse finalResponse = connection.readResponse();
        if (finalResponse.getCode() != 226) {
            throw new FTPException(finalResponse.getCode(), "Download failed: " + finalResponse.getMessage());
        }
    }

    public void put(String localPath, String remoteFile) throws IOException {
        executeCommand("TYPE I", 200); // Set to Binary/Image mode
        Socket dataSocket = connection.openPassiveDataConnection();

        executeCommand("STOR " + remoteFile, 150, 125);

        try (FileInputStream in = new FileInputStream(localPath);
             OutputStream out = dataSocket.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush(); // Ensure the last chunk is pushed to the network
        } finally {
            dataSocket.close();
        }

        FTPResponse finalResponse = connection.readResponse();
        if (finalResponse.getCode() != 226) {
            throw new FTPException(finalResponse.getCode(), "Upload failed: " + finalResponse.getMessage());
        }
    }
}

