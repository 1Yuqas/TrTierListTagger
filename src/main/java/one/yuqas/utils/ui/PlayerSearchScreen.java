package one.yuqas.utils.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.net.HttpURLConnection;
import java.net.URL;
import com.mojang.authlib.GameProfile;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import one.yuqas.utils.APIUtils;

public class PlayerSearchScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget searchField;
    private ButtonWidget searchButton;
    private String searchedName = "";
    private List<Text> foundTiers = null;
    private boolean isSearching = false;
    private OtherClientPlayerEntity fakePlayer = null;
    private float playerRotation = 0.0F;
    private GameProfile currentProfile = null;
    private float mouseX = 0.0F;
    private float mouseY = 0.0F;
    private final String presetName;

    public PlayerSearchScreen(Screen parent) {
        super(Text.literal("Oyuncu Arama"));
        this.parent = parent;
        this.presetName = null;
    }

    public PlayerSearchScreen(Screen parent, String presetName) {
        super(Text.literal("Oyuncu Arama"));
        this.parent = parent;
        this.presetName = presetName;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        searchField = new TextFieldWidget(this.textRenderer, centerX - 80, 60, 160, 20, Text.literal("Oyuncu Adı..."));
        searchField.setMaxLength(16);
        this.addSelectableChild(searchField);
        searchField.setFocused(true);

        searchButton = ButtonWidget.builder(Text.literal("Ara").styled(s -> s.withColor(0x3498DB)), btn -> startSearch())
                .dimensions(centerX - 80, 85, 160, 20).build();
        this.addDrawableChild(searchButton);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Bitti").styled(s -> s.withColor(0xCCCCCC)), btn -> this.client.setScreen(parent))
                .dimensions(centerX - 80, this.height - 30, 160, 20).build());

        updateVisibility();

        if (presetName != null && !presetName.isEmpty()) {
            searchField.setText(presetName);
            startSearch();
        }
    }

    private void updateVisibility() {
        boolean showSearch = !isSearching && (searchedName == null || searchedName.isEmpty());
        searchField.visible = showSearch;
        searchButton.visible = showSearch;
    }

    private void startSearch() {
        searchedName = searchField.getText().trim();
        if (searchedName.isEmpty()) return;

        isSearching = true;
        foundTiers = null;
        fakePlayer = null;
        currentProfile = null;
        updateVisibility();
        APIUtils.fetchSync(searchedName);

        // Profil UUID'sini Minecraft API'sinden çek
        new Thread(() -> {
            try {
                // Minecraft Yggdrasil API'sinden oyuncu profili çek
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + searchedName);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);

                if (con.getResponseCode() == 200) {
                    try (java.io.InputStreamReader reader = new java.io.InputStreamReader(con.getInputStream())) {
                        JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                        String uuidStr = json.get("id").getAsString();
                        String playerName = json.get("name").getAsString();

                        // UUID string'ini format et (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
                        String formattedUuid = uuidStr.replaceFirst(
                                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                                "$1-$2-$3-$4-$5"
                        );


                        UUID uuid = UUID.fromString(formattedUuid);
                        GameProfile profile = new GameProfile(uuid, playerName);

                        // SessionServer'dan profile properties'lerini doldur (skin URL'si için)
                        try {
                            URL sessionUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + formattedUuid);
                            HttpURLConnection sessionCon = (HttpURLConnection) sessionUrl.openConnection();
                            sessionCon.setConnectTimeout(5000);
                            sessionCon.setReadTimeout(5000);

                            if (sessionCon.getResponseCode() == 200) {
                                try (java.io.InputStreamReader sessionReader = new java.io.InputStreamReader(sessionCon.getInputStream())) {
                                    JsonObject sessionJson = new Gson().fromJson(sessionReader, JsonObject.class);

                                    // Properties array'den textures'ı ara
                                    if (sessionJson.has("properties")) {
                                        com.google.gson.JsonArray propsArray = sessionJson.getAsJsonArray("properties");
                                        for (int i = 0; i < propsArray.size(); i++) {
                                            JsonObject prop = propsArray.get(i).getAsJsonObject();
                                            if ("textures".equals(prop.get("name").getAsString())) {
                                                String textureValue = prop.get("value").getAsString();
                                                profile.getProperties().put("textures", new com.mojang.authlib.properties.Property("textures", textureValue));
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            // Main thread'de profile'ı set et ve skin fetch et
                            MinecraftClient client = MinecraftClient.getInstance();
                            client.execute(() -> {
                                currentProfile = profile;
                                fakePlayer = new OtherClientPlayerEntity(client.world, profile);
                            });

                            // Skin cache olsun diye zaman ver
                            new Thread(() -> {
                                try {
                                    Thread.sleep(200);  // 200ms bekle skin fetch'in cache edilmesi için
                                    client.execute(() -> {
                                        isSearching = false;
                                    });
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }).start();
                        } catch (Exception e) {
                            e.printStackTrace();
                            isSearching = false;
                        }

                    }
                } else if (con.getResponseCode() == 404) {
                    isSearching = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                isSearching = false;
            }
        }).start();

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (searchField.visible) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("OYUNCU SORGULAMA").styled(s -> s.withBold(true).withColor(0xFFCC00)), centerX, 40, 0xFFCC00);
            searchField.render(context, mouseX, mouseY, delta);
        }

        if (searchedName != null && !searchedName.isEmpty()) {
            if (APIUtils.hasData(searchedName)) {
                String error = APIUtils.getError(searchedName);
                isSearching = false;

                if (error != null) {
                    context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(error).styled(s -> s.withColor(0xFF5555)), centerX, 100, 0xFF5555);
                } else {
                    foundTiers = APIUtils.getAllTiers(searchedName);

                    context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(searchedName + "'s Profile").styled(s -> s.withBold(true).withColor(0xFFFFFF)), centerX, 20, 0xFFFFFF);

                    // --- WIDGET OLUŞTUR (Profile API'den gelecek) ---
                    if (fakePlayer == null && currentProfile != null && client.world != null) {
                        // 1.20.1'de Widget yerine sanal oyuncu entity'si kullanıyoruz
                        fakePlayer = new OtherClientPlayerEntity(client.world, currentProfile);

                        // Skin'in yüklenmesi için (opsiyonel ama sağlıklı olur)
                        client.getSkinProvider().loadSkin(currentProfile, (type, identifier, texture) -> {
                            // Skin yüklendiğinde yapılacak ekstra bir şey varsa buraya...
                        }, true);
                    }

                    // Widget'ı render et
                    if (fakePlayer != null) {
                        // Karakterin duracağı pozisyon
                        int x = centerX - 35;
                        int y = centerY + 60; // Ayak hizası


                        net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(
                                context,
                                x, y,
                                50, // Boyut (Büyütmek istersen artır)
                                (float)(x) - mouseX,
                                (float)(y - 70) - mouseY,
                                fakePlayer
                        );
                    }
                    int infoX = centerX + 10;
                    int infoY = centerY - 40;

                    context.drawTextWithShadow(this.textRenderer, Text.literal("RANKINGS").styled(s -> s.withBold(true).withColor(0xFFAA00)), infoX, infoY, 0xFFAA00);
                    infoY += 15;

                    if (foundTiers.isEmpty()) {
                        context.drawTextWithShadow(this.textRenderer, Text.literal("Tier bulunmuyor").styled(s -> s.withColor(0xAAAAAA)), infoX, infoY, 0xAAAAAA);
                    } else {
                        for (Text tier : foundTiers) {
                            context.drawTextWithShadow(this.textRenderer, tier, infoX, infoY, 0xFFFFFF);
                            infoY += 12;
                        }
                    }
                }

                if (this.children().stream().noneMatch(c -> c instanceof ButtonWidget && ((ButtonWidget)c).getMessage().getString().equals("Yeni Arama"))) {
                    this.addDrawableChild(ButtonWidget.builder(Text.literal("Yeni Arama").styled(s -> s.withColor(0x3498DB)), btn -> {
                        searchedName = "";
                        isSearching = false;
                        fakePlayer = null;
                        currentProfile = null;
                        foundTiers = null;

                        searchField.setText("");
                        searchField.setFocused(true);

                        updateVisibility();

                        this.remove(btn);
                    }).dimensions(centerX - 80, this.height - 55, 160, 20).build());
                }
            } else if (isSearching) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Sistemden Sorgulanıyor...").styled(s -> s.withColor(0xAAAAAA)), centerX, centerY, 0xAAAAAA);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (searchField.visible && !searchField.getText().isEmpty()) {
                startSearch();
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.setScreen(parent);
            return true;
        }

        if (searchField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // Eğer fare karakterin olduğu bölgedeyse veya genel olarak sürüklendiğinde dönsün istiyorsan:
        if (fakePlayer != null) {
            playerRotation -= (float) deltaX * 1.5F; // Hassasiyeti 1.5F ile ayarlayabilirsin
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
}
