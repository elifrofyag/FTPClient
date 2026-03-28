# FTP Client 

A high-performance FTP client built with Java 17+ and JavaFX.

## Prerequisites
* **Java SDK:** 17 or higher.
* **Environment:** `JAVA_HOME` must be set to your JDK installation directory.
* **Build Tool:** Maven (no need to install since wrapper is included).

## Environment Setup
1. Verify Java version 17 or higher in your terminal:
`java -version`   


2. Set `JAVA_HOME` in Environment Variables, ensuring it points to the correct JDK.
Check by running:
   - Windows (PowerShell): `$env:JAVA_HOME`
   - Linux/macOS: `echo $JAVA_HOME`

    Expected output: 
   - `C:\Program Files\Java\jdk-17` (Windows)
   - `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home` (macOS) 
   - `/usr/lib/jvm/java-17-openjdk` (Linux).
    > Note: If `JAVA_HOME` is not set, Maven will fail to locate the compiler.  

## Build and Run
1. Open your terminal in the project root and build by running:

    Windows:
    ```cmd
    .\mvnw.cmd clean install
    ```

    macOS / Linux:
    ```bash
    ./mvnw clean install
    ```

2. To run the application:

    Windows:
    ```cmd
    .\mvnw.cmd javafx:run
    ```

    macOS / Linux:
    ```bash
    ./mvnw javafx:run
    ```

## Testing

## Troubleshooting
* **JAVA_HOME Error:** Ensure `echo %JAVA_HOME%` (Windows) or `echo $JAVA_HOME` (Unix) points to a valid JDK 17+ path.
* **Permission Denied (Unix):** If the wrapper won't run, execute `chmod +x mvnw`.
* **Network Issues:** If connecting to `127.0.0.1`, ensure your **FileZilla Server** is active and "Require TLS" is disabled in settings.

