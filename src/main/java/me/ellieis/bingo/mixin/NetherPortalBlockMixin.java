package me.ellieis.bingo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.ellieis.bingo.Bingo;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {
    @ModifyArg(method="createTeleportTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(Lnet/minecraft/registry/RegistryKey;)Lnet/minecraft/server/world/ServerWorld;"), index = 0)
    private RegistryKey<World> bingo$allowNetherPortalsInGameWorld(RegistryKey<World> original, @Local(argsOnly = true) @NotNull ServerWorld world) {
        GameSpace gameSpace = GameSpaceManager.get().byWorld(world);
        if (gameSpace != null && Bingo.isGameWorld(gameSpace)) {
            ServerWorld overworld = null;
            ServerWorld nether = null;
            for (ServerWorld gameWorld : gameSpace.getWorlds()) {
                Identifier dimension = gameWorld.getDimension().effects();
                if (dimension.equals(DimensionTypes.OVERWORLD_ID)) {
                    overworld = gameWorld;
                } else if (dimension.equals(DimensionTypes.THE_NETHER_ID)) {
                    nether = gameWorld;
                }
            }

            if (overworld == null || nether == null) {
                return original;
            }

            if (world.getDimension().effects().equals(DimensionTypes.OVERWORLD_ID)) {
                return nether.getRegistryKey();
            } else if (world.getDimension().effects().equals(DimensionTypes.THE_NETHER_ID)) {
                return overworld.getRegistryKey();
            } else {
                return original;
            }
        }
        return original;
    }

    @ModifyArg(method = "createTeleportTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/NetherPortalBlock;getOrCreateExitPortalTarget(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/world/border/WorldBorder;)Lnet/minecraft/world/TeleportTarget;"))
    private boolean bingo$allowNetherPortalCreationInGameWorlds(boolean original, @Local(argsOnly = true) @NotNull ServerWorld serverWorld) {
        var gameSpace = GameSpaceManager.get().byWorld(serverWorld);
        if (gameSpace == null || !Bingo.isGameWorld(gameSpace)) {
            return original;
        }

        return serverWorld.getDimension().effects().equals(DimensionTypes.THE_NETHER_ID);
    }
}
