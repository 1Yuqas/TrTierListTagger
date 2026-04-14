package one.yuqas.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import one.yuqas.utils.APIUtils;
import one.yuqas.utils.TierConfigUtil;
import one.yuqas.utils.enums.Config;
import one.yuqas.utils.enums.TierType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerListEntry.class)
public class TabTagMixin {

    private Text customTier = null;

    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text injectTab(Text original) {
        PlayerListEntry self = (PlayerListEntry) (Object) this;

        Text baseName = original;
        if (baseName == null) {
            if (self.getScoreboardTeam() != null) {
                baseName = self.getScoreboardTeam().decorateName(Text.literal(self.getProfile().name()));
            } else {
                baseName = Text.literal(self.getProfile().name());
            }
        }

        try {
            if (!TierConfigUtil.getBoolean(Config.TAB_TAG)) return baseName;

            String playerName = getPlayerName();
            if (playerName == null || playerName.isEmpty()) return baseName;

            Text tierText = customTier;
            if (tierText == null) {
                tierText = APIUtils.getFormattedTier(TierType.BEST, playerName);
            }

            if (tierText == null || tierText.getString().isEmpty() || tierText.getString().contains("...")) {
                return baseName;
            }

            boolean isRightSide = TierConfigUtil.getBoolean(Config.TAB_SIDE);
            MutableText finalEntry = Text.empty();
            Text separator = Text.literal(" | ").formatted(Formatting.GRAY);

            if (isRightSide) {
                return finalEntry.append(baseName).append(separator).append(tierText);
            } else {
                return finalEntry.append(tierText).append(separator).append(baseName);
            }

        } catch (Exception e) {
            return baseName;
        }
    }

    private String getPlayerName() {
        try {
            PlayerListEntry self = (PlayerListEntry) (Object) this;
            if (self.getProfile() == null) return null;
            return self.getProfile().name();
        } catch (Exception e) {
            return null;
        }
    }
}