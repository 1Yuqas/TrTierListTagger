package one.yuqas.utils.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
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

        isSearching = true;
        foundTiers = null;
        skinWidget = null;
        currentProfile = null;
        updateVisibility();
        APIUtils.fetchSync(searchedName);

        // startSearch içindeki Thread kısmını bununla değiştir
        new Thread(() -> {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + searchedName);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);

                if (con.getResponseCode() == 200) {
                    try (java.io.InputStreamReader reader = new java.io.InputStreamReader(con.getInputStream())) {
                        JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                        String uuidStr = json.get("id").getAsString();
                        String playerName = json.get("name").getAsString();

                        String formattedUuid = uuidStr.replaceFirst(
                                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                                "$1-$2-$3-$4-$5"
                        );

                        UUID uuid = UUID.fromString(formattedUuid);

                        // --- SKIN ÇÖZÜMÜ BAŞLANGIÇ ---
                        // Sadece UUID ile profil oluşturmak yetmez, textures verisini session serverdan çekmeliyiz.
                        com.google.common.collect.Multimap<String, com.mojang.authlib.properties.Property> tempMap =
                                com.google.common.collect.HashMultimap.create();

                        URL sessionUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + formattedUuid + "?unsigned=false");
                        HttpURLConnection sessionCon = (HttpURLConnection) sessionUrl.openConnection();

                        if (sessionCon.getResponseCode() == 200) {
                            try (java.io.InputStreamReader sReader = new java.io.InputStreamReader(sessionCon.getInputStream())) {
                                JsonObject sessionJson = new Gson().fromJson(sReader, JsonObject.class);
                                if (sessionJson.has("properties")) {
                                    com.google.gson.JsonArray props = sessionJson.getAsJsonArray("properties");
                                    for (int i = 0; i < props.size(); i++) {
                                        JsonObject p = props.get(i).getAsJsonObject();
                                        String pName = p.get("name").getAsString();
                                        String pValue = p.get("value").getAsString();
                                        String pSig = p.has("signature") ? p.get("signature").getAsString() : null;
                                        tempMap.put(pName, new com.mojang.authlib.properties.Property(pName, pValue, pSig));
                                    }
                                }
                            }
                        }

                        // PropertyMap'i doldurarak profili oluşturuyoruz
                        com.mojang.authlib.properties.PropertyMap authProps = new com.mojang.authlib.properties.PropertyMap(tempMap);
                        GameProfile profile = new GameProfile(uuid, playerName, authProps);
                        // --- SKIN ÇÖZÜMÜ BİTİŞ ---

                        MinecraftClient client = MinecraftClient.getInstance();

                        // fetchSkinTextures yerine direk supply kullanabiliriz çünkü veriyi elle doldurduk
                        client.execute(() -> {
                            currentProfile = profile;
                            skinWidget = null;
                            isSearching = false;
                        });
                    }
                } else {
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
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("OYUNCU SORGULAMA").styled(s -> s.withBold(true).withColor(new Color(0xFFEA00).getRGB())), centerX, 40, 0xFFCC00);
            searchField.render(context, mouseX, mouseY, delta);
        }

        if (searchedName != null && !searchedName.isEmpty()) {
            if (APIUtils.hasData(searchedName)) {
                String error = APIUtils.getError(searchedName);
                isSearching = false;

                if (error != null) {
                    context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(error).styled(s -> s.withColor(new Color(0xFF5555).getRGB())), centerX, 100, new Color(0xFF5555).getRGB());
                } else {
                    foundTiers = APIUtils.getAllTiers(searchedName);

                    context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(searchedName + "'s Profile").styled(s -> s.withBold(true).withColor(new Color(0xFFFFFF).getRGB())), centerX, 20, new Color(0xFFFFFF).getRGB());

                    if (skinWidget == null && currentProfile != null) {
                        MinecraftClient client = MinecraftClient.getInstance();
                        try {
                            Supplier<SkinTextures> skinSupplier = client.getSkinProvider()
                                    .supplySkinTextures(currentProfile, true);

                            skinWidget = new PlayerSkinWidget(
                                    60,
                                    144,
                                    client.getLoadedEntityModels(),
                                    skinSupplier
                            );
                            skinWidget.setPosition(centerX - 65, centerY - 72);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (skinWidget != null) {
                        try {
                            skinWidget.render(context, (int) mouseX, (int) mouseY, delta);
                        } catch (Exception ignored) {
                        }
                    }

                    int infoX = centerX + 10;
                    int infoY = centerY - 60;

                    int rank = APIUtils.getPlayerRank(searchedName);
                    int totalPoints = APIUtils.getPlayerTotalPoints(searchedName);

                    if (rank > 0) {
                        context.drawTextWithShadow(this.textRenderer,
                                Text.literal("Rank: ").styled(s -> s.withColor(new Color(0xFFFFFF).getRGB()))
                                        .append(Text.literal("#"+rank).styled(s -> s.withColor(new Color(0x55FF55).getRGB()))),
                                infoX, infoY, new Color(0xFFFFFF).getRGB());
                        infoY += 12;
                    }

                    if (totalPoints > 0) {
                        context.drawTextWithShadow(this.textRenderer,
                                Text.literal("Points: ").styled(s -> s.withColor(new Color(0xFFFFFF).getRGB()))
                                        .append(Text.literal(String.valueOf(totalPoints)).styled(s -> s.withColor(new Color(0xFFFF55).getRGB()))),
                                infoX, infoY, new Color(0xFFFFFF).getRGB());
                        infoY += 12;
                    }

                    infoY += 3;
                    infoY += 4;

                    if (foundTiers.isEmpty()) {
                        context.drawTextWithShadow(this.textRenderer, Text.literal("Tier bulunmuyor").styled(s -> s.withColor(new Color(0xAAAAAA).getRGB())), infoX, infoY, new Color(0xAAAAAA).getRGB());
                    } else {
                        for (Text tier : foundTiers) {
                            context.drawTextWithShadow(this.textRenderer, tier, infoX, infoY, new Color(0xFFFFFF).getRGB());
                            infoY += 12;
                        }
                    }
                }

                if (this.children().stream().noneMatch(c -> c instanceof ButtonWidget && ((ButtonWidget) c).getMessage().getString().equals("Yeni Arama"))) {
                    this.addDrawableChild(ButtonWidget.builder(Text.literal("Yeni Arama").styled(s -> s.withColor(0x3498DB)), btn -> {
                        searchedName = "";
                        isSearching = false;
                        skinWidget = null;
                        currentProfile = null;
                        foundTiers = null;

                        searchField.setText("");
                        searchField.setFocused(true);

                        updateVisibility();

                        this.remove(btn);
                    }).dimensions(centerX - 80, this.height - 55, 160, 20).build());
                }
            } else if (isSearching) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Sistemden Sorgulanıyor...").styled(s -> s.withColor(new Color(0xAAAAAA).getRGB())), centerX, centerY, new Color(0xAAAAAA).getRGB());
            }
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (searchField.isVisible() && !searchField.getText().isEmpty()) {
                startSearch();
                return true;
            }
        }

        if (input.isEscape()) {
            this.client.setScreen(parent);
            return true;
        }

        if (searchField.keyPressed(input)) {
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (skinWidget != null && skinWidget.isMouseOver(click.x(), click.y())) {
            return skinWidget.mouseDragged(click, offsetX, offsetY);
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (skinWidget != null && skinWidget.isMouseOver(mouseX, mouseY)) {
            return skinWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}