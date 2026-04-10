package one.yuqas.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import one.yuqas.utils.enums.Config;
import one.yuqas.utils.enums.TierType;

import java.awt.*;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;

public class APIUtils {
    private static final String API_URL = "https://api.trtierlist.com/api/profile/";
    private static final Gson GSON = new Gson();

    private static final ConcurrentHashMap<String, ConcurrentHashMap<TierType, Component>> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> ERRORS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> FETCH_TIME = new ConcurrentHashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    private static final List<String> TIER_ORDER = List.of(
            "LT5","HT5", "LT4","HT4", "LT3","HT3", "LT2","HT2", "LT1","HT1"
    );

    public static Component getFormattedTier(TierType type, String playerName) {
        if (playerName == null || playerName.isEmpty()) return Component.empty();
        String key = playerName.toLowerCase();

        if (CACHE.containsKey(key) && CACHE.get(key).containsKey(type)) {
            return CACHE.get(key).get(type);
        }

        if (System.currentTimeMillis() - FETCH_TIME.getOrDefault(key, 0L) > 30000) {
            FETCH_TIME.put(key, System.currentTimeMillis());
            fetchAsync(playerName);
        }

        return TierConfigUtil.getBoolean(Config.SHOW_PLACEHOLDER) ? Component.literal("§7...") : Component.empty();
    }

    private static void fetchAsync(String playerName) {
        EXECUTOR.submit(() -> {
            String key = playerName.toLowerCase();
            try {
                URL url = new URL(API_URL + playerName);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5000);
                con.setRequestProperty("User-Agent", "Mozilla/5.0");

                int responseCode = con.getResponseCode();
                if (responseCode != 200) {
                    if (responseCode == 404 || responseCode == 400) {
                        ERRORS.put(key, "Oyuncu kayıt değil");
                    } else {
                        ERRORS.put(key, "Sunucu hatası: " + responseCode);
                    }
                    return;
                }

                try (InputStreamReader reader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json.has("error")) {
                        ERRORS.put(key, "Oyuncu kayıt değil");
                        return;
                    }
                    if (!json.has("rankings")) {
                        ERRORS.put(key, "Tier verisi bulunamadı");
                        return;
                    }

                    JsonObject rankings = json.getAsJsonObject("rankings");
                    ConcurrentHashMap<TierType, Component> map = new ConcurrentHashMap<>();
                    ERRORS.remove(key);

                    for (TierType type : TierType.values()) {
                        if (type == TierType.BEST) continue;
                        String apiKey = type.name().toLowerCase();
                        if (rankings.has(apiKey)) {
                            String tier = rankings.get(apiKey).getAsString();
                            if (tier != null && !tier.equalsIgnoreCase("none")) {
                                String tierName = tier.toUpperCase();
                                int colorValue = tierName.startsWith("HT") ?
                                        new Color(0x48FF00).getRGB() : new Color(0xF6402A).getRGB();

                                String typeName = type.name().substring(0, 1).toUpperCase() + type.name().substring(1).toLowerCase();
                                Component formatted = Component.empty()
                                        .append(Component.literal(type.getIcon() + " "))
                                        .append(Component.literal(typeName + ": ").withStyle(style -> style.withColor(0xFF55FF))) // Purple
                                        .append(Component.literal(tierName).withStyle(style -> style.withColor(colorValue).withBold(true)));
                                map.put(type, formatted);
                            }
                        }
                    }

                    BestTierResult best = findBest(rankings);
                    if (best != null) {
                        String tierName = best.tier.toUpperCase();
                        int colorValue = tierName.startsWith("HT") ?
                                new Color(0x48FF00).getRGB() : new Color(0xF6402A).getRGB();

                        Component formattedTag = Component.empty()
                                .append(Component.literal(best.type.getIcon() + " "))
                                .append(Component.literal(tierName).withStyle(style -> style.withColor(colorValue).withBold(true)));

                        map.put(TierType.BEST, formattedTag);
                    }
                    CACHE.put(key, map);
                }
            } catch (Exception e) {
                ERRORS.put(playerName.toLowerCase(), "Bağlantı hatası");
                e.printStackTrace();
            }
        });
    }

    public static List<Component> getAllTiers(String playerName) {
        String key = playerName.toLowerCase();
        if (CACHE.containsKey(key)) {
            return CACHE.get(key).entrySet().stream()
                    .filter(e -> e.getKey() != TierType.BEST)
                    .map(java.util.Map.Entry::getValue)
                    .toList();
        }
        return List.of();
    }

    public static String getError(String playerName) {
        return ERRORS.get(playerName.toLowerCase());
    }

    public static boolean hasData(String playerName) {
        String key = playerName.toLowerCase();
        return CACHE.containsKey(key) || ERRORS.containsKey(key);
    }

    public static void fetchSync(String playerName) {
        fetchAsync(playerName);
    }


    private static String colorize(String text) {
        return text.replace("&", "§");
    }

    private static BestTierResult findBest(JsonObject rankings) {
        String bestTier = null;
        TierType bestType = null;

        for (TierType type : TierType.values()) {
            if (type == TierType.BEST) continue;

            String apiKey = type.name().toLowerCase();
            if (rankings.has(apiKey)) {
                String tier = rankings.get(apiKey).getAsString();
                if (tier == null || tier.equalsIgnoreCase("none")) continue;

                if (bestTier == null || isBetter(tier, bestTier)) {
                    bestTier = tier;
                    bestType = type;
                }
            }
        }
        return (bestTier != null) ? new BestTierResult(bestType, bestTier) : null;
    }

    private static boolean isBetter(String current, String best) {
        int currentIndex = TIER_ORDER.indexOf(current.toUpperCase());
        int bestIndex = TIER_ORDER.indexOf(best.toUpperCase());
        return currentIndex > bestIndex;
    }

    private record BestTierResult(TierType type, String tier) {}

    public static void clearCache() {
        CACHE.clear();
        FETCH_TIME.clear();
    }
}