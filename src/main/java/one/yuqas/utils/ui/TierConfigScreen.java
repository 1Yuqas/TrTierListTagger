package one.yuqas.utils.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import one.yuqas.utils.TierConfigUtil;
import one.yuqas.utils.enums.Config;
import one.yuqas.utils.enums.TierType;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class TierConfigScreen extends Screen {
    private final Screen parent;
    private boolean isEnabled;
    private boolean isRightSide;
    private TierType selectedTier;
    private int tierIndex;
    private boolean isTabEnabled;
    private boolean isTabRightSide;

    private static final TierType[] SELECTABLE_TIERS = TierType.values();

    public TierConfigScreen(Screen parent) {
        super(Text.literal("TRTierList Ayarları"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        isEnabled = TierConfigUtil.getBoolean(Config.TAG);
        this.addDrawableChild(ButtonWidget.builder(
                getStatusText("§eEtiket Görünümü: ", isEnabled),
                btn -> {
                    isEnabled = !isEnabled;
                    btn.setMessage(getStatusText("§eEtiket Görünümü: ", isEnabled));
                }
        ).dimensions(centerX - 100, 45, 200, 20).build());

        isRightSide = TierConfigUtil.getBoolean(Config.SIDE);
        this.addDrawableChild(ButtonWidget.builder(
                getSideText(isRightSide),
                btn -> {
                    isRightSide = !isRightSide;
                    btn.setMessage(getSideText(isRightSide));
                }
        ).dimensions(centerX - 100, 80, 200, 20).build());

        selectedTier = TierConfigUtil.getTierType();
        tierIndex = findTierIndex(selectedTier);
        this.addDrawableChild(ButtonWidget.builder(
                getTierTypeText(),
                btn -> {
                    cycleTier();
                    btn.setMessage(getTierTypeText());
                }
        ).dimensions(centerX - 100, 115, 200, 20).build());

        isTabEnabled = TierConfigUtil.getBoolean(Config.TAB_TAG);
        this.addDrawableChild(ButtonWidget.builder(
                getStatusText("§eTab Menüsünde Etiket: ", isTabEnabled),
                btn -> {
                    isTabEnabled = !isTabEnabled;
                    btn.setMessage(getStatusText("§eTab Menüsünde Etiket: ", isTabEnabled));
                }
        ).dimensions(centerX - 100, 150, 200, 20).build());

        isTabRightSide = TierConfigUtil.getBoolean(Config.TAB_SIDE);
        this.addDrawableChild(ButtonWidget.builder(
                getTabSideText(isTabRightSide),
                btn -> {
                    isTabRightSide = !isTabRightSide;
                    btn.setMessage(getTabSideText(isTabRightSide));
                }
        ).dimensions(centerX - 100, 185, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Oyuncu Ara").styled(s -> s.withColor(0xFFCC00)),
                btn -> this.client.setScreen(new PlayerSearchScreen(this))
        ).dimensions(centerX - 100, 220, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Kaydet").styled(s -> s.withColor(0x2ECC71)),
                btn -> {
                    save();
                    this.client.setScreen(parent);
                }
        ).dimensions(centerX - 105, 255, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Vazgeç").styled(s -> s.withColor(0xE74C3C)),
                btn -> this.client.setScreen(parent)
        ).dimensions(centerX + 5, 255, 100, 20).build());
    }

    private int findTierIndex(TierType type) {
        for (int i = 0; i < SELECTABLE_TIERS.length; i++) {
            if (SELECTABLE_TIERS[i] == type) return i;
        }
        return 0;
    }

    private void cycleTier() {
        tierIndex = (tierIndex + 1) % SELECTABLE_TIERS.length;
        selectedTier = SELECTABLE_TIERS[tierIndex];
    }

    private Text getTierTypeText() {
        String name = selectedTier.name();
        int color = selectedTier.getHtColor();

        return Text.empty()
                .append(Text.literal("Tier: ").styled(s -> s.withColor(0xFFFFFF)))
                .append(Text.literal(selectedTier.getIcon() + " "))
                .append(Text.literal(name).styled(s -> s.withColor(color).withBold(true)));
    }

    private Text getStatusText(String prefix, boolean val) {
        return Text.empty()
                .append(Text.literal(prefix))
                .append(Text.literal(val ? "AKTİF" : "PASİF")
                        .styled(style -> style.withColor(val ? 0x2ECC71 : 0xE74C3C)));
    }

    private Text getSideText(boolean isRight) {
        return Text.empty()
                .append(Text.literal("Etiket Konumu: "))
                .append(Text.literal(isRight ? "SAĞ" : "SOL")
                        .styled(style -> style.withColor(0x3498DB)));
    }

    private Text getTabSideText(boolean isRight) {
        return Text.empty()
                .append(Text.literal("Tab Konumu: "))
                .append(Text.literal(isRight ? "SAĞ" : "SOL")
                        .styled(style -> style.withColor(0x3498DB)));
    }

    private void save() {
        TierConfigUtil.set(Config.TAG, isEnabled);
        TierConfigUtil.set(Config.SIDE, isRightSide);
        TierConfigUtil.set(Config.TIER_TYPE, selectedTier.name());
        TierConfigUtil.set(Config.TAB_TAG, isTabEnabled);
        TierConfigUtil.set(Config.TAB_SIDE, isTabRightSide);
        TierConfigUtil.save();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 10, new Color(0xFFCC00).getRGB());

        int descY = 35;
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(Config.TAG.getDescription()), centerX, descY, new Color(0x999999).getRGB());
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(Config.SIDE.getDescription()), centerX, descY + 35, new Color(0x999999).getRGB());
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(Config.TIER_TYPE.getDescription()), centerX, descY + 70, new Color(0x999999).getRGB());
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(Config.TAB_TAG.getDescription()), centerX, descY + 105, new Color(0x999999).getRGB());
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(Config.TAB_SIDE.getDescription()), centerX, descY + 140, new Color(0x999999).getRGB());
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