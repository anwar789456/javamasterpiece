package org.example.utils;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * Utility class for creating emoji selectors for text input controls
 */
public class EmojiSelector {
    // Common emoji groups
    private static final String[] FACES = {"😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "😊", "😇", "🥰", "😍", "🤩", "😘", "😚", "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐", "🤨", "😐", "😑", "😶", "😏"};
    private static final String[] EMOTIONS = {"😒", "🙄", "😬", "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "😎", "🤓", "🧐", "😕", "😟", "🙁", "☹️", "😮", "😯", "😲"};
    private static final String[] NEGATIVE = {"😳", "😦", "😧", "😨", "😰", "😥", "😢", "😭", "😱", "😖", "😣", "😞", "😓", "😩", "😫", "🥱", "😤", "😡", "😠", "🤬", "😈", "👿", "💀", "☠️", "💩", "🤡", "👹", "👺", "👻", "👽", "👾", "🤖"};
    private static final String[] GESTURES = {"👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "💪"};
    private static final String[] SYMBOLS = {"❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "☮️", "✝️", "☪️", "🕉️", "☸️", "✡️", "🔯", "🕎", "☯️", "☢️", "⚛️"};
    private static final String[] ANIMALS = {"🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🙈", "🙉", "🙊", "🐒", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🪱", "🐛", "🦋"};
    private static final String[] FOOD = {"🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🧄", "🧅", "🥔", "🍟", "🍕", "🌭"};
    private static final String[] OBJECTS = {"⌚", "📱", "💻", "⌨️", "🖥️", "🖨️", "🖱️", "🖲️", "📷", "🎥", "🎞️", "📞", "☎️", "📟", "📠", "📺", "📻", "🎙️", "🎚️", "🎛️", "🧭", "⏱️", "⏲️", "⏰", "🕰️", "⌛", "⏳", "📡", "🔋", "🔌", "💡", "🔦"};
    
    /**
     * Creates an emoji selector for a text input control
     * 
     * @param textInput The text input control (TextField, TextArea) to receive emojis
     * @return A FlowPane containing emoji buttons
     */
    public static Node createEmojiSelector(TextInputControl textInput) {
        VBox container = new VBox(5);
        container.setPadding(new Insets(5));
        container.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Label titleLabel = new Label("Emoji");
        titleLabel.setFont(Font.font("System", 12));
        titleLabel.setStyle("-fx-text-fill: #5e35b1; -fx-font-weight: bold;");
        
        FlowPane facesPane = createEmojiGroup(FACES, textInput);
        FlowPane emotionsPane = createEmojiGroup(EMOTIONS, textInput);
        FlowPane negativePane = createEmojiGroup(NEGATIVE, textInput);
        FlowPane gesturesPane = createEmojiGroup(GESTURES, textInput);
        FlowPane symbolsPane = createEmojiGroup(SYMBOLS, textInput);
        FlowPane animalsPane = createEmojiGroup(ANIMALS, textInput);
        FlowPane foodPane = createEmojiGroup(FOOD, textInput);
        FlowPane objectsPane = createEmojiGroup(OBJECTS, textInput);
        
        // Create category labels
        Label facesLabel = new Label("Faces");
        facesLabel.setStyle("-fx-text-fill: #757575; -fx-font-style: italic; -fx-font-size: 10;");
        
        // Add all components
        container.getChildren().addAll(
            titleLabel, 
            facesLabel, facesPane,
            createCategoryLabel("Emotions"), emotionsPane,
            createCategoryLabel("Negative"), negativePane,
            createCategoryLabel("Gestures"), gesturesPane,
            createCategoryLabel("Symbols"), symbolsPane,
            createCategoryLabel("Animals"), animalsPane,
            createCategoryLabel("Food"), foodPane,
            createCategoryLabel("Objects"), objectsPane
        );
        
        return container;
    }
    
    private static Label createCategoryLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #757575; -fx-font-style: italic; -fx-font-size: 10;");
        return label;
    }
    
    private static FlowPane createEmojiGroup(String[] emojis, TextInputControl textInput) {
        FlowPane pane = new FlowPane();
        pane.setHgap(2);
        pane.setVgap(2);
        pane.setPrefWrapLength(200);
        
        for (String emoji : emojis) {
            Button emojiButton = new Button(emoji);
            emojiButton.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand;");
            emojiButton.setFont(Font.font("System", 14));
            
            // Set tooltip with emoji
            Tooltip tooltip = new Tooltip(emoji);
            tooltip.setFont(Font.font("System", 14));
            emojiButton.setTooltip(tooltip);
            
            // Insert emoji at cursor position when clicked
            emojiButton.setOnAction(e -> {
                int caretPosition = textInput.getCaretPosition();
                textInput.insertText(caretPosition, emoji);
                // Request focus back to text input after emoji insertion
                textInput.requestFocus();
                textInput.positionCaret(caretPosition + emoji.length());
            });
            
            pane.getChildren().add(emojiButton);
        }
        
        return pane;
    }
} 