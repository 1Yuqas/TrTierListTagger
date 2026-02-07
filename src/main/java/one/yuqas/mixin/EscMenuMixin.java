package one.yuqas.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
        int buttonSize = 20; // kare buton

        int x = (this.width - buttonSize) / 2;
        int y = this.height / 2 - buttonSize;

        Text buttonText = Text.literal("\uE991").formatted(Formatting.RED);

        ButtonWidget button = ButtonWidget.builder(buttonText, b ->
                        MinecraftClient.getInstance().setScreen(new TierConfigScreen(this)))
                .dimensions(x, y, buttonSize, buttonSize)
                .build();

        this.addDrawableChild(button);
    }
}