package me.ellieis.bingo.game.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import xyz.nucleoid.plasmid.api.game.common.config.PlayerLimiterConfig;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;


public record BingoConfig(int timeLimit, Optional<GameTeamList> teams, Holder<WorldPreset> preset, boolean hasOverworld, boolean hasNether, boolean hasEnd, boolean separate, boolean lockout, boolean hardMode, boolean genericMusicDiscDrops, boolean genericArmorTrimDrops, boolean genericSherdDrops, ItemFilter itemFilter, Identifier starterDimension, WaitingLobbyConfig playerConfig) {
    public record ItemFilter(boolean isWhiteList, List<TagKey<Item>> allowedItemTags, List<Holder<Item>> allowedItems, List<TagKey<Item>> disallowedItemTags, List<Holder<Item>> disallowedItems) {
        public static final MapCodec<ItemFilter> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.BOOL.optionalFieldOf("is_whitelist", false).forGetter(ItemFilter::isWhiteList),
                    Codec.list(TagKey.codec(Registries.ITEM)).optionalFieldOf("allowed_item_tags", new ArrayList<>()).forGetter(ItemFilter::allowedItemTags),
                    Codec.list(Item.CODEC).optionalFieldOf("allowed_items", new ArrayList<>()).forGetter(ItemFilter::allowedItems),
                    Codec.list(TagKey.codec(Registries.ITEM)).optionalFieldOf("disallowed_item_tags", new ArrayList<>()).forGetter(ItemFilter::disallowedItemTags),
                    Codec.list(Item.CODEC).optionalFieldOf("disallowed_items", new ArrayList<>()).forGetter(ItemFilter::disallowedItems)
            ).apply(instance, ItemFilter::new)
        );
    }

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
                ItemFilter.CODEC.fieldOf("item_filter").forGetter(BingoConfig::itemFilter),
                Identifier.CODEC.optionalFieldOf("starter_dimension", Identifier.fromNamespaceAndPath("minecraft", "overworld")).forGetter(BingoConfig::starterDimension),
                WaitingLobbyConfig.CODEC.optionalFieldOf("players", new WaitingLobbyConfig(new PlayerLimiterConfig(OptionalInt.empty(), true), 1, 4, new WaitingLobbyConfig.Countdown(30, 5))).forGetter(BingoConfig::playerConfig)
        ).apply(instance, BingoConfig::new)
    );
}
