package me.ellieis.bingo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.ellieis.bingo.Bingo;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {
    @ModifyArg(method= "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"), index = 0)
    private ResourceKey<Level> bingo$allowNetherPortalsInGameWorld(ResourceKey<Level> original, @Local(argsOnly = true) @NotNull ServerLevel world) {
        GameSpace gameSpace = GameSpaceManager.get().byLevel(world);
        if (gameSpace != null && Bingo.isGameLevel(gameSpace)) {
            ServerLevel overworld = null;
            ServerLevel nether = null;
            for (ServerLevel gameLevel : gameSpace.getLevels()) {

                ResourceKey<DimensionType> dimension = gameLevel.dimensionTypeRegistration().unwrapKey().get();
                if (dimension.equals(BuiltinDimensionTypes.OVERWORLD)) {
                    overworld = gameLevel;
                } else if (dimension.equals(BuiltinDimensionTypes.NETHER)) {
                    nether = gameLevel;
                }
            }

            if (overworld == null || nether == null) {
                return original;
            }
            ResourceKey<DimensionType> dimension = world.dimensionTypeRegistration().unwrapKey().get();
            if (dimension.equals(BuiltinDimensionTypes.OVERWORLD)) {
                return nether.dimension();
            } else if (dimension.equals(BuiltinDimensionTypes.NETHER)) {
                return overworld.dimension();
            } else {
                return original;
            }
        }
        return original;
    }

    @ModifyArg(method = "getPortalDestination", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/NetherPortalBlock;getExitPortal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/level/border/WorldBorder;)Lnet/minecraft/world/level/portal/TeleportTransition;"))
    private boolean bingo$allowNetherPortalCreationInGameWorlds(boolean original, @Local(argsOnly = true) @NotNull ServerLevel serverWorld) {
        var gameSpace = GameSpaceManager.get().byLevel(serverWorld);
        if (gameSpace == null || !Bingo.isGameLevel(gameSpace)) {
            return original;
        }

        return serverWorld.dimensionTypeRegistration().unwrapKey().get().equals(BuiltinDimensionTypes.NETHER);
    }
}
