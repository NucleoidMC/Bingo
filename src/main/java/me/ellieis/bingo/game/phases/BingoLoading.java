package me.ellieis.bingo.game.phases;

import me.ellieis.bingo.game.config.BingoConfig;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;
import xyz.nucleoid.plasmid.api.game.*;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.world.GameSpaceLevels;

import java.util.Objects;

public class BingoLoading {

    // stolen from haykam
    private static ServerLevel addLevels(GameActivity activity, WorldPreset preset, ResourceKey<LevelStem> worldKey, long seed) {
        WorldDimensions optionsMap = preset.createWorldDimensions();
        GameSpaceLevels levels = activity.getGameSpace().getLevels();

        LevelStem options = optionsMap.get(worldKey).orElseThrow();

        RuntimeLevelConfig config = new RuntimeLevelConfig()
                .setDimensionType(options.type())
                .setGenerator(options.generator())
                .setGameRule(GameRules.SPAWN_MOBS, true)
                .setSeed(seed)
                .setShouldTickTime(true);
        ServerLevel level = levels.add(config);
        level.setSpawnSettings(true);
        return level;
    }
    @SuppressWarnings("deprecation")
    public static GameOpenProcedure Open(GameOpenContext<BingoConfig> context) {
        BingoConfig config = context.config();

        RuntimeLevelConfig waitingLevelConfig = new RuntimeLevelConfig()
                .setGenerator(new VoidChunkGenerator(context.server()));

        return context.open((activity) -> {
            ServerLevel waitingLevel = activity.getGameSpace().getLevels().add(waitingLevelConfig);
            long seed = RandomSource.create().nextLong();
            ServerLevel overworld = null;
            if (config.hasOverworld()) {
                overworld = addLevels(activity, config.preset().value(), LevelStem.OVERWORLD, seed);
            }

            ServerLevel nether = null;
            if (config.hasNether()) {
                nether = addLevels(activity, config.preset().value(), LevelStem.NETHER, seed);
            }

            ServerLevel end = null;
            if (config.hasEnd()) {
                end = addLevels(activity, config.preset().value(), LevelStem.END, seed);
                EnderDragonFight dragonFight = EnderDragonFight.createDefault();
                dragonFight.init(end, end.getSeed(), BlockPos.ZERO);
                end.setDragonFight(dragonFight);
            }

            ServerLevel starterWorld;
            Identifier starterDimension = config.starterDimension();

            if (overworld != null && overworld.dimensionTypeRegistration().unwrapKey().get().identifier().equals(starterDimension)) {
                starterWorld = overworld;
            } else if (nether != null && nether.dimensionTypeRegistration().unwrapKey().get().identifier().equals(starterDimension)) {
                starterWorld = nether;
            } else if (end != null && end.dimensionTypeRegistration().unwrapKey().get().identifier().equals(starterDimension)) {
                starterWorld = end;
            } else {
                throw new GameOpenException(Component.translatable("bingo.no_available_dimensions"));
            }

            BlockPos spawnPos = findSpawnPos(starterWorld);
            long chunkPos = new ChunkPos(spawnPos.getX(), spawnPos.getZ()).pack();
            activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
            activity.listen(GamePlayerEvents.ACCEPT, (acceptor) -> acceptor.teleport(waitingLevel, new Vec3(0, 64, 0))
                .thenRunForEach(plr -> {
                        plr.setGameMode(GameType.SPECTATOR);
                    }
                )
            );
            activity.listen(GameActivityEvents.CREATE, () -> {
                starterWorld.getChunkSource().addTicket(new Ticket(TicketType.FORCED, 2), new ChunkPos(spawnPos.getX(), spawnPos.getZ()));
            });
            activity.listen(GameActivityEvents.REQUEST_START, () -> GameResult.error(Component.translatable("bingo.generating")));
            activity.listen(GameActivityEvents.TICK, () -> {
                if (starterWorld.areEntitiesLoaded(chunkPos)) {
                    BingoWaiting.Open(activity.getGameSpace(), config, starterWorld, spawnPos);
                    System.out.println("boobs");
                    activity.getGameSpace().getLevels().remove(waitingLevel);
                } else {
                    activity.getGameSpace().getPlayers().forEach(
                        (plr) -> plr.sendSystemMessage(Component.translatable("bingo.generating"), true));
                    }
                }
            );
        });
    }

    // taken from item hunt, ty!
    private static BlockPos findSpawnPos(ServerLevel world) {
        var chunkManager = world.getChunkSource();
        var noiseConfig = chunkManager.randomState();
        var chunkGenerator = chunkManager.getGenerator();
        BlockPos pos = noiseConfig.sampler().findSpawnPosition();
        var startChunkPos = new ChunkPos(pos.getX(), pos.getZ());
        var dx = 0;
        var dz = 0;
        var stepX = 0;
        var stepZ = -1;
        for (var i = 0; i < 11 * 11; i++) {
            if (dx >= -5 && dx <= 5 && dz >= -5 && dz <= 5) {
                var chunkPos = new ChunkPos(startChunkPos.getRegionX() + dx, startChunkPos.getRegionZ() + dz);
                var x = chunkPos.getMinBlockX() + 8;
                var z = chunkPos.getMinBlockZ() + 8;
                var y = chunkGenerator.getFirstFreeHeight(x, z, Heightmap.Types.MOTION_BLOCKING, world, noiseConfig);
                var oceanFloorY = chunkGenerator.getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR, world, noiseConfig);
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

        var x = startChunkPos.getMinBlockX() + 8;
        var z = startChunkPos.getMinBlockZ() + 8;
        var y = chunkGenerator.getFirstFreeHeight(x, z, Heightmap.Types.MOTION_BLOCKING, world, noiseConfig);
        return new BlockPos(x, y, z);
    }
}
