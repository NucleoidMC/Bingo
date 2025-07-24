package me.ellieis.bingo.game.phases;

import me.ellieis.bingo.Bingo;
import me.ellieis.bingo.game.config.BingoConfig;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.world.GameSpaceWorlds;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.world.EndPortalOpenEvent;
import xyz.nucleoid.stimuli.event.world.NetherPortalOpenEvent;

public class BingoActive {
    GameSpace gameSpace;
    GameActivity activity;
    BingoConfig config;
    ServerWorld world;
    BlockPos spawnPos;

    public BingoActive(GameSpace gameSpace, GameActivity activity, BingoConfig config, ServerWorld world, BlockPos spawnPos) {
        this.gameSpace = gameSpace;
        this.activity = activity;
        this.config = config;
        this.world = world;
        this.spawnPos = spawnPos;

        world.setSpawnPos(spawnPos, 0);
        Bingo.gameSpaces.add(gameSpace);
        activity.listen(GameActivityEvents.DESTROY, (_reason) -> {
            Bingo.gameSpaces.remove(gameSpace);
        });
        activity.listen(NetherPortalOpenEvent.EVENT, (_world, _pos) -> EventResult.ALLOW);
        activity.listen(EndPortalOpenEvent.EVENT, (_context, _result) -> EventResult.ALLOW);
        BingoActive.rules(activity);

    }

    public static void rules(GameActivity activity) {
        activity.allow(GameRuleType.PVP);
        activity.allow(GameRuleType.FALL_DAMAGE);
        activity.allow(GameRuleType.INTERACTION);
        activity.allow(GameRuleType.CRAFTING);
        activity.allow(GameRuleType.BREAK_BLOCKS);
        activity.allow(GameRuleType.HUNGER);
    }
    public static void Open(GameSpace gameSpace, BingoConfig config, ServerWorld world, BlockPos spawnPos) {
        gameSpace.setActivity(activity -> {
            new BingoActive(gameSpace, activity, config, world, spawnPos);
        });
    }
}
