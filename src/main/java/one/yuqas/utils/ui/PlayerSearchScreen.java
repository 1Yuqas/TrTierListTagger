package one.yuqas.utils.ui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.text.Text;
import one.yuqas.utils.APIUtils;
import org.lwjgl.glfw.GLFW;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class PlayerSearchScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget searchField;
    private ButtonWidget searchButton;
    private String searchedName = "";
    private List<Text> foundTiers;
    private boolean isSearching = false;
    private AbstractClientPlayerEntity renderPlayer;

    private boolean rotating = false;
    private double lastMouseX;
    private float yaw = 180f;

    public PlayerSearchScreen(Screen parent) {
        super(Text.literal("Oyuncu Arama"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;

        searchField = new TextFieldWidget(textRenderer, cx - 80, 60, 160, 20, Text.literal("Oyuncu Adı..."));
        searchField.setMaxLength(16);
        addSelectableChild(searchField);
        searchField.setFocused(true);

        searchButton = ButtonWidget.builder(Text.literal("Ara").styled(s -> s.withColor(0x3498DB)), b -> startSearch())
                .dimensions(cx - 80, 85, 160, 20)
                .build();
        addDrawableChild(searchButton);

        addDrawableChild(ButtonWidget.builder(Text.literal("Bitti").styled(s -> s.withColor(0xCCCCCC)), b -> {
            client.setScreen(parent);
        }).dimensions(cx - 80, height - 30, 160, 20).build());

        updateVisibility();
    }

    private void updateVisibility() {
        boolean show = !isSearching && (searchedName == null || searchedName.isEmpty());
        searchField.visible = show;
        searchButton.visible = show;
    }

    private void startSearch() {
        searchedName = searchField.getText().trim();
        if (searchedName.isEmpty()) return;

        isSearching = true;
        foundTiers = null;
        renderPlayer = null;
        updateVisibility();
        APIUtils.fetchSync(searchedName);

        new Thread(() -> {
            try {
                URL uuidUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + searchedName);
                HttpURLConnection uuidCon = (HttpURLConnection) uuidUrl.openConnection();
                uuidCon.setConnectTimeout(3000);

                if (uuidCon.getResponseCode() != 200) return;

                com.google.gson.JsonObject uuidJson;
                try (var r = new java.io.InputStreamReader(uuidCon.getInputStream())) {
                    uuidJson = new com.google.gson.Gson().fromJson(r, com.google.gson.JsonObject.class);
                }

                UUID uuid = UUID.fromString(uuidJson.get("id").getAsString().replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                        "$1-$2-$3-$4-$5"
                ));

                URL profileUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
                HttpURLConnection profileCon = (HttpURLConnection) profileUrl.openConnection();
                profileCon.setConnectTimeout(3000);

                if (profileCon.getResponseCode() != 200) return;

                com.google.gson.JsonObject profileJson;
                try (var r = new java.io.InputStreamReader(profileCon.getInputStream())) {
                    profileJson = new com.google.gson.Gson().fromJson(r, com.google.gson.JsonObject.class);
                }

                com.google.gson.JsonObject tex = profileJson
                        .getAsJsonArray("properties")
                        .get(0).getAsJsonObject();

                String value = tex.get("value").getAsString();
                String sig = tex.get("signature").getAsString();

                GameProfile profile = new GameProfile(uuid, searchedName);
                profile.getProperties().put("textures", new Property("textures", value, sig));

                client.execute(() -> {
                    if (client.world != null) {
                        OtherClientPlayerEntity p = new OtherClientPlayerEntity(client.world, profile);
                        renderPlayer = p;
                    } else if (client.player != null) {
                        renderPlayer = client.player;
                    }
                });

            } catch (Exception ignored) {
            }
        }).start();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;

        if (searchField.visible) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("OYUNCU SORGULAMA").styled(s -> s.withBold(true).withColor(0xFFCC00)),
                    cx, 40, 0xFFCC00);
            searchField.render(ctx, mouseX, mouseY, delta);
        }

        if (searchedName != null && !searchedName.isEmpty()) {
            if (APIUtils.hasData(searchedName)) {
                isSearching = false;
                String error = APIUtils.getError(searchedName);

                if (error != null) {
                    ctx.drawCenteredTextWithShadow(textRenderer,
                            Text.literal(searchedName).styled(s -> s.withBold(true).withColor(0xCCFFFFFF)),
                            cx, 80, 0xCCFFFFFF);
                    ctx.drawCenteredTextWithShadow(textRenderer,
                            Text.literal(error).styled(s -> s.withColor(0xCCFF5555)),
                            cx, 100, 0xCCFF5555);
                } else {
                    foundTiers = APIUtils.getAllTiers(searchedName);

                    ctx.drawCenteredTextWithShadow(textRenderer,
                            Text.literal(searchedName + "'s profile").styled(s -> s.withColor(0xCCFFFFFF)),
                            cx, 30, 0xCCFFFFFF);

                    if (renderPlayer != null) {
                        renderPlayer.setYaw(yaw);
                        renderPlayer.setHeadYaw(yaw);
                        renderPlayer.bodyYaw = yaw;

                        InventoryScreen.drawEntity(
                                ctx,
                                cx - 120,
                                100,
                                cx - 20,
                                260,
                                80,
                                0.0625F,
                                mouseX,
                                mouseY,
                                renderPlayer
                        );
                    }

                    int x = cx + 10;
                    int y = 100;
                    ctx.drawTextWithShadow(textRenderer,
                            Text.literal("Rankings:").styled(s -> s.withColor(0xCCFFFFFF)),
                            x, y, 0xCCFFFFFF);
                    y += 15;

                    if (foundTiers == null || foundTiers.isEmpty()) {
                        ctx.drawTextWithShadow(textRenderer,
                                Text.literal("Tier bulunmuyor").styled(s -> s.withColor(0xCCFF5555)),
                                x, y, 0xCCFF5555);
                    } else {
                        for (Text t : foundTiers) {
                            ctx.drawTextWithShadow(textRenderer, t, x, y, 0xCCFFFFFF);
                            y += 12;
                        }
                    }
                }

                if (children().stream().noneMatch(c ->
                        c instanceof ButtonWidget b && b.getMessage().getString().equals("Yeni Arama"))) {

                    addDrawableChild(ButtonWidget.builder(
                            Text.literal("Yeni Arama").styled(s -> s.withColor(0x3498DB)),
                            b -> {
                                searchedName = "";
                                searchField.setText("");
                                isSearching = false;
                                renderPlayer = null;
                                clearChildren();
                                init();
                            }).dimensions(cx - 80, height - 55, 160, 20).build());
                }
            } else if (isSearching) {
                ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal("Sistemden Sorgulanıyor...").styled(s -> s.withColor(0x88AAAAAA)),
                        cx, 100, 0x88AAAAAA);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            rotating = true;
            lastMouseX = mx;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            rotating = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (rotating) {
            yaw += (mx - lastMouseX) * 0.5f;
            lastMouseX = mx;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (searchField.visible && !searchField.getText().isEmpty()) {
                startSearch();
                return true;
            }
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(parent);
            return true;
        }
        if (searchField.keyPressed(input)) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searchField.charTyped(input)) return true;
        return super.charTyped(input);
    }
}
