package org.example.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.json.JSONObject;

/**
 * Service class to handle sentiment analysis using API Ninja
 */
public class SentimentAnalysisService {
    
    // API Ninja endpoint URL - We will append the query parameter
    private static final String API_URL = "https://api.api-ninjas.com/v1/sentiment";
    
    // API key loaded from config.properties
    private String apiKey;
    
    // Constructor loads API key from properties file
    public SentimentAnalysisService() {
        loadApiKey();
    }
    
    // Load API key from properties file
    private void loadApiKey() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("Unable to find config.properties, API calls will fail");
                apiKey = "";
                return;
            }
            
            properties.load(input);
            apiKey = properties.getProperty("api.ninja.key");
            
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_NINJA_KEY_HERE")) {
                System.err.println("API key not configured correctly in config.properties");
                apiKey = "";
            }
        } catch (IOException e) {
            System.err.println("Error loading API key: " + e.getMessage());
            apiKey = "";
        }
    }
    
    // Sentiment enum
    public enum Sentiment {
        POSITIVE("#28a745"),       // Green
        WEAK_POSITIVE("#9fd876"),  // Light green
        NEUTRAL("#ffc107"),        // Yellow
        WEAK_NEGATIVE("#ff9800"),  // Orange
        NEGATIVE("#dc3545");       // Red
        
        private final String colorCode;
        
        Sentiment(String colorCode) {
            this.colorCode = colorCode;
        }
        
        public String getColorCode() {
            return colorCode;
        }
    }
    
    /**
     * Analyze the sentiment of the given text
     * @param text The text to analyze
     * @return The sentiment analysis result
     * @throws Exception If there was an error communicating with the API
     */
    public Sentiment analyzeSentiment(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            return Sentiment.NEUTRAL;
        }
        
        // Check if API key is configured
        if (apiKey.isEmpty()) {
            System.err.println("API key not configured, using NEUTRAL sentiment");
            return Sentiment.NEUTRAL;
        }
        
        try {
            // URL encode the text parameter
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            
            // Create URL with query parameter
            URL url = new URL(API_URL + "?text=" + encodedText);
            System.out.println("Sending request to: " + url);
            
            // Set up connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");  // API Ninja uses GET for sentiment analysis
            conn.setRequestProperty("X-Api-Key", apiKey);
            conn.setRequestProperty("Accept", "application/json");
            
            // Check the response code
            int responseCode = conn.getResponseCode();
            System.out.println("API response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read successful response
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    String responseStr = response.toString();
                    System.out.println("API response: " + responseStr);
                    
                    // Parse JSON response
                    JSONObject jsonResponse = new JSONObject(responseStr);
                    String sentimentText = jsonResponse.getString("sentiment");
                    
                    // Map the response to the Sentiment enum
                    return Sentiment.valueOf(sentimentText);
                }
            } else {
                // Handle error response
                System.err.println("API request failed with code: " + responseCode);
                
                // Try to read error response
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    System.err.println("Error response: " + response.toString());
                } catch (Exception e) {
                    System.err.println("Could not read error response: " + e.getMessage());
                }
                
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    System.err.println("Authentication error: Invalid API key");
                } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    System.err.println("Bad request: Check your request parameters");
                } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    System.err.println("Forbidden: You may have exceeded your rate limits");
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    System.err.println("Not found: The API endpoint might have changed");
                } else if (responseCode >= 500) {
                    System.err.println("Server error: API Ninja may be experiencing issues");
                }
                
                return Sentiment.NEUTRAL; // Default to neutral on failure
            }
        } catch (Exception e) {
            System.err.println("Exception during API call: " + e.getMessage());
            e.printStackTrace();
            return Sentiment.NEUTRAL;
        }
    }
    
    /**
     * Get the CSS style for a given sentiment
     * @param sentiment The sentiment to get the style for
     * @return CSS style string
     */
    public String getSentimentStyle(Sentiment sentiment) {
        return "-fx-background-color: " + sentiment.getColorCode() + "; " +
               "-fx-background-radius: 4px; " +
               "-fx-text-fill: white; " +
               "-fx-padding: 2px 8px; " +
               "-fx-font-weight: bold;";
    }
} 