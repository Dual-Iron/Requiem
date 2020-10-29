/*
 * Requiem
 * Copyright (C) 2019 Ladysnake
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
package ladysnake.pandemonium.client.handler;

import dev.onyxstudios.cca.api.v3.component.ComponentProvider;
import ladysnake.requiem.api.v1.event.minecraft.client.ApplyCameraTransformsCallback;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;

import javax.annotation.Nullable;

public class HeadDownTransformHandler implements ApplyCameraTransformsCallback {

    public static final Quaternion QUATERNION_180_X = Vector3f.POSITIVE_X.getDegreesQuaternion(180.0F);
    public static final Quaternion QUATERNION_180_Y = Vector3f.POSITIVE_Y.getDegreesQuaternion(180.0F);

    @Override
    public void applyCameraTransformations(Camera camera, MatrixStack matrices, float tickDelta) {
        if (!camera.isThirdPerson()) {
            Entity focusedEntity = camera.getFocusedEntity();
            if (focusedEntity != null && ComponentProvider.fromEntity(focusedEntity).getComponentContainer() != null) {
                Entity possessed = PossessionComponent.getPossessedEntity(focusedEntity);
                if (isUpsideDown(possessed)) {
                    matrices.multiply(QUATERNION_180_X);
                    matrices.multiply(QUATERNION_180_Y);
                }
            }
        }
    }

    private static boolean isUpsideDown(@Nullable Entity possessed) {
        return possessed instanceof ShulkerEntity && ((ShulkerEntity) possessed).getAttachedFace() == Direction.UP || possessed instanceof BatEntity && ((BatEntity) possessed).isRoosting();
    }
}
