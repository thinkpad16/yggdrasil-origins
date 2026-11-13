package org.origins_yggdrasil.yggdrasil_origins.clazz;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Our own registry of origins and selection layers - no Origins mod involved.
 * Data is read from any datapack:
 * <pre>
 *   data/&lt;namespace&gt;/origins/&lt;id&gt;.json      {"icon": {...}, "impact": 0..3, "order": N, "powers": [...]}
 *   data/&lt;namespace&gt;/selection/&lt;layer&gt;.json  {"order": N, "required": true, "default": "...",
 *                                              "entries": [{"id": "...", "requires": "..."}]}
 * </pre>
 * Powers keep the Apoli JSON shape: we do not rewrite the format, our engine runs it.
 */
public final class ClassRegistry implements IdentifiableResourceReloadListener, ResourceManagerReloadListener {

    public static final ClassRegistry INSTANCE = new ClassRegistry();
    private static final Gson GSON = new Gson();
    private static final ResourceLocation FABRIC_ID =
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "origins");

    public record Def(ResourceLocation id, ResourceLocation icon, int impact, int order,
                      List<ResourceLocation> powers,
                      org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile visual) { }

    public record Entry(ResourceLocation id, Optional<ResourceLocation> requires) { }

    public record Layer(ResourceLocation id, int order, boolean required,
                        Optional<ResourceLocation> defaultId, List<Entry> entries) { }

    private Map<ResourceLocation, Def> defs = Map.of();
    private List<Layer> layers = List.of();

    private ClassRegistry() { }

    public Map<ResourceLocation, Def> defs() {
        return defs;
    }

    public List<Layer> layers() {
        return layers;
    }

    /** Powers with "hidden": true - granted by the engine, hidden from the picker. */
    private Set<ResourceLocation> hiddenPowers = Set.of();

    /**
     * Whether a power helps the player (+1), costs him (-1) or is neither (0), worked out from
     * the power's own data rather than from its text, so the picker can colour it and never
     * disagree with what the engine actually does.
     */
    private Map<ResourceLocation, Integer> powerSense = Map.of();

    public Optional<Def> def(ResourceLocation id) {
        return Optional.ofNullable(defs.get(id));
    }

    public Optional<Layer> layer(ResourceLocation id) {
        return layers.stream().filter(layer -> layer.id().equals(id)).findFirst();
    }

    /** Classes of a layer available given choices in other layers and the config. */
    public List<Def> available(Layer layer, Map<String, String> chosen) {
        Set<String> disabled = Set.copyOf(YggdrasilOrigins.config().disabledOrigins);
        List<Def> out = new ArrayList<>();
        for (Entry entry : layer.entries()) {
            if (disabled.contains(entry.id().toString())) continue;
            if (entry.requires().isPresent() && !chosen.containsValue(entry.requires().get().toString())) continue;
            def(entry.id()).ifPresent(out::add);
        }
        out.sort(Comparator.comparingInt(Def::order).thenComparing(def -> def.id().toString()));
        return out;
    }

    @Override
    public ResourceLocation getFabricId() {
        return FABRIC_ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {

        Map<ResourceLocation, JsonObject> powerJsons = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Resource> found
                : manager.listResources("powers", id -> id.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation file = found.getKey();
            try (InputStreamReader reader = new InputStreamReader(found.getValue().open(), StandardCharsets.UTF_8)) {
                powerJsons.put(trim(file, "powers/"), GSON.fromJson(reader, JsonObject.class));
            } catch (Exception e) {
                YggdrasilOrigins.LOGGER.error("Could not read power {}: {}", file, e.toString());
            }
        }
        PowerEngine.INSTANCE.load(powerJsons);
        layersJson.clear();

        // utility powers (timers, counters) exist for the engine only; they have
        // no place on the picker screen and no translation either
        Set<ResourceLocation> readHidden = new LinkedHashSet<>();
        powerJsons.forEach((id, json) -> {
            if (json != null && json.has("hidden") && json.get("hidden").getAsBoolean()) {
                readHidden.add(id);
            }
        });
        hiddenPowers = Set.copyOf(readHidden);

        Map<ResourceLocation, Integer> readSense = new LinkedHashMap<>();
        powerJsons.forEach((id, json) -> readSense.put(id, PowerSense.of(json)));
        powerSense = Map.copyOf(readSense);

        Map<ResourceLocation, Def> readDefs = new LinkedHashMap<>();
        List<Layer> readLayers = new ArrayList<>();
        int brokenPowers = 0;

        for (Map.Entry<ResourceLocation, Resource> found
                : manager.listResources("origins", id -> id.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation file = found.getKey();
            ResourceLocation id = trim(file, "origins/");
            try (InputStreamReader reader = new InputStreamReader(found.getValue().open(), StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                ResourceLocation icon = ResourceLocation.parse(json.has("icon")
                    ? json.getAsJsonObject("icon").get("id").getAsString()
                    : "minecraft:player_head");
                int impact = json.has("impact") ? json.get("impact").getAsInt() : 0;
                int order = json.has("order") ? json.get("order").getAsInt() : 0;
                List<ResourceLocation> powers = new ArrayList<>();
                if (json.has("powers")) {
                    for (var element : json.getAsJsonArray("powers")) {
                        ResourceLocation power = ResourceLocation.parse(element.getAsString());
                        if (!PowerEngine.INSTANCE.hasData(power)) {
                            YggdrasilOrigins.LOGGER.error("Origin {}: power {} is missing from the data", id, power);
                            brokenPowers++;
                            continue;
                        }
                        powers.add(power);
                    }
                }
                var visual = org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile.parse(
                    json.has("visual") ? json.getAsJsonObject("visual") : null);
                readDefs.put(id, new Def(id, icon, impact, order, List.copyOf(powers), visual));
            } catch (Exception e) {
                YggdrasilOrigins.LOGGER.error("Could not read class {}: {}", file, e.toString());
            }
        }

        for (Map.Entry<ResourceLocation, Resource> found
                : manager.listResources("selection", id -> id.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation file = found.getKey();
            ResourceLocation id = trim(file, "selection/");
            try (InputStreamReader reader = new InputStreamReader(found.getValue().open(), StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                int order = json.has("order") ? json.get("order").getAsInt() : 0;
                boolean required = !json.has("required") || json.get("required").getAsBoolean();
                Optional<ResourceLocation> defaultId = json.has("default")
                    ? Optional.of(ResourceLocation.parse(json.get("default").getAsString()))
                    : Optional.empty();
                List<Entry> entries = new ArrayList<>();
                JsonArray array = json.has("entries") ? json.getAsJsonArray("entries") : new JsonArray();
                for (var element : array) {
                    JsonObject row = element.getAsJsonObject();
                    ResourceLocation classId = ResourceLocation.parse(row.get("id").getAsString());
                    Optional<ResourceLocation> requires = row.has("requires")
                        ? Optional.of(ResourceLocation.parse(row.get("requires").getAsString()))
                        : Optional.empty();
                    entries.add(new Entry(classId, requires));
                }
                readLayers.add(new Layer(id, order, required, defaultId, List.copyOf(entries)));
            } catch (Exception e) {
                YggdrasilOrigins.LOGGER.error("Could not read selection layer {}: {}", file, e.toString());
            }
        }

        readLayers.sort(Comparator.comparingInt(Layer::order).thenComparing(layer -> layer.id().toString()));
        defs = Map.copyOf(readDefs);
        layers = List.copyOf(readLayers);

        // one info line per load - everything else only when something is wrong
        YggdrasilOrigins.LOGGER.info("Loaded: {} origins, {} layers, {} powers",
            defs.size(), layers.size(), PowerEngine.INSTANCE.grantedTypes());
        if (brokenPowers > 0) {
            YggdrasilOrigins.LOGGER.warn("Broken power references: {}", brokenPowers);
        }
        // in a big pack foreign attributes vanish silently: one line if there is anything to fix
        int missing = PowerEngine.INSTANCE.missingAttributeUsers().size();
        int starved = PowerEngine.INSTANCE.attributeStarvedPowers().size();
        int unknown = PowerEngine.INSTANCE.unsupportedPowers().size();
        if (missing > 0) {
            YggdrasilOrigins.LOGGER.warn(
                "{} attributes come from mods that are not installed, so {} abilities stay off."
                    + " This is expected without the RPG stack. Details: /ygg doctor",
                missing, starved);
        }
        if (unknown > 0) {
            YggdrasilOrigins.LOGGER.warn(
                "{} abilities use a type this engine does not know and will not run."
                    + " Details: /ygg doctor", unknown);
        }
    }

    private static ResourceLocation trim(ResourceLocation file, String prefix) {
        String path = file.getPath();
        path = path.substring(prefix.length(), path.length() - ".json".length());
        return ResourceLocation.fromNamespaceAndPath(file.getNamespace(), path);
    }

    /**
     * The "layers" part of the snapshot, already written out, per set of choices. It only
     * changes with the data, and the same few choices are shared by many players: building it
     * once instead of on every join, respawn and reload of every player saves real work.
     */
    private final Map<Map<String, String>, String> layersJson = new java.util.concurrent.ConcurrentHashMap<>();

    /** Client snapshot: everything the picker needs (names and texts come from lang). */
    public String snapshot(Map<String, String> chosen, Map<String, Boolean> flags,
                           Map<String, Float> numbers) {
        String layersPart = layersJson.get(chosen);
        if (layersPart == null) {
            layersPart = GSON.toJson(layersArray(chosen));
            // a bound for safety: the number of choice combinations is small in practice
            if (layersJson.size() > 512) layersJson.clear();
            layersJson.put(Map.copyOf(chosen), layersPart);
        }
        JsonObject chosenJson = new JsonObject();
        chosen.forEach(chosenJson::addProperty);
        JsonObject flagsJson = new JsonObject();
        flags.forEach(flagsJson::addProperty);
        JsonObject numbersJson = new JsonObject();
        numbers.forEach(numbersJson::addProperty);
        return "{\"layers\":" + layersPart
            + ",\"chosen\":" + GSON.toJson(chosenJson)
            + ",\"flags\":" + GSON.toJson(flagsJson)
            + ",\"numbers\":" + GSON.toJson(numbersJson) + "}";
    }

    private JsonArray layersArray(Map<String, String> chosen) {
        JsonArray layerArray = new JsonArray();
        for (Layer layer : layers) {
            JsonObject layerJson = new JsonObject();
            layerJson.addProperty("id", layer.id().toString());
            layerJson.addProperty("required", layer.required());
            JsonArray classArray = new JsonArray();
            for (Def def : available(layer, chosen)) {
                JsonObject defJson = new JsonObject();
                defJson.addProperty("id", def.id().toString());
                defJson.addProperty("icon", def.icon().toString());
                defJson.addProperty("impact", def.impact());
                JsonArray powerArray = new JsonArray();
                def.powers().stream()
                    .filter(power -> !hiddenPowers.contains(power))
                    .forEach(power -> {
                        JsonObject powerJson = new JsonObject();
                        powerJson.addProperty("id", power.toString());
                        powerJson.addProperty("sense", powerSense.getOrDefault(power, 0));
                        powerArray.add(powerJson);
                    });
                defJson.add("powers", powerArray);
                // looks, so the picker can show a live portrait of the class
                if (!def.visual().isDefault() || def.visual().tint() != 0xFFFFFF) {
                    defJson.add("visual", def.visual().toJson());
                }
                classArray.add(defJson);
            }
            layerJson.add("origins", classArray);
            layerArray.add(layerJson);
        }
        return layerArray;
    }
}
