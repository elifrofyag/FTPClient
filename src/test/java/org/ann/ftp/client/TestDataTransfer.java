package org.ann.ftp.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestDataTransfer {

    private FTPClient client;

    // runs BEFORE every @Test method
    @BeforeEach
    void setUp() throws IOException {
        client = new FTPClient();
        System.out.println("Connecting and logging in...");
//        client.connect("ftp.dlptest.com", 21);
//        client.login("dlpuser", "rNrKYTX9g7z3RgJRmxWuGHbeu");
        client.connect("ftp.gnu.org", 21);
        client.login("anonymous", "guest@");
    }

    // runs AFTER every @Test method, even if the test fails
    @AfterEach
    void tearDown() throws IOException {
        System.out.println("Disconnecting...");
        client.quit();
    }

    @Test
    void testListDirectory() {
        assertDoesNotThrow(() -> {
            System.out.println("Testing LIST command...");
            List<String> files = client.list("");

            assertNotNull(files, "The file list should not be null.");
            System.out.println("Found " + files.size() + " items in the directory.");

            // just print the first few items to verify it works
            for (int i = 0; i < Math.min(3, files.size()); i++) {
                System.out.println(" - " + files.get(i));
            }
        });
    }

    @Test
    void testFullFileLifecycle() throws IOException {
        Path originalLocalFile = Files.createTempFile("ftp_upload_test_", ".txt");
        System.out.println(originalLocalFile);
        Path downloadedLocalFile = Files.createTempFile("ftp_download_test_", ".txt");
        String remoteFileName = originalLocalFile.getFileName().toString(); // Use the random name for the server

        String testData = "Hello FTP Server! This is an automated test from Java.";
        Files.writeString(originalLocalFile, testData);

        try {
            System.out.println("Uploading file: " + remoteFileName);
            assertDoesNotThrow(() -> client.put(originalLocalFile.toString(), remoteFileName));

            System.out.println("Verifying file exists on server...");
            List<String> files = client.list("");
            boolean found = files.stream().anyMatch(line -> line.contains(remoteFileName));
            assertTrue(found, "The uploaded file should appear in the LIST response.");

            System.out.println("Downloading file back...");
            assertDoesNotThrow(() -> client.get(remoteFileName, downloadedLocalFile.toString()));

            String downloadedData = Files.readString(downloadedLocalFile);
            assertEquals(testData, downloadedData, "The downloaded file content should match the original.");

            System.out.println("Deleting file from server...");
            assertDoesNotThrow(() -> client.delete(remoteFileName));

        } finally {
            Files.deleteIfExists(originalLocalFile);
            Files.deleteIfExists(downloadedLocalFile);
        }
    }

}
