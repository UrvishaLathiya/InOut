package com.example.Import_Export_Data.service;

import com.example.Import_Export_Data.DTO.DestinationDbConfig;
import org.springframework.stereotype.Service;

import java.sql.*;

@Service
public class DatabaseCreationService {

    public boolean createDatabase(DestinationDbConfig config) throws SQLException {
        String systemDb = "postgres"; // default base DB to connect for admin tasks
        String systemUrl = "jdbc:postgresql://localhost:5432/" + systemDb;

        System.out.println("🔁 Connecting to system DB to check/create destination DB...");

        // Step 1: Connect to system DB using user-entered credentials
        try (Connection conn = DriverManager.getConnection(systemUrl, config.getUsername(), config.getPassword())) {
            ResultSet rs = conn.getMetaData().getCatalogs();
            while (rs.next()) {
                if (rs.getString(1).equalsIgnoreCase(config.getDbName())) {
                    System.out.println("⚠️ Database already exists: " + config.getDbName());
                    return false; // Already exists, no need to create
                }
            }

            // Step 2: Create the new DB
            String createDbQuery = "CREATE DATABASE \"" + config.getDbName() + "\"";
            conn.createStatement().executeUpdate(createDbQuery);
            System.out.println("✅ Database created: " + config.getDbName());
        } catch (SQLException e) {
            System.err.println("❌ Failed to create DB or connect to system DB: " + e.getMessage());
            throw e;
        }

        // Step 3: Connect to the newly created DB to create schema
        String newDbUrl = "jdbc:postgresql://localhost:5432/" + config.getDbName();
        try (Connection dbConn = DriverManager.getConnection(newDbUrl, config.getUsername(), config.getPassword())) {
            dbConn.createStatement().executeUpdate("CREATE SCHEMA IF NOT EXISTS master");
            System.out.println("✅ Schema 'master' created or already exists.");
            String grantSql = "GRANT ALL PRIVILEGES ON SCHEMA master TO " + config.getUsername();
            dbConn.createStatement().executeUpdate(grantSql);
            System.out.println("✅ Granted privileges to user: " + config.getUsername());
        } catch (SQLException e) {
            System.err.println("❌ Could not create schema in new DB: " + e.getMessage());
            throw e;
        }

        return true; // Indicates the DB was created
    }
}
