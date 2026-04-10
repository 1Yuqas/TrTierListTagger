package one.yuqas.utils.ui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import one.yuqas.utils.APIUtils;
import org.lwjgl.glfw.GLFW;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;

public class PlayerSearchScreen extends Screen {
    private final Screen parent;
    private EditBox searchField;
    private String searchedName = "";
    private List<Component> foundTiers = null;
    private boolean isSearching = false;
    private RemotePlayer dummyPlayer = null;
    private final String presetName;

    public PlayerSearchScreen(Screen parent) {
        super(Component.literal("Oyuncu Arama"));
        this.parent = parent;
        this.presetName = null;
    }

    public PlayerSearchScreen(Screen parent, String presetName) {
        super(Component.literal("Oyuncu Arama"));
        this.parent = parent;
        this.presetName = presetName;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        if (!isSearching && (searchedName == null || searchedName.isEmpty())) {
            searchField = new EditBox(this.font, centerX - 80, 60, 160, 20, Component.literal("Oyuncu Adı..."));
            searchField.setMaxLength(16);
            this.addRenderableWidget(searchField);
            searchField.setFocused(true);

            // 0x3498DB -> 0xFF3498DB yapıldı
            this.addRenderableWidget(Button.builder(Component.literal("Ara").withStyle(s -> s.withColor(0xFF3498DB)), btn -> {
                startSearch();
            }).bounds(centerX - 80, 85, 160, 20).build());
        }

        if (APIUtils.hasData(searchedName)) {
            this.addRenderableWidget(Button.builder(Component.literal("Yeni Arama").withStyle(s -> s.withColor(0xFF3498DB)), btn -> {
                searchedName = "";
                isSearching = false;
                dummyPlayer = null;
                this.rebuildWidgets();
            }).bounds(centerX - 80, this.height - 55, 160, 20).build());
        }

        // 0xCCCCCC -> 0xFFCCCCCC yapıldı
        this.addRenderableWidget(Button.builder(Component.literal("Bitti").withStyle(s -> s.withColor(0xFFCCCCCC)), btn -> {
            this.minecraft.setScreen(parent);
        }).bounds(centerX - 80, this.height - 30, 160, 20).build());

        if (presetName != null && !presetName.isEmpty() && searchedName.isEmpty()) {
            searchField.setValue(presetName);
            startSearch();
        }
    }

    private void startSearch() {
        searchedName = searchField.getValue().trim();
        if (searchedName.isEmpty()) return;

        isSearching = true;
        this.rebuildWidgets();
        APIUtils.fetchSync(searchedName);

        new Thread(() -> {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + searchedName);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(3000);

                if (con.getResponseCode() == 200) {
                    try (java.io.InputStreamReader reader = new java.io.InputStreamReader(con.getInputStream())) {
                        com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(reader, com.google.gson.JsonObject.class);
                        String id = json.get("id").getAsString();
                        String formattedId = id.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5");
                        UUID uuid = UUID.fromString(formattedId);

                        GameProfile profile = new GameProfile(uuid, searchedName);
                        if (this.minecraft.level != null) {
                            this.minecraft.execute(() -> {
                                dummyPlayer = new RemotePlayer((ClientLevel) this.minecraft.level, profile);
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
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int centerX = this.width / 2;

        if (searchedName == null || searchedName.isEmpty()) {
            // Başlık rengi düzeltildi
            graphics.centeredText(this.font, Component.literal("OYUNCU SORGULAMA").withStyle(s -> s.withBold(true).withColor(0xFFFFCC00)), centerX, 40, 0xFFFFCC00);
        } else {
            if (APIUtils.hasData(searchedName)) {
                String error = APIUtils.getError(searchedName);
                isSearching = false;

                if (error != null) {
                    graphics.centeredText(this.font, Component.literal(searchedName).withStyle(s -> s.withBold(true).withColor(0xFFFFFFFF)), centerX, 80, 0xFFFFFFFF);
                    graphics.centeredText(this.font, Component.literal(error).withStyle(s -> s.withColor(0xFFFF5555)), centerX, 100, 0xFFFF5555);
                } else {
                    foundTiers = APIUtils.getAllTiers(searchedName);
                    graphics.centeredText(this.font, Component.literal(searchedName + "'s profile").withStyle(s -> s.withColor(0xFFFFFFFF)), centerX, 30, 0xFFFFFFFF);

                    if (dummyPlayer != null) {
                        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, centerX - 100, 80, centerX - 20, 220, 60, 0.0625F, mouseX, mouseY, dummyPlayer);
                    }

                    int ry = 90;
                    graphics.text(this.font, Component.literal("Rankings:").withStyle(s -> s.withColor(0xFFFFFFFF)), centerX + 10, ry, 0xFFFFFFFF);
                    ry += 15;

                    if (foundTiers.isEmpty()) {
                        graphics.text(this.font, Component.literal("Tier bulunmuyor").withStyle(s -> s.withColor(0xFFFF5555)), centerX + 10, ry, 0xFFFF5555);
                    } else {
                        for (Component tier : foundTiers) {
                            graphics.text(this.font, tier, centerX + 10, ry, 0xFFFFFFFF);
                            ry += 12;
                        }
                    }
                }
            } else if (isSearching) {
                graphics.centeredText(this.font, Component.literal("Sistemden Sorgulanıyor...").withStyle(s -> s.withColor(0xFFAAAAAA)), centerX, 100, 0xFFAAAAAA);
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (searchField != null && (searchedName == null || searchedName.isEmpty())) {
                startSearch();
                return true;
            }
        }
        if (event.isEscape()) {
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchField != null && searchField.charTyped(event)) return true;
        return super.charTyped(event);
    }
}