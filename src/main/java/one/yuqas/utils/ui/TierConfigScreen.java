package one.yuqas.utils.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import one.yuqas.utils.TierConfig;
import one.yuqas.utils.enums.Config;
import org.lwjgl.glfw.GLFW;

public class TierConfigScreen extends Screen {
    private final Screen parent;
    private boolean isEnabled;
    private boolean isRightSide;
    private boolean placeholder;

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
        ).dimensions(centerX - 100, 60, 200, 20).build());

        isRightSide = TierConfig.getBoolean(Config.SIDE);
        this.addDrawableChild(ButtonWidget.builder(
                getSideText(isRightSide),
                btn -> {
                    isRightSide = !isRightSide;
                    btn.setMessage(getSideText(isRightSide));
                }
        ).dimensions(centerX - 100, 110, 200, 20).build());

        placeholder = TierConfig.getBoolean(Config.SHOW_PLACEHOLDER);
        this.addDrawableChild(ButtonWidget.builder(
                getStatusText("Bekleme Göstergesi: ", placeholder),
                btn -> {
                    placeholder = !placeholder;
                    btn.setMessage(getStatusText("Bekleme Göstergesi: ", placeholder));
                }
        ).dimensions(centerX - 100, 160, 200, 20).build());

        // Alt butonlar - Daha dengeli yerleşim
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Kaydet").styled(s -> s.withColor(0x2ECC71)), btn -> {
            save();
            this.client.setScreen(parent);
        }).dimensions(centerX - 105, 200, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Vazgeç").styled(s -> s.withColor(0xE74C3C)), btn -> {
            this.client.setScreen(parent);
        }).dimensions(centerX + 5, 200, 100, 20).build());
    }

    private Text getStatusText(String prefix, boolean val) {
        return Text.literal(prefix).append(Text.literal(val ? "AKTİF" : "PASİF")
                .styled(style -> style.withColor(val ? 0x2ECC71 : 0xE74C3C)));
    }

    private Text getSideText(boolean isRight) {
        return Text.literal("Etiket Konumu: ").append(Text.literal(isRight ? "SAĞ" : "SOL")
                .styled(style -> style.withColor(0x3498DB)));
    }

    private void save() {
        TierConfig.set(Config.TAG, isEnabled);
        TierConfig.set(Config.SIDE, isRightSide);
        TierConfig.set(Config.SHOW_PLACEHOLDER, placeholder);
        TierConfig.save();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        // Arka plan paneli (Sade ve kaliteli bir görünüm için hafif karartma)
        context.fill(centerX - 120, 30, centerX + 120, 230, 0x88000000);
        
        // Kenarlık (Manual drawing for compatibility)
        int x1 = centerX - 120;
        int y1 = 30;
        int x2 = centerX + 120;
        int y2 = 230;
        int borderColor = 0xFF555555;
        
        context.fill(x1, y1, x2, y1 + 1, borderColor); // Üst
        context.fill(x1, y2 - 1, x2, y2, borderColor); // Alt
        context.fill(x1, y1 + 1, x1 + 1, y2 - 1, borderColor); // Sol
        context.fill(x2 - 1, y1 + 1, x2, y2 - 1, borderColor); // Sağ

        // Başlık (Altın sarısı/Beyaz karışımı asil bir görünüm)
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 40, 0xFFCC00);

        // Açıklama Metinleri (Soft Gri)
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(Config.TAG.getDescription()), centerX, 52, 0xCCCCCC);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(Config.SIDE.getDescription()), centerX, 102, 0xCCCCCC);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(Config.SHOW_PLACEHOLDER.getDescription()), centerX, 152, 0xCCCCCC);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.client.setScreen(parent);
            return true;
        }
        return super.keyPressed(input);
    }
}