package me.ellieis.bingo;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import me.ellieis.bingo.game.config.BingoConfig;
import me.ellieis.bingo.game.phases.BingoActive;
import me.ellieis.bingo.game.phases.BingoLoading;
import me.ellieis.bingo.resourcepack.GuiTextures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameType;

import java.util.ArrayList;

public class Bingo implements ModInitializer {
    public static final String MOD_ID = "bingo";
    public static final ArrayList<BingoActive> activeGames = new ArrayList<>();
    public static final Logger LOGGER = LogManager.getLogger(Bingo.class);
    public static boolean isGameWorld(GameSpace gameSpace) {
        return getGame(gameSpace) != null;
    }

    public static BingoActive getGame(GameSpace gameSpace) {
        BingoActive finalGame = null;
        for (BingoActive game : activeGames) {
            if (game.gameSpace.equals(gameSpace)) {
                finalGame = game;
                break;
            }
        }

        return finalGame;
    }
    @Override
    public void onInitialize() {
        GameType.register(Bingo.identifier("bingo"), BingoConfig.CODEC, BingoLoading::Open);
        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> ShowBingoCardCommand.register(commandDispatcher)));
        PolymerResourcePackUtils.addModAssets(MOD_ID);
        GuiTextures.register();
    }
    public static Identifier identifier(String value) {
        return Identifier.of(MOD_ID, value);
    }
}
