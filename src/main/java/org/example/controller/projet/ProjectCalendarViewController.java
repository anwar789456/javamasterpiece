package org.example.controller.projet;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.collections.FXCollections;
import javafx.beans.value.ChangeListener;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProjectCalendarViewController {
    @FXML private Label monthLabel;
    @FXML private GridPane calendarGrid;
    @FXML private Button prevMonthBtn;
    @FXML private Button nextMonthBtn;
    @FXML private ComboBox<String> projectListToggle;
    @FXML private ListView<String> projectListView;
    @FXML private ComboBox<String> statusFilter;

    private YearMonth currentMonth = YearMonth.now();
    private List<Project> projects = new ArrayList<>();
    private Project selectedProject = null;

    @FXML
    private void initialize() {
        if (projectListToggle != null) {
            projectListToggle.setItems(FXCollections.observableArrayList("All Projects", "This Month"));
            projectListToggle.setValue("All Projects");
            projectListToggle.setOnAction(e -> updateProjectListView());
        }
        if (projectListView != null) {
            projectListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
                int idx = newVal.intValue();
                Project p = null;
                String mode = projectListToggle.getValue();
                List<Project> filtered = new ArrayList<>();
                if (mode == null || mode.equals("All Projects")) {
                    filtered.addAll(projects);
                } else {
                    for (Project proj : projects) {
                        if (!proj.end.isBefore(currentMonth.atDay(1)) && !proj.start.isAfter(currentMonth.atEndOfMonth())) {
                            filtered.add(proj);
                        }
                    }
                }
                if (idx >= 0 && idx < filtered.size()) {
                    p = filtered.get(idx);
                }
                if (p != null) {
                    selectedProject = p;
                    currentMonth = YearMonth.from(p.start);
                    updateCalendar();
                    updateProjectListView();
                }
            });
        }
        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList("All", "Active", "Completed", "On Hold", "Cancelled"));
            statusFilter.setValue("All");
            statusFilter.setOnAction(e -> {
                updateCalendar();
                updateProjectListView();
            });
        }
        updateCalendar();
        updateProjectListView();
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
        updateCalendar();
        updateProjectListView();
    }

    @FXML
    private void showPrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        updateCalendar();
        updateProjectListView();
    }

    @FXML
    private void showNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        updateCalendar();
        updateProjectListView();
    }

    private void updateCalendar() {
        calendarGrid.getChildren().clear();
        monthLabel.setText(currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + currentMonth.getYear());

        // Add day headers
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < days.length; i++) {
            Label dayLabel = new Label(days[i]);
            dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #43e97b; -fx-padding: 5;");
            calendarGrid.add(dayLabel, i, 0);
        }

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int startDay = firstOfMonth.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
        int daysInMonth = currentMonth.lengthOfMonth();
        int row = 1;
        int col = (startDay - 1) % 7;

        // Fill in the days
        StackPane[][] dayCells = new StackPane[6][7];
        for (int day = 1; day <= daysInMonth; day++) {
            StackPane cell = new StackPane();
            cell.setPrefSize(100, 60);
            Label dayNum = new Label(String.valueOf(day));
            dayNum.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #333; -fx-padding: 5;");
            cell.getChildren().add(dayNum);
            calendarGrid.add(cell, col, row);
            dayCells[row - 1][col] = cell;
            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }

        // Filter projects by status
        String statusSelected = (statusFilter != null && statusFilter.getValue() != null) ? statusFilter.getValue() : "All";
        List<Project> filteredProjects = new ArrayList<>();
        for (Project p : projects) {
            if ("All".equalsIgnoreCase(statusSelected) || p.status.equalsIgnoreCase(statusSelected)) {
                filteredProjects.add(p);
            }
        }
        // Draw project bars spanning from start to end date (Gantt-style, one bar per project per row)
        List<Project> toShow = (selectedProject != null) ? List.of(selectedProject) : filteredProjects;
        for (Project p : toShow) {
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();
            // Only show if project overlaps with the current month
            if (p.end.isBefore(monthStart) || p.start.isAfter(monthEnd)) continue;
            LocalDate barStart = p.start.isBefore(monthStart) ? monthStart : p.start;
            LocalDate barEnd = p.end.isAfter(monthEnd) ? monthEnd : p.end;
            int barStartDay = barStart.getDayOfMonth();
            int barEndDay = barEnd.getDayOfMonth();
            int startCol = barStart.getDayOfWeek().getValue() - 1;
            int startRow = 1 + (barStartDay + monthStart.getDayOfWeek().getValue() - 2) / 7;
            int span = 1;
            // Calculate how many days the bar should span in this row
            int daysLeftInRow = 7 - startCol;
            int daysToSpan = barEndDay - barStartDay + 1;
            span = Math.min(daysLeftInRow, daysToSpan);
            // Draw bars row by row if the project spans multiple weeks
            int currentDay = barStartDay;
            int currentCol = startCol;
            int currentRow = startRow;
            int daysRemaining = daysToSpan;
            while (daysRemaining > 0) {
                int thisSpan = Math.min(7 - currentCol, daysRemaining);
                StackPane cell = dayCells[currentRow - 1][currentCol];
                if (cell != null) {
                    Rectangle bar = new Rectangle(90 * thisSpan + 4 * (thisSpan - 1), 28, getStatusColor(p.status));
                    bar.setArcWidth(12); bar.setArcHeight(12);
                    bar.setStyle("-fx-cursor: hand; -fx-effect: dropshadow(gaussian, #22222244, 6, 0.2, 0, 2)" + (p == selectedProject ? "; -fx-stroke: #FFD700; -fx-stroke-width: 4;" : ""));
                    Tooltip tip = new Tooltip(p.name + "\n" + p.status + "\n" + p.start + " - " + p.end + "\n" + p.description);
                    tip.setShowDelay(Duration.millis(100));
                    Tooltip.install(bar, tip);

                    Label nameLabel = new Label(p.name);
                    nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, #222, 3, 0.7, 0, 1); -fx-background-color: transparent" + (p == selectedProject ? "; -fx-underline: true" : ""));
                    nameLabel.setMaxWidth(86 * thisSpan + 4 * (thisSpan - 1));
                    nameLabel.setEllipsisString("…");
                    nameLabel.setWrapText(false);
                    nameLabel.setMouseTransparent(true);
                    Tooltip.install(nameLabel, new Tooltip(p.name));

                    StackPane barStack = new StackPane(bar, nameLabel);
                    barStack.setMaxWidth(90 * thisSpan + 4 * (thisSpan - 1));
                    barStack.setMouseTransparent(false);
                    Tooltip.install(barStack, tip);

                    bar.setOnMouseEntered(e -> bar.setFill(getStatusColorBrighter(p.status)));
                    bar.setOnMouseExited(e -> bar.setFill(getStatusColor(p.status)));

                    calendarGrid.add(barStack, currentCol, currentRow, thisSpan, 1);
                }
                daysRemaining -= thisSpan;
                currentDay += thisSpan;
                currentCol = 0;
                currentRow++;
            }
        }
    }

    private Color getStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "active": return Color.web("#43e97b", 0.8);
            case "completed": return Color.web("#2196F3", 0.7);
            case "on hold": return Color.web("#FFC107", 0.7);
            case "cancelled": return Color.web("#F44336", 0.7);
            default: return Color.web("#888888", 0.6);
        }
    }

    // Helper to get a brighter color for hover effect
    private Color getStatusColorBrighter(String status) {
        switch (status.toLowerCase()) {
            case "active": return Color.web("#6fffa0", 0.95);
            case "completed": return Color.web("#64b5f6", 0.9);
            case "on hold": return Color.web("#ffe082", 0.9);
            case "cancelled": return Color.web("#ff7961", 0.9);
            default: return Color.web("#bbbbbb", 0.8);
        }
    }

    // Helper to truncate project name for bar display
    private String truncateProjectName(String name, int maxLen) {
        if (name.length() <= maxLen) return name;
        return name.substring(0, maxLen - 1) + "…";
    }

    private void updateProjectListView() {
        if (projectListView == null || projectListToggle == null) return;
        String statusSelected = (statusFilter != null && statusFilter.getValue() != null) ? statusFilter.getValue() : "All";
        List<Project> filteredProjects = new ArrayList<>();
        for (Project p : projects) {
            if ("All".equalsIgnoreCase(statusSelected) || p.status.equalsIgnoreCase(statusSelected)) {
                filteredProjects.add(p);
            }
        }
        List<String> displayList = new ArrayList<>();
        String mode = projectListToggle.getValue();
        if (mode == null || mode.equals("All Projects")) {
            for (Project p : filteredProjects) {
                displayList.add("• " + p.name + ": " + p.start + " → " + p.end + " (" + p.status + ")");
            }
        } else { // This Month
            for (Project p : filteredProjects) {
                if (!p.end.isBefore(currentMonth.atDay(1)) && !p.start.isAfter(currentMonth.atEndOfMonth())) {
                    displayList.add("• " + p.name + ": " + p.start + " → " + p.end + " (" + p.status + ")");
                }
            }
        }
        projectListView.setItems(FXCollections.observableArrayList(displayList));
    }

    // Project data class for the calendar (public for passing from main view)
    public static class Project {
        public String name;
        public LocalDate start;
        public LocalDate end;
        public String status;
        public String description;
        public Project(String name, LocalDate start, LocalDate end, String status, String description) {
            this.name = name;
            this.start = start;
            this.end = end;
            this.status = status;
            this.description = description;
        }
    }
} 