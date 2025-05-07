package org.example.controller.course;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import org.example.models.course.Course;
import org.example.services.StatisticsService;

import java.sql.SQLException;
import java.util.Map;

public class CourseStatisticsController {

    @FXML
    private TabPane statisticsTabPane;
    
    @FXML
    private Tab mostLikedTab;
    
    @FXML
    private Tab mostPopularTab;
    
    @FXML
    private VBox mostLikedChartContainer;
    
    @FXML
    private VBox mostPopularChartContainer;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Button refreshButton;
    
    private StatisticsService statisticsService;
    
    @FXML
    public void initialize() {
        // Initialize services
        statisticsService = new StatisticsService();
        
        // Set up button actions
        refreshButton.setOnAction(event -> loadStatistics());
        
        // Initial load of statistics
        loadStatistics();
    }
    
    /**
     * Loads all statistics and displays them
     */
    private void loadStatistics() {
        statusLabel.setText("Loading statistics...");
        
        try {
            // Load most liked courses chart
            loadMostLikedCoursesChart();
            
            // Load most popular courses chart
            loadMostPopularCoursesChart();
            
            statusLabel.setText("Statistics loaded successfully");
        } catch (SQLException e) {
            statusLabel.setText("Error loading statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Loads and displays the most liked courses in a bar chart
     */
    private void loadMostLikedCoursesChart() throws SQLException {
        // Clear previous charts
        mostLikedChartContainer.getChildren().clear();
        
        // Get most liked courses (top 10)
        Map<Course, Integer> mostLikedCourses = statisticsService.getMostLikedCourses(10);
        
        if (mostLikedCourses.isEmpty()) {
            Label noDataLabel = new Label("No course review data available");
            mostLikedChartContainer.getChildren().add(noDataLabel);
            return;
        }
        
        // Create a bar chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        
        // Setup chart
        barChart.setTitle("Most Liked Courses");
        xAxis.setLabel("Course");
        yAxis.setLabel("Number of Likes");
        
        // Create a series for the data
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Likes");
        
        // Add data to the series
        for (Map.Entry<Course, Integer> entry : mostLikedCourses.entrySet()) {
            Course course = entry.getKey();
            Integer likeCount = entry.getValue();
            
            // Add data point
            series.getData().add(new XYChart.Data<>(course.getTitre(), likeCount));
        }
        
        // Add series to chart
        barChart.getData().add(series);
        
        // Make the chart responsive
        barChart.prefWidthProperty().bind(mostLikedChartContainer.widthProperty());
        barChart.prefHeightProperty().bind(mostLikedChartContainer.heightProperty());
        
        // Add chart to container
        mostLikedChartContainer.getChildren().add(barChart);
    }
    
    /**
     * Loads and displays the most popular courses in a bar chart based on subscription count
     */
    private void loadMostPopularCoursesChart() throws SQLException {
        // Clear previous charts
        mostPopularChartContainer.getChildren().clear();
        
        // Get most popular courses (top 10)
        Map<Course, Integer> mostPopularCourses = statisticsService.getMostPopularCourses(10);
        
        if (mostPopularCourses.isEmpty()) {
            Label noDataLabel = new Label("No course subscription data available");
            mostPopularChartContainer.getChildren().add(noDataLabel);
            return;
        }
        
        // Create a bar chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        
        // Setup chart
        barChart.setTitle("Most Popular Courses");
        xAxis.setLabel("Course");
        yAxis.setLabel("Number of Subscriptions");
        
        // Create a series for the data
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Subscriptions");
        
        // Add data to the series
        for (Map.Entry<Course, Integer> entry : mostPopularCourses.entrySet()) {
            Course course = entry.getKey();
            Integer subscriptionCount = entry.getValue();
            
            // Add data point
            series.getData().add(new XYChart.Data<>(course.getTitre(), subscriptionCount));
        }
        
        // Add series to chart
        barChart.getData().add(series);
        
        // Make the chart responsive
        barChart.prefWidthProperty().bind(mostPopularChartContainer.widthProperty());
        barChart.prefHeightProperty().bind(mostPopularChartContainer.heightProperty());
        
        // Add chart to container
        mostPopularChartContainer.getChildren().add(barChart);
    }
} 