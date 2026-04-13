package one.yuqas.utils.ui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.text.Text;
import one.yuqas.utils.APIUtils;
import org.lwjgl.glfw.GLFW;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

public class PlayerSearchScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget searchField;
    private ButtonWidget searchButton;
    private String searchedName = "";
    private List<Text> foundTiers = null;
    private boolean isSearching = false;
    private OtherClientPlayerEntity dummyPlayer = null;
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
        dummyPlayer = null;
        updateVisibility();
        APIUtils.fetchSync(searchedName);

        new Thread(() -> {
            try {
                URL url = new URL("https://api.trtierlist.com/api3/profil/" + searchedName);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5000);

                if (con.getResponseCode() == 200) {
                    try (java.io.InputStreamReader reader = new java.io.InputStreamReader(con.getInputStream())) {
                        JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                        String rawId = json.get("uuid").getAsString();
                        
                        UUID uuid = UUID.fromString(rawId.replaceFirst(
                            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", 
                            "$1-$2-$3-$4-$5"));

                        GameProfile profile = new GameProfile(uuid, searchedName);

                        if (this.client.world != null) {
                            this.client.execute(() -> {
                                dummyPlayer = new OtherClientPlayerEntity(this.client.world, profile);
                                // Skin dokularını asenkron olarak çek
                                this.client.getSkinProvider().fetchSkinTextures(profile);
                            });
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
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

                    // --- 3D PLAYER RENDER DÜZELTMESİ ---
                    if (dummyPlayer != null) {
                        // Karakterin skininin yüklenmesi için ticklenmesi şart
                        dummyPlayer.tick(); 
                        
                        // Pozisyonu sol tarafa hizalıyoruz (centerX - 100 civarı)
                        int x = centerX - 90;
                        int y = centerY + 50; 
                        
                        // drawEntity(context, x, y, size, mouseX, mouseY, entity)
                        InventoryScreen.drawEntity(context, x, y, 50, (float)x - mouseX, (float)(y - 50) - mouseY, dummyPlayer);
                    }
                    // ------------------------------------

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
                        dummyPlayer = null; // Eski oyuncuyu temizle
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
    public boolean charTyped(char chr, int modifiers) {
        if (searchField.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
}
