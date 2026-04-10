package one.yuqas.utils.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
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
        super(Component.literal("TRTierList Ayarları"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        isEnabled = TierConfig.getBoolean(Config.TAG);
        this.addRenderableWidget(Button.builder(
                getStatusText("Etiket Görünümü: ", isEnabled),
                btn -> {
                    isEnabled = !isEnabled;
                    btn.setMessage(getStatusText("Etiket Görünümü: ", isEnabled));
                }
        ).bounds(centerX - 100, 55, 200, 20).build());

        isRightSide = TierConfig.getBoolean(Config.SIDE);
        this.addRenderableWidget(Button.builder(
                getSideText(isRightSide),
                btn -> {
                    isRightSide = !isRightSide;
                    btn.setMessage(getSideText(isRightSide));
                }
        ).bounds(centerX - 100, 95, 200, 20).build());

        placeholder = TierConfig.getBoolean(Config.SHOW_PLACEHOLDER);
        this.addRenderableWidget(Button.builder(
                getStatusText("Bekleme Göstergesi: ", placeholder),
                btn -> {
                    placeholder = !placeholder;
                    btn.setMessage(getStatusText("Bekleme Göstergesi: ", placeholder));
                }
        ).bounds(centerX - 100, 135, 200, 20).build());

        isTabEnabled = TierConfig.getBoolean(Config.TAB_TAG);
        this.addRenderableWidget(Button.builder(
                getStatusText("Tab Menüsünde Etiket: ", isTabEnabled),
                btn -> {
                    isTabEnabled = !isTabEnabled;
                    btn.setMessage(getStatusText("Tab Menüsünde Etiket: ", isTabEnabled));
                }
        ).bounds(centerX - 100, 175, 200, 20).build());

        isTabRightSide = TierConfig.getBoolean(Config.TAB_SIDE);
        this.addRenderableWidget(Button.builder(
                getTabSideText(isTabRightSide),
                btn -> {
                    isTabRightSide = !isTabRightSide;
                    btn.setMessage(getTabSideText(isTabRightSide));
                }
        ).bounds(centerX - 100, 215, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Oyuncu Ara").withStyle(s -> s.withColor(0xFFFFCC00)), btn -> {
            this.minecraft.setScreen(new PlayerSearchScreen(this));
        }).bounds(centerX - 100, 250, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Kaydet").withStyle(s -> s.withColor(0xFF2ECC71)), btn -> {
            save();
            this.minecraft.setScreen(parent);
        }).bounds(centerX - 105, 275, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Vazgeç").withStyle(s -> s.withColor(0xFFE74C3C)), btn -> {
            this.minecraft.setScreen(parent);
        }).bounds(centerX + 5, 275, 100, 20).build());
    }

    private Component getStatusText(String prefix, boolean val) {
        return Component.literal(prefix).append(Component.literal(val ? "AKTİF" : "PASİF")
                .withStyle(style -> style.withColor(val ? 0xFF2ECC71 : 0xFFE74C3C)));
    }

    private Component getSideText(boolean isRight) {
        return Component.literal("Etiket Konumu: ").append(Component.literal(isRight ? "SAĞ" : "SOL")
                .withStyle(style -> style.withColor(0xFF3498DB)));
    }

    private Component getTabSideText(boolean isRight) {
        return Component.literal("Tab Konumu: ").append(Component.literal(isRight ? "SAĞ" : "SOL")
                .withStyle(style -> style.withColor(0xFF3498DB)));
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
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        int centerX = this.width / 2;

        graphics.centeredText(this.font, this.title, centerX, 15, 0xFFFFCC00);

        graphics.centeredText(this.font, Component.literal(Config.TAG.getDescription()), centerX, 43, 0xFFAAAAAA);
        graphics.centeredText(this.font, Component.literal(Config.SIDE.getDescription()), centerX, 83, 0xFFAAAAAA);
        graphics.centeredText(this.font, Component.literal(Config.SHOW_PLACEHOLDER.getDescription()), centerX, 123, 0xFFAAAAAA);
        graphics.centeredText(this.font, Component.literal(Config.TAB_TAG.getDescription()), centerX, 163, 0xFFAAAAAA);
        graphics.centeredText(this.font, Component.literal(Config.TAB_SIDE.getDescription()), centerX, 203, 0xFFAAAAAA);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(event);
    }
}