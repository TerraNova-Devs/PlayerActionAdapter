package de.mcterranova.playerActionAdapter;

import de.mcterranova.playerActionAdapter.database.HikariCP;
import de.mcterranova.playerActionAdapter.pojo.ProfessionConfig;
import de.mcterranova.playerActionAdapter.pojo.ProfessionConfigLoader;
import de.mcterranova.playerActionAdapter.pojo.ProfessionsYaml;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public final class PlayerActionAdapter extends JavaPlugin {

    public static HikariCP hikari;

    public static List<ProfessionConfig> professionConfigs;

    static public Plugin plugin;

    @Override
    public void onEnable() {
        plugin = this;

        initDatabase();
        loadConfigs();
        ProfessionManager.loadAll();

        Bukkit.getPluginManager().registerEvents(new GrindListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void initDatabase() {
        try {
            hikari = new HikariCP(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadConfigs() {
        File professionsFile = new File(this.getDataFolder(), "professions.yml");
        if (!professionsFile.exists()) {
            saveResource("professions.yml", false);
        }
        try {
            ProfessionsYaml data = ProfessionConfigLoader.load(professionsFile);
            professionConfigs = data.professions;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
