package org.example.models.quiz;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

public class emailing {
    public boolean sendEmail(String recipient, String subject, String body) {
        try {
            // Use a more generic approach to find Python
            String pythonPath = findPythonExecutable();
            if (pythonPath == null) {
                System.err.println("Python executable not found. Email cannot be sent.");
                return false;
            }
            
            // Get the current working directory for locating the script
            String currentDir = System.getProperty("user.dir");
            String scriptPath = currentDir + "\\src\\main\\python\\emailing.py";
            
            // Check if the script exists
            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                System.err.println("Python script not found at: " + scriptPath);
                return false;
            }
            
            System.out.println("Using Python: " + pythonPath);
            System.out.println("Using Script: " + scriptPath);
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonPath,
                    scriptPath,
                    recipient,
                    subject,
                    body
            );

            processBuilder.redirectErrorStream(true); // Capture both stdout & stderr
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Python Output: " + line);
            }

            int exitCode = process.waitFor();
            System.out.println("Exited with code: " + exitCode);

            return exitCode == 0; // Return true if successful
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Return false on failure
        }
    }
    
    /**
     * Find Python executable from common locations
     * @return Path to Python executable or null if not found
     */
    private String findPythonExecutable() {
        // Try to use the system's default "python" command
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "--version");
            Process process = pb.start();
            if (process.waitFor() == 0) {
                return "python";
            }
        } catch (Exception e) {
            // Continue to next attempts
        }
        
        // Try python3
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "--version");
            Process process = pb.start();
            if (process.waitFor() == 0) {
                return "python3";
            }
        } catch (Exception e) {
            // Continue to next attempts
        }
        
        // Check common installation paths
        String[] commonPaths = {
            System.getenv("LOCALAPPDATA") + "\\Programs\\Python\\Python311\\python.exe",
            System.getenv("LOCALAPPDATA") + "\\Programs\\Python\\Python310\\python.exe",
            System.getenv("LOCALAPPDATA") + "\\Programs\\Python\\Python39\\python.exe",
            "C:\\Python311\\python.exe",
            "C:\\Python310\\python.exe",
            "C:\\Python39\\python.exe"
        };
        
        for (String path : commonPaths) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                return path;
            }
        }
        
        return null;
    }
}
