package org.example.service;

import javafx.scene.image.Image;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CaptchaService {
    private static final Map<String, String> captchaMap = new HashMap<>();
    private static final Random random = new Random();

    static {
        // Initialize captcha images and their values using local paths
        captchaMap.put("/captcha/captcha-img1.png", "captcha246");
        captchaMap.put("/captcha/captcha-img2.png", "2A4ac");
        captchaMap.put("/captcha/captcha-img3.png", "B56mf");
        captchaMap.put("/captcha/captcha-img4.png", "hui6");
        captchaMap.put("/captcha/captcha-img5.png", "ATi7n");
    }

    private String currentCaptchaPath;
    private String currentCaptchaValue;

    public CaptchaService() {
        generateNewCaptcha();
    }

    public void generateNewCaptcha() {
        int index = random.nextInt(captchaMap.size());
        int i = 0;
        for (Map.Entry<String, String> entry : captchaMap.entrySet()) {
            if (i == index) {
                currentCaptchaPath = entry.getKey();
                currentCaptchaValue = entry.getValue();
                break;
            }
            i++;
        }
    }

    public Image getCurrentCaptchaImage() {
        try {
            // Load image from resources
            return new Image(getClass().getResourceAsStream(currentCaptchaPath));
        } catch (Exception e) {
            System.err.println("Error loading captcha image: " + e.getMessage());
            return null;
        }
    }

    public boolean validateCaptcha(String userInput) {
        return userInput != null && userInput.equals(currentCaptchaValue);
    }

    public String getCurrentCaptchaValue() {
        return currentCaptchaValue;
    }
} 