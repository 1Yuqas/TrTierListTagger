package one.yuqas.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import one.yuqas.utils.enums.TierType;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class APIUtils {
    private static final String API_URL = "https://api.trtierlist.com/api/profile/";
    private static final Gson GSON = new Gson();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final long CACHE_DURATION = 30_000;

    private static final Map<String, PlayerData> PLAYER_DATA = new ConcurrentHashMap<>();
    private static final Map<String, Long> FETCH_TIME = new ConcurrentHashMap<>();

    private static final List<String> TIER_ORDER = List.of(
            "LT5", "HT5", "LT4", "HT4", "LT3", "HT3", "LT2", "HT2", "LT1", "HT1"
    );

    public static Component getFormattedTier(TierType type, String playerName) {
        if (playerName == null || playerName.isEmpty()) return Component.empty();
        String key = playerName.toLowerCase();

        PlayerData data = PLAYER_DATA.get(key);
        if (data != null && data.hasTier(type)) {
            return data.getTierText(type);
        }

        long now = System.currentTimeMillis();
        if (now - FETCH_TIME.getOrDefault(key, 0L) > CACHE_DURATION) {
            FETCH_TIME.put(key, now);
            fetchAsync(playerName);
        }

        return Component.empty();
    }

    public static Component getFormattedTierCompact(TierType type, String playerName) {
        if (playerName == null || playerName.isEmpty()) return Component.empty();

        String key = playerName.toLowerCase();
        PlayerData data = PLAYER_DATA.get(key);

        if (data != null) {
            if (data.compactTierTexts.containsKey(type)) {
                return data.compactTierTexts.get(type);
            } else if (data.tierTexts.containsKey(type)) {
                return data.tierTexts.get(type);
            }
        }

        long now = System.currentTimeMillis();
        if (now - FETCH_TIME.getOrDefault(key, 0L) > CACHE_DURATION) {
            FETCH_TIME.put(key, now);
            fetchAsync(playerName);
        }

        return Component.empty();
    }

    private static void fetchAsync(String playerName) {
        EXECUTOR.submit(() -> fetchPlayerData(playerName));
    }

    public static void fetchSync(String playerName) {
        fetchPlayerData(playerName);
    }

    private static void fetchPlayerData(String playerName) {
        String key = playerName.toLowerCase();
        try {
            JsonObject json = fetchJson(playerName);
            if (json == null) return;

            if (json.has("error") || !json.has("rankings") || !json.get("rankings").isJsonObject()) {
                setError(key, "Oyuncu kayıt değil");
                return;
            }

            JsonObject rankings = json.getAsJsonObject("rankings");
            PlayerData data = new PlayerData();

            if (json.has("rank")) data.rank = json.get("rank").getAsInt();
            if (json.has("total_points")) data.totalPoints = json.get("total_points").getAsInt();

            for (TierType type : TierType.values()) {
                if (type == TierType.BEST) continue;

                String apiKey = type.name().toLowerCase();
                if (rankings.has(apiKey)) {
                    String tier = rankings.get(apiKey).getAsString();
                    if (tier != null && !tier.equalsIgnoreCase("none")) {
                        data.setTierText(type, formatTierText(type, tier, false));
                        data.setCompactTierText(type, formatTierText(type, tier, true));
                    }
                }
            }

            BestTierResult best = findBest(rankings);
            if (best != null) {
                Component bestText = formatBestText(best);
                data.setTierText(TierType.BEST, bestText);
                data.setCompactTierText(TierType.BEST, bestText);
            }

            PLAYER_DATA.put(key, data);

        } catch (Exception e) {
            setError(key, "Bağlantı hatası");
            e.printStackTrace();
        }
    }

    private static JsonObject fetchJson(String playerName) {
        String key = playerName.toLowerCase();
        try {
            URL url = new URL(API_URL + playerName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                String error = (responseCode == 404 || responseCode == 400)
                        ? "Oyuncu kayıt değil"
                        : "Sunucu hatası: " + responseCode;
                setError(key, error);
                return null;
            }

            try (InputStreamReader reader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, JsonObject.class);
            }
        } catch (Exception e) {
            setError(key, "Bağlantı hatası");
            return null;
        }
    }

    private static Component formatTierText(TierType type, String tier) {
        return formatTierText(type, tier, false);
    }

    private static Component formatTierText(TierType type, String tier, boolean compact) {
        String tierName = tier.toUpperCase();
        int tierColor = tierName.startsWith("HT")
                ? type.getHtColor()
                : type.getLtColor();

        Component component = Component.empty()
                .append(Component.literal(type.getIcon() + " "));

        if (!compact) {
            component = component.copy().append(
                    Component.literal(capitalize(type.name()) + ": ")
                            .withStyle(s -> s.withColor(type.getHtColor()))
            );
        }

        return component.copy().append(
                Component.literal(tierName)
                        .withStyle(s -> s.withColor(tierColor).withBold(false))
        );
    }

    private static Component formatBestText(BestTierResult best) {
        String tierName = best.tier.toUpperCase();
        int color = tierName.startsWith("HT") ? best.type.getHtColor() : best.type.getLtColor();

        return Component.empty()
                .append(Component.literal(best.type.getIcon() + " "))
                .append(Component.literal(tierName).withStyle(s -> s.withColor(color).withBold(false)));
    }

    private static BestTierResult findBest(JsonObject rankings) {
        String bestTier = null;
        TierType bestType = null;

        for (TierType type : TierType.values()) {
            if (type == TierType.BEST) continue;

            String apiKey = type.name().toLowerCase();
            if (!rankings.has(apiKey)) continue;

            String tier = rankings.get(apiKey).getAsString();
            if (tier == null || tier.equalsIgnoreCase("none")) continue;

            if (bestTier == null || isBetterTier(tier, bestTier)) {
                bestTier = tier;
                bestType = type;
            }
        }
        return (bestTier != null) ? new BestTierResult(bestType, bestTier) : null;
    }

    private static boolean isBetterTier(String current, String best) {
        int curIdx = TIER_ORDER.indexOf(current.toUpperCase());
        int bestIdx = TIER_ORDER.indexOf(best.toUpperCase());
        return curIdx > bestIdx;
    }

    public static List<Component> getAllTiers(String playerName) {
        PlayerData data = PLAYER_DATA.get(playerName.toLowerCase());
        if (data == null) return List.of();

        return data.getTierTexts().entrySet().stream()
                .filter(e -> e.getKey() != TierType.BEST)
                .map(Map.Entry::getValue)
                .toList();
    }

    public static String getError(String playerName) {
        PlayerData data = PLAYER_DATA.get(playerName.toLowerCase());
        return data != null ? data.error : null;
    }

    public static boolean hasData(String playerName) {
        return PLAYER_DATA.containsKey(playerName.toLowerCase());
    }

    public static int getPlayerRank(String playerName) {
        PlayerData data = PLAYER_DATA.get(playerName.toLowerCase());
        return data != null ? data.rank : -1;
    }

    public static int getPlayerTotalPoints(String playerName) {
        PlayerData data = PLAYER_DATA.get(playerName.toLowerCase());
        return data != null ? data.totalPoints : -1;
    }

    public static void clearCache() {
        PLAYER_DATA.clear();
        FETCH_TIME.clear();
    }

    private static void setError(String key, String error) {
        PlayerData data = PLAYER_DATA.computeIfAbsent(key, k -> new PlayerData());
        data.error = error;
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private static class PlayerData {
        final Map<TierType, Component> tierTexts = new ConcurrentHashMap<>();
        final Map<TierType, Component> compactTierTexts = new ConcurrentHashMap<>();

        int rank = -1;
        int totalPoints = -1;
        String error;

        void setTierText(TierType type, Component text) {
            tierTexts.put(type, text);
            error = null;
        }

        void setCompactTierText(TierType type, Component text) {
            compactTierTexts.put(type, text);
        }

        boolean hasTier(TierType type) {
            return tierTexts.containsKey(type) || compactTierTexts.containsKey(type);
        }

        Component getTierText(TierType type) {
            return tierTexts.get(type);
        }

        Component getCompactTierText(TierType type) {
            return compactTierTexts.get(type);
        }

        Map<TierType, Component> getTierTexts() {
            return Collections.unmodifiableMap(tierTexts);
        }
    }

    private record BestTierResult(TierType type, String tier) {}
}