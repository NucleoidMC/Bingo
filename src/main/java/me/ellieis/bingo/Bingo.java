package me.ellieis.bingo;

import me.ellieis.bingo.game.config.BingoConfig;
import me.ellieis.bingo.game.phases.BingoLoading;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;

public class Bingo implements ModInitializer {
    public static final String MOD_ID = "bingo";
    @Override
    public void onInitialize() {
        GameType.register(Bingo.identifier("bingo"), BingoConfig.CODEC, BingoLoading::Open);
    }
    public static Identifier identifier(String value) {
        return Identifier.of(MOD_ID, value);
    }
}
