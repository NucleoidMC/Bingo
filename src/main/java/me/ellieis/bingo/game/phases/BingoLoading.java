package me.ellieis.bingo.game.phases;

import me.ellieis.bingo.game.config.BingoConfig;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.dimension.DimensionOptions;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;
import xyz.nucleoid.plasmid.api.game.*;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;

import java.util.Set;

public class BingoLoading {
    public static GameOpenProcedure Open(GameOpenContext<BingoConfig> context) {
        BingoConfig config = context.config();
        DimensionOptions dimensionOptions = config.dimensionOptions();
        RuntimeWorldConfig waitingWorldConfig = new RuntimeWorldConfig()
                .setGenerator(new VoidChunkGenerator(context.server()));
        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(dimensionOptions.chunkGenerator())
                .setDimensionType(dimensionOptions.dimensionTypeEntry())
                .setSeed(Random.create().nextLong());

        return context.open((activity) -> {
            ServerWorld waitingWorld = activity.getGameSpace().getWorlds().add(waitingWorldConfig);
            ServerWorld realWorld = activity.getGameSpace().getWorlds().add(worldConfig);
            BlockPos spawnPos = findSpawnPos(realWorld);
            long chunkPos = new ChunkPos(spawnPos).toLong();
            activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
            activity.listen(GamePlayerEvents.ACCEPT, (acceptor) -> acceptor.teleport(waitingWorld, new Vec3d(0, 64, 0))
                .thenRunForEach(plr -> {
                        plr.changeGameMode(GameMode.SPECTATOR);
                    }
                )
            );
            activity.listen(GameActivityEvents.CREATE, () -> {
                realWorld.getChunkManager().addTicket(new ChunkTicket(ChunkTicketType.START, 2), new ChunkPos(spawnPos));
            });
            activity.listen(GameActivityEvents.REQUEST_START, () -> GameResult.error(Text.translatable("bingo.generating")));
            activity.listen(GameActivityEvents.TICK, () -> {
                if (realWorld.isChunkLoaded(chunkPos)) {
                    BingoWaiting.Open(activity.getGameSpace(), config, realWorld, spawnPos);
                    activity.getGameSpace().getWorlds().remove(waitingWorld);
                } else {
                    activity.getGameSpace().getPlayers().forEach(
                        (plr) -> plr.sendMessage(Text.translatable("bingo.generating"), true));
                    }
                }
            );
        });
    }

    // taken from item hunt, ty!
    private static BlockPos findSpawnPos(ServerWorld world) {
        var chunkManager = world.getChunkManager();
        var noiseConfig = chunkManager.getNoiseConfig();
        var chunkGenerator = chunkManager.getChunkGenerator();
        var startChunkPos = new ChunkPos(noiseConfig.getMultiNoiseSampler().findBestSpawnPosition());

        var dx = 0;
        var dz = 0;
        var stepX = 0;
        var stepZ = -1;
        for (var i = 0; i < 11 * 11; i++) {
            if (dx >= -5 && dx <= 5 && dz >= -5 && dz <= 5) {
                var chunkPos = new ChunkPos(startChunkPos.x + dx, startChunkPos.z + dz);
                var x = chunkPos.getStartX() + 8;
                var z = chunkPos.getStartZ() + 8;
                var y = chunkGenerator.getHeightOnGround(x, z, Heightmap.Type.MOTION_BLOCKING, world, noiseConfig);
                var oceanFloorY = chunkGenerator.getHeightOnGround(x, z, Heightmap.Type.OCEAN_FLOOR, world, noiseConfig);
                if (oceanFloorY >= y)
                    return new BlockPos(x, y, z);
            }
            if (dx == dz || dx < 0 && dx == -dz || dx > 0 && dx == 1 - dz) {
                var tmp = stepX;
                stepX = -stepZ;
                stepZ = tmp;
            }
            dx += stepX;
            dz += stepZ;
        }

        var x = startChunkPos.getStartX() + 8;
        var z = startChunkPos.getStartZ() + 8;
        var y = chunkGenerator.getHeightOnGround(x, z, Heightmap.Type.MOTION_BLOCKING, world, noiseConfig);
        return new BlockPos(x, y, z);
    }
}
