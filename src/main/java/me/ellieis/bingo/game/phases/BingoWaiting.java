package me.ellieis.bingo.game.phases;

import me.ellieis.bingo.game.config.BingoConfig;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;

import java.util.Set;

public class BingoWaiting {
    GameSpace gameSpace;
    GameActivity activity;
    BingoConfig config;
    ServerWorld world;
    BlockPos spawnPos;

    public BingoWaiting(GameSpace gameSpace, GameActivity activity, BingoConfig config, ServerWorld world, BlockPos spawnPos) {
        this.gameSpace = gameSpace;
        this.activity = activity;
        this.config = config;
        this.world = world;
        this.spawnPos = spawnPos;

        gameSpace.getPlayers().forEach((plr) -> {
            plr.teleport(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), Set.of(), 0, 0, false);
            plr.changeGameMode(GameMode.ADVENTURE);
        });

        activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
        activity.listen(GamePlayerEvents.ACCEPT, acceptor ->
                acceptor.teleport(world, spawnPos.toCenterPos())
                        .thenRunForEach(plr -> plr.changeGameMode(GameMode.ADVENTURE)));
        GameWaitingLobby.addTo(activity, config.playerConfig());
    }
    public static void Open(GameSpace gameSpace, BingoConfig config, ServerWorld world, BlockPos spawnPos) {
        gameSpace.setActivity(activity ->  {
            new BingoWaiting(gameSpace, activity, config, world, spawnPos);
        });
    }
}
