package org.example.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to fetch random facts from the API-Ninjas facts API
 */
public class FactService {
    private static final String API_URL = "https://api.api-ninjas.com/v1/facts?limit=";
    private static final String API_KEY = "qIqofalYxY7gFle5d508tw==GpR3z4ccU2fCGaAr";
    
    // Cache for facts to avoid excessive API calls
    private static final List<String> factCache = new ArrayList<>();
    private static final int MAX_CACHE_SIZE = 50;
    private static final int DEFAULT_FETCH_COUNT = 5;
    private static final Random random = new Random();
    
    /**
     * Get a random fact asynchronously
     * @return CompletableFuture containing a random fact string
     */
    public static CompletableFuture<String> getRandomFactAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getRandomFact();
            } catch (Exception e) {
                System.err.println("Error fetching fact asynchronously: " + e.getMessage());
                e.printStackTrace();
                return "Did you know? The ability to fetch interesting facts is fascinating, but sometimes connections fail.";
            }
        });
    }
    
    /**
     * Get a random fact synchronously
     * @return A random fact as a String
     * @throws IOException if there's an error fetching facts
     */
    public static String getRandomFact() throws IOException {
        // If cache is empty or nearly empty, fetch more facts
        if (factCache.size() < 3) {
            try {
                fetchFacts(DEFAULT_FETCH_COUNT);
                System.out.println("Fetched facts from API. Cache size: " + factCache.size());
            } catch (Exception e) {
                System.err.println("Error fetching facts: " + e.getMessage());
                e.printStackTrace();
                
                // Add some backup facts if the cache is still empty
                if (factCache.isEmpty()) {
                    addBackupFacts();
                    System.out.println("Added backup facts. Cache size: " + factCache.size());
                }
            }
        }
        
        // Get a random fact from the cache
        if (!factCache.isEmpty()) {
            int index = random.nextInt(factCache.size());
            return factCache.remove(index); // Remove to avoid repetition
        } else {
            return "Did you know? Facts are fascinating, but sometimes hard to retrieve.";
        }
    }
    
    /**
     * Fetch facts from the API and add them to the cache
     * @param limit Number of facts to fetch
     * @throws IOException if there's an error connecting to the API
     */
    private static void fetchFacts(int limit) throws IOException {
        System.out.println("Fetching " + limit + " facts from API...");
        
        try {
            URL url = new URL(API_URL + limit);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Setup request
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Api-Key", API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(10000); // 10 seconds timeout
            connection.setReadTimeout(10000);    // 10 seconds read timeout
            
            // Check response code
            int responseCode = connection.getResponseCode();
            System.out.println("API response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String jsonResponse = response.toString();
                System.out.println("API response: " + jsonResponse);
                
                // Parse facts from JSON response using regex (simple approach)
                Pattern pattern = Pattern.compile("\"fact\":\"(.*?)\"");
                Matcher matcher = pattern.matcher(jsonResponse);
                
                int factsFound = 0;
                
                while (matcher.find() && factCache.size() < MAX_CACHE_SIZE) {
                    String fact = matcher.group(1);
                    if (!fact.isEmpty()) {
                        factCache.add("Did you know? " + fact);
                        factsFound++;
                    }
                }
                
                System.out.println("Facts extracted: " + factsFound);
                
                if (factsFound == 0) {
                    // No facts found in the response, add backup facts
                    System.out.println("No facts found in API response, adding backup facts...");
                    addBackupFacts();
                }
            } else {
                System.err.println("API request failed with code: " + responseCode);
                // If API call fails, add backup facts
                addBackupFacts();
            }
            
            connection.disconnect();
        } catch (Exception e) {
            System.err.println("Exception during API request: " + e.getMessage());
            e.printStackTrace();
            // If an exception occurred, add backup facts
            addBackupFacts();
        }
    }
    
    /**
     * Add backup facts to the cache in case the API call fails
     */
    private static void addBackupFacts() {
        factCache.add("Did you know? The human brain can process information as fast as 120 meters per second.");
        factCache.add("Did you know? A day on Venus is longer than a year on Venus.");
        factCache.add("Did you know? Octopuses have three hearts.");
        factCache.add("Did you know? Honey never spoils. Archaeologists have found pots of honey in ancient Egyptian tombs that are over 3,000 years old and still perfectly good to eat.");
        factCache.add("Did you know? A bolt of lightning is five times hotter than the surface of the sun.");
        factCache.add("Did you know? The shortest war in history was between Britain and Zanzibar on August 27, 1896. Zanzibar surrendered after 38 minutes.");
        factCache.add("Did you know? A group of flamingos is called a 'flamboyance'.");
        factCache.add("Did you know? Cows have best friends and get stressed when they are separated.");
        factCache.add("Did you know? The fingerprints of koalas are so similar to humans that they have on occasion been confused at crime scenes.");
        factCache.add("Did you know? The Hawaiian alphabet has only 12 letters.");
        factCache.add("Did you know? The average person will spend six months of their life waiting for red lights to turn green.");
        factCache.add("Did you know? A day on Mercury is twice as long as its year. Mercury rotates very slowly but completes an orbit around the sun in just 88 Earth days.");
        factCache.add("Did you know? Bananas are berries, but strawberries aren't.");
        factCache.add("Did you know? An ostrich's eye is bigger than its brain.");
        factCache.add("Did you know? The word 'nerd' was first coined by Dr. Seuss in 'If I Ran the Zoo' in 1950.");
        factCache.add("Did you know? The Great Barrier Reef is the largest living structure on Earth and can be seen from space.");
        factCache.add("Did you know? Dolphins have names for each other and will respond when they hear their signature whistle.");
        factCache.add("Did you know? A hummingbird weighs less than a penny.");
        factCache.add("Did you know? The shortest commercial flight in the world is between the Scottish islands of Westray and Papa Westray, with a flight time of just under two minutes.");
        factCache.add("Did you know? The dictionary you looked up was powered by API Ninjas, just like these facts!");
    }
    
    /**
     * Get a fact suggestion for a new post
     * @return A string to be used as a suggested post title or content
     */
    public static String getFactSuggestion() {
        try {
            return getRandomFact();
        } catch (Exception e) {
            System.err.println("Error getting fact suggestion: " + e.getMessage());
            return "Share an interesting fact you recently learned!";
        }
    }
} 