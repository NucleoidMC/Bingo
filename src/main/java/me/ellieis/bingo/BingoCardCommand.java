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
import net.minecraft.datafixer.fix.ItemCustomNameToComponentFix;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.util.List;

import static me.ellieis.bingo.resourcepack.GuiTextures.CLAIMED_SLOT;
import static me.ellieis.bingo.resourcepack.GuiTextures.LOCKED_SLOT;

public class BingoCardCommand {
    public static boolean showGui(ServerPlayerEntity observer, ServerPlayerEntity plr) {
        var type = ScreenHandlerType.GENERIC_9X6;
        SimpleGui gui = new SimpleGui(type, observer, false);
        Text title = Text.translatable(observer.equals(plr) ? "bingo.gui.card.title.self" : "bingo.gui.card.title.others", plr.getName());
        boolean hasMainPack = PolymerResourcePackUtils.hasMainPack(observer);
        if (hasMainPack) {
            gui.setTitle(GuiTextures.BINGO_CARD.apply(title));
        } else {
            gui.setTitle(title);
        }

        // items
        GameSpace gameSpace = GameSpaceManager.get().byWorld(plr.getEntityWorld());
        if (gameSpace == null) {
            return false;
        }
        List<List<BingoSlot>> bingoCard = Bingo.getGame(gameSpace).bingoCards.get(new PlayerRef(plr.getUuid()));
        if (bingoCard == null) {
            return false;
        }
        for (int colIndex = 0; colIndex < 5; colIndex++) {
            List<BingoSlot> col = bingoCard.get(colIndex);
            for (int rowIndex = 0; rowIndex < 5; rowIndex++) {
                int index = 2 + (colIndex * 9) + rowIndex;
                BingoSlot slot = col.get(rowIndex);
                if (slot.marked()) {
                    if (hasMainPack) {
                        gui.setSlot(index, CLAIMED_SLOT.get().setName(slot.item().getName()));
                    } else {
                        gui.setSlot(index, Items.LIME_STAINED_GLASS_PANE.getDefaultStack());
                    }
                } else if (slot.locked()) {
                    if (hasMainPack) {
                        gui.setSlot(index, LOCKED_SLOT.get().setName(slot.item().getName()));
                    } else {
                        gui.setSlot(index, Items.BARRIER.getDefaultStack());
                    }
                }
                else {
                    gui.setSlot(index, slot.item().getDefaultStack());
                }
            }
        }

        gui.open();
        return true;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("bingocard")
                        .requires(BingoCardCommand::isInGame)
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(BingoCardCommand::commandArg)
                        )
                        .executes(BingoCardCommand::command)
        );
    }



    private static boolean isInGame(ServerCommandSource source) {
        if (!source.isExecutedByPlayer()) {
            return false;
        }
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
