package org.ann.ftp.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.ann.ftp.util.FTPException;

public class TestNavigation {

    @Test
    public void testPublicServerNavigation() {
        FTPClient client = new FTPClient();

        assertDoesNotThrow(() -> {
            System.out.println("Connecting...");
//            client.connect("ftp.gnu.org", 21);
            client.connect("ftp.dlptest.com", 21);

            System.out.println("Logging in...");
//            client.login("anonymous", "guest@");
            client.login("dlpuser", "rNrKYTX9g7z3RgJRmxWuGHbeu");

            System.out.println("Checking current directory...");
            String currentDir = client.pwd();
            assertEquals("/", currentDir, "Initial directory should be root '/'");

            //mkdir
            System.out.println("Creating directory 'test_dir'...");
            client.mkdir("test_dir");
            System.out.println("Changing directory to 'test_dir'...");
            client.cd("test_dir");
            System.out.println("Checking directory again...");
            String testDir = client.pwd();
            assertEquals("/test_dir", testDir, "Directory should now be '/test_dir'");

            // rmdir
            System.out.println("Changing back to parent directory...");
            client.cd("..");
            System.out.println("Removing directory 'test_dir'...");
            client.rmdir("test_dir");
            System.out.println("Checking directory again...");
            String parentDir = client.pwd();
            System.out.println("trying to change back to 'test_dir'...");
            Exception exception = assertThrows(FTPException.class, () -> {
                client.cd("test_dir");
            });
            assertTrue(exception.getMessage().contains("550"), "Exception should contain the 550 status code");

            System.out.println("Disconnecting...");
            client.quit();
        });
    }

    @Test
    public void testFailedLoginThrowsException() {
        FTPClient client = new FTPClient();

        Exception exception = assertThrows(FTPException.class, () -> {
            client.connect("ftp.gnu.org", 21);
            client.login("invalid_user_123", "wrong_password");
        });

        assertTrue(exception.getMessage().contains("530"), "Exception should contain the 530 status code");

        assertDoesNotThrow(() -> client.quit());
    }
}