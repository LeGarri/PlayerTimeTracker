package fr.legarri.ptt;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

	private PlayerTimeAPI playerTimeAPI;
    private HashMap<UUID, Long> playTimeMap = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            saveResource("data.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Charger et sauvegarder la config si elle n'existe pas
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        // Scheduler pour sauvegarder les temps de jeu toutes les secondes
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::savePlayTimes, 20L, 20L);

        playerTimeAPI = new PlayerTimeAPI(this);
        
        new Metrics(this, 22332);
        
        getLogger().info("PlayerTimeTracker Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        savePlayTimes();
        getLogger().info("PlayerTimeTracker Plugin Disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        long joinTime = System.currentTimeMillis();
        playTimeMap.put(playerUUID, joinTime);

        if (!dataConfig.contains(playerUUID.toString())) {
            dataConfig.set(playerUUID.toString(), 0L);
            saveDataConfig();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        long joinTime = playTimeMap.getOrDefault(playerUUID, 0L);
        long playTime = System.currentTimeMillis() - joinTime;
        long totalPlayTime = dataConfig.getLong(playerUUID.toString()) + playTime;
        dataConfig.set(playerUUID.toString(), totalPlayTime);
        saveDataConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("playtime")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                UUID playerUUID = player.getUniqueId();
                long joinTime = playTimeMap.getOrDefault(playerUUID, 0L);
                long playTime = System.currentTimeMillis() - joinTime;
                long totalPlayTime = dataConfig.getLong(playerUUID.toString()) + playTime;
                String playtimeMessage = getConfig().getString("messages.playtime")
                        .replace("{playtime}", formatPlayTime(totalPlayTime));
                player.sendMessage(playtimeMessage);
                return true;
            } else {
                sender.sendMessage("Only players can use this command!");
                return true;
            }
        }

        if (cmd.getName().equalsIgnoreCase("topplaytime")) {
            if (sender instanceof Player) {
                showTopPlayTime((Player) sender);
                return true;
            } else {
                sender.sendMessage("Only players can use this command!");
                return true;
            }
        }
        return false;
    }

    private String formatPlayTime(long playTime) {
        long hours = TimeUnit.MILLISECONDS.toHours(playTime);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(playTime) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(playTime) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void savePlayTimes() {
        for (UUID playerUUID : playTimeMap.keySet()) {
            long joinTime = playTimeMap.get(playerUUID);
            long playTime = System.currentTimeMillis() - joinTime;
            long totalPlayTime = dataConfig.getLong(playerUUID.toString()) + playTime;
            dataConfig.set(playerUUID.toString(), totalPlayTime);
            playTimeMap.put(playerUUID, System.currentTimeMillis());  // Reset join time to now
        }
        saveDataConfig();
    }

    private void saveDataConfig() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showTopPlayTime(Player player) {
        Map<UUID, Long> sortedMap = new LinkedHashMap<>();
        dataConfig.getKeys(false).stream()
                .map(UUID::fromString)
                .sorted((uuid1, uuid2) -> Long.compare(dataConfig.getLong(uuid2.toString()), dataConfig.getLong(uuid1.toString())))
                .forEach(uuid -> sortedMap.put(uuid, dataConfig.getLong(uuid.toString())));

        player.sendMessage("Top Playtime:");
        int rank = 1;
        for (Map.Entry<UUID, Long> entry : sortedMap.entrySet()) {
            if (rank > 10) break; // Limite au top 10
            String playerName = getServer().getOfflinePlayer(entry.getKey()).getName();
            player.sendMessage(rank + ". " + playerName + " - " + formatPlayTime(entry.getValue()));
            rank++;
        }
    }
    
    public HashMap<UUID, Long> getPlayTimeMap() {
        return playTimeMap;
    }

    public FileConfiguration getDataConfig() {
        return dataConfig;
    }
    
    public PlayerTimeAPI getPlayerTimeAPI() {
        return playerTimeAPI;
    }
}