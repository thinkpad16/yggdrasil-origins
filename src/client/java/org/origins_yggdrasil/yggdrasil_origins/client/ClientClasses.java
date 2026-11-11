package org.origins_yggdrasil.yggdrasil_origins.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Client snapshot of the class registry: sent by the server, lives until the next sync. */
public final class ClientClasses {

    private static final Gson GSON = new Gson();

    /** One ability as the picker shows it: the id plus whether it is a gain, a cost or neither. */
    public record PowerInfo(ResourceLocation id, int sense) {

        public Component name() {
            return Component.translatable("power." + id.getNamespace() + "." + id.getPath() + ".name");
        }

        public Component description() {
            return Component.translatable("power." + id.getNamespace() + "." + id.getPath() + ".description");
        }
    }

    public record ClassInfo(ResourceLocation id, ItemStack icon, int impact, List<PowerInfo> powers,
                            org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile visual) {

        public Component name() {
            return Component.translatable("origin." + id.getNamespace() + "." + id.getPath() + ".name");
        }

        private String key(String suffix) {
            return "origin." + id.getNamespace() + "." + id.getPath() + "." + suffix;
        }

        public Component description() {
            return Component.translatable(key("description"));
        }

        /** What the origin hands the player, shown in green. */
        public Component gives() {
            return Component.translatable(key("gives"));
        }

        /** What it takes in return, shown in red. */
        public Component pays() {
            return Component.translatable(key("pays"));
        }
    }

    public record LayerInfo(ResourceLocation id, boolean required, List<ClassInfo> classes) {

        public Component title() {
            return Component.translatable("layer." + id.getNamespace() + "." + id.getPath() + ".name");
        }
    }

    private static final Map<ResourceLocation, LayerInfo> LAYERS = new LinkedHashMap<>();
    private static final Map<String, String> CHOSEN = new LinkedHashMap<>();

    private ClientClasses() { }

    public static void accept(String json) {
        try {
            parse(json);
        } catch (Exception e) {
            org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins.LOGGER.error(
                "Broken packet from server, keeping previous data: {}", e.toString());
        }
    }

    private static void parse(String json) {
        // build into temporary maps: a broken packet must not leave the screen half empty
        Map<ResourceLocation, LayerInfo> layers = new LinkedHashMap<>();
        Map<String, String> chosen = new LinkedHashMap<>();
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        for (var layerElement : root.getAsJsonArray("layers")) {
            JsonObject layerJson = layerElement.getAsJsonObject();
            ResourceLocation layerId = ResourceLocation.parse(layerJson.get("id").getAsString());
            boolean required = layerJson.get("required").getAsBoolean();
            List<ClassInfo> classes = new ArrayList<>();
            for (var classElement : layerJson.getAsJsonArray("origins")) {
                JsonObject classJson = classElement.getAsJsonObject();
                ResourceLocation classId = ResourceLocation.parse(classJson.get("id").getAsString());
                ResourceLocation iconId = ResourceLocation.parse(classJson.get("icon").getAsString());
                ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(iconId));
                List<PowerInfo> powers = new ArrayList<>();
                for (var powerElement : classJson.getAsJsonArray("powers")) {
                    JsonObject powerJson = powerElement.getAsJsonObject();
                    powers.add(new PowerInfo(
                        ResourceLocation.parse(powerJson.get("id").getAsString()),
                        powerJson.has("sense") ? powerJson.get("sense").getAsInt() : 0));
                }
                var visual = classJson.has("visual")
                    ? org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile.fromJson(
                        classJson.getAsJsonObject("visual"))
                    : org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile.NONE;
                classes.add(new ClassInfo(classId, icon, classJson.get("impact").getAsInt(),
                    List.copyOf(powers), visual));
            }
            layers.put(layerId, new LayerInfo(layerId, required, List.copyOf(classes)));
        }
        if (root.has("flags")) {
            JsonObject flags = root.getAsJsonObject("flags");
            JsonObject numbers = root.has("numbers") ? root.getAsJsonObject("numbers") : new JsonObject();
            org.origins_yggdrasil.yggdrasil_origins.power.ClientFlags.set(
                flags.has("walk_on_lava") && flags.get("walk_on_lava").getAsBoolean(),
                flags.has("walk_on_water") && flags.get("walk_on_water").getAsBoolean(),
                flags.has("glow_wounded") && flags.get("glow_wounded").getAsBoolean(),
                numbers.has("night_vision_strength")
                    ? numbers.get("night_vision_strength").getAsFloat() : 1f,
                numbers.has("swim_speed") ? numbers.get("swim_speed").getAsFloat() : 1f,
                flags.has("lava_swim") && flags.get("lava_swim").getAsBoolean(),
                flags.has("lava_sight") && flags.get("lava_sight").getAsBoolean());
        }
        if (root.has("chosen")) {
            root.getAsJsonObject("chosen").entrySet()
                .forEach(entry -> chosen.put(entry.getKey(), entry.getValue().getAsString()));
        }

        LAYERS.clear();
        LAYERS.putAll(layers);
        CHOSEN.clear();
        CHOSEN.putAll(chosen);
    }

    public static Optional<LayerInfo> layer(ResourceLocation id) {
        return Optional.ofNullable(LAYERS.get(id));
    }

    public static List<LayerInfo> layers() {
        return List.copyOf(LAYERS.values());
    }

    public static Optional<ResourceLocation> chosen(ResourceLocation layer) {
        String value = CHOSEN.get(layer.toString());
        return value == null ? Optional.empty() : Optional.ofNullable(ResourceLocation.tryParse(value));
    }
}
