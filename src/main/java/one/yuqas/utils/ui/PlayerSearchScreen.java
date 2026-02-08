package one.yuqas.utils.ui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class PlayerSearchScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget searchField;
    private PlayerSkinWidget skinWidget;
    private String searchedName = "";
    private boolean isSearching;

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
        if (skinWidget != null) this.remove(skinWidget);

        new Thread(() -> {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + searchedName);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                if (con.getResponseCode() == 200) {
                    try (java.io.InputStreamReader reader = new java.io.InputStreamReader(con.getInputStream())) {
                        com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(reader, com.google.gson.JsonObject.class);
                        String id = json.get("id").getAsString();
                        String formattedId = id.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5");
                        UUID uuid = UUID.fromString(formattedId);
                        GameProfile profile = new GameProfile(uuid, searchedName);

                        client.execute(() -> {
                            // PlayerSkinWidget constructor: (width, height, entityModelLoader)
                            skinWidget = new PlayerSkinWidget(100, 150, client.getEntityModelLoader());
                            skinWidget.setPosition(width / 2 - 50, 110);
                            
                            // Skin texture'ı ayarla
                            skinWidget.setSkinTextures(client.getSkinProvider().getSkinTextures(profile));
                            
                            this.addDrawableChild(skinWidget);
                            isSearching = false;
                        });
                    }
                } else {
                    client.execute(() -> isSearching = false);
                }
            } catch (Exception e) { 
                e.printStackTrace(); 
                client.execute(() -> isSearching = false);
            }
        }).start();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        searchField.render(ctx, mouseX, mouseY, delta);
        
        if (isSearching) {
            ctx.drawCenteredTextWithShadow(textRenderer, "Aranıyor...", width / 2, 110, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER) { 
            startSearch(); 
            return true; 
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}