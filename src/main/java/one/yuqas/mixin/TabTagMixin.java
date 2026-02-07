package one.yuqas.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import one.yuqas.utils.APIUtils;
import one.yuqas.utils.TierConfig;
import one.yuqas.utils.enums.Config;
import one.yuqas.utils.enums.TierType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerListEntry.class)
public class TabTagMixin {

    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text injectTabTier(Text original) {
        PlayerListEntry self = (PlayerListEntry) (Object) this;

        // Orijinal null olsa bile profile.name() al
        Text baseName = original != null ? original : Text.literal(self.getProfile().name());

        if (!TierConfig.getBoolean(Config.TAB_TAG)) return baseName;

        Text tierText = APIUtils.getFormattedTier(TierType.BEST, self.getProfile().name());
        if (tierText == null || tierText.getString().isEmpty() || tierText.getString().contains("...")) {
            return baseName;
        }

        boolean isRightSide = TierConfig.getBoolean(Config.SIDE);
        MutableText result = Text.empty();
        if (isRightSide) {
            return result.append(baseName)
                    .append(Text.literal(" §7| ").formatted(net.minecraft.util.Formatting.GRAY))
                    .append(tierText);
        } else {
            return result.append(tierText)
                    .append(Text.literal(" §7| ").formatted(net.minecraft.util.Formatting.GRAY))
                    .append(baseName);
        }
    }
}