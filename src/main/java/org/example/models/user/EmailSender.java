package org.example.models.user;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class EmailSender {

    public boolean sendEmail(String recipient, String subject, String body) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "C:\\Python313\\python.exe",
                    "C:\\Users\\Expert Gaming\\Desktop\\newjava\\src\\main\\resources\\user\\emailsender.py",
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
}
