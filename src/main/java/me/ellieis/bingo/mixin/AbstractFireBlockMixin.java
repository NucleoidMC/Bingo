package me.ellieis.bingo.mixin;

import me.ellieis.bingo.Bingo;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

@Mixin(AbstractFireBlock.class)
public class AbstractFireBlockMixin {
    @Inject(method = "isOverworldOrNether", at = @At(value = "HEAD"), cancellable = true)
    private static void bingo$allowNetherPortalsInBingo(World world, CallbackInfoReturnable<Boolean> cir) {
        GameSpace gameSpace = GameSpaceManager.get().byWorld (world);
        if (gameSpace != null && Bingo.isGameWorld(gameSpace)) {
            Identifier dimension = world.getDimension().effects();
            cir.setReturnValue(dimension.equals(DimensionTypes.THE_NETHER_ID) || dimension.equals(DimensionTypes.OVERWORLD_ID));
        }
    }
}
