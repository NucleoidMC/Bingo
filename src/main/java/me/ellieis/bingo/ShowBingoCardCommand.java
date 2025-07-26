package me.ellieis.bingo;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.ellieis.bingo.game.phases.BingoSlot;
import me.ellieis.bingo.resourcepack.GuiTextures;
import me.ellieis.bingo.util.CommonGuiElements;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

import java.util.List;

public class ShowBingoCardCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("showbingocard").requires(ShowBingoCardCommand::isInGame).executes(ShowBingoCardCommand::command));
    }

    private static boolean isInGame(ServerCommandSource source) {
        GameSpace gameSpace = GameSpaceManager.get().byWorld(source.getWorld());
        return gameSpace != null && Bingo.isGameWorld(gameSpace);
    }

    private static int command(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity plr = context.getSource().getPlayer();
        if (plr != null) {
            var type = ScreenHandlerType.GENERIC_9X6;
            SimpleGui gui = new SimpleGui(type, plr, false);
            if (PolymerResourcePackUtils.hasMainPack(plr)) {
                gui.setTitle(GuiTextures.GAME_PORTAL_9X6.apply(Text.translatable("bingo.gui.card.title.self")));
            } else {
                gui.setTitle(Text.translatable("bingo.gui.card.title.self"));
            }

            // items
            GameSpace gameSpace = GameSpaceManager.get().byWorld(context.getSource().getWorld());
            List<List<BingoSlot>> bingoCard = Bingo.getGame(gameSpace).bingoCards.get(plr);
            for (int colIndex = 0; colIndex < 5; colIndex++) {
                List<BingoSlot> col = bingoCard.get(colIndex);
                for (int rowIndex = 0; rowIndex < 5; rowIndex++) {
                    int index = 2 + (colIndex * 9) + rowIndex;
                    BingoSlot slot = col.get(rowIndex);
                    if (slot.marked()) {
                        gui.setSlot(index, Items.LIME_STAINED_GLASS_PANE.getDefaultStack());
                    } else {
                        gui.setSlot(index, slot.item().getDefaultStack());
                    }
                }
            }

            gui.open();
        }
        return Command.SINGLE_SUCCESS;
    }
}
