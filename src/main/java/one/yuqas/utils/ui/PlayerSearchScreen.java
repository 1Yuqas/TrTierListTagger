package one.yuqas.utils.ui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.text.Text;
import one.yuqas.utils.APIUtils;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

public class PlayerSearchScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget searchField;
    private ButtonWidget searchButton;
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

        searchButton = ButtonWidget.builder(Text.literal("Ara").styled(s -> s.withColor(0x3498DB)), btn -> {
            startSearch();
        }).dimensions(centerX - 100, 65, 200, 20).build();
        this.addDrawableChild(searchButton);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Geri Dön").styled(s -> s.withColor(0xE74C3C)), btn -> {
            this.client.setScreen(parent);
        }).dimensions(centerX - 100, this.height - 30, 200, 20).build());

        updateVisibility();
    }

    private void updateVisibility() {
        boolean showSearch = !isSearching && (searchedName == null || searchedName.isEmpty());
        searchField.visible = showSearch;
        searchButton.visible = showSearch;
    }

    private void startSearch() {
        searchedName = searchField.getText();
        if (searchedName.isEmpty()) return;

        isSearching = true;
        foundTiers = null;
        dummyPlayer = null;
        updateVisibility();
        APIUtils.fetchSync(searchedName);
        
        // Async profile loading for skin
        new Thread(() -> {
            try {
                // In a real mod we'd fetch the actual UUID
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
        
        if (searchField.visible) {
            searchField.render(context, mouseX, mouseY, delta);
        }

        if (searchedName != null && !searchedName.isEmpty()) {
            if (APIUtils.hasData(searchedName)) {
                String error = APIUtils.getError(searchedName);
                isSearching = false;

                if (error != null) {
                    context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(searchedName).styled(s -> s.withBold(true).withColor(0xCCFFFFFF)), centerX, 80, 0xCCFFFFFF);
                    context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(error).styled(s -> s.withColor(0xCCFF5555)), centerX, 100, 0xCCFF5555);
                } else {
                    foundTiers = APIUtils.getAllTiers(searchedName);

                    // Player Skin Render - Adjusted for 1.21.1 and mouse tracking
                    if (dummyPlayer != null) {
                        // Drawing entity with mouse tracking (mouseX and mouseY relative to entity position)
                        InventoryScreen.drawEntity(context, centerX - 120, 80, centerX - 40, 160, 40, 0.0625F, mouseX, mouseY, dummyPlayer);
                    }

                    int y = 100;
                    context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(searchedName).styled(s -> s.withBold(true).withColor(0xCCFFFFFF)), centerX + 40, y - 15, 0xCCFFFFFF);

                    if (foundTiers.isEmpty()) {
                        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Tier bulunmuyor").styled(s -> s.withColor(0xCCFF5555)), centerX + 40, y, 0xCCFF5555);
                    } else {
                        for (Text tier : foundTiers) {
                            context.drawCenteredTextWithShadow(this.textRenderer, tier, centerX + 40, y, 0xCCFFFFFF);
                            y += 12;
                        }
                    }
                }

                // "Yeni Arama" butonu ekle
                if (this.children().stream().noneMatch(c -> c instanceof ButtonWidget && ((ButtonWidget)c).getMessage().getString().equals("Yeni Arama"))) {
                    this.addDrawableChild(ButtonWidget.builder(Text.literal("Yeni Arama").styled(s -> s.withColor(0x3498DB)), btn -> {
                        searchedName = "";
                        searchField.setText("");
                        isSearching = false;
                        dummyPlayer = null;
                        this.clearChildren();
                        this.init();
                    }).dimensions(centerX - 100, this.height - 55, 200, 20).build());
                }

            } else if (isSearching) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Aranıyor...").styled(s -> s.withColor(0x88AAAAAA)), centerX, 100, 0x88AAAAAA);
            }
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
            startSearch();
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
