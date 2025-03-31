package de.mcterranova.playerActionAdapter;

import de.mcterranova.playerActionAdapter.database.SettlementObjectiveProgressDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NationsHelper {

    private static final Map<String, String> queries = new HashMap<>();

    static {
        queries.put("getHome", "SELECT * FROM `access` WHERE `PUUID` = ? AND (access = ? or access = ? or access = ? or access = ?);");
    }

    public static UUID getMembersTown(UUID playerUUID) {
        String sql = queries.get("getHome");
        UUID settlementId = null;
        try (Connection con = PlayerActionAdapter.hikari.dataSource.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, "MAJOR");
            statement.setString(3, "VICE");
            statement.setString(4, "COUNCIL");
            statement.setString(5, "CITIZEN");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                settlementId = UUID.fromString(rs.getString("RUUID"));
            }
        } catch (SQLException e) {
            PlayerActionAdapter.plugin.getLogger().severe("Failed to get members access: " + playerUUID.toString());
        }
        return settlementId;
    }


    public static long getObjectiveProgress(String objectiveId, UUID settlementId) {
        return SettlementObjectiveProgressDAO.getProgress(settlementId.toString(),objectiveId);
    }

    public static void setObjectiveProgress(UUID settlementId, String objectiveId, long newValue) {
        SettlementObjectiveProgressDAO.setProgress(settlementId.toString(), objectiveId, newValue);
    }
}
