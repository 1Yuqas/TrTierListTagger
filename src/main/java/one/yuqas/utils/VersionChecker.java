package one.yuqas.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class VersionChecker {
    public static final String PROJECT_ID = "tgKll25i";
    public static String latestVersion = "Bilinmiyor";
    public static String updateUrl = ""; 
    public static boolean updateAvailable = false;

    public static void check() {
        try {
            // Mevcut Minecraft sürümünü al (Örn: 1.21.4)
            String mcVersion = FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion().getFriendlyString();
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version"))
                    .header("User-Agent", "TrTierListTagger/Updater")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
                
                for (JsonElement element : versions) {
                    JsonObject versionObj = element.getAsJsonObject();
                    JsonArray gameVersions = versionObj.getAsJsonArray("game_versions");

                    // Minecraft sürüm uyumluluğunu kontrol et
                    boolean isCompatible = false;
                    for (JsonElement gv : gameVersions) {
                        if (gv.getAsString().equals(mcVersion)) {
                            isCompatible = true;
                            break;
                        }
                    }

                    if (isCompatible) {
                        latestVersion = versionObj.get("version_number").getAsString();
                        
                        // "files" dizisindeki URL'yi çek
                        JsonArray files = versionObj.getAsJsonArray("files");
                        for (JsonElement fileElement : files) {
                            JsonObject fileObj = fileElement.getAsJsonObject();
                            // 'primary' olan dosyanın indirme linkini al
                            if (fileObj.get("primary").getAsBoolean()) {
                                updateUrl = fileObj.get("url").getAsString(); 
                                break;
                            }
                        }
                        
                        String currentModVersion = FabricLoader.getInstance()
                                .getModContainer("trtierlisttagger").get().getMetadata().getVersion().getFriendlyString();

                        // Sürüm karşılaştırması
                        if (!currentModVersion.equals(latestVersion)) {
                            updateAvailable = true;
                        }
                        break; 
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
