package me.ellieis.bingo;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.ellieis.bingo.game.phases.BingoSlot;
import me.ellieis.bingo.resourcepack.GuiTextures;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.util.datafix.fixes.ItemCustomNameToComponentFix;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.util.List;

import static me.ellieis.bingo.resourcepack.GuiTextures.CLAIMED_SLOT;
import static me.ellieis.bingo.resourcepack.GuiTextures.LOCKED_SLOT;

public class BingoCardCommand {
    public static boolean showGui(ServerPlayer observer, ServerPlayer plr) {
        var type = MenuType.GENERIC_9x6;
        SimpleGui gui = new SimpleGui(type, observer, false);
        Component title = Component.translatable(observer.equals(plr) ? "bingo.gui.card.title.self" : "bingo.gui.card.title.others", plr.getName());
        boolean hasMainPack = PolymerResourcePackUtils.hasMainPack(observer);
        if (hasMainPack) {
            gui.setTitle(GuiTextures.BINGO_CARD.apply(title));
        } else {
            gui.setTitle(title);
        }

        // items
        GameSpace gameSpace = GameSpaceManager.get().byLevel(plr.level());
        if (gameSpace == null) {
            return false;
        }
        List<List<BingoSlot>> bingoCard = Bingo.getGame(gameSpace).bingoCards.get(new PlayerRef(plr.getUUID()));
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
                        gui.setSlot(index, CLAIMED_SLOT.get().setName(slot.item().getDefaultInstance().getItemName()));
                    } else {
                        gui.setSlot(index, Items.LIME_STAINED_GLASS_PANE.getDefaultInstance());
                    }
                } else if (slot.locked()) {
                    if (hasMainPack) {
                        gui.setSlot(index, LOCKED_SLOT.get().setName(slot.item().getDefaultInstance().getItemName()));
                    } else {
                        gui.setSlot(index, Items.BARRIER.getDefaultInstance());
                    }
                }
                else {
                    gui.setSlot(index, slot.item().getDefaultInstance());
                }
            }
        }

        gui.open();
        return true;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("bingocard")
                        .requires(BingoCardCommand::isInGame)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(BingoCardCommand::commandArg)
                        )
                        .executes(BingoCardCommand::command)
        );
    }



    private static boolean isInGame(CommandSourceStack source) {
        if (!source.isPlayer()) {
            return false;
        }
        GameSpace gameSpace = GameSpaceManager.get().byLevel(source.getLevel());
        return gameSpace != null && Bingo.isGameLevel(gameSpace);
    }

    private static int command(CommandContext<CommandSourceStack> context) {
        ServerPlayer plr = context.getSource().getPlayer();
        if (plr != null) {
            showGui(plr, plr);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int commandArg(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer plr = context.getSource().getPlayer();
        ServerPlayer otherPlr = EntityArgument.getPlayer(context, "player");
        if (plr != null) {
            showGui(plr, otherPlr);
        }
        return Command.SINGLE_SUCCESS;
    }
}
