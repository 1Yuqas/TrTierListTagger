package one.yuqas.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import one.yuqas.utils.APIUtils;
import one.yuqas.utils.TierConfigUtil;
import one.yuqas.utils.enums.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.regex.Pattern;

@Mixin(PlayerListEntry.class)
public class TabTagMixin {

    private Text customTier = null;

    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("^[─━═█]+$");
    private static final Pattern PING_PATTERN = Pattern.compile("\\d+ms");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9_]");

    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text injectTab(Text original) {
        PlayerListEntry self = (PlayerListEntry) (Object) this;

        Text baseName = original != null ? original : getFallbackName(self);

        try {
            if (!TierConfigUtil.getBoolean(Config.TAB_TAG)) return baseName;

            String playerName = getRealPlayerName(self, original);
            if (playerName == null) return baseName;

            Text tierText = customTier != null ? customTier :
                    APIUtils.getFormattedTierCompact(TierConfigUtil.getTierType(), playerName);

            if (tierText == null || tierText.getString().isEmpty()) return baseName;

            return buildFinalText(baseName, tierText);

        } catch (Exception e) {
            return baseName;
        }
    }

    private Text getFallbackName(PlayerListEntry entry) {
        if (entry.getScoreboardTeam() != null && entry.getProfile() != null) {
            return entry.getScoreboardTeam().decorateName(Text.literal(entry.getProfile().name()));
        }
        if (entry.getProfile() != null && !entry.getProfile().name().isEmpty()) {
            return Text.literal(entry.getProfile().name());
        }
        return Text.literal("UNKNOWN");
    }

    private String getRealPlayerName(PlayerListEntry entry, Text displayName) {
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

    private Text buildFinalText(Text baseName, Text tierText) {
        MutableText result = Text.empty();
        Text separator = Text.literal(" | ").formatted(Formatting.GRAY);

        if (TierConfigUtil.getBoolean(Config.TAB_SIDE)) {
            return result.append(baseName).append(separator).append(tierText);
        } else {
            return result.append(tierText).append(separator).append(baseName);
        }
    }
}