package one.yuqas.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import one.yuqas.utils.APIUtils;
import one.yuqas.utils.TierConfig;
import one.yuqas.utils.enums.Config;
import one.yuqas.utils.enums.TierType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerInfo.class)
public class TabTagMixin {

    private Component customTier = null;

    @ModifyReturnValue(method = "getTabListDisplayName", at = @At("RETURN")) // getDisplayName -> getTabListDisplayName
    private Component injectTab(Component original) {
        PlayerInfo self = (PlayerInfo) (Object) this;

        // Orijinal isim yoksa (null dönebiliyor), takım rengine veya profile göre isim oluştur
        Component baseName = original;
        if (baseName == null) {
            if (self.getTeam() != null) { // getScoreboardTeam -> getTeam
                baseName = self.getTeam().getFormattedName(Component.literal(self.getProfile().name())); // decorateName -> getFormattedName
            } else {
                baseName = Component.literal(self.getProfile().name());
            }
        }

        try {
            if (!TierConfig.getBoolean(Config.TAB_TAG)) return baseName;

            String playerName = getPlayerName();
            if (playerName == null || playerName.isEmpty()) return baseName;

            Component tierText = customTier;
            if (tierText == null) {
                tierText = APIUtils.getFormattedTier(TierType.BEST, playerName);
            }

            // Veri yoksa veya yükleniyorsa orijinal ismi döndür
            if (tierText == null || tierText.getString().isEmpty() || tierText.getString().contains("...")) {
                return baseName;
            }

            boolean isRightSide = TierConfig.getBoolean(Config.TAB_SIDE);
            MutableComponent finalEntry = Component.empty();
            Component separator = Component.literal(" | ").withStyle(ChatFormatting.GRAY);

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
            PlayerInfo self = (PlayerInfo) (Object) this;
            if (self.getProfile() == null) return null;
            return self.getProfile().name(); // GameProfile bir record ise name() doğru
        } catch (Exception e) {
            return null;
        }
    }
}