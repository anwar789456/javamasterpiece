package org.example.utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that suggests better title alternatives for forum posts based on content analysis.
 * Uses NLP-like techniques to extract key phrases and generate engaging titles.
 */
public class AiTitleSuggester {
    
    // Keywords that indicate a question post
    private static final Set<String> QUESTION_INDICATORS = new HashSet<>(Arrays.asList(
        "how", "what", "why", "when", "where", "who", "which", "can", "could", "should", "would", "is", "are", "do", "does"
    ));
    
    // Keywords that indicate a problem post
    private static final Set<String> PROBLEM_INDICATORS = new HashSet<>(Arrays.asList(
        "problem", "issue", "error", "bug", "fail", "crash", "trouble", "help", "stuck", "can't", "cannot", "doesn't", "won't", "broken"
    ));
    
    // Keywords that indicate a sharing post
    private static final Set<String> SHARING_INDICATORS = new HashSet<>(Arrays.asList(
        "created", "built", "made", "developed", "implemented", "designed", "discovered", "tutorial", "guide", "howto", "tip", "trick", "share"
    ));
    
    // Keywords that make titles more engaging
    private static final List<String> ENGAGING_PREFIXES = Arrays.asList(
        "The Ultimate Guide to", 
        "How to Master", 
        "Quick Tips for", 
        "The Secret of", 
        "Top 5 Ways to", 
        "Essential Guide to",
        "Solving", 
        "Mastering",
        "Understanding"
    );
    
