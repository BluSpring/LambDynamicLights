/*
 * Copyright © 2023 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.mixin.sodium;

import dev.lambdaurora.lambdynlights.LambDynLights;
import me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.model.light.data.ArrayLightDataCache", remap = false)
public abstract class ArrayLightDataCacheMixin extends LightDataAccess {
    @Unique
    private final BlockPos.Mutable lambdynlights$pos = new BlockPos.Mutable();

    // In Sodium 0.5, LightDataAccess has replaced the Vanilla LightmapTextureManager,
    // and no longer uses WorldRenderer#getLightmapCoordinates.
    // Unfortunately, this breaks LambDynLights, and as a result is forced to mixin to Sodium.
    @Dynamic
    @Inject(method = "get(III)I", at = @At("RETURN"), cancellable = true)
    private void lambdynlights$modifyBlockLight(int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
        if ( ! LambDynLights.get().config.getDynamicLightsMode().isEnabled() )
          return;  // early out if disabled

        this.lambdynlights$pos.set(x, y, z);

        // Sodium's new packed format allows us to introspect lots of a data in one integer.
        // This means we can skip getting access to world in this context.
        int packed = cir.getReturnValueI();
        boolean fullopaque = LightDataAccess.unpackFO(packed);
        if ( fullopaque ) return;  // early out if block is full opaque

        double dynamic_precise_light = LambDynLights.get().getDynamicLightLevel(this.lambdynlights$pos);
        int block_light = LightDataAccess.unpackBL(packed);

        // repack new dynamic lighting if necessary (no lightmaps, just straight light and luminance)
        if (dynamic_precise_light > block_light) {
            // I'm not super familiar with LambDynamicLights codebase, but this seems close to old lightmap behaviour
            int dynamic_light = (int) (MathHelper.ceil(dynamic_precise_light));
            int dynamic_luminance = dynamic_light;  // if this is right then this could be optimized out

            // It would be nice if sodium provided LightDataAccess.wipeBL(), wipeLU(), etc for efficiency.
            // It would remove the need for unpacking and repacking to guarantee the right bitwise action.
            int block_luminance = LightDataAccess.unpackLU(packed);
            cir.setReturnValue(packed & ~(LightDataAccess.packBL(block_light) | LightDataAccess.packLU(block_luminance))
                                      | LightDataAccess.packBL(dynamic_light) | LightDataAccess.packLU(dynamic_luminance));
            // would become this:
            // cir.setReturnValue((packed & ~(LightDataAccess.wipeBL() | LightDataAccess.wipeLU()))
            //                            | LightDataAccess.packBL(dynamic_light) | LightDataAccess.packLU(dynamic_luminance);
        }
    }
}
