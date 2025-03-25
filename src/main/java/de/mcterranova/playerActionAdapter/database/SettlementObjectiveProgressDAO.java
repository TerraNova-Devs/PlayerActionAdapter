package de.mcterranova.playerActionAdapter.database;

import de.mcterranova.playerActionAdapter.PlayerActionAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettlementObjectiveProgressDAO {

    private static final String UPSERT = """
       INSERT INTO settlement_objective_progress (RUUID, ObjectiveID, Progress)
       VALUES (?, ?, ?)
       ON DUPLICATE KEY UPDATE Progress=VALUES(Progress)
    """;

    private static final String SELECT_PROGRESS = """
       SELECT Progress FROM settlement_objective_progress
       WHERE RUUID=? AND ObjectiveID=?;
    """;

    /**
     * Setzt den Fortschritt eines konkreten Objectives auf einen neuen Wert
     * (anstatt nur +1 wie in addProgress).
     */
    public static void setProgress(String ruuid, String objectiveId, long newProgress) {
        try (Connection con = PlayerActionAdapter.hikari.dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(UPSERT)) {
            ps.setString(1, ruuid);
            ps.setString(2, objectiveId);
            ps.setLong(3, newProgress);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static long getProgress(String ruuid, String objectiveId) {
        try (Connection con = PlayerActionAdapter.hikari.dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_PROGRESS)) {
            ps.setString(1, ruuid);
            ps.setString(2, objectiveId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("Progress");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
