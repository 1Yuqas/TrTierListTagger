package one.yuqas.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import one.yuqas.utils.APIUtils;
import one.yuqas.utils.TierConfigUtil;
import one.yuqas.utils.enums.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text injectTier(Text original) {
        if (!TierConfigUtil.getBoolean(Config.TAG)) return original;
        PlayerEntity self = (PlayerEntity) (Object) this;
        Text tierText = APIUtils.getFormattedTier(TierConfigUtil.getTierType(), self.getName().getString());

        if (tierText == null || tierText.getString().isEmpty()) {
            return original;
        }

        boolean isRightSide = TierConfigUtil.getBoolean(Config.SIDE);
        MutableText result = Text.empty();
        if (isRightSide) {
            return result.append(original)
                    .append(Text.literal(" §7| ").formatted(net.minecraft.util.Formatting.GRAY))
                    .append(tierText);
        } else {
            return result.append(tierText)
                    .append(Text.literal(" §7| ").formatted(net.minecraft.util.Formatting.GRAY))
                    .append(original);
        }
    }
}