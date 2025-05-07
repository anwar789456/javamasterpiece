package utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Utility class to manage timer for break reminders
 */
public class BreakTimer {
    private Timer timer;
    private long timeInMillis;
    private Consumer<Void> onComplete;
    private boolean isRunning;
    private long startTime;
    private long remainingTime;
    private Consumer<String> timeUpdateCallback;

    /**
     * Creates a new break timer
     * 
     * @param hours Hours component of the timer
     * @param minutes Minutes component of the timer
     * @param seconds Seconds component of the timer
     * @param onComplete Callback function to execute when timer completes
     * @param timeUpdateCallback Callback to update the timer display
     */
    public BreakTimer(int hours, int minutes, int seconds, Consumer<Void> onComplete, Consumer<String> timeUpdateCallback) {
        this.timeInMillis = TimeUnit.HOURS.toMillis(hours) + 
                           TimeUnit.MINUTES.toMillis(minutes) +
                           TimeUnit.SECONDS.toMillis(seconds);
        this.remainingTime = this.timeInMillis;
        this.onComplete = onComplete;
        this.isRunning = false;
        this.timeUpdateCallback = timeUpdateCallback;
    }

    /**
     * Starts or resumes the timer
     */
    public void start() {
        if (isRunning) return;
        
        isRunning = true;
        timer = new Timer(true); // Create as daemon timer
        startTime = System.currentTimeMillis();
        
        // Schedule a task to run every second to update the UI
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isRunning) return;
                
                long elapsedTime = System.currentTimeMillis() - startTime;
                remainingTime = timeInMillis - elapsedTime;
                
                if (remainingTime <= 0) {
                    stop();
                    Platform.runLater(() -> {
                        if (onComplete != null) {
                            onComplete.accept(null);
                        }
                    });
                } else {
                    // Update UI with remaining time
                    Platform.runLater(() -> updateTimeDisplay());
                }
            }
        }, 0, 1000);
    }
    
    /**
     * Stops the timer
     */
    public void stop() {
        if (!isRunning) return;
        
        isRunning = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    
    /**
     * Restarts the timer with the original time
     */
    public void restart() {
        stop();
        remainingTime = timeInMillis;
        updateTimeDisplay();
        start();
    }
    
    /**
     * Pauses the timer
     */
    public void pause() {
        if (!isRunning) return;
        
        isRunning = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    
    /**
     * Checks if the timer is currently running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Gets the remaining time in milliseconds
     */
    public long getRemainingTime() {
        return remainingTime;
    }
    
    /**
     * Updates the time display in the UI
     */
    private void updateTimeDisplay() {
        long hours = TimeUnit.MILLISECONDS.toHours(remainingTime);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTime) % 60;
        
        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        
        if (timeUpdateCallback != null) {
            timeUpdateCallback.accept(timeString);
        }
    }
    
    /**
     * Shows a break reminder notification
     * 
     * @return True if the user clicked "Continue Learning", false if they clicked "Take Break"
     */
    public static boolean showBreakReminder() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Break Reminder");
        alert.setHeaderText("Time to take a break!");
        alert.setContentText("You've been studying for a while. Take a short break to refresh your mind.");
        
        ButtonType continueBtn = new ButtonType("Continue Learning", ButtonBar.ButtonData.OK_DONE);
        ButtonType breakBtn = new ButtonType("Take Break", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(breakBtn, continueBtn);
        
        ButtonType result = alert.showAndWait().orElse(breakBtn);
        return result == continueBtn;
    }
} 