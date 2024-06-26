package fr.legarri.ptt;

import java.util.UUID;

public class PlayerTimeAPI {

	private Main plugin;

    public PlayerTimeAPI(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Obtenir le temps de jeu total d'un joueur en millisecondes.
     * @param playerUUID UUID du joueur
     * @return Temps de jeu total en millisecondes
     */
    public long getTotalPlayTime(UUID playerUUID) {
        long joinTime = plugin.getPlayTimeMap().getOrDefault(playerUUID, 0L);
        long currentPlayTime = System.currentTimeMillis() - joinTime;
        long totalPlayTime = plugin.getDataConfig().getLong(playerUUID.toString()) + currentPlayTime;
        return totalPlayTime;
    }
	
}
