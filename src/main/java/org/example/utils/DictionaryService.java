package org.example.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class to fetch word definitions from the API-Ninjas dictionary API
 */
public class DictionaryService {
    private static final String API_URL = "https://api.api-ninjas.com/v1/dictionary?word=";
    private static final String API_KEY = "qIqofalYxY7gFle5d508tw==GpR3z4ccU2fCGaAr";
    
    /**
     * Get the definition of a word asynchronously
     * @param word The word to look up
     * @return CompletableFuture containing the definition as a string
     */
    public static CompletableFuture<String> getDefinitionAsync(String word) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getDefinition(word);
            } catch (Exception e) {
                System.err.println("Error getting definition for '" + word + "': " + e.getMessage());
                e.printStackTrace();
                return "Could not find the definition for '" + word + "'. Please try another word.";
            }
        });
    }
    
    /**
     * Get the definition of a word synchronously
     * @param word The word to look up
     * @return The definition as a String
     * @throws IOException if there's an error fetching the definition
     */
    public static String getDefinition(String word) throws IOException {
        if (word == null || word.trim().isEmpty()) {
            return "Please enter a valid word to look up.";
        }
        
        System.out.println("Looking up definition for: " + word);
        
        try {
            // URL encode the word
            String encodedWord = java.net.URLEncoder.encode(word.trim(), "UTF-8");
            URL url = new URL(API_URL + encodedWord);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Setup request
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Api-Key", API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(10000); // 10 seconds timeout
            connection.setReadTimeout(10000);    // 10 seconds read timeout
            
            // Check response code
            int responseCode = connection.getResponseCode();
            System.out.println("Dictionary API response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // Parse the JSON response
                String jsonResponse = response.toString();
                System.out.println("Dictionary API response: " + jsonResponse);
                
                // Extract the definition
                String definition = parseDefinition(jsonResponse);
                
                if (definition.isEmpty()) {
                    System.out.println("No definition found for '" + word + "'");
                    return "No definition found for '" + word + "'.";
                }
                
                System.out.println("Definition found for '" + word + "'");
                return "Definition of '" + word + "':\n" + definition;
            } else {
                // API call failed
                System.err.println("Dictionary API request failed with code: " + responseCode);
                return "Failed to retrieve definition for '" + word + "'. Error code: " + responseCode;
            }
        } catch (Exception e) {
            System.err.println("Exception during dictionary API request: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Error looking up word: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse the definition from the JSON response
     * @param jsonResponse The JSON response from the API
     * @return The formatted definition
     */
    private static String parseDefinition(String jsonResponse) {
        try {
            // Check if it's a valid word
            if (jsonResponse.contains("\"valid\":false")) {
                return "";
            }
            
            // Look for definition field
            int defStart = jsonResponse.indexOf("\"definition\":");
            if (defStart < 0) return "";
            
            // Move past the field name and colon
            defStart += "\"definition\":".length();
            
            // Check if definition is null or empty
            if (jsonResponse.substring(defStart).trim().startsWith("null") || 
                jsonResponse.substring(defStart).trim().startsWith("\"\"")) {
                return "";
            }
            
            // Find the start of the actual definition string
            int valueStart = jsonResponse.indexOf("\"", defStart) + 1;
            if (valueStart <= 0) return "";
            
            // Find the end of the definition string (accounting for escaped quotes)
            int valueEnd = valueStart;
            boolean escaped = false;
            
            while (valueEnd < jsonResponse.length()) {
                char c = jsonResponse.charAt(valueEnd);
                
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    break;
                }
                
                valueEnd++;
            }
            
            if (valueEnd >= jsonResponse.length()) return "";
            
            // Extract the definition and unescape it
            String definition = jsonResponse.substring(valueStart, valueEnd);
            definition = definition.replace("\\\"", "\"")
                                .replace("\\\\", "\\")
                                .replace("\\n", "\n")
                                .replace("\\r", "\r")
                                .replace("\\t", "\t");
            
            return definition;
        } catch (Exception e) {
            System.err.println("Error parsing definition: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * Check if a word exists in the dictionary
     * @param word The word to check
     * @return CompletableFuture<Boolean> indicating whether the word exists
     */
    public static CompletableFuture<Boolean> wordExistsAsync(String word) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String definition = getDefinition(word);
                return !definition.contains("No definition found") && 
                       !definition.contains("Failed to retrieve definition");
            } catch (Exception e) {
                System.err.println("Error checking if word exists: " + e.getMessage());
                return false;
            }
        });
    }
} 