    // Common stopwords to filter out from content analysis
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        "the", "a", "an", "and", "or", "but", "is", "are", "was", "were", "be", "been",
        "have", "has", "had", "do", "does", "did", "to", "from", "in", "out", "on", "off", "over", "under",
        "again", "further", "then", "once", "here", "there", "when", "where", "why", "how",
        "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor",
        "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don",
        "should", "now", "also", "new", "one", "two", "like", "get", "make", "know", "take",
        "our", "we", "us", "my", "me", "i", "you", "your", "him", "his", "her", "she", "they", "them", "their",
        "about", "before", "after", "above", "below", "between", "into", "through", "during", "with", "without",
        "for", "of", "at", "by", "up", "down", "this", "that", "these", "those", "am", "im", "its", "it's", "it"
    ));
    
    /**
     * Generate AI title suggestions based on post content
     * @param content The post content
     * @param originalTitle The original title (optional)
     * @param tags Tags associated with the post (optional)
     * @return List of suggested titles
     */
    public static List<String> suggestTitles(String content, String originalTitle, String tags) {
        List<String> suggestions = new ArrayList<>();
        
        // Ensure we have content to work with
        if (content == null || content.trim().isEmpty()) {
            return suggestions;
        }
        
        try {
            // Clean and prepare the content - remove special characters but preserve sentence structure
            String cleanContent = content.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s.,?!]", " ") // Keep basic punctuation
                .replaceAll("\\s+", " ")
                .trim();
            
            // Break content into words for analysis
            List<String> words = Arrays.asList(cleanContent.split("\\s+"));
            
            // Also break into sentences to prioritize the first few sentences
            List<String> sentences = Arrays.asList(cleanContent.split("[.!?]+"));
            
            // Extract key phrases with higher weight to first paragraph
            List<String> keyPhrases = extractKeyPhrases(words, sentences);
            
            // If we couldn't extract good phrases, try a simpler approach
            if (keyPhrases.isEmpty() && !words.isEmpty()) {
                // Just take important words
                keyPhrases = words.stream()
                    .filter(w -> w.length() > 3 && !STOPWORDS.contains(w))
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());
            }
            
            // Determine post type based on content analysis
            PostType postType = determinePostType(words, originalTitle);
            
            // Generate suggestions based on post type
            switch(postType) {
                case QUESTION:
                    suggestions.addAll(generateQuestionTitles(keyPhrases, originalTitle, tags));
                    break;
                case PROBLEM:
                    suggestions.addAll(generateProblemTitles(keyPhrases, originalTitle, tags));
                    break;
                case SHARING:
                    suggestions.addAll(generateSharingTitles(keyPhrases, originalTitle, tags));
                    break;
                case DISCUSSION:
                    suggestions.addAll(generateDiscussionTitles(keyPhrases, originalTitle, tags));
                    break;
            }
            
            // Add some general engaging titles
            suggestions.addAll(generateEngagingTitles(keyPhrases, tags));
            
            // If we have the original title, try to improve it
            if (originalTitle != null && !originalTitle.trim().isEmpty()) {
                suggestions.addAll(improveOriginalTitle(originalTitle, keyPhrases));
            }
            
            // Filter out duplicates, null/empty values, and ensure proper length
            return suggestions.stream()
                    .filter(Objects::nonNull)
                    .filter(s -> !s.trim().isEmpty())
                    .filter(s -> s.length() <= 100) // Keep titles under reasonable length
                    .distinct()
                    .limit(5) // Return top 5 suggestions
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Log the error
            System.err.println("Error generating title suggestions: " + e.getMessage());
            // Return empty list in case of error
            return new ArrayList<>();
        }
    }
    
    private static List<String> extractKeyPhrases(List<String> words, List<String> sentences) {
        // Separate frequency maps for different parts of the content
        Map<String, Integer> phraseFrequency = new HashMap<>();
        
        // Process all words first
        processWords(words, phraseFrequency);
        
        // Process first 3 sentences with higher weight
        List<String> importantSentences = sentences.isEmpty() ? 
            Collections.emptyList() : 
            sentences.subList(0, Math.min(3, sentences.size()));
        
        for (String sentence : importantSentences) {
            // Split sentence into words
            List<String> sentenceWords = Arrays.asList(sentence.trim().split("\\s+"));
            // Process with higher weight (multiplier of 2)
            processWords(sentenceWords, phraseFrequency, 2);
        }
        
        // Sort by frequency and get top phrases
        return phraseFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(15) // Get more phrases to have better variety
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    private static void processWords(List<String> words, Map<String, Integer> phraseFrequency) {
        processWords(words, phraseFrequency, 1);
    }
    
    private static void processWords(List<String> words, Map<String, Integer> phraseFrequency, int weightMultiplier) {
        // Skip if there are too few words
        if (words.size() < 2) return;
        
        // Count single important words
        for (String word : words) {
            if (word.length() > 3 && !STOPWORDS.contains(word)) {
                phraseFrequency.put(word, 
                    phraseFrequency.getOrDefault(word, 0) + (1 * weightMultiplier));
            }
        }
        
        // Extract 2-word phrases
        for (int i = 0; i < words.size() - 1; i++) {
            String word1 = words.get(i);
            String word2 = words.get(i+1);
            
            if (word1.length() > 2 && word2.length() > 2 && 
                !STOPWORDS.contains(word1) && !STOPWORDS.contains(word2)) {
                String phrase = word1 + " " + word2;
                phraseFrequency.put(phrase, 
                    phraseFrequency.getOrDefault(phrase, 0) + (3 * weightMultiplier)); // Higher weight
            }
        }
        
        // Extract 3-word phrases
        for (int i = 0; i < words.size() - 2; i++) {
            String word1 = words.get(i);
            String word2 = words.get(i+1);
            String word3 = words.get(i+2);
            
            if (word1.length() > 2 && word2.length() > 2 && word3.length() > 2 && 
                !STOPWORDS.contains(word1) && !STOPWORDS.contains(word2) && !STOPWORDS.contains(word3)) {
                String phrase = word1 + " " + word2 + " " + word3;
                phraseFrequency.put(phrase, 
                    phraseFrequency.getOrDefault(phrase, 0) + (5 * weightMultiplier)); // Even higher weight
            }
        }
    }
    
    private static PostType determinePostType(List<String> words, String originalTitle) {
        // Count indicators for each type
        int questionScore = 0;
        int problemScore = 0;
        int sharingScore = 0;
        
        // Check content - limit to first 200 words for performance
        List<String> limitedWords = words.size() > 200 ? 
            words.subList(0, 200) : words;
            
        for (String word : limitedWords) {
            if (QUESTION_INDICATORS.contains(word)) {
                questionScore++;
            }
            if (PROBLEM_INDICATORS.contains(word)) {
                problemScore++;
            }
            if (SHARING_INDICATORS.contains(word)) {
                sharingScore++;
            }
        }
        
        // Count question marks in content (strong question indicator)
        long questionMarkCount = words.stream().filter(w -> w.endsWith("?")).count();
        questionScore += questionMarkCount * 2;
        
        // Check original title if provided
        if (originalTitle != null && !originalTitle.isEmpty()) {
            String lowerTitle = originalTitle.toLowerCase();
            for (String word : lowerTitle.split("\\s+")) {
                if (QUESTION_INDICATORS.contains(word)) {
                    questionScore += 2; // Give more weight to title words
                }
                if (PROBLEM_INDICATORS.contains(word)) {
                    problemScore += 2;
                }
                if (SHARING_INDICATORS.contains(word)) {
                    sharingScore += 2;
                }
            }
            
            // Check if title ends with a question mark
            if (originalTitle.endsWith("?")) {
                questionScore += 3;
            }
        }
        
        // Determine type based on highest score
        int maxScore = Math.max(questionScore, Math.max(problemScore, sharingScore));
        
        if (maxScore == 0) {
            return PostType.DISCUSSION; // Default type
        } else if (maxScore == questionScore) {
            return PostType.QUESTION;
        } else if (maxScore == problemScore) {
            return PostType.PROBLEM;
        } else {
            return PostType.SHARING;
        }
    }
    
    private static List<String> generateQuestionTitles(List<String> keyPhrases, String originalTitle, String tags) {
        List<String> suggestions = new ArrayList<>();
        
        if (!keyPhrases.isEmpty()) {
            String mainPhrase = capitalizeFirstLetter(keyPhrases.get(0));
            String secondPhrase = keyPhrases.size() > 1 ? keyPhrases.get(1) : "";
            
            suggestions.add("How to " + mainPhrase + "?");
            suggestions.add("What's the best way to " + mainPhrase + "?");
            
            if (!secondPhrase.isEmpty()) {
                suggestions.add("How can I " + mainPhrase + " when dealing with " + secondPhrase + "?");
            }
            
            // Additional question variations
            if (keyPhrases.size() > 2) {
                String thirdPhrase = keyPhrases.get(2);
                suggestions.add("Need help understanding " + mainPhrase + " for " + thirdPhrase);
            } else {
                suggestions.add("Need help understanding " + mainPhrase);
            }
            
            suggestions.add("Can someone explain " + mainPhrase + "?");
            suggestions.add("What are the steps for " + mainPhrase + "?");
        }
        
        // If we have tags, use them in suggestions
        if (tags != null && !tags.isEmpty()) {
            String[] tagArray = tags.split(",");
            if (tagArray.length > 0) {
                String mainTag = tagArray[0].trim();
                suggestions.add("Question about " + capitalizeFirstLetter(mainTag) + ": " + 
                        (keyPhrases.isEmpty() ? "need help" : keyPhrases.get(0)));
            }
        }
        
        return suggestions;
    }
    
    private static List<String> generateProblemTitles(List<String> keyPhrases, String originalTitle, String tags) {
        List<String> suggestions = new ArrayList<>();
        
        if (!keyPhrases.isEmpty()) {
            String mainPhrase = capitalizeFirstLetter(keyPhrases.get(0));
            String secondPhrase = keyPhrases.size() > 1 ? keyPhrases.get(1) : "";
            
            suggestions.add("Solving " + mainPhrase + " problem");
            suggestions.add("Troubleshooting: " + mainPhrase);
            
            if (!secondPhrase.isEmpty()) {
                suggestions.add(mainPhrase + " issue with " + secondPhrase);
            }
            
            suggestions.add("Help needed: " + mainPhrase + " not working");
            suggestions.add("Bug fix for " + mainPhrase);
            suggestions.add("How to resolve " + mainPhrase + " error");
        }
        
        // Use tags
        if (tags != null && !tags.isEmpty()) {
            String[] tagArray = tags.split(",");
            if (tagArray.length > 0) {
                String mainTag = tagArray[0].trim();
                suggestions.add(capitalizeFirstLetter(mainTag) + " error: " + 
                        (keyPhrases.isEmpty() ? "troubleshooting help" : keyPhrases.get(0)));
            }
        }
        
        return suggestions;
    }
    
    private static List<String> generateSharingTitles(List<String> keyPhrases, String originalTitle, String tags) {
        List<String> suggestions = new ArrayList<>();
        
        if (!keyPhrases.isEmpty()) {
            String mainPhrase = capitalizeFirstLetter(keyPhrases.get(0));
            String secondPhrase = keyPhrases.size() > 1 ? keyPhrases.get(1) : "";
            
            suggestions.add("I created " + mainPhrase + " - here's how");
            suggestions.add("Tutorial: Building " + mainPhrase);
            
            if (!secondPhrase.isEmpty()) {
                suggestions.add("How I combined " + mainPhrase + " with " + secondPhrase);
            }
            
            suggestions.add("Sharing my approach to " + mainPhrase);
            suggestions.add("New way to implement " + mainPhrase);
            suggestions.add("A practical guide to " + mainPhrase);
        }
        
        // Use tags
        if (tags != null && !tags.isEmpty()) {
            String[] tagArray = tags.split(",");
            if (tagArray.length > 0) {
                String mainTag = tagArray[0].trim();
                suggestions.add(capitalizeFirstLetter(mainTag) + " project: " + 
                        (keyPhrases.isEmpty() ? "my implementation" : keyPhrases.get(0)));
            }
        }
        
        return suggestions;
    }
    
    private static List<String> generateDiscussionTitles(List<String> keyPhrases, String originalTitle, String tags) {
        List<String> suggestions = new ArrayList<>();
        
        if (!keyPhrases.isEmpty()) {
            String mainPhrase = capitalizeFirstLetter(keyPhrases.get(0));
            String secondPhrase = keyPhrases.size() > 1 ? keyPhrases.get(1) : "";
            
            suggestions.add("Let's discuss " + mainPhrase);
            suggestions.add("Thoughts on " + mainPhrase + "?");
            
            if (!secondPhrase.isEmpty()) {
                suggestions.add(mainPhrase + " vs " + secondPhrase + ": your opinions?");
            }
            
            suggestions.add("The future of " + mainPhrase);
            suggestions.add("What do you think about " + mainPhrase + "?");
            suggestions.add("Exploring " + mainPhrase + " in depth");
        }
        
        // Use tags
        if (tags != null && !tags.isEmpty()) {
            String[] tagArray = tags.split(",");
            if (tagArray.length > 0) {
                String mainTag = tagArray[0].trim();
                suggestions.add(capitalizeFirstLetter(mainTag) + " discussion: " + 
                        (keyPhrases.isEmpty() ? "sharing ideas" : keyPhrases.get(0)));
            }
        }
        
        return suggestions;
    }
    
    private static List<String> generateEngagingTitles(List<String> keyPhrases, String tags) {
        List<String> suggestions = new ArrayList<>();
        
        if (!keyPhrases.isEmpty()) {
            String mainPhrase = keyPhrases.get(0);
            
            // Pick several engaging prefixes randomly
            Random random = new Random();
            List<String> shuffledPrefixes = new ArrayList<>(ENGAGING_PREFIXES);
            Collections.shuffle(shuffledPrefixes, random);
            
            for (int i = 0; i < Math.min(3, shuffledPrefixes.size()); i++) {
                suggestions.add(shuffledPrefixes.get(i) + " " + mainPhrase);
            }
            
            // Add some titles with multiple key phrases if available
            if (keyPhrases.size() > 2) {
                suggestions.add("From " + keyPhrases.get(1) + " to " + keyPhrases.get(2) + ": A Complete Guide");
            }
        }
        
        // Incorporate tags for more context-specific engaging titles
        if (tags != null && !tags.isEmpty() && !keyPhrases.isEmpty()) {
            String[] tagArray = tags.split(",");
            if (tagArray.length > 0) {
                String mainTag = capitalizeFirstLetter(tagArray[0].trim());
                suggestions.add(mainTag + ": The Complete Guide to " + keyPhrases.get(0));
            }
        }
        
        return suggestions;
    }
    
    private static List<String> improveOriginalTitle(String originalTitle, List<String> keyPhrases) {
        List<String> improvements = new ArrayList<>();
        
        // Skip if original title is too short to improve
        if (originalTitle.length() < 5) {
            return improvements;
        }
        
        // Get cleaned version of original title
        String cleanTitle = originalTitle.trim();
        boolean endsWithQuestion = cleanTitle.endsWith("?");
        
        // Remove excessive punctuation
        cleanTitle = cleanTitle.replaceAll("\\s*[!.]+$", "");
        if (endsWithQuestion) {
            cleanTitle = cleanTitle.replaceAll("\\s*\\?+$", "");
        }
        
        // Make the title more specific with key phrases
        if (!keyPhrases.isEmpty() && !cleanTitle.contains(keyPhrases.get(0))) {
            improvements.add(cleanTitle + ": " + capitalizeFirstLetter(keyPhrases.get(0)) + 
                (endsWithQuestion ? "?" : ""));
        }
        
        // Make the title more concise if it's too long
        if (cleanTitle.length() > 50) {
            String[] titleWords = cleanTitle.split("\\s+");
            if (titleWords.length > 8) {
                String shorterTitle = String.join(" ", Arrays.copyOfRange(titleWords, 0, 8));
                improvements.add(shorterTitle + (endsWithQuestion ? "?" : ""));
            }
        }
        
        // Add specificity if the title is generic
        List<String> genericPhrases = Arrays.asList("help me", "question", "problem", "issue", "need help");
        boolean isGeneric = genericPhrases.stream().anyMatch(cleanTitle.toLowerCase()::contains);
        
        if (isGeneric && !keyPhrases.isEmpty()) {
            improvements.add("Specific " + cleanTitle + " about " + keyPhrases.get(0) + 
                (endsWithQuestion ? "?" : ""));
        }
        
        return improvements;
    }
    
    private static String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
    
    private enum PostType {
        QUESTION,
        PROBLEM,
        SHARING,
        DISCUSSION
    }
} 