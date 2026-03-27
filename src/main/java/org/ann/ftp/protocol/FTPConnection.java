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

        // eg 227 Entering Passive Mode (h1,h2,h3,h4,p1,p2).
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

        System.out.println("DEBUG - Attempting to connect to Data Socket: " + ipAddress + ":" + dataPort);

        try { Thread.sleep(100); } catch (InterruptedException e) { } // try to fix intermittent connection timeout

        Socket dataSocket = new Socket();
        dataSocket.connect(new java.net.InetSocketAddress(ipAddress, dataPort), 10000);
        return dataSocket;
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
            connection.connect("127.0.0.1", 21);
//            connection.connect("ftp.gnu.org", 21);


            // Read the initial welcome message from the server
            FTPResponse welcome = connection.readResponse();
            System.out.println("Server 108: " + welcome);

            // Send Anonymous Login
            System.out.println("\nSending USER...");
            connection.sendCommand("USER ann");
//            connection.sendCommand("USER anonymous");
            System.out.println("Server 113: " + connection.readResponse());

//             Send Password
            System.out.println("\nSending PASS...");
            connection.sendCommand("PASS ");
            System.out.println("Server 118: " + connection.readResponse());



            // cli
            while (true) {
                System.out.print("\nEnter FTP Command (or 'quit' to exit): ");
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                String command = consoleReader.readLine();
                // pasv
                if (command.equalsIgnoreCase("pasv")) {
                    System.out.println("\nRequesting Passive Mode...");

                    Socket dataSocket = connection.openPassiveDataConnection();
                    System.out.println("Successfully opened Data Socket to: " +
                            dataSocket.getInetAddress().getHostAddress() +
                            " on port " + dataSocket.getPort());
                    //  command from user
                    String cmd = consoleReader.readLine();
                    connection.sendCommand(cmd);
                    System.out.println("Server PI 138: " + connection.readResponse());

                    try (BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()))) {
                        String line;
                        System.out.println("\n--- DATA RECEIVED ---");
                        while ((line = dataReader.readLine()) != null) {
                            System.out.println(line);
                        }
                        System.out.println("--- END OF DATA ---\n");
                    } finally {
                        dataSocket.close();
                    }



                    System.out.println("Server PI 159: " + connection.readResponse());
                    continue;

                }
                if (command.equalsIgnoreCase("quit")) {
                    connection.sendCommand("QUIT");
                    System.out.println("Server: " + connection.readResponse());
                    connection.close();
                    System.out.println("Connection closed safely.");
                    break;
                }
                connection.sendCommand(command);
                System.out.println("Server 169: " + connection.readResponse());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/
}