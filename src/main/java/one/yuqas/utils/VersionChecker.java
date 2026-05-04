package one.yuqas.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class VersionChecker {
    public static final String PROJECT_ID = "tgKll25i";
    public static String latestVersion = null;
    public static boolean updateAvailable = false;

    public static void check() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version"))
                    .header("User-Agent", "TrTierListTagger/UpdateChecker")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
                if (versions.size() > 0) {
                    latestVersion = versions.get(0).getAsJsonObject().get("version_number").getAsString();
                    
                    String currentVersion = FabricLoader.getInstance()
                            .getModContainer("trtierlisttagger").get().getMetadata().getVersion().getFriendlyString();

                    if (!currentVersion.equals(latestVersion)) {
                        updateAvailable = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
