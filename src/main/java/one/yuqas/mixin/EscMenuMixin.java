package one.yuqas.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import one.yuqas.mixin.accessor.ClickableWidgetAccessor;
import one.yuqas.utils.ui.TierConfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class EscMenuMixin extends Screen {

    protected EscMenuMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addButton(CallbackInfo ci) {
        ButtonWidget settingsButton = null;
        for (var child : this.children()) {
            if (child instanceof ButtonWidget b) {
                if (b.getMessage().getString().contains("Options") ||
                        b.getMessage().getString().contains("Ayarlar")) {
                    settingsButton = b;
                    break;
                }
            }
        }
        if (settingsButton == null) return;

        ClickableWidgetAccessor acc = (ClickableWidgetAccessor) settingsButton;

        int buttonSize = 20;
        int x = acc.getX() - buttonSize - 5;
        int y = acc.getY() + (acc.getHeight() - buttonSize) / 2;

        Text buttonText = Text.literal("\uE991").formatted(Formatting.RED);

        ButtonWidget button = ButtonWidget.builder(buttonText, b ->
                        MinecraftClient.getInstance().setScreen(new TierConfigScreen(this)))
                .dimensions(x, y, buttonSize, buttonSize)
                .narrationSupplier(supplier -> Text.literal("Settings"))
                .build();

        this.addDrawableChild(button);
    }
}