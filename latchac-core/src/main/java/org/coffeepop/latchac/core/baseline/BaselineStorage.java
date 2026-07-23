package org.coffeepop.latchac.core.baseline;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * SQLite persistence for individual player baselines and population baselines.
 */
public class BaselineStorage {
    private final String dbPath;

    public BaselineStorage(File dataFolder) {
        this.dbPath = new File(dataFolder, "baselines.db").getAbsolutePath();
        initDb();
    }

    private void initDb() {
        try (Connection conn = connect()) {
            var stmt = conn.createStatement();
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS baselines (
                    player_uuid TEXT NOT NULL,
                    metric_name TEXT NOT NULL,
                    baseline REAL NOT NULL,
                    baseline_std REAL NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, metric_name)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS population_baseline (
                    metric_name TEXT PRIMARY KEY,
                    mean REAL NOT NULL,
                    std REAL NOT NULL,
                    sample_count INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init baseline database", e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    public void saveBaseline(UUID playerId, String metricName, double baseline, double baselineStd) {
        try (Connection conn = connect()) {
            var ps = conn.prepareStatement("""
                INSERT OR REPLACE INTO baselines (player_uuid, metric_name, baseline, baseline_std, updated_at)
                VALUES (?, ?, ?, ?, ?)
            """);
            ps.setString(1, playerId.toString());
            ps.setString(2, metricName);
            ps.setDouble(3, baseline);
            ps.setDouble(4, baselineStd);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public Map<String, double[]> loadBaselines(UUID playerId) {
        Map<String, double[]> result = new HashMap<>();
        try (Connection conn = connect()) {
            var ps = conn.prepareStatement(
                "SELECT metric_name, baseline, baseline_std FROM baselines WHERE player_uuid = ?");
            ps.setString(1, playerId.toString());
            var rs = ps.executeQuery();
            while (rs.next()) {
                result.put(rs.getString("metric_name"),
                    new double[]{rs.getDouble("baseline"), rs.getDouble("baseline_std")});
            }
        } catch (SQLException ignored) {}
        return result;
    }

    public void savePopulationBaseline(String metricName, double mean, double std, int samples) {
        try (Connection conn = connect()) {
            var ps = conn.prepareStatement("""
                INSERT OR REPLACE INTO population_baseline (metric_name, mean, std, sample_count, updated_at)
                VALUES (?, ?, ?, ?, ?)
            """);
            ps.setString(1, metricName);
            ps.setDouble(2, mean);
            ps.setDouble(3, std);
            ps.setInt(4, samples);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public Map<String, double[]> loadPopulationBaselines() {
        Map<String, double[]> result = new HashMap<>();
        try (Connection conn = connect()) {
            var rs = conn.createStatement().executeQuery(
                "SELECT metric_name, mean, std FROM population_baseline");
            while (rs.next()) {
                result.put(rs.getString("metric_name"),
                    new double[]{rs.getDouble("mean"), rs.getDouble("std")});
            }
        } catch (SQLException ignored) {}
        return result;
    }

    public void rebuildPopulationBaseline() {
        try (Connection conn = connect()) {
            var rs = conn.createStatement().executeQuery("""
                SELECT metric_name, AVG(baseline) as avg_mean, AVG(baseline_std) as avg_std, COUNT(*) as cnt
                FROM baselines GROUP BY metric_name
            """);
            while (rs.next()) {
                String name = rs.getString("metric_name");
                double mean = rs.getDouble("avg_mean");
                double std = rs.getDouble("avg_std");
                int count = rs.getInt("cnt");
                if (count >= 3) {
                    savePopulationBaseline(name, mean, std, count);
                }
            }
        } catch (SQLException ignored) {}
    }
}
