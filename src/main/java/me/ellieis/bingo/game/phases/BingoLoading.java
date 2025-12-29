package me.ellieis.bingo.game.phases;

import me.ellieis.bingo.game.config.BingoConfig;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.gen.WorldPreset;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;
import xyz.nucleoid.plasmid.api.game.*;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.world.GameSpaceWorlds;

import java.util.Objects;

public class BingoLoading {

    // stolen from haykam
    private static ServerWorld addWorld(GameActivity activity, WorldPreset preset, RegistryKey<DimensionOptions> worldKey, long seed) {
        DimensionOptionsRegistryHolder optionsMap = preset.createDimensionsRegistryHolder();
        GameSpaceWorlds worlds = activity.getGameSpace().getWorlds();

        DimensionOptions options = optionsMap.getOrEmpty(worldKey).orElseThrow();

        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setDimensionType(options.dimensionTypeEntry())
                .setGenerator(options.chunkGenerator())
                .setGameRule(GameRules.DO_MOB_SPAWNING, true)
                .setSeed(seed)
                .setShouldTickTime(true);
        ServerWorld world = worlds.add(config);
        world.setMobSpawnOptions(true);
        return world;
    }
    @SuppressWarnings("deprecation")
    public static GameOpenProcedure Open(GameOpenContext<BingoConfig> context) {
        BingoConfig config = context.config();

        RuntimeWorldConfig waitingWorldConfig = new RuntimeWorldConfig()
                .setGenerator(new VoidChunkGenerator(context.server()));

        return context.open((activity) -> {
            ServerWorld waitingWorld = activity.getGameSpace().getWorlds().add(waitingWorldConfig);
            long seed = Random.create().nextLong();
            ServerWorld overworld = null;
            if (config.hasOverworld()) {
                overworld = addWorld(activity, config.preset().value(), DimensionOptions.OVERWORLD, seed);
            }

            ServerWorld nether = null;
            if (config.hasNether()) {
                nether = addWorld(activity, config.preset().value(), DimensionOptions.NETHER, seed);
            }

            ServerWorld end = null;
            if (config.hasEnd()) {
                end = addWorld(activity, config.preset().value(), DimensionOptions.END, seed);
                end.setEnderDragonFight(new EnderDragonFight(end, seed, EnderDragonFight.Data.DEFAULT));
            }

            ServerWorld starterWorld;
            Identifier starterDimension = config.starterDimension();

            if (overworld != null && overworld.getDimensionEntry().getKey().get().getValue().equals(starterDimension)) {
                starterWorld = overworld;
            } else if (nether != null && nether.getDimensionEntry().getKey().get().getValue().equals(starterDimension)) {
                starterWorld = nether;
            } else if (end != null && end.getDimensionEntry().getKey().get().getValue().equals(starterDimension)) {
                starterWorld = end;
            } else {
                throw new GameOpenException(Text.translatable("bingo.no_available_dimensions"));
            }

            BlockPos spawnPos = findSpawnPos(starterWorld);
            long chunkPos = new ChunkPos(spawnPos).toLong();
            activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
            activity.listen(GamePlayerEvents.ACCEPT, (acceptor) -> acceptor.teleport(waitingWorld, new Vec3d(0, 64, 0))
                .thenRunForEach(plr -> {
                        plr.changeGameMode(GameMode.SPECTATOR);
                    }
                )
            );
            activity.listen(GameActivityEvents.CREATE, () -> {
                starterWorld.getChunkManager().addTicket(new ChunkTicket(ChunkTicketType.FORCED, 2), new ChunkPos(spawnPos));
            });
            activity.listen(GameActivityEvents.REQUEST_START, () -> GameResult.error(Text.translatable("bingo.generating")));
            activity.listen(GameActivityEvents.TICK, () -> {
                if (starterWorld.isChunkLoaded(chunkPos)) {
                    BingoWaiting.Open(activity.getGameSpace(), config, starterWorld, spawnPos);
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
