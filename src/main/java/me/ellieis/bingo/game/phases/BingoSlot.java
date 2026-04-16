package me.ellieis.bingo.game.phases;

import net.minecraft.world.item.Item;

public record BingoSlot(Item item, boolean marked, boolean locked) {
    public BingoSlot(BingoSlot slot) {
        this(slot.item(), slot.marked(), slot.locked());
    }
}
