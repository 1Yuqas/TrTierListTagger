package one.yuqas.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import one.yuqas.utils.APIUtils;
import one.yuqas.utils.TierConfig;
import one.yuqas.utils.enums.Config;
import one.yuqas.utils.enums.TierType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {

    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Component injectTier(Component original) {
        if (!TierConfig.getBoolean(Config.TAG)) return original;

        Player self = (Player) (Object) this;

        Component tierText = APIUtils.getFormattedTier(TierType.BEST, self.getName().getString());

        if (tierText == null || tierText.getString().isEmpty() || tierText.getString().contains("...")) {
            return original;
        }

        boolean isRightSide = TierConfig.getBoolean(Config.SIDE);
        MutableComponent result = Component.empty(); // Text.empty() -> Component.empty()

        if (isRightSide) {
            return result.append(original)
                    .append(Component.literal(" §7| ").withStyle(ChatFormatting.GRAY))
                    .append(tierText);
        } else {
            return result.append(tierText)
                    .append(Component.literal(" §7| ").withStyle(ChatFormatting.GRAY))
                    .append(original);
        }
    }
}