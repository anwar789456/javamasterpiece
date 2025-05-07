package org.example.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for filtering profanity in user-generated content
 */
public class ProfanityFilter {
    
    // Common bad words list - this should be expanded in a real application
    private static final Set<String> BAD_WORDS = new HashSet<>(Arrays.asList(
        "badword", "profanity", "offensive", "explicit", "inappropriate", 
        "vulgar", "obscene", "curse", "swear", "rude"
    ));
    
    // Cache the patterns for better performance
    private static Pattern wordPattern;
    
    static {
        // Create a regex pattern that matches whole words only
        StringBuilder patternBuilder = new StringBuilder("\\b(");
        int count = 0;
        for (String word : BAD_WORDS) {
            if (count > 0) {
                patternBuilder.append("|");
            }
            patternBuilder.append(Pattern.quote(word));
            count++;
        }
        patternBuilder.append(")\\b");
        wordPattern = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }
    
    /**
     * Filter potentially offensive words from a string, replacing them with asterisks
     * @param input The string to filter
     * @return The filtered string with bad words replaced by asterisks
     */
    public static String filter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        Matcher matcher = wordPattern.matcher(input);
        StringBuilder result = new StringBuilder(input);
        
        // Track offset as we replace text
        int offset = 0;
        
        while (matcher.find()) {
            int start = matcher.start() + offset;
            int end = matcher.end() + offset;
            String badWord = result.substring(start, end);
            
            // Replace with asterisks of same length
            String replacement = "*".repeat(badWord.length());
            
            result.replace(start, end, replacement);
            
            // No offset change since replacement is same length
        }
        
        return result.toString();
    }
    
    /**
     * Check if a string contains any bad words
     * @param input The string to check
     * @return true if the string contains profanity, false otherwise
     */
    public static boolean containsProfanity(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        Matcher matcher = wordPattern.matcher(input);
        return matcher.find();
    }
    
    /**
     * Add a custom bad word to the filter
     * @param word The word to add to the filter
     */
    public static void addBadWord(String word) {
        if (word != null && !word.isEmpty()) {
            BAD_WORDS.add(word.toLowerCase());
            // Rebuild the pattern
            rebuildPattern();
        }
    }
    
    /**
     * Add multiple bad words to the filter
     * @param words The words to add to the filter
     */
    public static void addBadWords(String[] words) {
        if (words != null && words.length > 0) {
            for (String word : words) {
                if (word != null && !word.isEmpty()) {
                    BAD_WORDS.add(word.toLowerCase());
                }
            }
            // Rebuild the pattern once after adding all words
            rebuildPattern();
        }
    }
    
    /**
     * Rebuild the regex pattern after modifying the bad words list
     */
    private static void rebuildPattern() {
        StringBuilder patternBuilder = new StringBuilder("\\b(");
        int count = 0;
        for (String word : BAD_WORDS) {
            if (count > 0) {
                patternBuilder.append("|");
            }
            patternBuilder.append(Pattern.quote(word));
            count++;
        }
        patternBuilder.append(")\\b");
        wordPattern = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }
} 