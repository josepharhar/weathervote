package net.arhar.weathervote;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class VoteRunner extends BukkitRunnable {
    
    private static final long TICKS_PER_SECOND = 30;
    
    private WeatherVotePlugin plugin;
    private VoteType voteType;
    Map<Player, Boolean> playerVotes;
    Set<Player> playersInWorld;
    private long tickCounter;
    private long thresholdTicks;
    private World world;
    
    public VoteRunner(WeatherVotePlugin plugin, World world, VoteType voteType, long timeoutSeconds) {
        this.plugin = plugin;
        this.world = world;
        this.voteType = voteType;
        this.thresholdTicks = timeoutSeconds * TICKS_PER_SECOND;
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
    }

    @Override
    public void run() {
        tickCounter++;
        world.getPlayers().forEach(player -> player.sendMessage("tickCounter: " + tickCounter));
        if (tickCounter > thresholdTicks) {
            endVote(isVoteSuccess());
        }
    }
    
    private void endVote(boolean isSuccess) {
        if (isSuccess) {
            world.getPlayers().forEach(player -> player.sendMessage(
                "Vote passed for " + voteType.getName() + " in world " + world.getName()));
            voteType.execute(world);
        } else {
            world.getPlayers().forEach(player -> player.sendMessage(
                "Vote failed for " + voteType.getName() + " in world " + world.getName()));
        }
        plugin.voteFinished(world);
        this.cancel();
    }
}