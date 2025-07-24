package me.ellieis.bingo.game.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.common.config.PlayerLimiterConfig;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

import java.util.OptionalInt;

public record BingoConfig(int timeLimit, boolean hasOverworld, boolean hasNether, boolean hasEnd, Identifier starterDimension, WaitingLobbyConfig playerConfig) {
    public static final MapCodec<BingoConfig> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
                Codec.INT.optionalFieldOf("time_limit", 1200).forGetter(BingoConfig::timeLimit),
                Codec.BOOL.optionalFieldOf("has_overworld", true).forGetter(BingoConfig::hasOverworld),
                Codec.BOOL.optionalFieldOf("has_nether", true).forGetter(BingoConfig::hasNether),
                Codec.BOOL.optionalFieldOf("has_end", true).forGetter(BingoConfig::hasEnd),
                Identifier.CODEC.optionalFieldOf("starter_dimension", Identifier.of("minecraft", "overworld")).forGetter(BingoConfig::starterDimension),
                WaitingLobbyConfig.CODEC.optionalFieldOf("players", new WaitingLobbyConfig(new PlayerLimiterConfig(OptionalInt.empty(), true), 1, 4, new WaitingLobbyConfig.Countdown(30, 5))).forGetter(BingoConfig::playerConfig)
        ).apply(instance, BingoConfig::new)
    );
}
