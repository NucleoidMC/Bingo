package me.ellieis.bingo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.ellieis.bingo.Bingo;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
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

                RegistryKey<DimensionType> dimension = gameWorld.getDimensionEntry().getKey().get();
                if (dimension.equals(DimensionTypes.OVERWORLD)) {
                    overworld = gameWorld;
                } else if (dimension.equals(DimensionTypes.THE_NETHER)) {
                    nether = gameWorld;
                }
            }

            if (overworld == null || nether == null) {
                return original;
            }
            RegistryKey<DimensionType> dimension = world.getDimensionEntry().getKey().get();
            if (dimension.equals(DimensionTypes.OVERWORLD)) {
                return nether.getRegistryKey();
            } else if (dimension.equals(DimensionTypes.THE_NETHER)) {
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

        return serverWorld.getDimensionEntry().getKey().get().equals(DimensionTypes.THE_NETHER);
    }
}
