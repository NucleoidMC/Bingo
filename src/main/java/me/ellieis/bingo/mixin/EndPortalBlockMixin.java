package me.ellieis.bingo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import me.ellieis.bingo.Bingo;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
    @ModifyArg(method = "getPortalDestination",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"), index = 0)
    private ResourceKey<Level> bingo$allowEndPortalsInGameWorld(ResourceKey<Level> original, @Local(argsOnly = true) @NotNull ServerLevel world) {
        GameSpace gameSpace = GameSpaceManager.get().byLevel(world);
        if (gameSpace != null && Bingo.isGameLevel(gameSpace)) {
            ServerLevel overworld = null;
            ServerLevel end = null;
            for (ServerLevel gameLevel : gameSpace.getLevels()) {
                ResourceKey<DimensionType> dimension = gameLevel.dimensionTypeRegistration().unwrapKey().get();
                if (dimension.equals(BuiltinDimensionTypes.OVERWORLD)) {
                    overworld = gameLevel;
                } else if (dimension.equals(BuiltinDimensionTypes.END)) {
                    end = gameLevel;
                }
            }

            if (overworld == null || end == null) {
                return original;
            }
            ResourceKey<DimensionType> dimension = world.dimensionTypeRegistration().unwrapKey().get();
            if (dimension.equals(BuiltinDimensionTypes.OVERWORLD)) {
                return end.dimension();
            } else if (dimension.equals(BuiltinDimensionTypes.END)) {
                return overworld.dimension();
            } else {
                return original;
            }
        }
        return original;
    }

    @Inject(method = "getPortalDestination",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getBottomCenter()Lnet/minecraft/world/phys/Vec3;"))
    private void bingo$isEndCheck(ServerLevel world, Entity entity, BlockPos pos, CallbackInfoReturnable<TeleportTransition> cir, @Local(ordinal = 1) @NotNull ServerLevel otherServerWorld, @Local(ordinal = 0) LocalBooleanRef isEnd) {
        ResourceKey<DimensionType> otherDimension = otherServerWorld.dimensionTypeRegistration().unwrapKey().get();
        isEnd.set(otherDimension.equals(BuiltinDimensionTypes.END));
    }
}
