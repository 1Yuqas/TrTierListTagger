package one.yuqas.utils.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import one.yuqas.utils.TierConfig;
import one.yuqas.utils.enums.Config;
import org.lwjgl.glfw.GLFW;

public class TierConfigScreen extends Screen {
    private final Screen parent;
    private boolean isEnabled;
    private boolean isRightSide;
    private boolean placeholder;
    private boolean isTabEnabled;
    private boolean isTabRightSide;

    public TierConfigScreen(Screen parent) {
        super(Text.literal("TRTierList Ayarları"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        isEnabled = TierConfig.getBoolean(Config.TAG);
        this.addDrawableChild(ButtonWidget.builder(
                getStatusText("Etiket Görünümü: ", isEnabled),
                btn -> {
                    isEnabled = !isEnabled;
                    btn.setMessage(getStatusText("Etiket Görünümü: ", isEnabled));
                }
        ).dimensions(centerX - 100, 55, 200, 20).build());

        isRightSide = TierConfig.getBoolean(Config.SIDE);
        this.addDrawableChild(ButtonWidget.builder(
                getSideText(isRightSide),
                btn -> {
                    isRightSide = !isRightSide;
                    btn.setMessage(getSideText(isRightSide));
                }
        ).dimensions(centerX - 100, 95, 200, 20).build());

        placeholder = TierConfig.getBoolean(Config.SHOW_PLACEHOLDER);
        this.addDrawableChild(ButtonWidget.builder(
                getStatusText("Bekleme Göstergesi: ", placeholder),
                btn -> {
                    placeholder = !placeholder;
                    btn.setMessage(getStatusText("Bekleme Göstergesi: ", placeholder));
                }
        ).dimensions(centerX - 100, 135, 200, 20).build());

        // Tab menüsü etiket ayarları
        isTabEnabled = TierConfig.getBoolean(Config.TAB_TAG);
        this.addDrawableChild(ButtonWidget.builder(
                getStatusText("Tab Menüsünde Etiket: ", isTabEnabled),
                btn -> {
                    isTabEnabled = !isTabEnabled;
                    btn.setMessage(getStatusText("Tab Menüsünde Etiket: ", isTabEnabled));
                }
        ).dimensions(centerX - 100, 160, 200, 20).build());

        isTabRightSide = TierConfig.getBoolean(Config.TAB_SIDE);
        this.addDrawableChild(ButtonWidget.builder(
                getTabSideText(isTabRightSide),
                btn -> {
                    isTabRightSide = !isTabRightSide;
                    btn.setMessage(getTabSideText(isTabRightSide));
                }
        ).dimensions(centerX - 100, 200, 200, 20).build());

        // Arama Butonu
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Oyuncu Ara").styled(s -> s.withColor(0xFFCC00)), btn -> {
            this.client.setScreen(new PlayerSearchScreen(this));
        }).dimensions(centerX - 100, 225, 200, 20).build());

        // Alt butonlar - Daha yakın ve toplu
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Kaydet").styled(s -> s.withColor(0x2ECC71)), btn -> {
            save();
            this.client.setScreen(parent);
        }).dimensions(centerX - 105, 255, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Vazgeç").styled(s -> s.withColor(0xE74C3C)), btn -> {
            this.client.setScreen(parent);
        }).dimensions(centerX + 5, 255, 100, 20).build());
    }

    private Text getStatusText(String prefix, boolean val) {
        return Text.literal(prefix).append(Text.literal(val ? "AKTİF" : "PASİF")
                .styled(style -> style.withColor(val ? 0x2ECC71 : 0xE74C3C)));
    }

    private Text getSideText(boolean isRight) {
        return Text.literal("Etiket Konumu: ").append(Text.literal(isRight ? "SAĞ" : "SOL")
                .styled(style -> style.withColor(0x3498DB)));
    }

    private Text getTabSideText(boolean isRight) {
        return Text.literal("Tab Konumu: ").append(Text.literal(isRight ? "SAĞ" : "SOL")
                .styled(style -> style.withColor(0x3498DB)));
    }

    private void save() {
        TierConfig.set(Config.TAG, isEnabled);
        TierConfig.set(Config.SIDE, isRightSide);
        TierConfig.set(Config.SHOW_PLACEHOLDER, placeholder);
        TierConfig.set(Config.TAB_TAG, isTabEnabled);
        TierConfig.set(Config.TAB_SIDE, isTabRightSide);
        TierConfig.save();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        // Başlık (Altın sarısı/Beyaz karışımı asil bir görünüm)
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 15, 0xFFCC00);

        // Açıklama Metinleri (Soft Gri) - Butonlara daha yakın
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(Config.TAG.getDescription()), centerX, 45, 0xCCCCCC);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(Config.SIDE.getDescription()), centerX, 85, 0xCCCCCC);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(Config.SHOW_PLACEHOLDER.getDescription()), centerX, 125, 0xCCCCCC);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(Config.TAB_TAG.getDescription()), centerX, 150, 0xCCCCCC);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(Config.TAB_SIDE.getDescription()), centerX, 190, 0xCCCCCC);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}