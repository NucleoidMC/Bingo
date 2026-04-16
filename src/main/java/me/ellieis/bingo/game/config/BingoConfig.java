package me.ellieis.bingo.game.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import xyz.nucleoid.plasmid.api.game.common.config.PlayerLimiterConfig;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamList;

import java.util.Optional;
import java.util.OptionalInt;


public record BingoConfig(int timeLimit, Optional<GameTeamList> teams, Holder<WorldPreset> preset, boolean hasOverworld, boolean hasNether, boolean hasEnd, boolean separate, boolean lockout, boolean hardMode, boolean genericMusicDiscDrops, boolean genericArmorTrimDrops, boolean genericSherdDrops, Identifier starterDimension, WaitingLobbyConfig playerConfig) {

    public static final MapCodec<BingoConfig> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
                Codec.INT.optionalFieldOf("time_limit", 0).forGetter(BingoConfig::timeLimit),
                GameTeamList.CODEC.optionalFieldOf("teams").forGetter(BingoConfig::teams),
                WorldPreset.CODEC.fieldOf("preset").forGetter(BingoConfig::preset),
                Codec.BOOL.optionalFieldOf("has_overworld", true).forGetter(BingoConfig::hasOverworld),
                Codec.BOOL.optionalFieldOf("has_nether", true).forGetter(BingoConfig::hasNether),
                Codec.BOOL.optionalFieldOf("has_end", true).forGetter(BingoConfig::hasEnd),
                Codec.BOOL.optionalFieldOf("separate", false).forGetter(BingoConfig::separate),
                Codec.BOOL.optionalFieldOf("lockout", false).forGetter(BingoConfig::lockout),
                Codec.BOOL.optionalFieldOf("hard_mode", false).forGetter(BingoConfig::hardMode),
                Codec.BOOL.optionalFieldOf("generic_music_disc_drops", true).forGetter(BingoConfig::genericMusicDiscDrops),
                Codec.BOOL.optionalFieldOf("generic_armor_trim_drops", true).forGetter(BingoConfig::genericArmorTrimDrops),
                Codec.BOOL.optionalFieldOf("generic_shred_drops", true).forGetter(BingoConfig::genericSherdDrops),
                Identifier.CODEC.optionalFieldOf("starter_dimension", Identifier.fromNamespaceAndPath("minecraft", "overworld")).forGetter(BingoConfig::starterDimension),
                WaitingLobbyConfig.CODEC.optionalFieldOf("players", new WaitingLobbyConfig(new PlayerLimiterConfig(OptionalInt.empty(), true), 1, 4, new WaitingLobbyConfig.Countdown(30, 5))).forGetter(BingoConfig::playerConfig)
        ).apply(instance, BingoConfig::new)
    );
}
