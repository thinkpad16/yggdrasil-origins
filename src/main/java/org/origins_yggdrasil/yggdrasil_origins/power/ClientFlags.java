package org.origins_yggdrasil.yggdrasil_origins.power;

/**
 * A small mirror of the engine for the client: a few flags without which physics and
 * highlighting would render wrong (the client has no datapack data).
 */
public final class ClientFlags {

    public static boolean walkOnLava;
    public static boolean walkOnWater;
    public static boolean glowWounded;
    /** Lava carries the player the way water does. */
    public static boolean lavaSwim;
    /** The player sees through lava instead of a wall of orange. */
    public static boolean lavaSight;
    /** 0..1 - how bright night vision is (vanilla only knows 1.0). */
    public static float nightVisionStrength = 1f;
    /** Swim speed multiplier: the client needs it to predict movement. */
    public static float swimSpeed = 1f;

    private ClientFlags() { }

    /** Back to a player with no powers - on leaving a server. */
    public static void reset() {
        set(false, false, false, 1f, 1f, false, false);
    }

    public static void set(boolean lava, boolean water, boolean glow, float nightVision, float swim,
                           boolean swimsInLava, boolean seesInLava) {
        walkOnLava = lava;
        walkOnWater = water;
        glowWounded = glow;
        nightVisionStrength = nightVision;
        swimSpeed = swim;
        lavaSwim = swimsInLava;
        lavaSight = seesInLava;
    }
}
