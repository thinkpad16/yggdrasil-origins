package org.origins_yggdrasil.yggdrasil_origins.visual;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a player did to their own looks after the origin and the path were settled: the colour,
 * the size, the nudge and the variant of each part they carry.
 * <p>
 * This never touches the hitbox. An origin decides how tall and how wide a body is - a player
 * may only change what hangs off it, so two players of the same origin still collide the same.
 */
public record VisualStyle(Map<VisualPart, Trim> trims) {

    public static final VisualStyle EMPTY = new VisualStyle(Map.of());

    /**
     * Whether anything but the colour of an element is in a player's hands.
     * <p>
     * It is off: a player picks colours and nothing else. Everything the full editor could
     * set - shape, size, seat, tilt, shine, whether an element is worn at all - is dropped
     * wherever a look is read, so a look written while it was on, or sent by a client that
     * was changed, cannot move an element either. Turning this back on is the single switch
     * that brings {@code CustomiseScreen} back into play.
     */
    public static final boolean TRIM_ENABLED = false;

    /**
     * Bounds a player may not cross, so nobody turns their horns into a wall. Growing an
     * element is what made it swallow the face it sits on and crash into its neighbours,
     * so there is very little room upwards: a tenth, no more. Shrinking is safe - a small
     * element only ever sits closer to the body - so it keeps a wider run.
     */
    public static final float MIN_SCALE = 0.80F;
    public static final float MAX_SCALE = 1.10F;
    /** Nudge range in model pixels - enough to seat a part, not enough to detach it. */
    public static final float MAX_SHIFT = 1.0F;
    public static final int NO_TINT = -1;
    /** How far the shine of a glowing part may be turned up or down. */
    public static final float MIN_GLOW = 0.40F;
    public static final float MAX_GLOW = 1.60F;

    /**
     * One part's settings. {@code tint} is {@link #NO_TINT} while the part still takes the
     * origin's own colour.
     */
    public record Trim(int tint, float scale, float dx, float dy, float dz, int variant,
                       float roll, float glow, boolean hidden, boolean shown) {

        public static final Trim DEFAULT =
            new Trim(NO_TINT, 1.0F, 0.0F, 0.0F, 0.0F, 0, 0.0F, 1.0F, false, false);

        /** The same settings with the element put away, or brought back out. */
        public Trim withHidden(boolean value) {
            return new Trim(tint, scale, dx, dy, dz, variant, roll, glow, value, shown);
        }

        /** Marks an element the origin only offers as one the player has asked for. */
        public Trim withShown(boolean value) {
            return new Trim(tint, scale, dx, dy, dz, variant, roll, glow, hidden, value);
        }

        public Trim(int tint, float scale, float dx, float dy, float dz, int variant,
                    float roll, float glow) {
            this(tint, scale, dx, dy, dz, variant, roll, glow, false, false);
        }

        /** How far a part may be tilted either way, in radians - about ten degrees. */
        public static final float MAX_ROLL = 0.18F;

        public boolean isDefault() {
            return equals(DEFAULT);
        }

        /** Reins a part's settings in to what that particular part allows. */
        public Trim clamped(VisualPart part) {
            // colour only: everything else goes back to the way the origin wears it
            if (!TRIM_ENABLED) {
                return tint == NO_TINT ? DEFAULT : DEFAULT.withTint(tint);
            }
            return new Trim(
                tint,
                Math.max(part.minScale(), Math.min(part.maxScale(), scale)),
                clampTo(dx, part.maxShift()), clampTo(dy, part.maxShift()),
                clampTo(dz, part.maxShift()),
                part.variants() <= 1 ? 0 : Math.floorMod(variant, part.variants()),
                clampTo(roll, part.maxRoll()),
                Math.max(MIN_GLOW, Math.min(MAX_GLOW, glow)),
                hidden, shown);
        }

        private static float clampTo(float value, float bound) {
            return Math.max(-bound, Math.min(bound, value));
        }

        public Trim clamped(int variants) {
            return clamped(variants, MIN_SCALE, MAX_SCALE);
        }

        public Trim clamped(int variants, float minScale, float maxScale) {
            return new Trim(
                tint,
                Math.max(minScale, Math.min(maxScale, scale)),
                clampShift(dx), clampShift(dy), clampShift(dz),
                variants <= 1 ? 0 : Math.floorMod(variant, variants),
                Math.max(-MAX_ROLL, Math.min(MAX_ROLL, roll)),
                Math.max(MIN_GLOW, Math.min(MAX_GLOW, glow)),
                hidden, shown);
        }

        private static float clampShift(float value) {
            return Math.max(-MAX_SHIFT, Math.min(MAX_SHIFT, value));
        }

        public Trim withTint(int value) {
            return new Trim(value, scale, dx, dy, dz, variant, roll, glow, hidden, shown);
        }

        public Trim withScale(float value) {
            return new Trim(tint, value, dx, dy, dz, variant, roll, glow, hidden, shown);
        }

        public Trim withShift(float x, float y, float z) {
            return new Trim(tint, scale, x, y, z, variant, roll, glow, hidden, shown);
        }

        public Trim withVariant(int value) {
            return new Trim(tint, scale, dx, dy, dz, value, roll, glow, hidden, shown);
        }

        public Trim withRoll(float value) {
            return new Trim(tint, scale, dx, dy, dz, variant, value, glow, hidden, shown);
        }

        public Trim withGlow(float value) {
            return new Trim(tint, scale, dx, dy, dz, variant, roll, value, hidden, shown);
        }
    }

