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
package ladysnake.requiem.api.v1;

import ladysnake.requiem.api.v1.dialogue.DialogueTracker;
import ladysnake.requiem.api.v1.entity.MovementAlterer;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * A player with extended capabilities allowing interaction with specific Requiem
 * functionality. When an API provider is installed, every {@link PlayerEntity}
 * implements this interface.
 *
 * @since 1.0.0
 */
public interface RequiemPlayer {

    /**
     * Return a player's {@link PossessionComponent}. The possession component is
     * live, and every modification made to it is reflected on the player.
     *
     * @return the player's possession component
     * @since 1.0.0
     */
    @Contract(pure = true)
    PossessionComponent asPossessor();

    /**
     * Return the {@link MovementAlterer} altering this player's movement.
     * @return the player's {@link MovementAlterer}
     * @since 1.0.0
     */
    @Contract(pure = true)
    MovementAlterer getMovementAlterer();

    /**
     * Return the {@link DialogueTracker} handling cutscene dialogues for this player.
     * @return the player's {@link DialogueTracker}
     * @since 1.0.0
     */
    @Contract(pure = true)
    DialogueTracker getDialogueTracker();

    /**
     * Return a {@code RequiemPlayer} instance that corresponds to the passed in player.
     * <p>If an API provider is not guaranteed to be present in the current game instance,
     * {@link #fromSafely(PlayerEntity)} should be used instead.
     *
     * @param player a player to see as a {@code RequiemPlayer}
     * @return {@code player} as a {@link RequiemPlayer}
     * @see #fromSafely(PlayerEntity)
     * @since 1.0.0
     */
    @Nullable
    @Contract(value = "null -> null; !null -> !null", pure = true)
    static RequiemPlayer from(@Nullable PlayerEntity player) {
        return (RequiemPlayer) player;
    }

    /**
     * Return an {@link Optional} describing {@code player} as a {@code RequiemPlayer},
     * or an empty {@code Optional} if it cannot be coerced to one.
     * <p>
     * When an API provider is available in the current game instance, the returned
     * {@code Optional} can only be empty if {@code player} is {@code null}.
     * When no API provider is available, this method will always return an empty {@code Optional}.
     *
     * @return an {@code Optional} describing {@code player} as a {@code RequiemPlayer}
     * @see #from(PlayerEntity)
     * @since 1.0.0
     */
    @Contract(pure = true)
    static Optional<RequiemPlayer> fromSafely(@Nullable PlayerEntity player) {
        if (player instanceof RequiemPlayer) {
            return Optional.of(RequiemPlayer.from(player));
        }
        return Optional.empty();
    }
}
