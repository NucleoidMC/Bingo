package me.ellieis.bingo.game.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.dimension.DimensionOptions;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public record BingoConfig(int timeLimit, DimensionOptions dimensionOptions, WaitingLobbyConfig playerConfig) {
    public static final MapCodec<BingoConfig> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
                Codec.INT.optionalFieldOf("time_limit", 1200).forGetter(BingoConfig::timeLimit),
                DimensionOptions.CODEC.fieldOf("dimension_options").forGetter(BingoConfig::dimensionOptions),
                WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(BingoConfig::playerConfig)
        ).apply(instance, BingoConfig::new)
    );
}
