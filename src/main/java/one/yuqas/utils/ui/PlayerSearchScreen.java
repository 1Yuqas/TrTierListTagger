package one.yuqas.utils.ui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.Click;
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
import java.util.List;
import java.util.UUID;

public class PlayerSearchScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget searchField;
    private ButtonWidget searchButton;
    private String searchedName = "";
    private List<Text> foundTiers;
    private boolean isSearching;
    private AbstractClientPlayerEntity renderPlayer;

    private boolean rotating;
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

        searchButton = ButtonWidget.builder(Text.literal("Ara"), b -> startSearch())
                .dimensions(cx - 80, 85, 160, 20)
                .build();
        addDrawableChild(searchButton);

        addDrawableChild(ButtonWidget.builder(Text.literal("Bitti"), b -> client.setScreen(parent))
                .dimensions(cx - 80, height - 30, 160, 20)
                .build());
    }

    private void startSearch() {
        searchedName = searchField.getText().trim();
        if (searchedName.isEmpty()) return;

        isSearching = true;
        foundTiers = null;
        renderPlayer = null;
        APIUtils.fetchSync(searchedName);

        new Thread(() -> {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + searchedName);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                if (con.getResponseCode() != 200) return;

                com.google.gson.JsonObject json;
                try (var r = new java.io.InputStreamReader(con.getInputStream())) {
                    json = new com.google.gson.Gson().fromJson(r, com.google.gson.JsonObject.class);
                }

                UUID uuid = UUID.fromString(json.get("id").getAsString().replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                        "$1-$2-$3-$4-$5"
                ));

                GameProfile profile = new GameProfile(uuid, searchedName);

                client.execute(() -> {
                    client.getSkinProvider().fetchSkinTextures(profile);
                    if (client.world != null) {
                        renderPlayer = new OtherClientPlayerEntity(client.world, profile);
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

        searchField.render(ctx, mouseX, mouseY, delta);

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
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            rotating = true;
            lastMouseX = click.x();
            return true;
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(Click click) {
        rotating = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (rotating) {
            yaw += (click.x() - lastMouseX) * 0.5f;
            lastMouseX = click.x();
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        return searchField.charTyped(input) || super.charTyped(input);
    }
}
