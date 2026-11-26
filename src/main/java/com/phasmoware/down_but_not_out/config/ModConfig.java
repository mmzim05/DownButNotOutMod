package com.phasmoware.down_but_not_out.config;

public final class ModConfig {
    public static ModConfig INSTANCE;

    // default values set here
    public boolean MOD_ENABLED = true;
    public boolean SKIP_DOWNED_STATE_IF_NO_OTHER_PLAYERS_ONLINE = true;
    public boolean REVIVING_REQUIRES_EMPTY_HAND = true;
    public boolean ALLOW_DOWNED_STATE_IN_LAVA = false;
    public boolean USE_OVERLAY_MESSAGES = true;
    public boolean BROADCAST_PLAYER_DOWNED_NOTIFICATIONS = true;
    public boolean DOWNED_PLAYERS_HAVE_GLOW_EFFECT = true;
    public boolean DOWNED_PLAYERS_HAVE_BLINDNESS_EFFECT = true;
    public boolean RESTRICT_REVIVE_TO_TEAMMATES_OR_TEAMLESS = false;
    public boolean USE_CUSTOM_DOWNED_TEAMS = true;
    public boolean ALLOW_CHANGE_GAME_MODE = true;
    public boolean SHOW_REVIVE_TAG_ABOVE_PLAYER = true;
    public long REVIVE_PENALTY_COOLDOWN_TICKS = 600L;
    public int REVIVE_PENALTY_MULTIPLIER = 4;
    public long BLEEDING_OUT_DURATION_TICKS = 900L;
    public long REVIVE_DURATION_TICKS = 60L;
    public float HEARTBEAT_SOUND_VOLUME = 1.0F;
    public float REVIVED_SOUND_VOLUME = 2.5F;
    public float DOWNED_SOUND_VOLUME = 1.0F;
    public float DOWNED_MOVE_SPEED = 0.01F;
    public double MULTI_REVIVER_REDUCTION_PERCENT = 10.0D;
    public double MULTI_REVIVER_MIN_DURATION_PERCENT = 25.0D;
    public boolean DOWNED_PARTICLES_ENABLED = true;
    public int DOWNED_PARTICLE_INTERVAL_TICKS = 10;
    public boolean COLOR_REVIVE_TAG_BY_BLEED_OUT_TIME = true;
    public boolean REVIVER_SLOWNESS_ENABLED = true;
    public int REVIVER_SLOWNESS_AMPLIFIER = 1;
    public String DOWNED_PARTICLE_TYPE = "minecraft:angry_villager";
    public boolean REVIVE_PARTICLES_ENABLED = true;
    public String REVIVE_PARTICLE_TYPE = "minecraft:happy_villager";
    public int REVIVE_PARTICLE_INTERVAL_TICKS = 2;
    public String REVIVE_COMPLETE_PARTICLE_TYPE = "minecraft:heart";
    public long REVIVE_GRACE_TICKS = 10L;
    public double REVIVE_AIM_TOLERANCE = 0.3D;
    public boolean DOWNED_LOCK_POSITION = false;
    public float REVIVED_HEALTH = 0.0F;

    public ModConfig() {
    }


    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = ConfigUtility.loadConfig();
        }
    }

}
