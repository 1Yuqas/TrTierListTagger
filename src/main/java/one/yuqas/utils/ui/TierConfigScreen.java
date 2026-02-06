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
                getStatusText("Oyuncu Etiketinde Göster: ", isEnabled),
                btn -> {
                    isEnabled = !isEnabled;
                    btn.setMessage(getStatusText("Oyuncu Etiketinde Göster: ", isEnabled));
                }
        ).dimensions(centerX - 100, 45, 200, 20).build());

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
                getStatusText("Bekleme Yazısı: ", placeholder),
                btn -> {
                    placeholder = !placeholder;
                    btn.setMessage(getStatusText("Bekleme Yazısı: ", placeholder));
                }
        ).dimensions(centerX - 100, 145, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Ayarları Kaydet").styled(s -> s.withColor(0x55FF55)), btn -> {
            save();
            this.client.setScreen(parent);
        }).dimensions(centerX - 100, this.height - 40, 200, 20).build());
    }

    private Text getStatusText(String prefix, boolean val) {
        return Text.literal(prefix).append(Text.literal(val ? "AÇIK" : "KAPALI")
                .styled(style -> style.withColor(val ? 0x55FF55 : 0xFF5555)));
    }

    private Text getSideText(boolean isRight) {
        return Text.literal("Tag Tarafı: ").append(Text.literal(isRight ? "SAĞ" : "SOL")
                .styled(style -> style.withColor(0x55FFFF)));
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

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 15, 0xFFFF55);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(Config.TAG.getDescription()), centerX, 35, 0xAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(Config.SIDE.getDescription()), centerX, 85, 0xAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(Config.SHOW_PLACEHOLDER.getDescription()), centerX, 135, 0xAAAAAA);
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