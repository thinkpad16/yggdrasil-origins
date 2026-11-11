package org.origins_yggdrasil.yggdrasil_origins.visual;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Origin looks: body proportions and small parts (ears, fangs, horns...).
 * <p>
 * Part names are parsed into {@link VisualPart} once - when the data is read and when a
 * packet arrives - so only the enum remains inside a frame.
 */
public record VisualProfile(float height, float width, List<VisualPart> parts, int tint,
                            List<Integer> palette, List<Look> looks, List<VisualPart> optional) {

    public static final VisualProfile NONE =
        new VisualProfile(1f, 1f, List.of(), 0xFFFFFF, List.of(), List.of(), List.of());

    public VisualProfile(float height, float width, List<VisualPart> parts, int tint) {
        this(height, width, parts, tint, List.of(), List.of(), List.of());
    }

    public VisualProfile(float height, float width, List<VisualPart> parts, int tint,
                         List<Integer> palette, List<Look> looks) {
        this(height, width, parts, tint, palette, looks, List.of());
    }

    /** An element the origin offers but does not put on until the player asks for it. */
    public boolean optionalPart(VisualPart part) {
        return optional.contains(part);
    }

    /**
     * A ready-made look an origin offers its own people: a colour, a size and a shape for
     * each of the elements that origin wears. It is a starting point, not a cage - every
     * control on the picker still works afterwards.
     */
    public record Look(String id, int tint, float size, java.util.Map<VisualPart, Integer> shapes) {

        public static Look parse(JsonObject json) {
            if (json == null || !json.has("id")) return null;
            int tint = colour(json, "tint", VisualStyle.NO_TINT);
            float size = json.has("size") ? json.get("size").getAsFloat() : 1.0f;
            java.util.Map<VisualPart, Integer> shapes = new java.util.LinkedHashMap<>();
            if (json.has("shapes")) {
                for (var entry : json.getAsJsonObject("shapes").entrySet()) {
                    VisualPart part = VisualPart.of(entry.getKey());
                    if (part != null) shapes.put(part, entry.getValue().getAsInt());
                }
            }
            return new Look(json.get("id").getAsString(), tint, size, java.util.Map.copyOf(shapes));
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("i", id);
            json.addProperty("t", tint);
            json.addProperty("s", size);
            JsonObject shapeJson = new JsonObject();
            shapes.forEach((part, variant) -> shapeJson.addProperty(part.key(), variant));
            json.add("v", shapeJson);
            return json;
        }

        public static Look fromJson(JsonObject json) {
            java.util.Map<VisualPart, Integer> shapes = new java.util.LinkedHashMap<>();
            if (json.has("v")) {
                for (var entry : json.getAsJsonObject("v").entrySet()) {
                    VisualPart part = VisualPart.of(entry.getKey());
                    if (part != null) shapes.put(part, entry.getValue().getAsInt());
                }
            }
            return new Look(
                json.has("i") ? json.get("i").getAsString() : "",
                json.has("t") ? json.get("t").getAsInt() : VisualStyle.NO_TINT,
                json.has("s") ? json.get("s").getAsFloat() : 1.0f,
                java.util.Map.copyOf(shapes));
        }
    }

    /** Reads a "#rrggbb" field, falling back when it is missing or malformed. */
    private static int colour(JsonObject json, String key, int fallback) {
        if (!json.has(key)) return fallback;
        try {
            return Integer.parseInt(json.get(key).getAsString().replace("#", ""), 16);
        } catch (NumberFormatException broken) {
            return fallback;
        }
    }

    /** Bounds the data may not cross: a player must stay a player. */
    private static final float MIN_HEIGHT = 0.7f;
    /** 1.8 x 1.11 = 1.998 - exactly under a two-block ceiling, without suffocating. */
    private static final float MAX_HEIGHT = 1.11f;
    private static final float MIN_WIDTH = 0.8f;
    private static final float MAX_WIDTH = 1.25f;

    /** An unknown part is warned about once, not every time. */
    private static final Set<String> WARNED = new java.util.HashSet<>();

    public boolean isDefault() {
        return height == 1f && width == 1f && parts.isEmpty();
    }

    public boolean has(VisualPart part) {
        return parts.contains(part);
    }

    public static VisualProfile parse(JsonObject json) {
        if (json == null) return NONE;
        float height = clamp(json.has("height") ? json.get("height").getAsFloat() : 1f, MIN_HEIGHT, MAX_HEIGHT);
        float width = clamp(json.has("width") ? json.get("width").getAsFloat() : 1f, MIN_WIDTH, MAX_WIDTH);
        List<VisualPart> parts = new ArrayList<>();
        if (json.has("parts")) {
            for (var element : json.getAsJsonArray("parts")) {
                String name = element.getAsString();
                VisualPart part = VisualPart.of(name);
                if (part == null) {
                    if (WARNED.add(name)) {
                        YggdrasilOrigins.LOGGER.warn("Unknown visual part: {}", name);
                    }
                    continue;
                }
                if (!parts.contains(part)) parts.add(part);
            }
        }
        int tint = colour(json, "tint", 0xFFFFFF);
        // colours this origin is known by, offered before the general palette
        List<Integer> palette = new ArrayList<>();
        if (json.has("palette")) {
            for (var element : json.getAsJsonArray("palette")) {
                try {
                    palette.add(Integer.parseInt(element.getAsString().replace("#", ""), 16));
                } catch (NumberFormatException ignored) {
                    // a malformed swatch is dropped rather than shown as black
                }
            }
        }
        List<Look> looks = new ArrayList<>();
        if (json.has("looks")) {
            for (var element : json.getAsJsonArray("looks")) {
                Look look = Look.parse(element.getAsJsonObject());
                if (look != null) looks.add(look);
            }
        }
        List<VisualPart> optional = new ArrayList<>();
        if (json.has("optional")) {
            for (var element : json.getAsJsonArray("optional")) {
                VisualPart part = VisualPart.of(element.getAsString());
                if (part != null && !optional.contains(part)) optional.add(part);
            }
        }
        return new VisualProfile(height, width, List.copyOf(parts), tint,
            List.copyOf(palette), List.copyOf(looks), List.copyOf(optional));
    }

    /** Merges looks from several layers: multipliers multiply, parts add up. */
    public VisualProfile merge(VisualProfile other) {
        if (other == null || other == NONE) return this;
        List<VisualPart> merged = new ArrayList<>(parts);
        for (VisualPart part : other.parts()) {
            if (!merged.contains(part)) merged.add(part);
        }
        List<Integer> swatches = new ArrayList<>(palette);
        other.palette().forEach(colour -> {
            if (!swatches.contains(colour)) swatches.add(colour);
        });
        List<Look> allLooks = new ArrayList<>(looks);
        other.looks().forEach(look -> {
            if (allLooks.stream().noneMatch(known -> known.id().equals(look.id()))) {
                allLooks.add(look);
            }
        });
        List<VisualPart> bothOptional = new ArrayList<>(optional);
        for (VisualPart part : other.optional()) {
            if (!bothOptional.contains(part)) bothOptional.add(part);
        }
        return new VisualProfile(
            clamp(height * other.height(), MIN_HEIGHT, MAX_HEIGHT),
            clamp(width * other.width(), MIN_WIDTH, MAX_WIDTH),
            List.copyOf(merged),
            other.tint() != 0xFFFFFF ? other.tint() : tint,
            List.copyOf(swatches), List.copyOf(allLooks), List.copyOf(bothOptional));
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("h", height);
        json.addProperty("w", width);
        json.addProperty("t", tint);
        JsonArray array = new JsonArray();
        parts.forEach(part -> array.add(part.key()));
        json.add("p", array);
        if (!palette.isEmpty()) {
            JsonArray swatches = new JsonArray();
            palette.forEach(swatches::add);
            json.add("c", swatches);
        }
        if (!looks.isEmpty()) {
            JsonArray lookJson = new JsonArray();
            looks.forEach(look -> lookJson.add(look.toJson()));
            json.add("l", lookJson);
        }
        if (!optional.isEmpty()) {
            JsonArray off = new JsonArray();
            optional.forEach(part -> off.add(part.key()));
            json.add("o", off);
        }
        return json;
    }

    public static VisualProfile fromJson(JsonObject json) {
        List<VisualPart> parts = new ArrayList<>();
        if (json.has("p")) {
            for (var element : json.getAsJsonArray("p")) {
                VisualPart part = VisualPart.of(element.getAsString());
                if (part != null && !parts.contains(part)) parts.add(part);
            }
        }
        List<Integer> palette = new ArrayList<>();
        if (json.has("c")) {
            for (var element : json.getAsJsonArray("c")) palette.add(element.getAsInt());
        }
        List<Look> looks = new ArrayList<>();
        if (json.has("l")) {
            for (var element : json.getAsJsonArray("l")) {
                looks.add(Look.fromJson(element.getAsJsonObject()));
            }
        }
        List<VisualPart> optional = new ArrayList<>();
        if (json.has("o")) {
            for (var element : json.getAsJsonArray("o")) {
                VisualPart part = VisualPart.of(element.getAsString());
                if (part != null) optional.add(part);
            }
        }
        return new VisualProfile(
            json.has("h") ? json.get("h").getAsFloat() : 1f,
            json.has("w") ? json.get("w").getAsFloat() : 1f,
            List.copyOf(parts),
            json.has("t") ? json.get("t").getAsInt() : 0xFFFFFF,
            List.copyOf(palette), List.copyOf(looks), List.copyOf(optional));
    }

    /** Parts of one anchor, precomputed so the render layer need not filter every frame. */
    public EnumSet<VisualPart> byAnchor(VisualPart.Anchor anchor) {
        EnumSet<VisualPart> set = EnumSet.noneOf(VisualPart.class);
        for (VisualPart part : parts) {
            if (part.anchor() == anchor) set.add(part);
        }
        return set;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
