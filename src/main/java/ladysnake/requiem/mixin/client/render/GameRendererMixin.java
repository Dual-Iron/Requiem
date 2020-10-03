/*
 * Requiem
 * Copyright (C) 2017-2020 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.mixin.client.render;

import ladysnake.requiem.api.v1.RequiemPlayer;
import ladysnake.requiem.api.v1.event.minecraft.client.ApplyCameraTransformsCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final private Camera camera;

    @Shadow @Final private MinecraftClient client;

    @SuppressWarnings("UnresolvedMixinReference") // Synthetic method
    @Inject(
            // Inject into the synthetic method corresponding to the lambda in updateTargetedEntity
            method = "method_18144",
            at = @At(
                    value = "RETURN"
            ),
            cancellable = true,
            remap = false
    )
    private static void unselectPossessedEntity(Entity tested, CallbackInfoReturnable<Boolean> info) {
        Entity camera = MinecraftClient.getInstance().getCameraEntity();
        if (camera instanceof RequiemPlayer && ((RequiemPlayer) camera).asPossessor().getPossessedEntity() == tested) {
            info.setReturnValue(false);
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V"))
    private void applyCameraTransformations(float tickDelta, long nanoTime, MatrixStack matrices, CallbackInfo ci) {
        ApplyCameraTransformsCallback.EVENT.invoker().applyCameraTransformations(this.camera, matrices, tickDelta);
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void cancelBlockOutlineRender(CallbackInfoReturnable<Boolean> cir) {
        Entity camera = this.client.getCameraEntity();
        if (camera instanceof RequiemPlayer && (((RequiemPlayer) camera).getDeathSuspender().isLifeTransient() || ((RequiemPlayer) camera).asRemnant().isIncorporeal())) {
            cir.setReturnValue(false);
        }
    }
}
