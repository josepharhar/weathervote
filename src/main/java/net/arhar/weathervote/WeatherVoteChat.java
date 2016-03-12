package net.arhar.weathervote;

import org.bukkit.ChatColor;

public class WeatherVoteChat {

    private static final String TYPE_COLOR = ChatColor.DARK_AQUA.toString();
    private static final String WORLD_COLOR = ChatColor.DARK_GREEN.toString();
    private static final String PLAYER_COLOR = ChatColor.RED.toString();
    private static final String RESET_COLOR = ChatColor.RESET.toString();
    private static final String PLUGIN_COLOR = ChatColor.DARK_PURPLE.toString();

    public static final String PLUGIN_PREFIX = "[" + plugin("WeatherVote") + "] ";

    public static String type(String message) {
        return TYPE_COLOR + message + RESET_COLOR;
    }

    public static String world(String message) {
        return WORLD_COLOR + message + RESET_COLOR;
    }

    public static String player(String message) {
        return PLAYER_COLOR + message + RESET_COLOR;
    }

    public static String plugin(String message) {
        return PLUGIN_COLOR + message + RESET_COLOR;
    }
}