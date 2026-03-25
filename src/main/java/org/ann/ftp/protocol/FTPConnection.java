package org.ann.ftp.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * FTPConnection manages the control connection to an FTP server, allowing us to send commands and read responses.
 * It also provides a method to open a passive data connection based on the server's response to the PASV command.
 *
 * @author Van An Nguyen
 */
public class FTPConnection {
    private Socket controlSocket;
    private BufferedReader reader;
    private PrintWriter writer;

    /**
     * Establishes the control connection to the FTP server.
     */
    public void connect(String host, int port) throws IOException {
        controlSocket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), "UTF-8"));
        // Auto-flush is false; we will manually flush to ensure commands are sent immediately
        writer = new PrintWriter(new OutputStreamWriter(controlSocket.getOutputStream(), "UTF-8"), false);
    }

    /**
     * Sends a raw command to the server over the control connection.
     */
    public void sendCommand(String command) {
        if (writer != null) {
            writer.print(command + "\r\n"); // FTP standard requires CRLF line endings
            writer.flush();
        }
    }

    /**
     * Reads a response from the control connection using our FTPResponse parser.
     */
    public FTPResponse readResponse() throws IOException {
        if (reader == null) {
            throw new IOException("Not connected to server.");
        }
        return FTPResponse.readFull(reader);
    }

    /**
     * Sends the PASV command, parses the server's response, and opens a new Data Socket.
     */
    public Socket openPassiveDataConnection() throws IOException {
        sendCommand("PASV");
        FTPResponse response = readResponse();

        if (response.getCode() != 227) {
            throw new IOException("Could not enter passive mode: " + response.getMessage());
        }

        // The response format is usually: 227 Entering Passive Mode (h1,h2,h3,h4,p1,p2).
        String message = response.getMessage();
        int openParen = message.indexOf('(');
        int closeParen = message.indexOf(')');

        if (openParen == -1 || closeParen == -1) {
            throw new IOException("Failed to parse PASV response: " + message);
        }

        String data = message.substring(openParen + 1, closeParen);
        String[] parts = data.split(",");

        if (parts.length != 6) {
            throw new IOException("Invalid PASV data format: " + data);
        }

        // Construct IP address from the first 4 parts
        String ipAddress = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];

        // Calculate the port number from the last 2 parts
        int p1 = Integer.parseInt(parts[4]);
        int p2 = Integer.parseInt(parts[5]);
        int dataPort = (p1 * 256) + p2;

        return new Socket(ipAddress, dataPort);
    }

    /**
     * Closes the control connection and its streams.
     */
    public void close() throws IOException {
        if (writer != null) writer.close();
        if (reader != null) reader.close();
        if (controlSocket != null && !controlSocket.isClosed()) {
            controlSocket.close();
        }
    }
    /*
    public static void main(String[] args) {
        FTPConnection connection = new FTPConnection();
        try {
            System.out.println("Connecting to ftp.gnu.org...");
            connection.connect("ftp.gnu.org", 21);

            // Read the initial welcome message from the server
            FTPResponse welcome = connection.readResponse();
            System.out.println("Server: " + welcome);

            // Send Anonymous Login
            System.out.println("\nSending USER...");
            connection.sendCommand("USER anonymous");
            System.out.println("Server: " + connection.readResponse());

            // Request Passive Mode
            System.out.println("\nRequesting Passive Mode...");
            Socket dataSocket = connection.openPassiveDataConnection();
            System.out.println("Successfully opened Data Socket to: " +
                    dataSocket.getInetAddress().getHostAddress() +
                    " on port " + dataSocket.getPort());

            // Clean up
            dataSocket.close();
            connection.sendCommand("QUIT");
            System.out.println("Server: " + connection.readResponse());
            connection.close();
            System.out.println("Connection closed safely.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     */
}