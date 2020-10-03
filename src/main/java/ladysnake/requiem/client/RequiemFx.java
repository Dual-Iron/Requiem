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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.RequiemPlayer;
import ladysnake.requiem.client.render.RequiemBuilderStorage;
import ladysnake.satin.api.event.EntitiesPostRenderCallback;
import ladysnake.satin.api.event.ResolutionChangeCallback;
import ladysnake.satin.api.event.ShaderEffectRenderCallback;
import ladysnake.satin.api.experimental.managed.Uniform1f;
import ladysnake.satin.api.experimental.managed.Uniform3f;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

import static ladysnake.requiem.client.FxHelper.impulse;
import static ladysnake.requiem.common.network.RequiemNetworking.*;

public final class RequiemFx implements EntitiesPostRenderCallback, ResolutionChangeCallback, ShaderEffectRenderCallback {
    public static final Identifier SPECTRE_SHADER_ID = Requiem.id("shaders/post/spectre.json");
    public static final Identifier FISH_EYE_SHADER_ID = Requiem.id("shaders/post/fish_eye.json");
    private static final float[] ETHEREAL_COLOR = {0.0f, 0.7f, 1.0f};

    public static final RequiemFx INSTANCE = new RequiemFx();
    public static final int PULSE_ANIMATION_TIME = 20;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ManagedShaderEffect spectreShader = ShaderEffectManager.getInstance().manage(SPECTRE_SHADER_ID);
    private final ManagedShaderEffect fishEyeShader = ShaderEffectManager.getInstance().manage(FISH_EYE_SHADER_ID);
    private float accentColorR;
    private float accentColorG;
    private float accentColorB;
    @Nullable
    private Framebuffer framebuffer;

    private int fishEyeAnimation = -1;
    private int etherealAnimation = 0;
    private int pulseAnimation;
    private int pulseIntensity;
    /**
     * Incremented every tick for animations
     */
    private int ticks = 0;
    @Nullable
    private WeakReference<Entity> possessionTarget;
    private Uniform3f uniformOverlayColor = spectreShader.findUniform3f("OverlayColor");
    private Uniform1f uniformZoom = spectreShader.findUniform1f("Zoom");
    private Uniform1f uniformRaysIntensity = spectreShader.findUniform1f("RaysIntensity");
    private Uniform1f uniformSolidIntensity = spectreShader.findUniform1f("SolidIntensity");
    private Uniform1f uniformSlider = fishEyeShader.findUniform1f("Slider");
    private Uniform1f uniformSTime = spectreShader.findUniform1f("STime");

    void registerCallbacks() {
        ShaderEffectRenderCallback.EVENT.register(this);
        EntitiesPostRenderCallback.EVENT.register(this);
        ResolutionChangeCallback.EVENT.register(this);
        ClientTickCallback.EVENT.register(this::update);
    }

    public void update(@SuppressWarnings("unused") MinecraftClient client) {
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
            if (!((RequiemPlayer) client.player).asRemnant().isIncorporeal()) {
                this.possessionTarget = null;
            }
        }
    }

    public void onPossessionAck() {
        this.possessionTarget = null;
    }

    @Nullable
    public Entity getAnimationEntity() {
        return this.possessionTarget != null ? this.possessionTarget.get() : null;
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
        drawZoomFramebuffer();

        if (this.possessionTarget != null && this.possessionTarget.get() != null) {
            uniformSlider.set((fishEyeAnimation - tickDelta) / 40 + 0.25f);
            fishEyeShader.render(tickDelta);
            if (this.possessionTarget != null && this.framebuffer != null) {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
                this.framebuffer.draw(this.mc.getWindow().getWidth(), this.mc.getWindow().getHeight(), false);
                MinecraftClient.getInstance().worldRenderer.drawEntityOutlinesFramebuffer();
            }
        }
        assert mc.player != null;
        boolean incorporeal = ((RequiemPlayer) mc.player).asRemnant().isIncorporeal();
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

    private boolean canDrawEntityOutlines() {
        return this.framebuffer != null && !this.fishEyeShader.isErrored() && MinecraftClient.getInstance().player != null;
    }

    private void drawZoomFramebuffer() {
        if (this.canDrawEntityOutlines()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
            RequiemBuilderStorage.INSTANCE.getRequiemVertexConsumers().draw();
            RenderSystem.disableBlend();
        }
    }

    @Override
    public void onEntitiesRendered(Camera camera, Frustum frustum, float tickDelta) {

    }

    @Override
    public void onResolutionChanged(int newWidth, int newHeight) {
        if (this.framebuffer != null) {
            this.framebuffer.resize(newWidth, newHeight, MinecraftClient.IS_SYSTEM_MAC);
        }
    }

    public Framebuffer getFramebuffer() {
        if (this.framebuffer == null) {
            this.framebuffer = new Framebuffer(mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, MinecraftClient.IS_SYSTEM_MAC);
        }
        return framebuffer;
    }
}
