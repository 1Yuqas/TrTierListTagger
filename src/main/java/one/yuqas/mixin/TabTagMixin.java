package one.yuqas.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import one.yuqas.utils.APIUtils;
import one.yuqas.utils.TierConfigUtil;
import one.yuqas.utils.enums.Config;
import one.yuqas.utils.enums.TierType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.regex.Pattern;

@Mixin(PlayerInfo.class)
public class TabTagMixin {

    private Component customTier = null;

    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("^[─━═█]+$");
    private static final Pattern PING_PATTERN = Pattern.compile("\\d+ms");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9_]");

    @ModifyReturnValue(method = "getTabListDisplayName", at = @At("RETURN"))
    private Component injectTab(Component original) {
        PlayerInfo self = (PlayerInfo) (Object) this;

        Component baseName = original != null ? original : getFallbackName(self);

        try {
            if (!TierConfigUtil.getBoolean(Config.TAB_TAG)) return baseName;

            String playerName = getRealPlayerName(self, original);
            if (playerName == null) return baseName;

            Component tierText = customTier != null ? customTier :
                    APIUtils.getFormattedTier(TierConfigUtil.getTierType(), playerName);

            if (tierText == null || tierText.getString().isEmpty()) return baseName;

            return buildFinalText(baseName, tierText);

        } catch (Exception e) {
            return baseName;
        }
    }

    private Component getFallbackName(PlayerInfo entry) {
        if (entry.getTeam() != null && entry.getProfile() != null) {
            return entry.getTeam().getFormattedName(Component.literal(entry.getProfile().name()));
        }
        if (entry.getProfile() != null && !entry.getProfile().name().isEmpty()) {
            return Component.literal(entry.getProfile().name());
        }
        return Component.literal("UNKNOWN");
    }

    private String getRealPlayerName(PlayerInfo entry, Component displayName) {
        if (entry.getProfile() != null && !entry.getProfile().name().isEmpty()) {
            return entry.getProfile().name();
        }

        if (displayName == null) return null;

        String display = displayName.getString();

        if (SEPARATOR_PATTERN.matcher(display).matches()) return null;

        if (!display.matches(".*[a-zA-Z_]{2,}.*")) return null;

        String cleaned = PING_PATTERN.matcher(display).replaceAll("");
        cleaned = NON_ALPHANUMERIC.matcher(cleaned).replaceAll(" ").trim();

        String[] parts = cleaned.split("\\s+");
        String bestName = "";
        for (String part : parts) {
            if (part.length() > bestName.length() && part.length() >= 2) {
                bestName = part;
            }
        }

        return bestName.isEmpty() ? null : bestName;
    }

    private Component buildFinalText(Component baseName, Component tierText) {
        MutableComponent result = Component.empty();
        Component separator = Component.literal(" | ").withStyle(ChatFormatting.GRAY);

        if (TierConfigUtil.getBoolean(Config.TAB_SIDE)) {
            return result.append(baseName).append(separator).append(tierText);
        } else {
            return result.append(tierText).append(separator).append(baseName);
        }
    }
}