package one.yuqas.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
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

    @Inject(
            method = "createPauseMenu",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void addTierListButton(CallbackInfo ci, @Local LinearLayout iconButtonRow) {
        if (iconButtonRow == null) return;

        int buttonSize = 20;

        Component buttonText = Component.literal("\uE991")
                .withStyle(ChatFormatting.RED)
                .withoutShadow();

        Button customButton = Button.builder(buttonText, _ ->
                        Minecraft.getInstance().gui.setScreen(new TierConfigScreen(this)))
                .bounds(0, 0, buttonSize, buttonSize)
                .build();

        iconButtonRow.addChild(customButton);
    }
}