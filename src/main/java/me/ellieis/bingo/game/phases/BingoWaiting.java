package me.ellieis.bingo.game.phases;

import me.ellieis.bingo.game.config.BingoConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;

import java.util.Optional;
import java.util.Set;

public class BingoWaiting {
    GameSpace gameSpace;
    GameActivity activity;
    BingoConfig config;
    ServerLevel level;
    BlockPos spawnPos;
    Optional<TeamSelectionLobby> teamSelection;

    public BingoWaiting(GameSpace gameSpace, GameActivity activity, BingoConfig config, ServerLevel level, BlockPos spawnPos, Optional<TeamSelectionLobby> teamSelection) {
        this.gameSpace = gameSpace;
        this.activity = activity;
        this.config = config;
        this.level = level;
        this.spawnPos = spawnPos;
        this.teamSelection = teamSelection;

        gameSpace.getPlayers().forEach((plr) -> {
            plr.teleportTo(level, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), Set.of(), 0, 0, false);
            plr.setGameMode(GameType.ADVENTURE);
        });

        activity.listen(GameActivityEvents.REQUEST_START, () -> {
            BingoActive.Open(gameSpace, config, level, spawnPos, teamSelection);
            return GameResult.ok();
        });

        activity.listen(PlayerDamageEvent.EVENT, (_plr, _source, _damage) -> EventResult.DENY);
        activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
        activity.listen(GamePlayerEvents.ACCEPT, acceptor ->
                acceptor.teleport(level, Vec3.atCenterOf(spawnPos))
                        .thenRunForEach(plr -> plr.setGameMode(GameType.ADVENTURE)));


        GameWaitingLobby.addTo(activity, config.playerConfig());
    }
    public static void Open(GameSpace gameSpace, BingoConfig config, ServerLevel world, BlockPos spawnPos) {
        gameSpace.setActivity(activity ->  {
            Optional<TeamSelectionLobby> teamSelection = config.teams().map(teams -> TeamSelectionLobby.addTo(activity, teams));
            new BingoWaiting(gameSpace, activity, config, world, spawnPos, teamSelection);
        });
    }
}
