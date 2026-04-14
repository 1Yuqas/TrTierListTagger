package one.yuqas.utils.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.SkinTextures;
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
    private PlayerSkinWidget skinWidget = null;
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

        System.out.println("[PlayerSearchScreen] startSearch: " + searchedName);
        isSearching = true;
        foundTiers = null;
        skinWidget = null;
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

                System.out.println("[PlayerSearchScreen] API çağrısı: " + con.getResponseCode());

                if (con.getResponseCode() == 200) {
                    try (java.io.InputStreamReader reader = new java.io.InputStreamReader(con.getInputStream())) {
                        JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                        String uuidStr = json.get("id").getAsString();
                        String playerName = json.get("name").getAsString();

                        System.out.println("[PlayerSearchScreen] API Response - Oyuncu: " + playerName + " UUID (raw): " + uuidStr);

                        // UUID string'ini format et (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
                        String formattedUuid = uuidStr.replaceFirst(
                            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                            "$1-$2-$3-$4-$5"
                        );
                        
                        System.out.println("[PlayerSearchScreen] Formatted UUID: " + formattedUuid);

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
                                                System.out.println("[PlayerSearchScreen] Skin properties yüklendi");
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
                                skinWidget = null;  // Widget'ı sıfırla, yenisi oluşturulsun
                                // Skin yüklenmesini başla
                                client.getSkinProvider().fetchSkinTextures(profile);
                                System.out.println("[PlayerSearchScreen] Skin fetch başladı - Profile: " + profile.getName() + " UUID: " + profile.getId());
                            });
                            
                            // Skin cache olsun diye zaman ver
                            new Thread(() -> {
                                try {
                                    Thread.sleep(200);  // 200ms bekle skin fetch'in cache edilmesi için
                                    client.execute(() -> {
                                        System.out.println("[PlayerSearchScreen] Skin yükleme tamamlandı, widget hazırlama başlıyor");
                                        isSearching = false;
                                    });
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }).start();
                        } catch (Exception e) {
                            System.out.println("[PlayerSearchScreen] Profile properties yükleme hatası: " + e.getMessage());
                            e.printStackTrace();
                            isSearching = false;
                        }

                    }
                } else if (con.getResponseCode() == 404) {
                    System.out.println("[PlayerSearchScreen] Oyuncu bulunamadı: " + searchedName);
                    isSearching = false;
                }
            } catch (Exception e) {
                System.out.println("[PlayerSearchScreen] UUID fetch hatası: " + e.getMessage());
                e.printStackTrace();
                isSearching = false;
            }
        }).start();

        System.out.println("[PlayerSearchScreen] UUID fetch başladı");
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
                    if (skinWidget == null && currentProfile != null) {
                        // PlayerSkinWidget'ı oluştur
                        MinecraftClient client = MinecraftClient.getInstance();
                        
                        try {
                            // Textures property var mı kontrol et (crack oyuncu kontrolü)
                            boolean hasSkin = currentProfile.getProperties().containsKey("textures");
                            System.out.println("[PlayerSearchScreen] Oyuncu textures var mı: " + hasSkin);
                            
                            // Skin supplier - profile skin texture'sini sağla
                            // Eğer textures yoksa (crack), Steve skin gösterilir
                            Supplier<SkinTextures> skinSupplier = client.getSkinProvider()
                                .getSkinTexturesSupplier(currentProfile);
                            
                            // Debug: Supplier'ı kontrol et
                            SkinTextures textures = skinSupplier.get();
                            System.out.println("[PlayerSearchScreen] Supplier'dan SkinTextures: " + textures);
                            if (textures != null) {
                                System.out.println("[PlayerSearchScreen] Texture URL: " + textures.texture());
                                System.out.println("[PlayerSearchScreen] Cape URL: " + textures.capeTexture());
                            } else {
                                System.out.println("[PlayerSearchScreen] WARNING: SkinTextures null!");
                            }
                            
                            System.out.println("[PlayerSearchScreen] Widget oluşturuluyor - Profile: " + currentProfile.getName() + " UUID: " + currentProfile.getId());
                            
                            // Widget'ı büyüt: 60x144 → 90x160 (küçültüldü)
                            skinWidget = new PlayerSkinWidget(
                                90,   // Genişlik (100'den 90'a azaltıldı)
                                160,   // Yükseklik (200'den 160'a azaltıldı)
                                client.getLoadedEntityModels(), // 3D Modeller
                                skinSupplier // Skin dokusu supplier (cape dahil)
                            );
                            // Position güncelle: ortalanmış
                            skinWidget.setPosition(centerX - 100, centerY - 90);
                            System.out.println("[PlayerSearchScreen] PlayerSkinWidget başarıyla oluşturuldu (90x160)");
                        } catch (Exception e) {
                            System.out.println("[PlayerSearchScreen] Widget oluşturma hatası: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    // Widget'ı render et
                    if (skinWidget != null) {
                        try {
                            skinWidget.render(context, (int)mouseX, (int)mouseY, delta);
                        } catch (Exception e) {
                            System.out.println("[PlayerSearchScreen] Widget render hatası: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                        }
                    } else {
                        System.out.println("[PlayerSearchScreen] skinWidget hala null - currentProfile: " + currentProfile);
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
                        skinWidget = null;
                        currentProfile = null;
                        foundTiers = null;
                        this.clearChildren();
                        this.init();
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
        if (skinWidget != null && skinWidget.isMouseOver(mouseX, mouseY)) {
            return skinWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (skinWidget != null && skinWidget.isMouseOver(mouseX, mouseY)) {
            return skinWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
