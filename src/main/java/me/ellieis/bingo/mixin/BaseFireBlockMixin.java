package me.ellieis.bingo.mixin;

import me.ellieis.bingo.Bingo;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

@Mixin(BaseFireBlock.class)
public class BaseFireBlockMixin {
    @Inject(method = "inPortalDimension", at = @At(value = "HEAD"), cancellable = true)
    private static void bingo$allowNetherPortalsInBingo(Level level, CallbackInfoReturnable<Boolean> cir) {
        GameSpace gameSpace = GameSpaceManager.get().byLevel(level);
        if (gameSpace != null && Bingo.isGameLevel(gameSpace)) {
            ResourceKey<DimensionType> dimension = level.dimensionTypeRegistration().unwrapKey().get();;
            cir.setReturnValue(dimension.equals(BuiltinDimensionTypes.NETHER) || dimension.equals(BuiltinDimensionTypes.OVERWORLD));
        }
    }
}
