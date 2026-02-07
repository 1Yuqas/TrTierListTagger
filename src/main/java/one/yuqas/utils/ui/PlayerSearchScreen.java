package one.yuqas.utils.ui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import one.yuqas.utils.APIUtils;
import org.lwjgl.glfw.GLFW;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerSearchScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget searchField;
    private ButtonWidget searchButton;
    private String searchedName = "";
    private List<Text> foundTiers = null;
    private boolean isSearching = false;
    private OtherClientPlayerEntity dummyPlayer = null;
    private int animationTick = 0;

    public PlayerSearchScreen(Screen parent) {
        super(Text.literal("Oyuncu Arama"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        // Search Field with improved styling
        searchField = new TextFieldWidget(this.textRenderer, centerX - 100, 80, 200, 24, Text.literal("Oyuncu Adı..."));
        searchField.setMaxLength(16);
        searchField.setPlaceholder(Text.literal("Minecraft kullanıcı adı...").styled(s -> s.withColor(0x888888)));
        this.addSelectableChild(searchField);
        searchField.setFocused(true);

        // Search Button
        searchButton = ButtonWidget.builder(Text.literal("🔍 Ara").styled(s -> s.withColor(0xFFFFFF)), btn -> {
            startSearch();
        }).dimensions(centerX - 100, 110, 200, 24).build();
        this.addDrawableChild(searchButton);

        // Back Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("« Geri").styled(s -> s.withColor(0xAAAAAA)), btn -> {
            this.client.setScreen(parent);
        }).dimensions(10, this.height - 30, 80, 20).build());

        updateVisibility();
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
        
        // Fetch UUID and create player entity with skin
        new Thread(() -> {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + searchedName);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                
                if (con.getResponseCode() == 200) {
                    try (java.io.InputStreamReader reader = new java.io.InputStreamReader(con.getInputStream())) {
                        com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(reader, com.google.gson.JsonObject.class);
                        String id = json.get("id").getAsString();
                        String formattedId = id.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5");
                        UUID uuid = UUID.fromString(formattedId);

                        // Create profile with textures
                        GameProfile profile = new GameProfile(uuid, searchedName);
                        
                        this.client.execute(() -> {
                            // Create dummy player with world (even in menu)
                            if (this.client.world != null) {
                                dummyPlayer = new OtherClientPlayerEntity(this.client.world, profile);
                            } else {
                                // If world is null, we need to handle it differently
                                // Store profile for later rendering
                            }
                            
                            // Fetch skin textures - this is crucial!
                            MinecraftClient.getInstance().getSkinProvider().fetchSkinTextures(profile).thenAccept(textures -> {
                                if (dummyPlayer != null && textures.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                                    // Skin is now loaded
                                }
                            });
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Gradient background
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        
        super.render(context, mouseX, mouseY, delta);
        
        animationTick++;
        int centerX = this.width / 2;

        if (searchField.visible) {
            // Title with glow effect
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("━━━━━━━━━━━━━━━━━━━━━━").styled(s -> s.withColor(0x3498DB)), 
                centerX, 35, 0x3498DB);
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("OYUNCU SORGULAMA").styled(s -> s.withBold(true).withColor(0xFFCC00)), 
                centerX, 50, 0xFFCC00);
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("━━━━━━━━━━━━━━━━━━━━━━").styled(s -> s.withColor(0x3498DB)), 
                centerX, 65, 0x3498DB);
            
            searchField.render(context, mouseX, mouseY, delta);
        }

        if (searchedName != null && !searchedName.isEmpty()) {
            if (APIUtils.hasData(searchedName)) {
                String error = APIUtils.getError(searchedName);
                isSearching = false;

                if (error != null) {
                    // Error display with box
                    int boxWidth = 300;
                    int boxHeight = 80;
                    int boxX = centerX - boxWidth / 2;
                    int boxY = 100;
                    
                    context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xDD000000);
                    context.drawBorder(boxX, boxY, boxWidth, boxHeight, 0xFFFF5555);
                    
                    context.drawCenteredTextWithShadow(this.textRenderer, 
                        Text.literal("⚠ HATA").styled(s -> s.withBold(true).withColor(0xFFFF5555)), 
                        centerX, boxY + 15, 0xFFFF5555);
                    context.drawCenteredTextWithShadow(this.textRenderer, 
                        Text.literal(searchedName).styled(s -> s.withColor(0xFFFFFF)), 
                        centerX, boxY + 35, 0xFFFFFF);
                    context.drawCenteredTextWithShadow(this.textRenderer, 
                        Text.literal(error).styled(s -> s.withColor(0xFF5555)), 
                        centerX, boxY + 55, 0xFF5555);
                } else {
                    foundTiers = APIUtils.getAllTiers(searchedName);

                    // Profile container
                    int containerWidth = 500;
                    int containerHeight = 280;
                    int containerX = centerX - containerWidth / 2;
                    int containerY = 60;
                    
                    // Background box with border
                    context.fill(containerX, containerY, containerX + containerWidth, containerY + containerHeight, 0xEE000000);
                    context.drawBorder(containerX, containerY, containerWidth, containerHeight, 0xFF3498DB);
                    
                    // Header
                    context.fill(containerX, containerY, containerX + containerWidth, containerY + 30, 0xAA3498DB);
                    context.drawCenteredTextWithShadow(this.textRenderer, 
                        Text.literal("👤 " + searchedName + "'s Profile").styled(s -> s.withBold(true).withColor(0xFFFFFF)), 
                        centerX, containerY + 10, 0xFFFFFF);

                    // Left side - Player Skin with enhanced rendering
                    int skinBoxX = containerX + 20;
                    int skinBoxY = containerY + 45;
                    int skinBoxSize = 180;
                    
                    context.fill(skinBoxX, skinBoxY, skinBoxX + skinBoxSize, skinBoxY + skinBoxSize, 0x55000000);
                    context.drawBorder(skinBoxX, skinBoxY, skinBoxSize, skinBoxSize, 0xFF2980B9);
                    
                    if (dummyPlayer != null) {
                        // Enhanced 3D player rendering with animation
                        float rotation = (animationTick % 360);
                        int centerSkinX = skinBoxX + skinBoxSize / 2;
                        int centerSkinY = skinBoxY + skinBoxSize - 20;
                        
                        InventoryScreen.drawEntity(context, 
                            centerSkinX - 40, centerSkinY, 
                            centerSkinX + 40, centerSkinY + 80, 
                            50, 
                            rotation * 0.3f, 
                            mouseX - centerSkinX, 
                            (float)(skinBoxY + 100 - mouseY), 
                            dummyPlayer);
                    } else {
                        // Loading animation
                        int dots = (animationTick / 10) % 4;
                        String loadingText = "Yükleniyor" + ".".repeat(dots);
                        context.drawCenteredTextWithShadow(this.textRenderer, 
                            Text.literal(loadingText).styled(s -> s.withColor(0xAAAAA)), 
                            skinBoxX + skinBoxSize / 2, skinBoxY + skinBoxSize / 2, 0xAAAAAA);
                    }

                    // Right side - Rankings
                    int rankingsX = containerX + 220;
                    int rankingsY = containerY + 45;
                    int rankingsWidth = 260;
                    int rankingsHeight = 180;
                    
                    context.fill(rankingsX, rankingsY, rankingsX + rankingsWidth, rankingsY + rankingsHeight, 0x55000000);
                    context.drawBorder(rankingsX, rankingsY, rankingsWidth, rankingsHeight, 0xFF2980B9);
                    
                    int y = rankingsY + 10;
                    context.drawTextWithShadow(this.textRenderer, 
                        Text.literal("📊 Rankings:").styled(s -> s.withBold(true).withColor(0xFFCC00)), 
                        rankingsX + 10, y, 0xFFCC00);
                    y += 20;

                    if (foundTiers.isEmpty()) {
                        context.drawTextWithShadow(this.textRenderer, 
                            Text.literal("• Tier bulunamadı").styled(s -> s.withColor(0xFF5555)), 
                            rankingsX + 15, y, 0xFF5555);
                    } else {
                        for (Text tier : foundTiers) {
                            context.drawTextWithShadow(this.textRenderer, 
                                Text.literal("• ").styled(s -> s.withColor(0x3498DB)).append(tier), 
                                rankingsX + 15, y, 0xFFFFFF);
                            y += 14;
                        }
                    }
                }

                // "New Search" button
                if (this.children().stream().noneMatch(c -> c instanceof ButtonWidget && ((ButtonWidget)c).getMessage().getString().contains("Yeni Arama"))) {
                    this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("🔄 Yeni Arama").styled(s -> s.withColor(0xFFFFFF)), 
                        btn -> {
                            searchedName = "";
                            searchField.setText("");
                            isSearching = false;
                            dummyPlayer = null;
                            this.clearChildren();
                            this.init();
                        }).dimensions(centerX - 100, this.height - 60, 200, 24).build());
                }

            } else if (isSearching) {
                // Loading animation
                int dots = (animationTick / 15) % 4;
                String loadingDots = "●".repeat(dots + 1) + "○".repeat(3 - dots);
                context.drawCenteredTextWithShadow(this.textRenderer, 
                    Text.literal(loadingDots).styled(s -> s.withColor(0x3498DB)), 
                    centerX, 100, 0x3498DB);
                context.drawCenteredTextWithShadow(this.textRenderer, 
                    Text.literal("Sistemden Sorgulanıyor...").styled(s -> s.withColor(0xAAAAAA)), 
                    centerX, 120, 0xAAAAAA);
            }
        }
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
            this.client.setScreen(parent);
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
    
    @Override
    public void tick() {
        super.tick();
        searchField.tick();
    }
}