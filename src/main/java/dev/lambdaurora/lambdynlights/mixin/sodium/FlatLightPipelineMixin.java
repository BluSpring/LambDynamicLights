/*
 * Copyright © 2023 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.mixin.sodium;

import dev.lambdaurora.lambdynlights.util.SodiumDynamicLightHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.model.light.flat.FlatLightPipeline", remap = false)
public abstract class FlatLightPipelineMixin {
    @Dynamic
    @Inject(method = "getOffsetLightmap", at = @At(value = "RETURN", ordinal = 1), remap = false, locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void lambdynlights$getLightmap(BlockPos pos, Direction face, CallbackInfoReturnable<Integer> cir, int word, int adjWord) {
        int lightmap = SodiumDynamicLightHandler.lambdynlights$getLightmap(pos, adjWord, cir.getReturnValueI());
        cir.setReturnValue(lightmap);
    }
}
