package one.yuqas.utils.ui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.text.Text;
import one.yuqas.utils.APIUtils;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

public class PlayerSearchScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget searchField;
    private String searchedName = "";
    private List<Text> foundTiers = null;
    private boolean isSearching = false;
    private OtherClientPlayerEntity dummyPlayer = null;

    public PlayerSearchScreen(Screen parent) {
        super(Text.literal("Oyuncu Arama"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        searchField = new TextFieldWidget(this.textRenderer, centerX - 100, 40, 200, 20, Text.literal("Oyuncu Adı..."));
        searchField.setMaxLength(16);
        this.addSelectableChild(searchField);
        searchField.setFocused(true);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Ara").styled(s -> s.withColor(0x3498DB)), btn -> {
            startSearch();
        }).dimensions(centerX - 100, 65, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Geri Dön").styled(s -> s.withColor(0xE74C3C)), btn -> {
            this.client.setScreen(parent);
        }).dimensions(centerX - 100, this.height - 30, 200, 20).build());
    }

    private void startSearch() {
        searchedName = searchField.getText();
        if (searchedName.isEmpty()) return;

        isSearching = true;
        foundTiers = null;
        dummyPlayer = null;
        APIUtils.fetchSync(searchedName);
        
        // Async profile loading for skin
        new Thread(() -> {
            try {
                // Simplified profile creation - in a real mod you'd fetch the UUID from Mojang API
                // For demonstration, we use a random UUID if not found or just a placeholder
                GameProfile profile = new GameProfile(UUID.randomUUID(), searchedName);
                if (this.client.world != null) {
                    this.client.execute(() -> {
                        dummyPlayer = new OtherClientPlayerEntity(this.client.world, profile);
                    });
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

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 15, 0xFFCC00);
        searchField.render(context, mouseX, mouseY, delta);

        if (searchedName != null && !searchedName.isEmpty()) {
            if (APIUtils.hasData(searchedName)) {
                foundTiers = APIUtils.getAllTiers(searchedName);
                isSearching = false;

                // Player Skin Render
                if (dummyPlayer != null) {
                    InventoryScreen.drawEntity(context, centerX - 80, 160, 40, new Quaternionf().rotateY((float) Math.toRadians(180)), null, dummyPlayer);
                }

                int y = 100;
                // Translucent effect using color with alpha
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(searchedName).styled(s -> s.withBold(true).withColor(0xCCFFFFFF)), centerX + 40, y - 15, 0xCCFFFFFF);

                if (foundTiers.isEmpty()) {
                    context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Tier bulunmuyor").styled(s -> s.withColor(0xCCFF5555)), centerX + 40, y, 0xCCFF5555);
                } else {
                    for (Text tier : foundTiers) {
                        // Apply translucency to tier text if possible or just use the color
                        context.drawCenteredTextWithShadow(this.textRenderer, tier, centerX + 40, y, 0xCCFFFFFF);
                        y += 12;
                    }
                }
            } else if (isSearching) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Aranıyor...").styled(s -> s.withColor(0x88AAAAAA)), centerX, 100, 0x88AAAAAA);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            startSearch();
            return true;
        }
        if (searchField.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchField.charTyped(chr, modifiers)) return true;
        return super.charTyped(chr, modifiers);
    }
}
