package one.yuqas.utils.ui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import one.yuqas.utils.APIUtils;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class PlayerSearchScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget searchField;
    private String searchedName = "";
    private String foundPlayerName = null;
    private UUID foundPlayerUUID = null;
    private boolean isSearching;
    private boolean isLoadingTiers = false;
    private boolean isLoadingSkin = false;
    private String errorMessage = null;
    private List<Text> tierList = null;
    private Identifier skinTextureId = null;

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
        skinTextureId = null;
        isLoadingSkin = false;

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
                            
                            // NMSR API'den 3D skin görüntüsü yükle
                            isLoadingSkin = true;
                            new Thread(() -> {
                                try {
                                    // NMSR API - 3D full body render
                                    String nmsrUrl = "https://nmsr.nickac.dev/fullbody/" + uuid.toString() + "?size=512";
                                    java.net.URL skinUrl = new java.net.URL(nmsrUrl);
                                    java.net.HttpURLConnection skinCon = (java.net.HttpURLConnection) skinUrl.openConnection();
                                    skinCon.setConnectTimeout(5000);
                                    skinCon.setReadTimeout(5000);
                                    
                                    try (InputStream stream = skinCon.getInputStream()) {
                                        BufferedImage bufferedImage = ImageIO.read(stream);
                                        if (bufferedImage != null) {
                                            // BufferedImage'i NativeImage'e dönüştür
                                            NativeImage nativeImage = new NativeImage(bufferedImage.getWidth(), bufferedImage.getHeight(), true);
                                            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                                                for (int x = 0; x < bufferedImage.getWidth(); x++) {
                                                    int argb = bufferedImage.getRGB(x, y);
                                                    nativeImage.setColor(x, y, argb);
                                                }
                                            }
                                            
                                            client.execute(() -> {
                                                // Texture'ı kaydet
                                                try {
                                                    Identifier texId = Identifier.of("trtierlisttagger", "nmsr_skin_" + uuid.toString());
                                                    client.getTextureManager().registerTexture(texId, new NativeImageBackedTexture(nativeImage));
                                                    skinTextureId = texId;
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                                isLoadingSkin = false;
                                            });
                                        } else {
                                            client.execute(() -> isLoadingSkin = false);
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    client.execute(() -> isLoadingSkin = false);
                                }
                            }).start();
                            
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
            // 3D Skin render (NMSR API) - Sol tarafta
            int skinX = width / 2 - 180;
            int skinY = 120;
            int skinSize = 150;
            
            if (isLoadingSkin) {
                ctx.drawCenteredTextWithShadow(textRenderer, 
                    Text.literal("⏳ Skin yükleniyor...").formatted(Formatting.GRAY), 
                    skinX + skinSize / 2, skinY + skinSize / 2, 0xAAAAAA);
            } else if (skinTextureId != null) {
                // 3D Skin'i çiz
                ctx.drawTexture(skinTextureId, skinX, skinY, skinSize, skinSize, 0, 0, skinSize, skinSize, skinSize, skinSize);
            }
            
            // Tier bilgilerini 3D skinin SAĞ YANINDA göster
            int tierX = width / 2 - 10;
            int yPos = 120;
            
            // Başlık
            ctx.drawTextWithShadow(textRenderer, 
                Text.literal("✓ Oyuncu Bulundu").formatted(Formatting.GREEN, Formatting.BOLD), 
                tierX, yPos, 0x55FF55);
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