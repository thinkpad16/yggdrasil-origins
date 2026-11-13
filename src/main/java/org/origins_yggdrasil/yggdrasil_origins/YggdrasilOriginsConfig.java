package org.origins_yggdrasil.yggdrasil_origins;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Add-on config: {@code config/yggdrasil_origins.json}.
 * The file is created on first launch with default values.
 */
public class YggdrasilOriginsConfig {

    private static final Path PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve(YggdrasilOrigins.MOD_ID + ".json");

    private static final Gson GSON = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setPrettyPrinting()
        .create();

    /** Explanations inside the file itself - rewritten on every load. */
    private static final String[] README = {
        "remove_vanilla_origins: silences the default Origins layer. Only matters if Origins",
        "  is still installed; without it the setting does nothing.",
        "  Works through a built-in datapack, enabled by default. Origins files are not touched.",
        "  Applies to NEW worlds; for an existing one run",
        "  /datapack disable \"file/yggdrasil_origins:disable_vanilla_origins\" (or enable), then /reload.",
        "  A server restart is required.",
        "enable_visuals: class looks - height, build and small details (ears, fangs, horns).",
        "  false - every player looks and collides exactly like vanilla Steve.",
        "  The server value wins: it drives hitboxes and what is sent to clients.",
        "enable_hud: ability bars above the hotbar. Purely client-side:",
        "  toggle with /ygghud if another interface mod draws over them.",
        "disabled_origins: classes to hide from the picker, e.g. [\"yggdrasil_origins:orc\"].",
        "  Re-read on /reload, no restart needed.",
        "grace_seconds: how long a player is warded right after picking an origin or respawning.",
        "  An undead who chooses at noon, or respawns into daylight, would otherwise burn before",
        "  they could move. During the grace the racial curse is off - and so is the gift that",
        "  comes with it, exactly like the ward gem. 0 switches the grace off entirely."
    };

    public String[] readme = README;

    public boolean removeVanillaOrigins = true;
    public boolean enableVisuals = true;
    /**
     * The player's own switch for seeing class looks on this client. Separate from
     * {@code enableVisuals}: in singleplayer and LAN the client and the server share this
     * file, and a personal preference must not turn hitboxes and looks off for everyone.
     */
    public boolean showVisuals = true;
    public boolean enableHud = true;
    public List<String> disabledOrigins = new ArrayList<>();
    public int graceSeconds = 30;

    public static YggdrasilOriginsConfig load() {

        YggdrasilOriginsConfig config = new YggdrasilOriginsConfig();

        if (Files.exists(PATH)) {
            try (BufferedReader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                YggdrasilOriginsConfig read = GSON.fromJson(reader, YggdrasilOriginsConfig.class);
                if (read != null) {
                    config = read;
                }
            } catch (Exception e) {
                // A broken file is never rewritten, so one stray comma cannot wipe the settings.
                YggdrasilOrigins.LOGGER.error("Could not read {}: {}. Falling back to defaults.", PATH, e.toString());
                return new YggdrasilOriginsConfig();
            }
        }

        if (config.disabledOrigins == null) {
            config.disabledOrigins = new ArrayList<>();
        }
        config.graceSeconds = Math.max(0, Math.min(600, config.graceSeconds));
        config.readme = README;
        config.save();

        return config;

    }

    /** Public: the client toggles looks right in the game. */
    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            YggdrasilOrigins.LOGGER.error("Could not write {}: {}", PATH, e.toString());
        }
    }
}
