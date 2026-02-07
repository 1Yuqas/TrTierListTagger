package one.yuqas.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
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
        int buttonWidth = 100;
        int buttonHeight = 20;

        int x = (this.width - buttonWidth) / 2;
        int y = this.height / 2 - buttonHeight;

        Text buttonText = Text.empty()
                .append(Text.literal("TR").styled(style -> style.withColor(0xFF5555)))
                .append(Text.literal("TierList").styled(style -> style.withColor(0xFFFFFF)));

        this.addDrawableChild(
                ButtonWidget.builder(buttonText, button ->
                        MinecraftClient.getInstance().setScreen(new TierConfigScreen(this))
                ).dimensions(x, y, buttonWidth, buttonHeight).build()
        );
    }
}