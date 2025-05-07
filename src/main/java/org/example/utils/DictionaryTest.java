package org.example.utils;

/**
 * Simple test class for the DictionaryService
 */
public class DictionaryTest {
    public static void main(String[] args) {
        // Test the dictionary service
        try {
            System.out.println("Testing DictionaryService...");
            
            // Test a valid word
            String testWord = "code";
            System.out.println("\nTesting word: '" + testWord + "'");
            System.out.println(DictionaryService.getDefinition(testWord));
            
            // Test another valid word
            String testWord2 = "programming";
            System.out.println("\nTesting word: '" + testWord2 + "'");
            System.out.println(DictionaryService.getDefinition(testWord2));
            
            // Test a non-existent word
            String nonExistentWord = "xyzabc";
            System.out.println("\nTesting non-existent word: '" + nonExistentWord + "'");
            System.out.println(DictionaryService.getDefinition(nonExistentWord));
            
            System.out.println("\nAll tests completed!");
        } catch (Exception e) {
            System.err.println("Error running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 