package net.arhar.weathervote;

import static net.arhar.weathervote.WeatherVoteChat.PLUGIN_PREFIX;
import static net.arhar.weathervote.WeatherVoteChat.player;
import static net.arhar.weathervote.WeatherVoteChat.type;
import static net.arhar.weathervote.WeatherVoteChat.world;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.collect.ImmutableSet;

public class VoteRunner extends BukkitRunnable {

    private static final long TICKS_PER_SECOND = 20;

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
        player.sendMessage(PLUGIN_PREFIX + "You voted " + type((isVoteYes ? "yes" : "no"))
            + " for " + type(voteType.getName()) + " in " + world(world.getName()));
        updateVoteStatus();
    }

    private void updateVoteStatus() {
        // this assumes that world.getPlayers() will update when players join/leave
        // this should also retain players who left the world
        playersInWorld.addAll(world.getPlayers());

        if (isVoteAutoPassed()) {
            endVote(true);
        }
    }

    private boolean isVoteAutoPassed() {
        int yesVotes = 0;
        for (Entry<Player, Boolean> vote : playerVotes.entrySet()) {
            if (vote.getValue()) {
                yesVotes++;
            }
        }

        // >50% yes votes required to pass a vote immediately
        return yesVotes / (double) playersInWorld.size() > 0.5;
    }

    private boolean isVotePassed() {
        int yesVotes = 0;
        int noVotes = 0;
        for (Entry<Player, Boolean> vote : playerVotes.entrySet()) {
            if (vote.getValue()) {
                yesVotes++;
            } else {
                noVotes++;
            }
        }

        return yesVotes > noVotes;
    }

    public void start() {
        this.tickCounter = 0;
        this.runTaskTimer(plugin, 0, 0);
        broadcast("Vote by " + player(player.getName())
            + " for " + type(voteType.getName())
            + " in " + world(world.getName()));
    }

    @Override
    public void run() {
        tickCounter++;
        if (checkupTicks.contains(tickCounter)) {
        	long secondsRemaining = (thresholdTicks - tickCounter) / TICKS_PER_SECOND;
        	broadcast(secondsRemaining + " seconds remaining for " + type(voteType.getName())
        	    + " in " + world(world.getName()));
        }
        if (tickCounter > thresholdTicks) {
            endVote(isVotePassed());
        }
    }

    private void endVote(boolean isSuccess) {
        if (isSuccess) {
            broadcast("Vote passed for " + type(voteType.getName())
                + " in " + world(world.getName()));
            voteType.execute(world);
        } else {
            broadcast("Vote failed for " + type(voteType.getName())
                + " in " + world(world.getName()));
        }
        plugin.voteFinished(world);
        this.cancel();
    }

    private void broadcast(String message) {
        world.getPlayers().forEach(player -> player.sendMessage(PLUGIN_PREFIX + message));
    }
}