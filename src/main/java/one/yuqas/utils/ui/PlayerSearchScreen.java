package one.yuqas.utils.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;
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
    private EditBox searchField;
    private Button searchButton;
    private String searchedName = "";
    private List<Component> foundTiers = null;
    private boolean isSearching = false;
    private PlayerSkinWidget skinWidget = null;
    private GameProfile currentProfile = null;
    private float mouseX = 0.0F;
    private float mouseY = 0.0F;
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

        searchField = new EditBox(this.font, centerX - 80, 60, 160, 20, Component.literal("Oyuncu Adı..."));
        searchField.setMaxLength(16);
        this.addRenderableWidget(searchField);
        searchField.setFocused(true);

        searchButton = Button.builder(Component.literal("Ara").withStyle(s -> s.withColor(0x3498DB)), btn -> startSearch())
                .bounds(centerX - 80, 85, 160, 20).build();
        this.addRenderableWidget(searchButton);

        this.addRenderableWidget(Button.builder(Component.literal("Bitti").withStyle(s -> s.withColor(0xCCCCCC)), btn -> this.minecraft.gui.setScreen(parent))
                .bounds(centerX - 80, this.height - 30, 160, 20).build());

        updateVisibility();

        if (presetName != null && !presetName.isEmpty()) {
            searchField.insertText(presetName);
            startSearch();
        }
    }

    private void updateVisibility() {
        boolean showSearch = !isSearching && (searchedName == null || searchedName.isEmpty());
        searchField.visible = showSearch;
        searchButton.visible = showSearch;
    }

    private void startSearch() {
        searchedName = searchField.getValue().trim();
        if (searchedName.isEmpty()) return;

        isSearching = true;
        foundTiers = null;
        skinWidget = null;
        currentProfile = null;
        updateVisibility();
        APIUtils.fetchSync(searchedName);

        new Thread(() -> {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + searchedName);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                if (con.getResponseCode() == 200) {
                    try (java.io.InputStreamReader reader = new java.io.InputStreamReader(con.getInputStream())) {
                        JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                        String uuidStr = json.get("id").getAsString();
                        String playerName = json.get("name").getAsString();
                        String formattedUuid = uuidStr.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5");
                        UUID uuid = UUID.fromString(formattedUuid);

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

                        com.mojang.authlib.properties.PropertyMap properties = new com.mojang.authlib.properties.PropertyMap(tempMap);

                        GameProfile profileWithSkin = new GameProfile(uuid, playerName, properties);

                        Minecraft.getInstance().execute(() -> {
                            this.currentProfile = profileWithSkin;
                            this.skinWidget = null;
                            this.isSearching = false;
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Minecraft.getInstance().execute(() -> isSearching = false);
            }
        }).start();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.mouseX = (float)mouseX;
        this.mouseY = (float)mouseY;

        super.extractRenderState(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (searchField.visible) {
            context.centeredText(this.font, Component.literal("OYUNCU SORGULAMA").withStyle(s -> s.withBold(true).withColor(new Color(0xFFCC00).getRGB())), centerX, 40, new Color(0xFFCC00).getRGB());
            searchField.extractRenderState(context, mouseX, mouseY, delta);
        }

        if (searchedName != null && !searchedName.isEmpty()) {
            if (APIUtils.hasData(searchedName)) {
                String error = APIUtils.getError(searchedName);
                isSearching = false;

                if (error != null) {
                    context.centeredText(this.font, Component.literal(error).withStyle(s -> s.withColor(new Color(0xFF5555).getRGB())), centerX, 100, new Color(0xFF5555).getRGB());
                } else {
                    foundTiers = APIUtils.getAllTiers(searchedName);

                    context.centeredText(this.font, Component.literal(searchedName + "'s Profile").withStyle(s -> s.withBold(true).withColor(new Color(0xFFFFFF).getRGB())), centerX, 20, new Color(0xFFFFFF).getRGB());

                    if (skinWidget == null && currentProfile != null) {
                        Minecraft client = Minecraft.getInstance();
                        try {
                            SkinManager skinManager = client.getSkinManager();
                            Supplier<PlayerSkin> skinSupplier = skinManager.createLookup(currentProfile, true);

                            skinWidget = new PlayerSkinWidget(60, 144, client.getEntityModels(), skinSupplier);
                            skinWidget.setX(centerX - 65);
                            skinWidget.setY(centerY - 72);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (skinWidget != null) {
                        try {
                            skinWidget.extractRenderState(context, mouseX, mouseY, delta);
                        } catch (Exception ignored) {}
                    }

                    int infoX = centerX + 10;
                    int infoY = centerY - 60;

                    int rank = APIUtils.getPlayerRank(searchedName);
                    int totalPoints = APIUtils.getPlayerTotalPoints(searchedName);

                    if (rank > 0) {
                        context.text(this.font,
                                Component.literal("Rank: ").withStyle(s -> s.withColor(new Color(0xFFFFFF).getRGB()))
                                        .append(Component.literal("#"+rank).withStyle(s -> s.withColor(new Color(0x55FF55).getRGB()))),
                                infoX, infoY, new Color(0xFFFFFF).getRGB());
                        infoY += 12;
                    }

                    if (totalPoints > 0) {
                        context.text(this.font,
                                Component.literal("Points: ").withStyle(s -> s.withColor(new Color(0xFFFFFF).getRGB()))
                                        .append(Component.literal(String.valueOf(totalPoints)).withStyle(s -> s.withColor(new Color(0xFFFF55).getRGB()))),
                                infoX, infoY, new Color(0xFFFFFF).getRGB());
                        infoY += 12;
                    }

                    infoY += 3;
                    infoY += 4;

                    if (foundTiers == null || foundTiers.isEmpty()) {
                        int grayColor = new Color(0xAAAAAA).getRGB();
                        context.centeredText(this.font, Component.literal("Tier bulunmuyor").withStyle(s -> s.withColor(grayColor)), infoX, infoY, grayColor);
                    } else {
                        for (Component tier : foundTiers) {
                            int tierColor = new Color(0xFFFFFF).getRGB();
                            context.text(this.font, tier, infoX, infoY, tierColor);
                            infoY += 12;
                        }
                    }
                }

                if (this.children().stream().noneMatch(c -> c instanceof Button && ((Button)c).getMessage().getString().equals("Yeni Arama"))) {
                    this.addRenderableWidget(Button.builder(Component.literal("Yeni Arama").withStyle(s -> s.withColor(new Color(0x3498DB).getRGB())), btn -> {
                        searchedName = "";
                        isSearching = false;
                        skinWidget = null;
                        currentProfile = null;
                        foundTiers = null;
                        searchField.setValue("");
                        searchField.setFocused(true);
                        updateVisibility();
                        this.removeWidget(btn);
                    }).bounds(centerX - 80, this.height - 55, 160, 20).build());
                }
            } else if (isSearching) {
                int loadingColor = new Color(0xAAAAAA).getRGB();
                context.centeredText(this.font, Component.literal("Sistemden Sorgulanıyor...").withStyle(s -> s.withColor(loadingColor)), centerX, centerY, loadingColor);
            }
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.searchField.isVisible() && !this.searchField.getValue().isEmpty()) {
                this.startSearch();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.gui.setScreen(this.parent);
            return true;
        }
        if (this.searchField.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, final double dx, final double dy) {
        if (this.skinWidget != null && this.skinWidget.isMouseOver(event.x(), event.y())) {
            return this.skinWidget.mouseDragged(event, dx, dy);
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (skinWidget != null && skinWidget.isMouseOver(mouseX, mouseY)) {
            return skinWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}