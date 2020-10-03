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
package ladysnake.requiem.mixin.client.gui.hud;

import ladysnake.requiem.api.v1.RequiemPlayer;
import ladysnake.requiem.api.v1.event.minecraft.client.CrosshairRenderCallback;
import ladysnake.requiem.api.v1.event.minecraft.client.HotbarRenderCallback;
import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.api.v1.remnant.SoulbindingRegistry;
import ladysnake.requiem.common.entity.effect.AttritionStatusEffect;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.tag.Tag;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin extends DrawableHelper {

    @Unique
    private boolean skippedFood;

    @Shadow
    @Final
    private MinecraftClient client;
    @Unique
    private boolean boundSpecialBackground;
    @Unique
    private StatusEffectInstance renderedEffect;

    @Shadow
    private int scaledWidth;
    @Shadow
    private int scaledHeight;

    @Inject(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SrcFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DstFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SrcFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DstFactor;)V"), cancellable = true)
    private void colorCrosshair(MatrixStack matrices, CallbackInfo ci) {
        CrosshairRenderCallback.EVENT.invoker().onCrosshairRender(matrices, this.scaledWidth, this.scaledHeight);
    }

    @ModifyVariable(
        method = "renderStatusBars",
        slice = @Slice(
            // precise slice makes it more likely to detect errors from wrong variable index
            from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getArmor()I"),
            to = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/entity/player/PlayerEntity;getArmor()I", shift = At.Shift.AFTER)
        ),
        at = @At("STORE"),
        index = 20 // there are too many ints in this method, so we just take the variable index from the bytecode
    )
    private int preventArmorRender(int armor) {
        assert client.player != null;

        if (RemnantComponent.get(client.player).isIncorporeal()) {
            // Make everything that follows *invisible*
            return 0;
        }

        return armor;
    }

    @ModifyVariable(
        method = "renderStatusBars",
        slice = @Slice(
            // precise slice makes it more likely to detect errors from wrong variable index
            from = @At(value = "FIELD", target = "Lnet/minecraft/entity/attribute/EntityAttributes;GENERIC_MAX_HEALTH:Lnet/minecraft/entity/attribute/EntityAttribute;"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getAbsorptionAmount()F")
        ),
        at = @At(value = "STORE"),
        index = 13 // there are too many ints in this method, so we just take the variable index from the bytecode
    )
    private float preventHealthRender(float maxHealth) {
        assert client.player != null;
        if (RemnantComponent.get(client.player).isIncorporeal()) {
            return 0;
        }
        return maxHealth;
    }

    @ModifyVariable(
        method = "renderStatusBars",
        // precise slice makes it more likely to detect errors from wrong variable index
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getAbsorptionAmount()F"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;ceil(F)I", ordinal = 1)
        ),
        at = @At(value = "STORE"),
        index = 14 // there are too many ints in this method, so we just take the variable index from the bytecode
    )
    private int preventAbsorptionRender(int absorption) {
        assert client.player != null;
        if (RemnantComponent.get(client.player).isIncorporeal()) {
            return 0;
        }
        return absorption;
    }

    @ModifyVariable(
        method = "renderStatusBars",
        at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/gui/hud/InGameHud;getHeartCount(Lnet/minecraft/entity/LivingEntity;)I"),
        index = 23
    )
    private int preventFoodRender(int mountHeartCount) {
        RequiemPlayer player = (RequiemPlayer) this.client.player;
        if (mountHeartCount == 0 && player != null && RemnantComponent.get(this.client.player).isSoul()) {
            Possessable possessed = (Possessable) player.asPossessor().getPossessedEntity();
            if (possessed == null || !possessed.isRegularEater()) {
                skippedFood = true;
                return -1;
            }
        }
        skippedFood = false;
        return mountHeartCount;
    }

    @ModifyVariable(
        method = "renderStatusBars",
        at = @At(value = "CONSTANT", args = "stringValue=air"),
        index = 23
    )
    private int fixAirRender(int mountHeartCount) {
        if (skippedFood) return 0;
        return mountHeartCount;
    }

    @Redirect(
        method = "renderStatusBars",
        slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=air")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isSubmergedIn(Lnet/minecraft/tag/Tag;)Z")
    )
    private boolean preventAirRender(PlayerEntity playerEntity, Tag<Fluid> fluid) {
        if (RemnantComponent.get(playerEntity).isSoul()) {
            LivingEntity possessed = ((RequiemPlayer) playerEntity).asPossessor().getPossessedEntity();
            if (possessed == null) {
                return false;
            } else if (possessed.canBreatheInWater()) {
                return false;
            }
        }
        return playerEntity.isSubmergedIn(fluid);
    }

    @ModifyVariable(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J"), ordinal = 0)
    private int substituteHealth(int health) {
        assert client.player != null;
        LivingEntity entity = ((RequiemPlayer) client.player).asPossessor().getPossessedEntity();
        if (entity != null) {
            return MathHelper.ceil(entity.getHealth());
        }
        return health;
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void fireHotBarRenderEvent(float tickDelta, MatrixStack matrices, CallbackInfo info) {
        if (HotbarRenderCallback.EVENT.invoker().onHotbarRender(matrices, tickDelta) != ActionResult.PASS) {
            info.cancel();
        }
    }

    // ModifyVariable is only used to capture the local variable more easily
    @ModifyVariable(method = "renderStatusEffectOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V"))
    private StatusEffectInstance customizeDrawnBackground(StatusEffectInstance effect) {
        if (SoulbindingRegistry.instance().isSoulbound(effect.getEffectType())) {
            assert this.client != null;
            this.client.getTextureManager().bindTexture(AttritionStatusEffect.ATTRITION_BACKGROUND);
            boundSpecialBackground = true;
        }
        renderedEffect = effect;
        return effect;
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V", shift = At.Shift.AFTER))
    private void restoreDrawnBackground(CallbackInfo ci) {
        if (boundSpecialBackground) {
            this.client.getTextureManager().bindTexture(HandledScreen.BACKGROUND_TEXTURE);
            boundSpecialBackground = false;
        }
    }

    @ModifyVariable(method = "renderStatusEffectOverlay", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/texture/StatusEffectSpriteManager;getSprite(Lnet/minecraft/entity/effect/StatusEffect;)Lnet/minecraft/client/texture/Sprite;"))
    private Sprite customizeDrawnSprite(Sprite baseSprite) {
        return RequiemStatusEffects.substituteSprite(baseSprite, renderedEffect);
    }
}
