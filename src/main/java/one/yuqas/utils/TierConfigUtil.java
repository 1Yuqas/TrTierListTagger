package one.yuqas.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import one.yuqas.TrTierListTagger;
import one.yuqas.utils.enums.Config;
import one.yuqas.utils.enums.TierType;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class TierConfigUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static JsonObject VALUES = new JsonObject();
    private static File configFile;

    public static void load() {
        try {
            File dir = new File("config");
            if (!dir.exists()) dir.mkdirs();

            configFile = new File(dir, TrTierListTagger.getId() + ".json");

            if (!configFile.exists()) {
                createDefaultFile();
            } else {
                readFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void set(Config key, String value) {
        if (!VALUES.has(key.getKey())) VALUES.add(key.getKey(), new JsonObject());
        VALUES.getAsJsonObject(key.getKey()).addProperty("value", value);
        save();
    }

    public static void set(Config key, boolean value) {
        if (!VALUES.has(key.getKey())) VALUES.add(key.getKey(), new JsonObject());
        VALUES.getAsJsonObject(key.getKey()).addProperty("value", value);
        save();
    }

    public static void save() {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            GSON.toJson(VALUES, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createDefaultFile() throws Exception {
        JsonObject root = new JsonObject();
        for (Config key : Config.values()) {
            JsonObject obj = new JsonObject();
            Object def = key.getDefaultValue();

            if (def instanceof Boolean) obj.addProperty("value", (Boolean) def);
            else if (def instanceof Number) obj.addProperty("value", (Number) def);
            else obj.addProperty("value", def.toString());

            root.add(key.getKey(), obj);
        }

        VALUES = root;
        save();
    }

    public static TierType getTierType() {
        String typeName = getString(Config.TIER_TYPE);
        try {
            return TierType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TierType.BEST;
        }
    }

    private static void readFile() throws Exception {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            JsonObject loaded = GSON.fromJson(reader, JsonObject.class);
            if (loaded != null) VALUES = loaded;
        }
    }

    public static boolean getBoolean(Config key) {
        try {
            return VALUES.getAsJsonObject(key.getKey()).get("value").getAsBoolean();
        } catch (Exception e) { return (boolean) key.getDefaultValue(); }
    }

    public static String getString(Config key) {
        try {
            return VALUES.getAsJsonObject(key.getKey()).get("value").getAsString();
        } catch (Exception e) { return key.getDefaultValue().toString(); }
    }
}
