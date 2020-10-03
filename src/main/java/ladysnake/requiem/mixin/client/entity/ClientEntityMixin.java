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
 */
package ladysnake.requiem.mixin.client.entity;

import ladysnake.requiem.api.v1.RequiemPlayer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ClientEntityMixin {
    @Inject(method = "shouldLeaveSwimmingPose", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isTouchingWater()Z"), cancellable = true)
    private void isCrawling(CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof RequiemPlayer && ((RequiemPlayer) this).asRemnant().isIncorporeal()) {
            cir.setReturnValue(false);
        }
    }
}
