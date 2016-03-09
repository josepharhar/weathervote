package net.arhar.weathervote;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.ImmutableSet;

public class WeatherVotePlugin extends JavaPlugin implements Listener {

    public static final String[] HELP_MESSAGE = {
        "Weathervote command usage examples",
        "Start a vote: /wvt <time/weather>",
        "Vote yes: /wvt yes",
        "Vote no: /wvt no",
        "Types to vote for: day, night, clear, storm, thunder"
    };
    private static final Set<String> COMMAND_HEADER_TAGS = ImmutableSet.of(
        "wvt",
        "weathervote");
    private static final Set<String> YES_VOTE_TAGS = ImmutableSet.of(
        "yes",
        "y");
    private static final Set<String> NO_VOTE_TAGS = ImmutableSet.of(
        "no",
        "n");

    private static final long VOTE_TIMEOUT_SECONDS = 30; // TODO move this to config

    private Map<World, VoteRunner> voteRunners;

    public void voteFinished(World world) {
        voteRunners.remove(world);
    }

    @Override
    public void onEnable() {
        getLogger().info("Weathervote Enabled");
        getServer().getPluginManager().registerEvents(this, this);
        voteRunners = new HashMap<>();
    }

    @Override
    public void onDisable() {
        voteRunners.forEach((world, vote) -> vote.cancel());
        voteRunners.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (COMMAND_HEADER_TAGS.contains(command.getName().toLowerCase())
                && sender instanceof Player) {
            Player player = (Player) sender;
            World world = player.getWorld();

            if (args.length < 1) {
                player.sendMessage(HELP_MESSAGE);
                return true;
            }

            String argument = args[0].toLowerCase();
            Optional<VoteType> voteType = VoteType.parseVoteTarget(argument);

            if (voteType.isPresent()) {
                // player wants to either start a vote or vote
                if (!voteRunners.containsKey(world)) {
                    // start a new vote
                    VoteRunner newVote = new VoteRunner(this, world, voteType.get(), VOTE_TIMEOUT_SECONDS);
                    voteRunners.put(world, newVote);
                    newVote.start();
                }
                voteRunners.get(world).vote(player, voteType.get());
                return true;
            }

            boolean isVoteYes;
            if (YES_VOTE_TAGS.contains(argument)) {
                isVoteYes = true;
            } else if (NO_VOTE_TAGS.contains(argument)) {
                isVoteYes = false;
            } else {
                player.sendMessage("unrecognized weathervote argument: " + argument);
                player.sendMessage(HELP_MESSAGE);
                return true;
            }

            voteRunners.get(world).vote(player, isVoteYes);
        }

        return true;
    }
}