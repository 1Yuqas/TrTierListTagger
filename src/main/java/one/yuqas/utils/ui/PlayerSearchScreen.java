package one.yuqas.utils.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import com.mojang.authlib.GameProfile;
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

        System.out.println("[PlayerSearchScreen] startSearch: " + searchedName);
        isSearching = true;
        foundTiers = null;
        skinWidget = null;
        currentProfile = null;
        updateVisibility();
        APIUtils.fetchSync(searchedName);

        // Profili ve skin'i async olarak fetch et
        new Thread(() -> {
            try {
                // Test için Notch profili (UUID biliyoruz)
                UUID uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
                GameProfile profile = new GameProfile(uuid, searchedName);

                // Skin dokusu fetch et
                MinecraftClient client = MinecraftClient.getInstance();
                client.getSkinProvider().fetchSkinTextures(profile);

                currentProfile = profile;
                System.out.println("[PlayerSearchScreen] Profile fetch tamamlandı: " + profile.getName());

                // Ana thread'de widget oluştur
                client.execute(() -> {
                    System.out.println("[PlayerSearchScreen] PlayerSkinWidget oluşturuluyor...");
                });

            } catch (Exception e) {
                System.out.println("[PlayerSearchScreen] Profile fetch hatası: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        System.out.println("[PlayerSearchScreen] Profile fetch başladı");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (searchField.visible) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("OYUNCU SORGULAMA").styled(s -> s.withBold(true).withColor(0xFFCC00)), centerX, 40, 0xFFCC00);
            searchField.render(context, mouseX, mouseY, delta);
        }

        if (searchedName != null && !searchedName.isEmpty()) {
            if (APIUtils.hasData(searchedName)) {
                String error = APIUtils.getError(searchedName);
                isSearching = false;

                if (error != null) {
                    context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(error).styled(s -> s.withColor(0xFF5555)), centerX, 100, 0xFF5555);
                } else {
                    foundTiers = APIUtils.getAllTiers(searchedName);
                    
                    context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(searchedName + "'s Profile").styled(s -> s.withBold(true).withColor(0xFFFFFF)), centerX, 20, 0xFFFFFF);

                    // --- PROFILE VE WIDGET OLUŞTUR ---
                    if (currentProfile == null) {
                        // Profili oluştur
                        UUID uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
                        currentProfile = new GameProfile(uuid, searchedName);
                        System.out.println("[PlayerSearchScreen] GameProfile oluşturuldu: " + currentProfile);
                    }

                    if (skinWidget == null && currentProfile != null) {
                        // PlayerSkinWidget'ı oluştur
                        MinecraftClient client = MinecraftClient.getInstance();
                        
                        try {
                            // Skin supplier - profile skin texture'sini sağla
                            Supplier<SkinTextures> skinSupplier = client.getSkinProvider()
                                .getSkinTexturesSupplier(currentProfile);
                            
                            skinWidget = new PlayerSkinWidget(
                                60,   // Genişlik
                                144,  // Yükseklik
                                client.getLoadedEntityModels(), // 3D Modeller
                                skinSupplier // Skin dokusu supplier
                            );
                            skinWidget.setPosition(centerX - 65, centerY - 72);
                            System.out.println("[PlayerSearchScreen] PlayerSkinWidget oluşturuldu - UUID: " + currentProfile.getId());
                        } catch (Exception e) {
                            System.out.println("[PlayerSearchScreen] Widget oluşturma hatası: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    // Widget'ı render et
                    if (skinWidget != null) {
                        skinWidget.render(context, (int)mouseX, (int)mouseY, delta);
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
                        skinWidget = null;
                        currentProfile = null;
                        foundTiers = null;
                        this.clearChildren();
                        this.init();
                    }).dimensions(centerX - 80, this.height - 55, 160, 20).build());
                }
            } else if (isSearching) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Sistemden Sorgulanıyor...").styled(s -> s.withColor(0xAAAAAA)), centerX, centerY, 0xAAAAAA);
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
        if (skinWidget != null && skinWidget.isMouseOver(mouseX, mouseY)) {
            return skinWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (skinWidget != null && skinWidget.isMouseOver(mouseX, mouseY)) {
            return skinWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
