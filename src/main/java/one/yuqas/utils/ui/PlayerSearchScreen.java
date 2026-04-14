package one.yuqas.utils.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import com.mojang.authlib.GameProfile;

public class PlayerSearchScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget searchField;
    private ButtonWidget searchButton;
    private String searchedName = "";
    private List<Text> foundTiers = null;
    private boolean isSearching = false;
    private OtherClientPlayerEntity dummyPlayer = null;
    private float yaw = 0.0F;
    private float pitch = 0.0F;
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
        boolean showSearch = searchedName == null || searchedName.isEmpty();
        searchField.visible = showSearch;
        searchButton.visible = showSearch;
    }

    private void startSearch() {
        searchedName = searchField.getText().trim();
        if (searchedName.isEmpty()) return;

        System.out.println("[PlayerSearchScreen] startSearch: " + searchedName);
        isSearching = true;
        foundTiers = null;
        dummyPlayer = null;
        yaw = 0.0F;
        pitch = 0.0F;
        updateVisibility();

        // Basit tier listesi
        foundTiers = List.of(
            Text.literal("§aHT1 - High Tier 1"),
            Text.literal("§bLT2 - Low Tier 2")
        );

        // Skin widget oluştur
        MinecraftClient client = MinecraftClient.getInstance();
        GameProfile profile = new GameProfile(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), searchedName);
        if (client.world != null) {
            dummyPlayer = new OtherClientPlayerEntity(client.world, profile);
            client.getSkinProvider().fetchSkinTextures(profile);
        }

        isSearching = false;
        updateVisibility();
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
            if (foundTiers != null) {
                String error = APIUtils.getError(searchedName);
                isSearching = false;

                if (error != null) {
                    context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(error).styled(s -> s.withColor(0xFF5555)), centerX, 100, 0xFF5555);
                } else {
                    foundTiers = APIUtils.getAllTiers(searchedName);
                    
                    context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(searchedName + "'s Profile").styled(s -> s.withBold(true).withColor(0xFFFFFF)), centerX, 20, 0xFFFFFF);

                    // --- 3D PLAYER RENDER DÜZELTMESİ ---
  if (dummyPlayer != null) {
    dummyPlayer.tick(); 
    System.out.println("[PlayerSearchScreen] rendering dummyPlayer for " + searchedName);
    
    int x = centerX - 90;
    int y = centerY + 50; 

    // Mouse ile döndürme için yaw ve pitch kullan
    InventoryScreen.drawEntity(
        context, 
        x - 30, y - 70, x + 30, y + 10, // Karakterin kutu sınırları
        50,                             // Boyut (Scale)
        0.0625F,                        // Look delta
        yaw,                            // Yaw (döndürme)
        pitch,                          // Pitch (döndürme)
        dummyPlayer                     // Render edilecek oyuncu
    );
}
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
                        yaw = 0.0F;
                        pitch = 0.0F;
                        this.clearChildren();
                        this.init();
                    }).dimensions(centerX - 80, this.height - 55, 160, 20).build());
                }
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dummyPlayer != null) {
            yaw += (float) deltaX * 0.5F;
            pitch = Math.max(-90.0F, Math.min(90.0F, pitch + (float) deltaY * 0.5F));
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
