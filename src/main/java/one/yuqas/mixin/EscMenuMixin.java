package one.yuqas.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import one.yuqas.utils.ui.TierConfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class EscMenuMixin extends Screen {

    protected EscMenuMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addButton(CallbackInfo ci) {
        Button settingsButton = null;

        for (var renderable : this.children()) {
            if (renderable instanceof Button b) {
                String label = b.getMessage().getString();
                if (label.contains("Options") || label.contains("Ayarlar")) {
                    settingsButton = b;
                    break;
                }
            }
        }

        if (settingsButton == null) return;

        int buttonSize = 20;
        int x = settingsButton.getX() - buttonSize - 5;
        int y = settingsButton.getY() + (settingsButton.getHeight() - buttonSize) / 2;

        Component buttonText = Component.literal("\uE991").withStyle(ChatFormatting.RED);

        Button customButton = Button.builder(buttonText, b ->
                        Minecraft.getInstance().setScreen(new TierConfigScreen(this)))
                .bounds(x, y, buttonSize, buttonSize)
                .build();

        this.addRenderableWidget(customButton);
    }
}