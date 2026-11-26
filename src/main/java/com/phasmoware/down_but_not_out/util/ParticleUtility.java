package com.phasmoware.down_but_not_out.util;

import com.phasmoware.down_but_not_out.config.ModConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Particle feedback. Only parameterless particles (no colour/size data) are used, and every particle is
 * sent on its own with no random spread, so Java and Geyser Bedrock clients get the same pattern.
 * Level broadcasts only reach players tracking the position.
 */
public class ParticleUtility {
    private static final int COMPLETE_RING_POINTS = 12;
    private static final int MAX_SPIRAL_POINTS = 4;
    private static final Map<String, ParticleOptions> CACHE = new HashMap<>();

    private static ParticleOptions resolve(String id, ParticleOptions fallback) {
        return CACHE.computeIfAbsent(id, key -> {
            Identifier identifier = Identifier.tryParse(key);
            ParticleType<?> type = identifier == null ? null : BuiltInRegistries.PARTICLE_TYPE.getValue(identifier);
            if (type instanceof SimpleParticleType simple) {
                return simple;
            }
            Constants.LOGGER.warn("Unknown or unsupported particle type '{}', using default", key);
            return fallback;
        });
    }

    private static void point(ServerPlayer player, ParticleOptions particle, double angle, double radius, double height) {
        player.level().sendParticles(particle, player.getX() + Math.cos(angle) * radius, player.getY() + height,
                player.getZ() + Math.sin(angle) * radius, 1, 0.0, 0.0, 0.0, 0.0);
    }

    // slow ring around the downed player, three points per burst
    public static void spawnDownedIndicator(ServerPlayer player) {
        int interval = ModConfig.INSTANCE.DOWNED_PARTICLE_INTERVAL_TICKS;
        if (!ModConfig.INSTANCE.DOWNED_PARTICLES_ENABLED || interval <= 0 || player.tickCount % interval != 0) {
            return;
        }
        ParticleOptions particle = resolve(ModConfig.INSTANCE.DOWNED_PARTICLE_TYPE, ParticleTypes.ANGRY_VILLAGER);
        double start = player.tickCount * 0.3;
        for (int i = 0; i < 3; i++) {
            point(player, particle, start + i * (2 * Math.PI / 3), 0.6, 0.4);
        }
    }

    // rising spiral: density and height grow with the shared revive progress (0.0 to 1.0)
    public static void spawnReviveProgress(ServerPlayer downed, double progress) {
        int interval = ModConfig.INSTANCE.REVIVE_PARTICLE_INTERVAL_TICKS;
        if (!ModConfig.INSTANCE.REVIVE_PARTICLES_ENABLED || interval <= 0 || downed.tickCount % interval != 0) {
            return;
        }
        ParticleOptions particle = resolve(ModConfig.INSTANCE.REVIVE_PARTICLE_TYPE, ParticleTypes.HAPPY_VILLAGER);
        int points = 1 + (int) (progress * (MAX_SPIRAL_POINTS - 1));
        double height = 0.2 + progress * 1.6;
        double start = downed.tickCount * 0.4;
        for (int i = 0; i < points; i++) {
            point(downed, particle, start + i * (2 * Math.PI / points), 0.7, height);
        }
    }

    // one burst on completion; nothing is spawned when a revive is cancelled
    public static void spawnReviveComplete(ServerPlayer revived) {
        if (!ModConfig.INSTANCE.REVIVE_PARTICLES_ENABLED) {
            return;
        }
        ParticleOptions particle = resolve(ModConfig.INSTANCE.REVIVE_COMPLETE_PARTICLE_TYPE, ParticleTypes.HEART);
        for (int i = 0; i < COMPLETE_RING_POINTS; i++) {
            point(revived, particle, i * (2 * Math.PI / COMPLETE_RING_POINTS), 0.9, 0.6);
        }
    }
}
