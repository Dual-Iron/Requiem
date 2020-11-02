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
package ladysnake.requiem.client;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.satin.api.event.ShaderEffectRenderCallback;
import ladysnake.satin.api.managed.ManagedFramebuffer;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
import ladysnake.satin.api.managed.uniform.Uniform1f;
import ladysnake.satin.api.managed.uniform.Uniform3f;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

import static ladysnake.requiem.client.FxHelper.impulse;
import static ladysnake.requiem.common.network.RequiemNetworking.*;

public final class RequiemFx implements ShaderEffectRenderCallback, ClientTickEvents.EndTick {
    public static final Identifier SPECTRE_SHADER_ID = Requiem.id("shaders/post/spectre.json");
    public static final Identifier FISH_EYE_SHADER_ID = Requiem.id("shaders/post/fish_eye.json");
    public static final Identifier ZOOM_SHADER_ID = Requiem.id("shaders/post/zoom.json");
    private static final float[] ETHEREAL_COLOR = {0.0f, 0.7f, 1.0f};

    public static final int PULSE_ANIMATION_TIME = 20;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ManagedShaderEffect spectreShader = ShaderEffectManager.getInstance().manage(SPECTRE_SHADER_ID);
    private final ManagedShaderEffect zoomShader = ShaderEffectManager.getInstance().manage(ZOOM_SHADER_ID);
    private final ManagedFramebuffer zoomFramebuffer = zoomShader.getTarget("zoom_focus");
    private float accentColorR;
    private float accentColorG;
    private float accentColorB;

    private int fishEyeAnimation = -1;
    private int etherealAnimation = 0;
    private int pulseAnimation;
    private int pulseIntensity;
    /**
     * Incremented every tick for animations
     */
    private int ticks = 0;
    private WeakReference<Entity> possessionTarget = new WeakReference<>(null);
    private final Uniform3f uniformOverlayColor = spectreShader.findUniform3f("OverlayColor");
    private final Uniform1f uniformZoom = spectreShader.findUniform1f("Zoom");
    private final Uniform1f uniformRaysIntensity = spectreShader.findUniform1f("RaysIntensity");
    private final Uniform1f uniformSolidIntensity = spectreShader.findUniform1f("SolidIntensity");
    private final Uniform1f uniformSlider = zoomShader.findUniform1f("Slider");
    private final Uniform1f uniformSTime = spectreShader.findUniform1f("STime");

    void registerCallbacks() {
        ShaderEffectRenderCallback.EVENT.register(this);
        ClientTickEvents.END_CLIENT_TICK.register(this);
    }

    @Override
    public void onEndTick(MinecraftClient client) {
        ++ticks;
        --etherealAnimation;
        if (--pulseAnimation < 0 && spectreShader.isInitialized()) {
            pulseIntensity = 1;
            uniformOverlayColor.set(ETHEREAL_COLOR[0], ETHEREAL_COLOR[1], ETHEREAL_COLOR[2]);
        }
        Entity possessed = getAnimationEntity();
        if (possessed != null) {
            if (--fishEyeAnimation == 2) {
                sendToServer(POSSESSION_REQUEST, createPossessionRequestBuffer(possessed));
            }
            assert client.player != null;
            if (!RemnantComponent.get(client.player).isIncorporeal()) {
                this.possessionTarget.clear();
            }
        }
    }

    public void onPossessionAck() {
        this.possessionTarget.clear();
    }

    @Nullable
    public Entity getAnimationEntity() {
        return this.possessionTarget.get();
    }

    public void beginFishEyeAnimation(Entity possessed) {
        this.fishEyeAnimation = 10;
        this.possessionTarget = new WeakReference<>(possessed);
    }

    public void beginEtherealAnimation() {
        this.etherealAnimation = 10;
    }

    public void playEtherealPulseAnimation(int intensity, float accentColorR, float accentColorG, float accentColorB) {
        this.pulseAnimation = PULSE_ANIMATION_TIME * intensity;
        this.accentColorR = accentColorR;
        this.accentColorG = accentColorG;
        this.accentColorB = accentColorB;
        uniformOverlayColor.set(accentColorR, accentColorG, accentColorB);
        this.pulseIntensity = intensity;
    }

    @Override
    public void renderShaderEffects(float tickDelta) {
        if (this.possessionTarget.get() != null) {
            uniformSlider.set((fishEyeAnimation - tickDelta) / 40 + 0.25f);
            zoomShader.render(tickDelta);
            zoomFramebuffer.clear();
        }

        assert mc.player != null;
        boolean incorporeal = RemnantComponent.get(mc.player).isIncorporeal();
        if (incorporeal || this.etherealAnimation > 0 || this.pulseAnimation >= 0) {
            // 10 -> 1
            float zoom = Math.max(1, (etherealAnimation - tickDelta));
            float intensity = (incorporeal ? 0.6f : 0f) / zoom;
            float rayIntensity = 1.0f;
            uniformSTime.set((ticks + tickDelta) / 20f);
            // 10 -> 1
            if (pulseAnimation >= 0) {
                // 10 -> 0 => 0 -> 1
                float progress = 1 - Math.max(0, pulseAnimation - tickDelta) / (PULSE_ANIMATION_TIME * this.pulseIntensity);
                float value = impulse(8, progress);
                intensity += value;
                zoom += value / 2f;
                if (incorporeal) {
                    float r = ETHEREAL_COLOR[0] * (1 - value) + this.accentColorR * value;
                    float g = ETHEREAL_COLOR[1] * (1 - value) + this.accentColorG * value;
                    float b = ETHEREAL_COLOR[2] * (1 - value) + this.accentColorB * value;
                    uniformOverlayColor.set(r, g, b);
                } else {
                    rayIntensity = value;
                }
            }
            uniformZoom.set(zoom);
            uniformRaysIntensity.set(rayIntensity);
            uniformSolidIntensity.set(intensity);
            spectreShader.render(tickDelta);
        }
    }

    public RenderLayer getZoomFx(RenderLayer base) {
        return this.zoomFramebuffer.getRenderLayer(base);
    }

}
