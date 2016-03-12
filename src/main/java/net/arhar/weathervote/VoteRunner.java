package net.arhar.weathervote;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.collect.ImmutableSet;

public class VoteRunner extends BukkitRunnable {

    private static final long TICKS_PER_SECOND = 20;
    private static final String TYPE_COLOR = ChatColor.DARK_AQUA.toString();
    private static final String WORLD_COLOR = ChatColor.DARK_GREEN.toString();
    private static final String PLAYER_COLOR = ChatColor.RED.toString();
    private static final String RESET_COLOR = ChatColor.RESET.toString();

    private WeatherVotePlugin plugin;
    private VoteType voteType;
    Map<Player, Boolean> playerVotes;
    Set<Player> playersInWorld;
    private long tickCounter;
    private long thresholdTicks;
    private Set<Long> checkupTicks;
    private World world;
    private Player player;

    public VoteRunner(WeatherVotePlugin plugin, Player player, World world, VoteType voteType, long timeoutSeconds) {
        this.plugin = plugin;
        this.world = world;
        this.player = player;
        this.voteType = voteType;
        this.thresholdTicks = timeoutSeconds * TICKS_PER_SECOND;
        this.checkupTicks = ImmutableSet.of(
    		(long) (thresholdTicks * 0.25),
    		(long) (thresholdTicks * 0.5),
    		(long) (thresholdTicks * 0.75));
        this.playerVotes = new HashMap<>();
        this.playersInWorld = new HashSet<>();
    }

    public void vote(Player player, VoteType voteType) {
        if (this.voteType.equals(voteType)) {
            // player is voting in favor of the current vote
            vote(player, true);
        } else {
            // player is voting for a vote that isnt the current vote
            player.sendMessage(
                "a vote is currently running for " + voteType.getName()
                + ", you must wait until it is complete");
        }
    }

    public void vote(Player player, boolean isVoteYes) {
        playerVotes.put(player, isVoteYes);
        updateVoteStatus();
    }

    private void updateVoteStatus() {
        // this assumes that world.getPlayers() will update when players join/leave
        // this will also not account for players leaving the world
        playersInWorld.addAll(world.getPlayers());

        if (isVoteSuccess()) {
            endVote(true);
        }
    }

    private boolean isVoteSuccess() {
        double yesVotes = 0;
        double noVotes = 0;
        for (Map.Entry<Player, Boolean> vote : playerVotes.entrySet()) {
            if (vote.getValue()) {
                yesVotes++;
            } else {
                noVotes++;
            }
        }
        // 50% yes required to pass a vote
        // TODO move this to config
        // this does nothing with "no" votes for now
        return yesVotes / playersInWorld.size() > 0.5;
    }

    public void start() {
        this.tickCounter = 0;
        this.runTaskTimer(plugin, 0, 0);
        broadcast("Vote started by " + PLAYER_COLOR + player.getName() + RESET_COLOR
            + " for " + TYPE_COLOR + voteType.getName() + RESET_COLOR
            + " in world " + WORLD_COLOR + world.getName() + RESET_COLOR);
    }

    @Override
    public void run() {
        tickCounter++;
        if (checkupTicks.contains(tickCounter)) {
        	long secondsRemaining = (thresholdTicks - tickCounter) / TICKS_PER_SECOND;
        	broadcast(secondsRemaining + " seconds remaining for " + voteType.getName() + " in world \"" + world.getName() + "\"");
        }
        if (tickCounter > thresholdTicks) {
            endVote(isVoteSuccess());
        }
    }

    private void endVote(boolean isSuccess) {
        if (isSuccess) {
            broadcast("Vote passed for " + TYPE_COLOR + voteType.getName() + RESET_COLOR
                + " in world " + WORLD_COLOR + world.getName() + RESET_COLOR);
            voteType.execute(world);
        } else {
            broadcast("Vote failed for " + TYPE_COLOR + voteType.getName() + RESET_COLOR
                    + " in world " + WORLD_COLOR + world.getName() + RESET_COLOR);
        }
        plugin.voteFinished(world);
        this.cancel();
    }

    private void broadcast(String message) {
        world.getPlayers().forEach(player -> player.sendMessage(message));
    }
}