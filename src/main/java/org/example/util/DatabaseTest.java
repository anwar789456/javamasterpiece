package org.example.util;

import utils.dataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseTest {
    public static void main(String[] args) {
        try {
            System.out.println("Testing database connection...");
            Connection conn = dataSource.getInstance().getConnection();
            System.out.println("Database connection successful!");

            // Check if the projet table exists
            System.out.println("Checking if projet table exists...");
            PreparedStatement checkTableStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'pidevversion3' AND table_name = 'projet'"
            );
            ResultSet tableRs = checkTableStmt.executeQuery();
            tableRs.next();
            int tableCount = tableRs.getInt(1);

            if (tableCount > 0) {
                System.out.println("projet table exists!");

                // Count projects in the table
                PreparedStatement countStmt = conn.prepareStatement("SELECT COUNT(*) FROM projet");
                ResultSet countRs = countStmt.executeQuery();
                countRs.next();
                int projectCount = countRs.getInt(1);
                System.out.println("Found " + projectCount + " projects in the database.");

                // List all projects
                if (projectCount > 0) {
                    System.out.println("\nProject details:");
                    PreparedStatement listStmt = conn.prepareStatement("SELECT * FROM projet");
                    ResultSet listRs = listStmt.executeQuery();

                    while (listRs.next()) {
                        System.out.println("----------------------------------------");
                        System.out.println("ID: " + listRs.getInt("id"));
                        System.out.println("Name: " + listRs.getString("project_name"));
                        System.out.println("Description: " + listRs.getString("description"));
                        System.out.println("Status: " + listRs.getString("status"));
                        System.out.println("Start Date: " + listRs.getDate("start_date"));
                        System.out.println("End Date: " + listRs.getDate("end_date"));
                    }
                }
            } else {
                System.out.println("projet table does not exist!");
            }

            // Release the connection
            dataSource.getInstance().releaseConnection(conn);
            System.out.println("Database test completed.");

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}