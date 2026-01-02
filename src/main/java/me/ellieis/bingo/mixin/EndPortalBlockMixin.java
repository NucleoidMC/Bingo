package me.ellieis.bingo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import me.ellieis.bingo.Bingo;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
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
    @ModifyArg(method = "createTeleportTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(Lnet/minecraft/registry/RegistryKey;)Lnet/minecraft/server/world/ServerWorld;"), index = 0)
    private RegistryKey<World> bingo$allowEndPortalsInGameWorld(RegistryKey<World> original, @Local(argsOnly = true) @NotNull ServerWorld world) {
        GameSpace gameSpace = GameSpaceManager.get().byWorld(world);
        if (gameSpace != null && Bingo.isGameWorld(gameSpace)) {
            ServerWorld overworld = null;
            ServerWorld end = null;
            for (ServerWorld gameWorld : gameSpace.getWorlds()) {
                RegistryKey<DimensionType> dimension = gameWorld.getDimensionEntry().getKey().get();
                if (dimension.equals(DimensionTypes.OVERWORLD)) {
                    overworld = gameWorld;
                } else if (dimension.equals(DimensionTypes.THE_END)) {
                    end = gameWorld;
                }
            }

            if (overworld == null || end == null) {
                return original;
            }
            RegistryKey<DimensionType> dimension = world.getDimensionEntry().getKey().get();
            if (dimension.equals(DimensionTypes.OVERWORLD)) {
                return end.getRegistryKey();
            } else if (dimension.equals(DimensionTypes.THE_END)) {
                return overworld.getRegistryKey();
            } else {
                return original;
            }
        }
        return original;
    }

    @Inject(method = "createTeleportTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;toBottomCenterPos()Lnet/minecraft/util/math/Vec3d;"))
    private void bingo$isEndCheck(ServerWorld world, Entity entity, BlockPos pos, CallbackInfoReturnable<TeleportTarget> cir, @Local(ordinal = 1) @NotNull ServerWorld otherServerWorld, @Local(ordinal = 0) LocalBooleanRef isEnd) {
        RegistryKey<DimensionType> otherDimension = otherServerWorld.getDimensionEntry().getKey().get();
        !isEnd.set(otherDimension.equals(DimensionTypes.THE_END));
    }
}
