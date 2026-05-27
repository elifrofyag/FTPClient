package org.ann.ftp.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * FTPConnection manages the control connection to an FTP server. It allows sending commands and reading responses.
 * It also provides a method to open a passive data connection based on the server's response to PASV command.
 *
 * @author Van An Nguyen
 */
public class FTPConnection {
    private Socket controlSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Consumer<String> trafficListener;

    public void setTrafficListener(Consumer<String> trafficListener) {
        this.trafficListener = trafficListener;
    }

    /**
     * Establishes control connection to FTP server.
     */
    public void connect(String host, int port) throws IOException {
        controlSocket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), "UTF-8"));
        writer = new PrintWriter(new OutputStreamWriter(controlSocket.getOutputStream(), "UTF-8"), false);
    }

    /**
     * Sends a raw command to the server over the control connection
     */
    public void sendCommand(String command) {
        if (writer != null) {
            writer.print(command + "\r\n");
            writer.flush();
            logTraffic("[client] " + command);
        }
    }

    /**
     * Reads a response from the control connection using FTPResponse parser
     */
    public FTPResponse readResponse() throws IOException {
        if (reader == null) {
            throw new IOException("Not connected to server.");
        }
        return FTPResponse.readFull(reader, line -> logTraffic("[server] " + line));
    }

    private void logTraffic(String line) {
        if (trafficListener != null) {
            trafficListener.accept(line);
        }
    }

    /**
     * Sends PASV command, parses server's response, and opens a new Data Socket
     */
    public Socket openPassiveDataConnection() throws IOException {
        sendCommand("PASV");
        FTPResponse response = readResponse();

        if (response.getCode() != 227) {
            throw new IOException("Could not enter passive mode: " + response.getMessage());
        }

        // eg 227 enter passive mode (h1,h2,h3,h4,p1,p2)
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

        String ipAddress = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];

        int p1 = Integer.parseInt(parts[4]);
        int p2 = Integer.parseInt(parts[5]);
        int dataPort = (p1 * 256) + p2;

        System.out.println("DEBUG - Connecting to DATA socket: " + ipAddress + ":" + dataPort);

        try { Thread.sleep(100); } catch (InterruptedException e) { }

        Socket dataSocket = new Socket();
        dataSocket.connect(new java.net.InetSocketAddress(ipAddress, dataPort), 10000);
        return dataSocket;
    }

    /**
     * Closes control connection and its streams.
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
            System.out.println("connect to ftp.gnu.org");
            connection.connect("127.0.0.1", 21);
//            connection.connect("ftp.gnu.org", 21);

            FTPResponse welcome = connection.readResponse();
            System.out.println("server line 108: " + welcome);

            // anonymous login
            connection.sendCommand("USER ann");
//            connection.sendCommand("USER anonymous");
            System.out.println("server line 113: " + connection.readResponse());

//             send pass
            connection.sendCommand("PASS ");
            System.out.println("server line 118: " + connection.readResponse());



            // cli
            while (true) {
                System.out.print("\enter ftp or 'quit': ");
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                String command = consoleReader.readLine();
                // pasv
                if (command.equalsIgnoreCase("pasv")) {
                    System.out.println("\n request passive mode");

                    Socket dataSocket = connection.openPassiveDataConnection();
                    System.out.println("successfully open data socket: " +
                            dataSocket.getInetAddress().getHostAddress() +
                            " on port " + dataSocket.getPort());
                    //  command from user
                    String cmd = consoleReader.readLine();
                    connection.sendCommand(cmd);
                    System.out.println("Server PI 138: " + connection.readResponse());

                    try (BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()))) {
                        String line;
                        System.out.println("\nDATA RECEIVED---");
                        while ((line = dataReader.readLine()) != null) {
                            System.out.println(line);
                        }
                        System.out.println("-END OF DATA---\n");
                    } finally {
                        dataSocket.close();
                    }



                    System.out.println("server PI 159: " + connection.readResponse());
                    continue;

                }
                if (command.equalsIgnoreCase("quit")) {
                    connection.sendCommand("QUIT");
                    System.out.println("Server: " + connection.readResponse());
                    connection.close();
                    System.out.println("connection closed");
                    break;
                }
                connection.sendCommand(command);
                System.out.println("server line 169: " + connection.readResponse());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/
}
