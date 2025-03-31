package de.mcterranova.playerActionAdapter.database;

import de.mcterranova.playerActionAdapter.PlayerActionAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettlementProfessionRelationDAO {

    private static final String SELECT_ACTIVE_STATUS = """
       SELECT `ProfessionID` FROM settlement_profession_relation
       WHERE `RUUID`=? AND `Status`='ACTIVE'
    """;

    /**
     * Liefert die ID der aktiven Profession einer Stadt.
     */
    public static String getActiveProfessionID(String ruuid) {
        try (Connection con = PlayerActionAdapter.hikari.dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_ACTIVE_STATUS)) {
            ps.setString(1, ruuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("ProfessionID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
