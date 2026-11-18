package one.yuqas.utils;

import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import one.yuqas.TrTierListTagger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TelemetryManager {
    private static final String HEARTBEAT_URL = "https://api.trtierlist.com/api/telemetry/heartbeat";
    private static final long INTERVAL_MINUTES = 5;

    private static ScheduledExecutorService scheduler;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static synchronized void start() {
        if (!TierConfigUtil.isTelemetryEnabled()) {
            return;
        }

        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "TrTierListTagger-Telemetry");
            thread.setDaemon(true);
            return thread;
        });

        scheduler.scheduleAtFixedRate(
                TelemetryManager::sendHeartbeat,
                0,
                INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    private static void sendHeartbeat() {
        try {
            if (!TierConfigUtil.isTelemetryEnabled()) {
                stop();
                return;
            }

            String modVersion = getModVersion();
            String accountType = getAccountType();

            JsonObject payload = new JsonObject();
            payload.addProperty("type", accountType);
            payload.addProperty("version", modVersion);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HEARTBEAT_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "TrTierListTagger/" + modVersion)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                    .build();

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> null);
        } catch (Exception ignored) {
        }
    }

    private static String getAccountType() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getSession() != null) {
                UUID uuid = client.getSession().getUuidOrNull();
                if (uuid != null && uuid.version() == 4) {
                    return "premium";
                }
            }
        } catch (Exception ignored) {
        }
        return "cracked";
    }

    private static String getModVersion() {
        try {
            return FabricLoader.getInstance()
                    .getModContainer(TrTierListTagger.getId())
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    public static synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
