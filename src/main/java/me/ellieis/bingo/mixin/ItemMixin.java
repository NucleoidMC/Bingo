package me.ellieis.bingo.mixin;

import me.ellieis.bingo.ItemCraftEvent;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.EventResult;

@Mixin(Item.class)
public abstract class ItemMixin implements FeatureElement, ItemLike, FabricItem {
    @Inject(method = "onCraftedBy", at = @At("HEAD"), cancellable = true)
    public void onCraft(ItemStack stack, Player plr, CallbackInfo ci) {
        if (plr.level().isClientSide()) {
            return;
        }
        try (var invokers = Stimuli.select().forEntity(plr)) {
            var result = invokers.get(ItemCraftEvent.EVENT).onItemCraft((ServerPlayer) plr, stack);
            if (result == EventResult.DENY) {
                ci.cancel();
            }
        }
    }
}