    public Trim of(VisualPart part) {
        return trims.getOrDefault(part, Trim.DEFAULT);
    }

    public boolean isEmpty() {
        return trims.isEmpty();
    }

    public VisualStyle with(VisualPart part, Trim trim) {
        Map<VisualPart, Trim> copy = new LinkedHashMap<>(trims);
        if (trim == null || trim.isDefault()) copy.remove(part);
        else copy.put(part, trim);
        return new VisualStyle(Map.copyOf(copy));
    }

    /** Compact on purpose: this rides along with every look update. */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        trims.forEach((part, trim) -> json.addProperty(part.key(), encode(trim)));
        return json;
    }

    public static VisualStyle fromJson(JsonObject json) {
        if (json == null || json.isEmpty()) return EMPTY;
        Map<VisualPart, Trim> read = new LinkedHashMap<>();
        for (var entry : json.entrySet()) {
            VisualPart part = VisualPart.of(entry.getKey());
            if (part == null) continue;
            Trim trim = decode(entry.getValue().getAsString()).clamped(part);
            if (!trim.isDefault()) read.put(part, trim);
        }
        return read.isEmpty() ? EMPTY : new VisualStyle(Map.copyOf(read));
    }

    public static String encode(Trim trim) {
        return trim.tint() + ";" + round(trim.scale()) + ";" + round(trim.dx()) + ";"
            + round(trim.dy()) + ";" + round(trim.dz()) + ";" + trim.variant()
            + ";" + round(trim.roll()) + ";" + round(trim.glow())
            + (trim.hidden() ? ";h" : trim.shown() ? ";s" : "");
    }

    public static Trim decode(String raw) {
        if (raw == null) return Trim.DEFAULT;
        String[] bits = raw.split(";");
        if (bits.length < 6) return Trim.DEFAULT;
        try {
            return new Trim(
                Integer.parseInt(bits[0]),
                Float.parseFloat(bits[1]),
                Float.parseFloat(bits[2]),
                Float.parseFloat(bits[3]),
                Float.parseFloat(bits[4]),
                Integer.parseInt(bits[5]),
                bits.length > 6 ? Float.parseFloat(bits[6]) : 0.0F,
                // written before glow existed: those looks keep the plain shine
                bits.length > 7 ? Float.parseFloat(bits[7]) : 1.0F,
                // written before an element could be put away: those looks all show
                bits.length > 8 && bits[8].equals("h"),
                bits.length > 8 && bits[8].equals("s")).clamped(Integer.MAX_VALUE);
        } catch (NumberFormatException broken) {
            return Trim.DEFAULT;
        }
    }

    private static float round(float value) {
        return Math.round(value * 100.0F) / 100.0F;
    }

    /** The stored form: part key -> packed settings, ready for a player attachment. */
    public Map<String, String> toStorage() {
        Map<String, String> out = new LinkedHashMap<>();
        trims.forEach((part, trim) -> out.put(part.key(), encode(trim)));
        return out;
    }

    public static VisualStyle fromStorage(Map<String, String> stored) {
        if (stored == null || stored.isEmpty()) return EMPTY;
        Map<VisualPart, Trim> read = new LinkedHashMap<>();
        stored.forEach((key, value) -> {
            VisualPart part = VisualPart.of(key);
            if (part == null) return;
            Trim trim = decode(value).clamped(part);
            if (!trim.isDefault()) read.put(part, trim);
        });
        return read.isEmpty() ? EMPTY : new VisualStyle(Map.copyOf(read));
    }
}
