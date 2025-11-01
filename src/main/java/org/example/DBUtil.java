package org.example;

import java.sql.*;

public class DBUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/bank_manage?serverTimezone=UTC";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "sumit"; // change if needed

    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println(ANSI_RED + "MySQL driver not found: " + e.getMessage() + ANSI_RESET);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}