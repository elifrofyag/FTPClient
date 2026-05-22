package org.ann.ftp.protocol;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Represents a parsed FTP response, including the status code, message, and whether it's a multiline response.
 * provides methods to parse raw FTP response lines and to read full responses from a BufferedReader, handling both single-line and multiline formats
 *
 * @author Van An Nguyen
 *
 */
public class FTPResponse {
    private final int code;
    private final String message;
    private final boolean isMultiline;

    public FTPResponse(int code, String message, boolean isMultiline) {
        this.code = code;
        this.message = message;
        this.isMultiline = isMultiline;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public boolean isMultiline() {
        return isMultiline;
    }

    /**
     * Parses a single line of raw FTP response.
     */
    public static FTPResponse parse(String rawLine) {
        if (rawLine == null || rawLine.length() < 3) {
            return new FTPResponse(-1, rawLine != null ? rawLine : "null", false);
        }

        try {
            // ftp status code XXX
            int code = Integer.parseInt(rawLine.substring(0, 3));

            // multiline response eg "220-"
            boolean isMulti = rawLine.length() >= 4 && rawLine.charAt(3) == '-';

            // extract text message
            String msg = rawLine.length() >= 4 ? rawLine.substring(4) : "";

            return new FTPResponse(code, msg, isMulti);

        } catch (NumberFormatException e) {
            return new FTPResponse(-1, rawLine, false);
        }
    }

    /**
     * Reads a full response from the server, handling both single and multiline formats.
     */
    public static FTPResponse readFull(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Connection closed prematurely by the FTP server.");
        }

        FTPResponse firstLineResponse = parse(line);

        // if standard single line res -> done
        if (!firstLineResponse.isMultiline()) {
            return firstLineResponse;
        }

        // multiline response -> read until find the termination line
        int expectedCode = firstLineResponse.getCode();
        StringBuilder fullMessage = new StringBuilder(firstLineResponse.getMessage());
        fullMessage.append("\n");

        while ((line = reader.readLine()) != null) {
            fullMessage.append(line).append("\n");

            // termination condition for multiline "XXX "
            if (line.length() >= 4 && line.startsWith(expectedCode + " ")) {
                break;
            }
        }

        return new FTPResponse(expectedCode, fullMessage.toString().trim(), true);
    }

    @Override
    public String toString() {
        if (isMultiline) {
            return code + "- " + message.replace("\n", "\n   ");
        }
        return code + " " + message;
    }

    /*
    public static void main(String[] args) {
        String simulatedServerData =
                "123-First line\n" +
                        "Second line\n" +
                        "234 A line beginning with numbers\n" +
                        "123 The last line\n";
        String simulatedSingleLineResponse = "220 Service ready for new user.\n";

        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(simulatedServerData));
        BufferedReader singleLineReader = new BufferedReader(new java.io.StringReader(simulatedSingleLineResponse))) {

            FTPResponse response = FTPResponse.readFull(reader);
            FTPResponse singleLineResponseObj = FTPResponse.readFull(singleLineReader);

            System.out.println("Single Line Response:");
            System.out.println(singleLineResponseObj.toString());


            System.out.println("---INTERNAL DATA");
            System.out.println("Parsed Code: " + response.getCode());
            System.out.println("Parsed Message:\n" + response.getMessage());
            System.out.println("\n--- GUI LOG OUTPUT ---");
            System.out.println(response.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     */
}