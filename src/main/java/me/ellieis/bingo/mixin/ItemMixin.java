package me.ellieis.bingo.mixin;

import me.ellieis.bingo.ItemCraftEvent;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.featuretoggle.ToggleableFeature;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.EventResult;

@Mixin(Item.class)
public abstract class ItemMixin implements ToggleableFeature, ItemConvertible, FabricItem {
    @Inject(method = "onCraftByPlayer", at = @At("HEAD"), cancellable = true)
    public void onCraft(ItemStack stack, PlayerEntity plr, CallbackInfo ci) {
        if (plr.getEntityWorld().isClient()) {
            return;
        }
        try (var invokers = Stimuli.select().forEntity(plr)) {
            var result = invokers.get(ItemCraftEvent.EVENT).onItemCraft((ServerPlayerEntity) plr, stack);
            if (result == EventResult.DENY) {
                ci.cancel();
            }
        }
    }
}
