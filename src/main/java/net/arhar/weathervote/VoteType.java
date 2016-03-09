package net.arhar.weathervote;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.World;

import com.google.common.collect.ImmutableSet;

public enum VoteType {
    DAY(
        "Day",
        ImmutableSet.of("day", "light"),
        world -> world.setTime(1000)), // TODO move to config?
    NIGHT(
        "Night",
        ImmutableSet.of("night", "dark"),
        world -> world.setTime(13000)),
    CLEAR(
        "Clear",
        ImmutableSet.of("clear", "sun"),
        world -> world.setStorm(false)),
    STORM(
        "Storm",
        ImmutableSet.of("rain", "storm"),
        world -> world.setStorm(true)),
    THUNDER(
        "Thunder",
        ImmutableSet.of("lightning", "thunder"),
        world -> world.setThundering(true));

    private final String name;
    private final Set<String> aliases;
    private final Consumer<World> executor;

    private VoteType(
            String name,
            ImmutableSet<String> aliases,
            Consumer<World> executor) {
        this.name = name;
        this.aliases = aliases;
        this.executor = executor;
    }

    public void execute(World world) {
        executor.accept(world);
    }

    public boolean matchesName(String targetName) {
        return aliases.contains(targetName);
    }

    public String getName() {
        return name;
    }

    public static Optional<VoteType> parseVoteTarget(String targetName) {
        for (VoteType voteTarget : VoteType.values()) {
            if (voteTarget.matchesName(targetName)) {
                return Optional.of(voteTarget);
            }
        }
        return Optional.empty();
    }
}