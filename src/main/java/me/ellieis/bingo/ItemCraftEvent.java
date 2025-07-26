package me.ellieis.bingo;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface ItemCraftEvent {
    StimulusEvent<ItemCraftEvent> EVENT = StimulusEvent.create(ItemCraftEvent.class, ctx -> {
       return (plr, stack) -> {
            try {
                for (var listener : ctx.getListeners()) {
                    var result = listener.onItemCraft(plr, stack);
                    if (result != EventResult.PASS) {
                        return result;
                    }
                }
            } catch (Throwable t) {
                ctx.handleException(t);
            }
            return EventResult.PASS;
       };
    });

    EventResult onItemCraft(ServerPlayerEntity plr, ItemStack stack);
}
