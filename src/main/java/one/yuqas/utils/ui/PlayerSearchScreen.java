package one.yuqas.utils.ui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import one.yuqas.utils.APIUtils;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class PlayerSearchScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget searchField;
    private PlayerSkinWidget skinWidget;
    private String searchedName = "";
    private String foundPlayerName = null;
    private UUID foundPlayerUUID = null;
    private boolean isSearching;
    private boolean isLoadingTiers = false;
    private String errorMessage = null;
    private List<Text> tierList = null;

    public PlayerSearchScreen(Screen parent) {
        super(Text.literal("Oyuncu Arama"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;

        // Arama Alanı
        searchField = new TextFieldWidget(textRenderer, cx - 80, 60, 160, 20, Text.literal("Oyuncu Adı..."));
        searchField.setMaxLength(16);
        addSelectableChild(searchField);
        searchField.setFocused(true);

        // Ara Butonu
        addDrawableChild(ButtonWidget.builder(Text.literal("Ara"), b -> startSearch())
                .dimensions(cx - 80, 85, 160, 20)
                .build());

        // Geri Butonu
        addDrawableChild(ButtonWidget.builder(Text.literal("Bitti"), b -> client.setScreen(parent))
                .dimensions(cx - 80, height - 30, 160, 20)
                .build());
    }

    private void startSearch() {
        searchedName = searchField.getText().trim();
        if (searchedName.isEmpty()) return;

        isSearching = true;
        foundPlayerName = null;
        foundPlayerUUID = null;
        errorMessage = null;
        tierList = null;
        isLoadingTiers = false;

        new Thread(() -> {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + searchedName);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                if (con.getResponseCode() == 200) {
                    try (java.io.InputStreamReader reader = new java.io.InputStreamReader(con.getInputStream())) {
                        com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(reader, com.google.gson.JsonObject.class);
                        String id = json.get("id").getAsString();
                        String name = json.get("name").getAsString();
                        String formattedId = id.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5");
                        UUID uuid = UUID.fromString(formattedId);

                        GameProfile profile = new GameProfile(uuid, name);
                        
                        client.execute(() -> {
                            foundPlayerName = name;
                            foundPlayerUUID = uuid;
                            isSearching = false;
                            
                            // 3D Skin Widget oluştur
                            if (skinWidget != null) {
                                this.remove(skinWidget);
                            }
                            
                            skinWidget = new PlayerSkinWidget(
                                100, 150,
                                client.getEntityModels(),
                                () -> client.getSkinProvider().fetchSkinTextures(profile).join().orElse(null)
                            );
                            
                            // Widget'ı ekranda ortala (solda)
                            int skinX = width / 2 - 150;
                            int skinY = 140;
                            skinWidget.setPosition(skinX, skinY);
                            this.addDrawableChild(skinWidget);
                            
                            // Tier bilgilerini yükle
                            isLoadingTiers = true;
                            APIUtils.fetchSync(name);
                            
                            // Tier'leri kontrol et (birkaç deneme yap)
                            new Thread(() -> {
                                for (int i = 0; i < 10; i++) {
                                    try { Thread.sleep(500); } catch (Exception ignored) {}
                                    if (APIUtils.hasData(name)) {
                                        List<Text> tiers = APIUtils.getAllTiers(name);
                                        String error = APIUtils.getError(name);
                                        client.execute(() -> {
                                            if (error != null) {
                                                tierList = List.of(Text.literal("⚠ " + error).formatted(Formatting.GOLD));
                                            } else if (tiers != null && !tiers.isEmpty()) {
                                                tierList = tiers;
                                            } else {
                                                tierList = List.of(Text.literal("Tier bilgisi yok").formatted(Formatting.GRAY));
                                            }
                                            isLoadingTiers = false;
                                        });
                                        break;
                                    }
                                }
                                // Timeout durumu
                                if (isLoadingTiers) {
                                    client.execute(() -> {
                                        tierList = List.of(Text.literal("Tier yüklenemedi").formatted(Formatting.RED));
                                        isLoadingTiers = false;
                                    });
                                }
                            }).start();
                        });
                    }
                } else {
                    client.execute(() -> {
                        errorMessage = "Oyuncu bulunamadı!";
                        isSearching = false;
                    });
                }
            } catch (Exception e) { 
                e.printStackTrace(); 
                client.execute(() -> {
                    errorMessage = "Hata: " + e.getMessage();
                    isSearching = false;
                });
            }
        }).start();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        searchField.render(ctx, mouseX, mouseY, delta);
        
        if (isSearching) {
            ctx.drawCenteredTextWithShadow(textRenderer, "Aranıyor...", width / 2, 120, 0xFFFFFF);
        } else if (foundPlayerName != null) {
            // Tier bilgilerini 3D karakterin SAĞ YANINDA göster
            int tierX = width / 2 - 30; // 3D karakterin sağında
            int yPos = 120;
            
            // Başlık
            ctx.drawTextWithShadow(textRenderer, Text.literal("✓ Oyuncu Bulundu").formatted(Formatting.GREEN, Formatting.BOLD), tierX, yPos, 0x55FF55);
            yPos += 20;
            
            // İsim
            ctx.drawTextWithShadow(textRenderer, 
                Text.literal("İsim: ").formatted(Formatting.GRAY)
                    .append(Text.literal(foundPlayerName).formatted(Formatting.YELLOW, Formatting.BOLD)), 
                tierX, yPos, 0xFFFFFF);
            yPos += 25;
            
            // Tier Başlığı
            ctx.drawTextWithShadow(textRenderer, 
                Text.literal("━━━ TİER BİLGİLERİ ━━━").formatted(Formatting.AQUA), 
                tierX, yPos, 0x55FFFF);
            yPos += 18;
            
            // Tier'ler
            if (isLoadingTiers) {
                ctx.drawTextWithShadow(textRenderer, 
                    Text.literal("⏳ Yükleniyor...").formatted(Formatting.YELLOW), 
                    tierX, yPos, 0xFFFF55);
            } else if (tierList != null && !tierList.isEmpty()) {
                for (Text tier : tierList) {
                    ctx.drawTextWithShadow(textRenderer, tier, tierX, yPos, 0xFFFFFF);
                    yPos += 15;
                }
            } else {
                ctx.drawTextWithShadow(textRenderer, 
                    Text.literal("Tier bilgisi bekleniyor...").formatted(Formatting.GRAY), 
                    tierX, yPos, 0xAAAAAA);
            }
            
            // UUID (en altta küçük - ortalı)
            ctx.drawCenteredTextWithShadow(textRenderer, 
                Text.literal("UUID: " + foundPlayerUUID.toString()).formatted(Formatting.DARK_GRAY), 
                width / 2, height - 50, 0x555555);
        } else if (errorMessage != null) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(errorMessage).formatted(Formatting.RED), width / 2, 120, 0xFF5555);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ENTER) { 
            startSearch(); 
            return true; 
        }
        return super.keyPressed(input);
    }
}