package me.ellieis.bingo;

import me.ellieis.bingo.game.config.BingoConfig;
import me.ellieis.bingo.game.phases.BingoLoading;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameType;

import java.util.ArrayList;

public class Bingo implements ModInitializer {
    public static final String MOD_ID = "bingo";
    public static final ArrayList<GameSpace> gameSpaces = new ArrayList<>();

    public static boolean isGameWorld(GameSpace gameSpace) {
        return gameSpaces.contains(gameSpace);
    }

    @Override
    public void onInitialize() {
        GameType.register(Bingo.identifier("bingo"), BingoConfig.CODEC, BingoLoading::Open);
    }
    public static Identifier identifier(String value) {
        return Identifier.of(MOD_ID, value);
    }
}
