/*
 * Requiem
 * Copyright (C) 2017-2020 Ladysnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; If not, see <https://www.gnu.org/licenses>.
 */
package ladysnake.requiem.api.v1.entity;

import dev.onyxstudios.cca.api.v3.component.ClientTickingComponent;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.component.ServerTickingComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import javax.annotation.CheckForNull;

/**
 * A {@link MovementAlterer} alters the movement of an {@link net.minecraft.entity.Entity}
 * according to a {@link MovementConfig}.
 */
public interface MovementAlterer extends ServerTickingComponent, ClientTickingComponent {
    ComponentKey<MovementAlterer> KEY = ComponentRegistryV3.INSTANCE.getOrCreate(new Identifier("requiem", "movement_alterer"), MovementAlterer.class);

    static MovementAlterer get(PlayerEntity player) {
        return KEY.get(player);
    }

    void setConfig(@CheckForNull MovementConfig config);

    void applyConfig();

    /**
     * Gets the acceleration that this entity has underwater.
     *
     * @param baseAcceleration the default acceleration computed in {@link net.minecraft.entity.LivingEntity#travel(Vec3d)}
     * @return the modified acceleration
     */
    float getSwimmingAcceleration(float baseAcceleration);

    boolean canClimbWalls();
}
