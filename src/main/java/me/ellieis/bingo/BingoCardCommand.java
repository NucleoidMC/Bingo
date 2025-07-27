package me.ellieis.bingo;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.ellieis.bingo.game.phases.BingoSlot;
import me.ellieis.bingo.resourcepack.GuiTextures;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

import java.util.List;

import static me.ellieis.bingo.resourcepack.GuiTextures.CLAIMED_SLOT;

public class BingoCardCommand {
    public static void showGui(ServerPlayerEntity observer, ServerPlayerEntity plr) {
        var type = ScreenHandlerType.GENERIC_9X6;
        SimpleGui gui = new SimpleGui(type, observer, false);
        Text title = Text.translatable(observer.equals(plr) ? "bingo.gui.card.title.self" : "bingo.gui.card.title.others");
        boolean hasMainPack = PolymerResourcePackUtils.hasMainPack(observer);
        if (hasMainPack) {
            gui.setTitle(GuiTextures.BINGO_CARD.apply(title));
        } else {
            gui.setTitle(title);
        }

        // items
        GameSpace gameSpace = GameSpaceManager.get().byWorld(plr.getWorld());
        List<List<BingoSlot>> bingoCard = Bingo.getGame(gameSpace).bingoCards.get(plr);
        for (int colIndex = 0; colIndex < 5; colIndex++) {
            List<BingoSlot> col = bingoCard.get(colIndex);
            for (int rowIndex = 0; rowIndex < 5; rowIndex++) {
                int index = 2 + (colIndex * 9) + rowIndex;
                BingoSlot slot = col.get(rowIndex);
                if (slot.marked()) {
                    if (hasMainPack) {
                        gui.setSlot(index, CLAIMED_SLOT.get().hideTooltip());
                    } else {
                        gui.setSlot(index, Items.LIME_STAINED_GLASS_PANE.getDefaultStack());
                    }
                } else {
                    gui.setSlot(index, slot.item().getDefaultStack());
                }
            }
        }

        gui.open();
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("bingocard")
                        .requires(BingoCardCommand::isInGame)
                        .executes(BingoCardCommand::command)
        );
    }



    private static boolean isInGame(ServerCommandSource source) {
        GameSpace gameSpace = GameSpaceManager.get().byWorld(source.getWorld());
        return gameSpace != null && Bingo.isGameWorld(gameSpace);
    }

    private static int command(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity plr = context.getSource().getPlayer();
        if (plr != null) {
            showGui(plr, plr);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int commandArg(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity plr = context.getSource().getPlayer();
        ServerPlayerEntity otherPlr = EntityArgumentType.getPlayer(context, "player");
        if (plr != null) {
            showGui(plr, otherPlr);
        }
        return Command.SINGLE_SUCCESS;
    }
}
