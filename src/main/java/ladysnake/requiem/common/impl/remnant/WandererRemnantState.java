/*
 * Requiem
 * Copyright (C) 2017-2021 Ladysnake
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
package ladysnake.requiem.common.impl.remnant;

import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.AttritionFocus;
import ladysnake.requiem.api.v1.remnant.RemnantState;
import ladysnake.requiem.common.entity.effect.AttritionStatusEffect;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.common.particle.RequiemParticleTypes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class WandererRemnantState extends MutableRemnantState {
    public static final int ATTRITION_MEND_PROBABILITY = 4000;

    public WandererRemnantState(PlayerEntity player) {
        super(player);
    }

    @Override
    public void setup(RemnantState oldHandler) {
        this.setVagrant(true);
    }

    @Override
    public boolean canDissociateFrom(MobEntity possessed) {
        return true;
    }

    @Override
    public boolean canRegenerateBody() {
        return false;
    }

    @Override
    public void serverTick() {
        MobEntity possessedEntity = PossessionComponent.get(this.player).getPossessedEntity();
        ServerPlayerEntity player = (ServerPlayerEntity) this.player;

        if (possessedEntity != null && player.hasStatusEffect(RequiemStatusEffects.ATTRITION) && player.getRandom().nextInt(ATTRITION_MEND_PROBABILITY) == 0) {
            AttritionFocus.KEY.get(possessedEntity).addAttrition(this.player.getUuid(), 1);
            AttritionStatusEffect.reduce(player, 1);

            player.getServerWorld().spawnParticles(
                RequiemParticleTypes.ATTRITION,
                possessedEntity.getX(),
                possessedEntity.getBodyY(0.5),
                possessedEntity.getZ(),
                60,
                possessedEntity.getWidth() * 0.8,
                possessedEntity.getHeight() * 0.6,
                possessedEntity.getWidth() *0.8,
                1.0
            );
        }
    }
}
