package org.example.utils;

/**
 * Classe utilitaire pour la validation des entrées utilisateur
 */
public class InputValidator {

    /**
     * Valide le titre d'une publication
     * @param title Titre à valider
     * @return Résultat de validation avec message d'erreur si invalide
     */
    public static ValidationResult validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return new ValidationResult(false, "Le titre ne peut pas être vide");
        }
        
        title = title.trim();
        
        if (title.length() < 5) {
            return new ValidationResult(false, "Le titre doit contenir au moins 5 caractères");
        }
        
        if (title.length() > 100) {
            return new ValidationResult(false, "Le titre ne peut pas dépasser 100 caractères");
        }
        
        if (!title.matches("^[a-zA-Z0-9\\sÀ-ÿ\\.,\\?!;:'\"-_\\(\\)]+$")) {
            return new ValidationResult(false, "Le titre contient des caractères non autorisés");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Valide le contenu d'une publication
     * @param content Contenu à valider
     * @return Résultat de validation avec message d'erreur si invalide
     */
    public static ValidationResult validatePostContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ValidationResult(false, "Le contenu ne peut pas être vide");
        }
        
        content = content.trim();
        
        if (content.length() < 10) {
            return new ValidationResult(false, "Le contenu doit contenir au moins 10 caractères");
        }
        
        if (content.length() > 5000) {
            return new ValidationResult(false, "Le contenu ne peut pas dépasser 5000 caractères");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Valide un tag individuel
     * @param tag Tag à valider
     * @return Résultat de validation avec message d'erreur si invalide
     */
    public static ValidationResult validateTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return new ValidationResult(false, "Le tag ne peut pas être vide");
        }
        
        tag = tag.trim();
        
        if (tag.length() < 2) {
            return new ValidationResult(false, "Le tag doit contenir au moins 2 caractères");
        }
        
        if (tag.length() > 20) {
            return new ValidationResult(false, "Le tag ne peut pas dépasser 20 caractères");
        }
        
        if (!tag.matches("^[a-zA-Z0-9_-]+$")) {
            return new ValidationResult(false, "Le tag ne peut contenir que des lettres, chiffres, tirets et underscores");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Valide une liste de tags séparés par des virgules
     * @param tagsString Chaîne de tags à valider
     * @return Résultat de validation avec message d'erreur si invalide
     */
    public static ValidationResult validateTags(String tagsString) {
        if (tagsString == null || tagsString.trim().isEmpty()) {
            return new ValidationResult(true, null); // Tags optionnels
        }
        
        String[] tags = tagsString.split(",");
        
        if (tags.length > 10) {
            return new ValidationResult(false, "Le nombre maximum de tags est de 10");
        }
        
        for (String tag : tags) {
            ValidationResult tagResult = validateTag(tag);
            if (!tagResult.isValid()) {
                return tagResult;
            }
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Valide le contenu d'un commentaire
     * @param content Contenu du commentaire à valider
     * @return Résultat de validation avec message d'erreur si invalide
     */
    public static ValidationResult validateCommentContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ValidationResult(false, "Le commentaire ne peut pas être vide");
        }
        
        content = content.trim();
        
        if (content.length() < 2) {
            return new ValidationResult(false, "Le commentaire doit contenir au moins 2 caractères");
        }
        
        if (content.length() > 1000) {
            return new ValidationResult(false, "Le commentaire ne peut pas dépasser 1000 caractères");
        }
        
        if (!content.matches("^[a-zA-Z0-9\\sÀ-ÿ\\.,\\?!;:'\"-_\\(\\)\\[\\]]+$")) {
            return new ValidationResult(false, "Le commentaire contient des caractères non autorisés");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Classe pour représenter le résultat d'une validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
} 