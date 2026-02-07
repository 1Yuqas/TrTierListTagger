package one.yuqas.utils.ui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import one.yuqas.utils.APIUtils;
import org.lwjgl.glfw.GLFW;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class PlayerSearchScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget searchField;
    private ButtonWidget searchButton;
    private String searchedName = "";
    private List<Text> foundTiers;
    private boolean isSearching;
    private PlayerSkinWidget skinWidget;

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

        updateVisibility();
    }

    private void updateVisibility() {
        boolean showSearch = !isSearching && (searchedName == null || searchedName.isEmpty());
        if (searchField != null) searchField.visible = showSearch;
        if (searchButton != null) searchButton.visible = showSearch;
        if (skinWidget != null) skinWidget.visible = !showSearch;
    }

    private void startSearch() {
        searchedName = searchField.getText().trim();
        if (searchedName.isEmpty()) return;

        isSearching = true;
        foundTiers = null;
        updateVisibility();
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
                        
                        client.execute(() -> {
                            // Fetch skin supplier using modern SkinProvider
                            Supplier<SkinTextures> skinSupplier = client.getSkinProvider().getSkinTexturesSupplier(profile);
                            
                            // Create the 3D Player Widget (width, height, models, supplier)
                            skinWidget = new PlayerSkinWidget(100, 150, client.getEntityModelLoader(), skinSupplier);
                            skinWidget.setX(width / 2 - 110);
                            skinWidget.setY(80);
                            
                            this.addDrawableChild(skinWidget);
                            updateVisibility();
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;

        if (searchField != null && searchField.visible) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("OYUNCU SORGULAMA").styled(s -> s.withBold(true).withColor(0xFFCC00)), cx, 40, 0xFFCC00);
            searchField.render(ctx, mouseX, mouseY, delta);
        }

        if (searchedName != null && !searchedName.isEmpty()) {
            if (APIUtils.hasData(searchedName)) {
                String error = APIUtils.getError(searchedName);
                isSearching = false;

                if (error != null) {
                    ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(searchedName).styled(s -> s.withBold(true).withColor(0xCCFFFFFF)), cx, 80, 0xCCFFFFFF);
                    ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(error).styled(s -> s.withColor(0xCCFF5555)), cx, 100, 0xCCFF5555);
                } else {
                    foundTiers = APIUtils.getAllTiers(searchedName);

                    // Profile Title
                    ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(searchedName + "'s profile").styled(s -> s.withColor(0xCCFFFFFF)), cx, 30, 0xCCFFFFFF);

                    // Rankings (Right side of the skin)
                    int rankingsX = cx + 10;
                    int y = 90;
                    ctx.drawTextWithShadow(textRenderer, Text.literal("Rankings:").styled(s -> s.withColor(0xCCFFFFFF)), rankingsX, y, 0xCCFFFFFF);
                    y += 15;

                    if (foundTiers != null) {
                        if (foundTiers.isEmpty()) {
                            ctx.drawTextWithShadow(textRenderer, Text.literal("Tier bulunmuyor").styled(s -> s.withColor(0xCCFF5555)), rankingsX, y, 0xCCFF5555);
                        } else {
                            for (Text tier : foundTiers) {
                                ctx.drawTextWithShadow(textRenderer, tier, rankingsX, y, 0xCCFFFFFF);
                                y += 12;
                            }
                        }
                    }
                }

                // "Yeni Arama" Button
                if (this.children().stream().noneMatch(c -> c instanceof ButtonWidget && ((ButtonWidget)c).getMessage().getString().equals("Yeni Arama"))) {
                    this.addDrawableChild(ButtonWidget.builder(Text.literal("Yeni Arama").styled(s -> s.withColor(0x3498DB)), btn -> {
                        searchedName = "";
                        searchField.setText("");
                        isSearching = false;
                        this.clearChildren();
                        this.init();
                    }).dimensions(cx - 80, this.height - 55, 160, 20).build());
                }

            } else if (isSearching) {
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Sistemden Sorgulanıyor...").styled(s -> s.withColor(0x88AAAAAA)), cx, 100, 0x88AAAAAA);
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(Click click) {
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (searchField != null && searchField.visible && !searchField.getText().isEmpty()) {
                startSearch();
                return true;
            }
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(parent);
            return true;
        }
        if (searchField != null && searchField.keyPressed(input)) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        return (searchField != null && searchField.charTyped(input)) || super.charTyped(input);
    }
}
